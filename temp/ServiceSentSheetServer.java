package com.victjava.scales.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import com.google.gdata.util.ServiceException;
import com.victjava.scales.provider.*;
import com.victjava.scales.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 14.10.13
 * Time: 11:58
 * To change this template use File | Settings | File Templates.
 */
public class ServiceSentSheetServer extends Service {
    private final ThreadIsCheck threadIsCheck = new ThreadIsCheck();
    private NotificationManager notificationManager;
    private Internet internet;
    private static BroadcastReceiver broadcastReceiver;
    //private static final String INTERNET_CONNECT = "internet_connect";
    //private static final String INTERNET_DISCONNECT = "internet_disconnect";
    private GoogleSpreadsheets googleSpreadsheets;

    private final int ID_NOTIFY_SERVICE = 1;
    private final int ID_NOTIFY_CLOUD = 2;
    private final int ID_NOTIFY_MAIL = 3;
    private final int ID_NOTIFY_MESSAGE = 4;

    //==================================================================================================================
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //==================================================================================================================
    public class ThreadIsCheck extends AsyncTask<Void, Integer, Void> {
        private boolean closed = true;
        protected final Date dateExecute = new Date();

        public void executeStart(Void... params) {
            if (Build.VERSION_CODES.HONEYCOMB <= Build.VERSION.SDK_INT) {
                executePostHoneycomb(params);
            } else {
                super.execute(params);
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private void executePostHoneycomb(Void... params) {
            super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
            try {
                googleSpreadsheets = new GoogleSpreadsheets(Scales.username, Scales.password, Scales.spreadsheet, Main.versionName);
            } catch (RuntimeException ignored) {
                new ErrorDBAdapter(getApplicationContext()).insertNewEntry("501", ignored.getMessage());
                stopSelf();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int count = 0/*, time_wait = 0*/;
            while (!isCancelled()) {

                if (count++ > 3) {
                    new ErrorDBAdapter(getApplicationContext()).insertNewEntry("502", getString(R.string.TEXT_MESSAGE10));
                    stopSelf();
                }
                if (!getConnection(1000, 10)) {
                    continue;
                }

                try {
                    googleSpreadsheets.login();
                    googleSpreadsheets.getSheetEntry(Scales.spreadsheet);
                    googleSpreadsheets.UpdateListWorksheets();
                    if (!isTaskReady()) {
                        sendBroadcast(new Intent(Internet.INTERNET_DISCONNECT));
                    }
                } catch (IOException e) {
                    new ErrorDBAdapter(getApplicationContext()).insertNewEntry("503", e.getMessage());
                    continue;
                } catch (ServiceException e) {
                    new ErrorDBAdapter(getApplicationContext()).insertNewEntry("504", e.getMessage());
                    continue;
                } catch (Exception e) {
                    new ErrorDBAdapter(getApplicationContext()).insertNewEntry("505", e.getMessage());
                    continue;
                }
                break;
            }
            while (!isCancelled()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                if (!isTaskReady()) {
                    if (dayDiff(new Date(), dateExecute) > 1)                                                             //Сколько живет сервис в днях
                    {
                        break;//stopForeground(true);//stopSelf();
                    }
                    continue;
                }
                count = 0;                                                                                              //Колличество попыток передать данные
                while (!isCancelled()) {

                    if (count++ > 3)                                                                                     //Колличество больше прекращяем попытки передачи
                    {
                        break;
                    }
                    if (!getConnection(1000, 10)) {
                        continue;
                    }

                    processingTasks();                                                                                  //выполняем задачи

                    /*if(!flagNewVersion)
                        flagNewVersion = isNewVersion();//todo эта стока для проверки новой версии программы user должен dev market*/

                    oldCheckSetReady();                                                                                 //не закрытые чеки закрыть по условию даты

                    new CheckDBAdapter(getApplicationContext())
                            .invisibleCheckIsReady(Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CHECK_DELETE, Main.default_day_delete_check));   //Скрываем чеки закрытые через n дней

                    new CheckDBAdapter(getApplicationContext()).deleteCheckIsServer();                               //Удаляем чеки отправленые на сервер через n дней

                    sendErrorsToDisk();                                                                                 //отправляем ошибки на сервер и удаляем

                    if (isTaskReady()) {
                        continue;
                    }

                    break;
                }
                sendBroadcast(new Intent(Internet.INTERNET_DISCONNECT));
            }
            closed = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopSelf();
            closed = true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            notificationManager.notify(values[4], generateNotification(values[1], getString(values[2]), getString(R.string.Check_N) + ' ' + String.valueOf(values[0]) + ' ' + getString(values[3])));
            //notificationManager.notify(0,generateNotification(R.drawable.ic_stat_cloud_comment,getString(R.string.Check_send), getString(R.string.Check_N)+ ' ' + String.valueOf(values[0]) + ' ' +getString(R.string.sent_to_the_server)));
        }

        //==============================================================================================================
        private boolean sendPrefToDisk(int id) {
            Cursor cursor = new PreferencesTable(getApplicationContext()).getEntryItem(id);
            if (cursor == null) {
                return false;
            }
            boolean flag = false;
            if (cursor.moveToFirst()) {
                try {
                    googleSpreadsheets.addRow(cursor, PreferencesTable.TABLE);
                    new PreferencesTable(getApplicationContext()).removeEntry(id);
                    flag = true;
                } catch (Exception e) {
                    new ErrorDBAdapter(getApplicationContext()).insertNewEntry("400", e.getMessage());
                }
            }
            cursor.close();
            return flag;
        }

        //==============================================================================================================
        private boolean sendCheckToDisk(int id) {
            Cursor cursor = new CheckDBAdapter(getApplicationContext()).getEntryItem(id);
            if (cursor == null) {
                return false;
            }
            boolean flag = false;
            if (cursor.moveToFirst()) {
                try {
                    googleSpreadsheets.addRow(cursor, CheckDBAdapter.TABLE_CHECKS);
                    new CheckDBAdapter(getApplicationContext()).updateEntry(id, CheckDBAdapter.KEY_CHECK_ON_SERVER, 1);
                    flag = true;
                } catch (Exception e) {
                    new ErrorDBAdapter(getApplicationContext()).insertNewEntry("401", e.getMessage());
                }
            }
            cursor.close();
            return flag;
        }

        //==============================================================================================================
        private boolean sendErrorsToDisk() {
            Cursor cursor = new ErrorDBAdapter(getApplicationContext()).getAllEntries();
            if (cursor == null) {
                return false;
            }
            boolean flag = false;
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    do {
                        try {
                            int id = cursor.getInt(cursor.getColumnIndex(ErrorDBAdapter.KEY_ID));
                            googleSpreadsheets.addRow(cursor, ErrorDBAdapter.TABLE_ERROR);
                            new ErrorDBAdapter(getApplicationContext()).removeEntry(id);
                        } catch (Exception e) {
                        }
                    } while (cursor.moveToNext());
                    flag = true;
                }
            }
            cursor.close();
            return flag;
        }

        //==============================================================================================================
        private boolean getConnection(int timeout, int countConnect) {
            int count = 0;
            while (!isCancelled()) {
                sendBroadcast(new Intent(Internet.INTERNET_CONNECT));
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ignored) {
                }
                //if(!Internet.flagIsInternet){
                if (!Internet.isOnline()) {
                    if (count++ > countConnect) {
                        break;
                    }
                    continue;
                }
                return true;
            }
            return false;
        }

        //==============================================================================================================
        private void processingTasks() {
            Cursor cursor = new TaskTable(getApplicationContext()).getAllEntries();
            if (cursor == null) {
                return;
            }
            if (cursor.moveToFirst()) {
                if (!cursor.isAfterLast()) {
                    do {
                        TASK_TYPE mimeType = TASK_TYPE.values()[cursor.getInt(cursor.getColumnIndex(TaskTable.KEY_MIME_TYPE))];
                        int taskId = cursor.getInt(cursor.getColumnIndex(TaskTable.KEY_ID));
                        int checkId = cursor.getInt(cursor.getColumnIndex(TaskTable.KEY_DOC));
                        Integer[] value = {checkId, R.drawable.ic_stat_cloud_comment, R.string.Check_send, R.string.sent_to_the_server, ID_NOTIFY_CLOUD};
                        switch (mimeType) {
                            case TYPE_CHECK_SEND_HTTP_POST:
                                if (sendCheckToDisk(checkId)) {
                                    new TaskTable(getApplicationContext()).removeEntry(taskId);
                                    //onProgressUpdate(checkId);//publishProgress(id);
                                } else
                                    continue;
                                break;
                            case TYPE_PREF_SEND_HTTP_POST:
                                if (sendPrefToDisk(checkId)) {
                                    new TaskTable(getApplicationContext()).removeEntry(taskId);
                                    value[2] = R.string.Settings_send;
                                    //onProgressUpdate(checkId);
                                } else
                                    continue;
                                break;
                            case TYPE_CHECK_SEND_MAIL_CONTACT:
                            case TYPE_CHECK_SEND_MAIL_SERVER:
                            case TYPE_CHECK_SEND_SMS_CONTACT:
                            case TYPE_CHECK_SEND_SMS_SERVER:
                                String address = cursor.getString(cursor.getColumnIndex(TaskTable.KEY_DATA1));
                                StringBuilder body = new StringBuilder(getString(R.string.WEIGHT_CHECK_N) + checkId + '\n' + '\n');
                                Cursor check = new CheckDBAdapter(getApplicationContext()).getEntryItem(checkId);
                                if (check == null) {
                                    body.append(getString(R.string.No_data_check)).append(checkId).append(getString(R.string.delete));
                                } else {
                                    if (check.moveToFirst()) {
                                        body.append(getString(R.string.Date)).append('_').append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE))).append("__").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_TIME_CREATE))).append('\n');
                                        body.append(getString(R.string.Contact)).append("__").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_VENDOR))).append('\n');
                                        body.append(getString(R.string.GROSS)).append("___").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_FIRST))).append('\n');
                                        body.append(getString(R.string.TAPE)).append("_____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_SECOND))).append('\n');
                                        body.append(getString(R.string.Netto)).append(":____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_NETTO))).append('\n');
                                        body.append(getString(R.string.Goods)).append("____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_TYPE))).append('\n');
                                        body.append(getString(R.string.Price)).append("_____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_PRICE))).append('\n');
                                        body.append(getString(R.string.Sum)).append(":____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_PRICE_SUM))).append('\n');
                                    } else {
                                        body.append(getString(R.string.No_data_check)).append(checkId).append(getString(R.string.delete));
                                    }
                                    check.close();
                                }
                                switch (mimeType) {
                                    case TYPE_CHECK_SEND_MAIL_CONTACT:
                                    case TYPE_CHECK_SEND_MAIL_SERVER:
                                        MailSend mail = new MailSend(getApplicationContext(), address, getString(R.string.Check_N) + checkId, body.toString());
                                        try {
                                            mail.sendMail();
                                        } catch (MessagingException e) {
                                            continue;
                                        } catch (UnsupportedEncodingException e) {
                                            continue;
                                        }/**/

                                        //if (mail.sendMail()) {
                                        new TaskTable(getApplicationContext()).removeEntry(taskId);
                                        value[1] = R.drawable.ic_stat_mail;
                                        value[2] = R.string.Mail_sent;
                                        value[3] = R.string.Send_to_mail;
                                        value[4] = ID_NOTIFY_MAIL;
                                        //} else
                                        //continue;
                                        break;
                                    case TYPE_CHECK_SEND_SMS_CONTACT:
                                    case TYPE_CHECK_SEND_SMS_SERVER:
                                        try {
                                            new TaskTable(getApplicationContext()).removeEntry(taskId);
                                            value[1] = R.drawable.ic_stat_messages;
                                            value[2] = R.string.Message_sent;
                                            value[3] = R.string.Send_to_phone;
                                            value[4] = ID_NOTIFY_MESSAGE;

                                        } catch (Exception e) {
                                            continue;
                                        }
                                        break;
                                    default:
                                        continue;
                                }
                                break;
                            default:
                                continue;
                        }
                        onProgressUpdate(value);
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }

        //==============================================================================================================
        /*private boolean sendSMS(String phoneNumber, String message) {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message);
            try {
                sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } catch (RuntimeException ignored) {
                return false;
            }
            return true;
        }*/

        //==============================================================================================================
        private void oldCheckSetReady() {
            Cursor cursor = new CheckDBAdapter(getApplicationContext()).getNotReady();
            if (cursor == null) {
                return;
            }
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    do {
                        String date = cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE));
                        try {
                            long day = dayDiff(new Date(), new SimpleDateFormat("dd.MM.yy").parse(date));
                            if (day >= Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CLOSED_CHECK, 5)) {
                                int id = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_ID));
                                new CheckDBAdapter(getApplicationContext()).updateEntry(id, CheckDBAdapter.KEY_IS_READY, 1);
                                //Integer[] value = new Integer[]{id, R.drawable.ic_stat_cloud_comment, R.string.Check_send, R.string.sent_to_the_server};
                                //threadIsCheck.onProgressUpdate(id);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }

        public boolean isClosed() {
            return closed;
        }

        public void setClosed(boolean closed) {
            this.closed = closed;
        }
    }

    //==================================================================================================================
    @Override
    public void onCreate() {
        super.onCreate();
        internet = new Internet(this);
        new CheckDBAdapter(getApplicationContext()).deleteCheckIsServer();                                          //Удаляем чеки отправленые на сервер черз n дня
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {                                                 //контроль состояний сетей
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(Internet.INTERNET_CONNECT)) {
                        internet.connect();
                    } else if (action.equals(Internet.INTERNET_DISCONNECT)) {
                        internet.disconnect();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(Internet.INTERNET_CONNECT);
        filter.addAction(Internet.INTERNET_DISCONNECT);
        registerReceiver(broadcastReceiver, filter);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        threadIsCheck.executeStart();
    }

    //==================================================================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        threadIsCheck.cancel(true);
        while (!threadIsCheck.isClosed()) ;
        internet.disconnect();
        unregisterReceiver(broadcastReceiver);
    }

    //==================================================================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(ID_NOTIFY_SERVICE, generateNotification(R.drawable.ic_stat_truck_notifi, getString(R.string.TEXT_MESSAGE8), getString(R.string.TEXT_MESSAGE9)));
        return START_STICKY;
    }

    //==================================================================================================================
    long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;//86400000
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return day1 - day2;
    }

    //==================================================================================================================
    boolean isTaskReady() {
        return new TaskTable(this).isTaskReady();
    }

    //==================================================================================================================
    private Notification generateNotification(int icon, String title, CharSequence message) {

        //NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, title, System.currentTimeMillis());
        Intent notificationIntent = new Intent(getApplicationContext(), ActivityScales.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getString(R.string.app_name), message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notificationManager.notify(0, notification);
        return notification;
    }

    //==================================================================================================================
    boolean isNewDataToServer() {

        return new CheckDBAdapter(this).getCheckServerIsReady()
                || new CheckDBAdapter(this).getCheckServerOldIsReady()
                || new PreferencesTable(this).getPrefServerIsReady();
    }

}
