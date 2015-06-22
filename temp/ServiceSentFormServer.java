package com.victjava.scales.service;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import com.gc.android.market.api.MarketSession;
import com.gc.android.market.api.model.Market;
import com.victjava.scales.provider.CheckDBAdapter;
import com.victjava.scales.provider.PreferencesTable;
import com.victjava.scales.provider.TaskTable;
import org.apache.http.message.BasicNameValuePair;
import android.provider.Settings.Secure;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 14.10.13
 * Time: 11:58
 * To change this template use File | Settings | File Templates.
 */
public class ServiceSentFormServer extends Service {
    private final ThreadIsCheck threadIsCheck = new ThreadIsCheck();
    private static BroadcastReceiver broadcastReceiver;
    private NotificationManager notificationManager;
    protected String androidId;
    private static boolean flagNewVersion;
    private static final String INTERNET_CONNECT = "internet_connect";
    private static final String INTERNET_DISCONNECT = "internet_disconnect";
    public static final String CLOSED_SCALE = "closed_scale";

    static final String GO_FORM_HTTP = "https://docs.google.com/forms/d/11C5mq1Z-Syuw7ScsMlWgSnr9yB4L_eP-NhxnDdohtrw/formResponse"; // Форма движения

    static final String GO_DATE_HTTP = "entry.1974893725";     // Дата создания
    static final String GO_BT_HTTP = "entry.1465497317";     // Номер весов
    static final String GO_WEIGHT_HTTP = "entry.683315711";      // Вес
    static final String GO_TYPE_HTTP = "entry.138748566";      // Тип
    static final String GO_IS_READY_HTTP = "entry.1691625234";     // Готов
    static final String GO_TIME_HTTP = "entry.1280991625";     //Время

    static final String PREF_FORM_HTTP = "https://docs.google.com/forms/d/1T2Q5pEhtkNc039QrD3CMJZ15d0v-BXmGC0uQw9LxBzg/formResponse"; // Форма настроек

    static final String PREF_DATE_HTTP = "entry.1036338564";     // Дата создания
    static final String PREF_BT_HTTP = "entry.1127481796";     // Номер весов
    static final String PREF_COEFF_A_HTTP = "entry.167414049";      // Коэфициент А
    static final String PREF_COEFF_B_HTTP = "entry.1149110557";     // Коэфициент Б
    static final String PREF_MAX_WEIGHT_HTTP = "entry.2120930895";     // Максимальный вес
    static final String PREF_FILTER_ADC_HTTP = "entry.947786976";      // Фильтер АЦП
    static final String PREF_STEP_SCALE_HTTP = "entry.1522652368";     // Шаг измерения
    static final String PREF_STEP_CAPTURE_HTTP = "entry.1143754554";     // Шаг захвата
    static final String PREF_TIME_OFF_HTTP = "entry.1936919325";     // Время выключения
    static final String PREF_BT_TERMINAL_HTTP = "entry.152097551";      // Номер БТ терминала

    private Internet internet;
    private static final int NUM_TIME_WAIT = 150;

    //==================================================================================================================
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //==================================================================================================================
    public class ThreadIsCheck extends AsyncTask<Void, Integer, Void> {
        protected final Date dateExecute = new Date();
        private boolean closed = true;
        private int time_wait;

        public void executeStart(Void... params) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
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
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopSelf();
            closed = true;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            while (!isCancelled()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                if (!isTaskReady()) {
                    if (dayDiff(new Date(), dateExecute) > 1)                                 //Сколько живет сервис в днях
                    {
                        break;//stopForeground(true);//stopSelf();
                    }
                    continue;
                }
                int count = 0;                                             //Колличество попыток передать данные
                while (!isCancelled()) {

                    if (count++ > 3)                                                         //Колличество больше прекращяем попытки передачи
                    {
                        break;
                    }
                    if (!getConnection(1000, 10)) {
                        continue;
                    }

                    processingTasks();                          //выполняем задачи

                    if (!flagNewVersion) {
                        flagNewVersion = isNewVersion();//todo эта стока для проверки новой версии программы user должен dev market
                    }

                    oldCheckSetReady();                          //не закрытые чеки закрыть по условию даты

                    new CheckDBAdapter(ServiceSentFormServer.this)
                            .invisibleCheckIsReady(Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CHECK_DELETE, Main.default_day_delete_check));  //Скрываем чеки закрытые через n дней
                    /*new CheckDBAdapter(ServiceGetDateServer.this)
                            .invisibleCheckIsReady(getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE)
                                    .getInt(ActivityPreferences.KEY_DAY_CHECK_DELETE, Main.default_day_delete_check));  //Скрываем чеки закрытые через n дней*/

                    new CheckDBAdapter(ServiceSentFormServer.this).deleteCheckIsServer();  //Удаляем чеки отправленые на сервер через n дней

                    if (isTaskReady()) {
                        continue;
                    }

                    break;
                }
                sendBroadcast(new Intent(INTERNET_DISCONNECT));
            }
            closed = true;
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            notificationManager.notify(0, generateNotification(R.drawable.ic_stat_cloud_comment, getString(R.string.Check_sent), getString(R.string.Check) + " № " + values[0] + getString(R.string.sent_to_the_server)));
        }

        //==============================================================================================================
        private boolean getConnection(int timeout, int countConnect) {
            int count = 0;
            while (!isCancelled()) {
                sendBroadcast(new Intent(INTERNET_CONNECT));
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
                        switch (mimeType) {
                            case TYPE_CHECK_SEND_HTTP_POST:
                            case TYPE_CHECK_SEND_SHEET_DISK:
                                if (sendCheckToDisk(checkId)) {
                                    new TaskTable(getApplicationContext()).removeEntry(taskId);
                                }
                                break;
                            case TYPE_PREF_SEND_HTTP_POST:
                                if (sendPrefToDisk(checkId)) {
                                    new TaskTable(getApplicationContext()).removeEntry(taskId);
                                }
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
                                        body.append(getString(R.string.Date)).append("_").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE))).append("__").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_TIME_CREATE))).append('\n');
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
                                        MailSend mail = new MailSend(getApplicationContext(), address, getString(R.string.Check_N) + checkId, body.toString());
                                        try {
                                            mail.sendMail();
                                        } catch (MessagingException e) {
                                            break;
                                        } catch (UnsupportedEncodingException e) {
                                            break;
                                        }
                                        //if (mail.sendMail()) {
                                        new TaskTable(getApplicationContext()).removeEntry(taskId);
                                        //}
                                        break;
                                    case TYPE_CHECK_SEND_SMS_CONTACT:
                                        try {
                                            SMS.sendSMS(address, body.toString());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        new TaskTable(getApplicationContext()).removeEntry(taskId);

                                        break;
                                }
                                break;
                        }
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }

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

                                new CheckDBAdapter(ServiceSentFormServer.this).updateEntry(id, CheckDBAdapter.KEY_IS_READY, 1);
                                threadIsCheck.onProgressUpdate(id);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }

        //==============================================================================================================
        private boolean sendCheckToDisk(int doc) {
            Cursor cursor = new CheckDBAdapter(getApplicationContext()).getEntryItem(doc);
            if (cursor == null) {
                return false;
            }
            boolean flag = false;
            if (cursor.moveToFirst()) {
                //int id = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_ID));

                List<BasicNameValuePair> results = new ArrayList<>();
                results.add(new BasicNameValuePair(GO_DATE_HTTP, cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE))));
                results.add(new BasicNameValuePair(GO_BT_HTTP, cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_NUMBER_BT))));
                results.add(new BasicNameValuePair(GO_WEIGHT_HTTP, String.valueOf(cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_NETTO)))));
                results.add(new BasicNameValuePair(GO_TYPE_HTTP, cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_TYPE))));
                results.add(new BasicNameValuePair(GO_IS_READY_HTTP, String.valueOf(cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_IS_READY)))));
                results.add(new BasicNameValuePair(GO_TIME_HTTP, cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_TIME_CREATE))));

                try {
                    Internet.submitData(GO_FORM_HTTP, results);
                    new CheckDBAdapter(ServiceSentFormServer.this).updateEntry(doc, CheckDBAdapter.KEY_CHECK_ON_SERVER, 1);
                    threadIsCheck.onProgressUpdate(doc);
                    flag = true;
                } catch (Exception e) {
                }
            }
            cursor.close();
            return flag;
        }

        //==============================================================================================================
        private boolean sendPrefToDisk(int id) {
            Cursor cursor = new PreferencesTable(getApplicationContext()).getEntryItem(id);
            if (cursor == null) {
                return false;
            }
            boolean flag = false;
            if (cursor.moveToFirst()) {
                //int id = cursor.getInt(cursor.getColumnIndex(PreferencesDBAdapter.KEY_ID));

                List<BasicNameValuePair> results = new ArrayList<>();
                results.add(new BasicNameValuePair(PREF_DATE_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_DATE_CREATE))));
                results.add(new BasicNameValuePair(PREF_BT_HTTP, String.valueOf(cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_NUMBER_BT)))));
                results.add(new BasicNameValuePair(PREF_COEFF_A_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_COEFFICIENT_A))));
                results.add(new BasicNameValuePair(PREF_COEFF_B_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_COEFFICIENT_B))));
                results.add(new BasicNameValuePair(PREF_MAX_WEIGHT_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_MAX_WEIGHT))));
                results.add(new BasicNameValuePair(PREF_FILTER_ADC_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_FILTER_ADC))));
                results.add(new BasicNameValuePair(PREF_STEP_SCALE_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_STEP_SCALE))));
                results.add(new BasicNameValuePair(PREF_STEP_CAPTURE_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_STEP_CAPTURE))));
                results.add(new BasicNameValuePair(PREF_TIME_OFF_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_TIME_OFF))));
                results.add(new BasicNameValuePair(PREF_BT_TERMINAL_HTTP, cursor.getString(cursor.getColumnIndex(PreferencesTable.KEY_NUMBER_BT_TERMINAL))));

                try {
                    Internet.submitData(PREF_FORM_HTTP, results);
                    new PreferencesTable(getApplicationContext()).removeEntry(id);
                    threadIsCheck.onProgressUpdate(id);
                    flag = true;
                } catch (Exception e) {
                }
            }
            cursor.close();
            return flag;
        }

        //==============================================================================================================
        private boolean sendSMS(String phoneNumber, String message) {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message);
            try {
                sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } catch (RuntimeException ignored) {
                return false;
            }
            return true;
        }

        public boolean isClosed() {
            return closed;
        }

        public void setClosed(boolean closed) {
            this.closed = closed;
        }

        public int getTime_wait() {
            return time_wait;
        }

        public void setTime_wait(int time_wait) {
            this.time_wait = time_wait;
        }
    }

    //==================================================================================================================
    @Override
    public void onCreate() {
        super.onCreate();
        //instance = true;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        internet = new Internet(this);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) { //контроль состояний сетей
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case INTERNET_CONNECT:
                            internet.connect();
                            break;
                        case INTERNET_DISCONNECT:
                            if (!flagNewVersion) {
                                internet.disconnect();
                            }
                            break;
                        case CLOSED_SCALE:
                            threadIsCheck.setTime_wait(NUM_TIME_WAIT);
                            break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(INTERNET_CONNECT);
        filter.addAction(INTERNET_DISCONNECT);
        filter.addAction(CLOSED_SCALE);
        registerReceiver(broadcastReceiver, filter);

        //flag_new_data = isNewDataToServer();
        androidId = getAndroidId(getApplicationContext());
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
        //db.close();
        //instance = false;

    }

    //==================================================================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            startForeground(1, generateNotification(R.drawable.ic_stat_truck_notifi, getString(R.string.TEXT_MESSAGE7), getString(R.string.TEXT_MESSAGE9)));
        }
        return START_STICKY;
    }

    //==================================================================================================================
    long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return day1 - day2;
    }

    //==================================================================================================================
    private Notification generateNotification(int icon, String title, CharSequence message) {
        Notification notification = new Notification(icon, title, System.currentTimeMillis());
        Intent notificationIntent = new Intent(getApplicationContext(), ActivityListChecks.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getString(R.string.app_name), message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notificationManager.notify(0, notification);
        return notification;
    }

    //==================================================================================================================
    boolean isTaskReady() {
        return new TaskTable(this).isTaskReady();
    }

    //==================================================================================================================
    String getAndroidId(Context ctx) {
        String[] params = {Secure.ANDROID_ID};
        Cursor c = ctx.getContentResolver().query(Uri.parse("content://com.google.android.gsf.gservices"), null, null, params, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst() || c.getColumnCount() < 2) {
            return null;
        }
        try {
            return Long.toHexString(Long.parseLong(c.getString(1)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    //==================================================================================================================
    boolean isNewVersion() {
        try {
            MarketSession session = new MarketSession();
            session.login(Scales.username, Scales.password);    // пароль для маркет develop market
            session.getContext().setAndroidId(androidId);
            String query = "pname:" + getPackageName();
            Market.AppsRequest appsRequest = Market.AppsRequest.newBuilder()
                    .setQuery(query)
                    .setStartIndex(0).setEntriesCount(1)
                    .setWithExtendedInfo(false)
                    .build();

            session.append(appsRequest, new MarketSession.Callback<Market.AppsResponse>() {
                @Override
                public void onResult(Market.ResponseContext context, Market.AppsResponse response) {
                    //String v = response.getApp(0).getVersion();
                    // Your code here
                    // response.getApp(0).getCreator() ...
                    // see AppsResponse class definition for more info
                }
            });
            session.flush();
            Market.Request.RequestGroup requestGroup = Market.Request.RequestGroup.newBuilder().setAppsRequest(appsRequest).build();
            Market.Response.ResponseGroup responseGroup = session.execute(requestGroup);
            Market.AppsResponse response = responseGroup.getAppsResponse();
            int newVersion = response.getApp(0).getVersionCode();
            Application application = getApplication();
            if (application == null) {
                return false;
            }
            PackageManager packageManager = getApplication().getPackageManager();
            if (packageManager == null) {
                return false;
            }
            int curVersion = packageManager.getPackageInfo(getPackageName(), 0).versionCode;
            return curVersion < newVersion;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
