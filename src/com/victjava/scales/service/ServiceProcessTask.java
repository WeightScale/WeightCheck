package com.victjava.scales.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.database.Cursor;
import android.os.IBinder;
import android.os.Message;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.victjava.scales.provider.ErrorTable;
import com.victjava.scales.provider.TaskTable;
import com.victjava.scales.*;
import com.victjava.scales.TaskCommand.*;

import java.util.ArrayList;
import java.util.Map;

/** Сервис обработки задач.
 * Задачи отправки данных.
 * @author Kostya
 */
public class ServiceProcessTask extends Service {
    /** Таблица задач */
    private TaskTable taskTable;
    private Internet internet;
    private static BroadcastReceiver broadcastReceiver;
    private NotificationManager notificationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        taskProcess();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        /**Экземпляр таблици задач*/
        taskTable = new TaskTable(getApplicationContext());
        /**Экземпляр интернет класса*/
        internet = new Internet(this);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
    }

    /** Процесс выполнения задач */
    private void taskProcess() {
        /**Экземпляр команд задач*/
        TaskCommand taskCommand = new TaskCommand(getApplicationContext(), msgHandler);
        /**Сообщение на обработчик запущен процесс задач*/
        msgHandler.obtainMessage(TaskCommand.HANDLER_TASK_START, TaskType.values().length, 0).sendToTarget();
        for (TaskType taskType : TaskType.values()) {
            Cursor cursor = taskTable.getTypeEntry(taskType);
            ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
            Map<String, ContentValues> map = mQueryMap.getRows();
            cursor.close();
            try {
                taskCommand.execute(taskType, map);
            } catch (Exception e) {
                msgHandler.sendEmptyMessage(TaskCommand.HANDLER_FINISH_THREAD);
            }
        }
        /** Останавливаем сервис */
        stopSelf();
    }

    /** Обработчик сообщений */
    public final HandlerTaskNotification msgHandler = new HandlerTaskNotification() {

        /** Количество запущеных процессов */
        int numThread;

        /** Сообщение на удаление задачи
         * @param what Тип сообщения
         * @param arg1 Номер записи
         */
        @Override
        public void handleRemoveEntry(int what, int arg1) {
            switch (what) {
                case TaskCommand.HANDLER_NOTIFY_CHECK_UNSEND:
                    taskTable.removeEntryIfErrorOver(arg1);
                break;
                default:
                    taskTable.removeEntry(arg1);
            }
        }

        @Override
        public void handleNotificationError(int what, int arg1, MessageNotify msg) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(ServiceProcessTask.this);
            builder.setSmallIcon(R.drawable.ic_stat_information);
            builder.setTicker("Ошибка").setContentText(msg.getMessage());
            notificationManager.notify(what, generateNotification(new Intent(), builder, what));
            handleError(arg1, msg.getMessage());
        }

        @Override
        public void handleError(int what, String msg) {
            new ErrorTable(getApplicationContext()).insertNewEntry(String.valueOf(what), msg);
        }

        @Override
        public void handleMessage(Message msg) {
            Intent intent = new Intent();
            intent.setAction("notifyChecks");
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ServiceProcessTask.this);
            switch (msg.what) {
                case TaskCommand.HANDLER_TASK_START:
                    numThread += msg.arg1;
                    return;
                case TaskCommand.HANDLER_FINISH_THREAD:
                    if (--numThread <= 0)
                        sendBroadcast(new Intent(Internet.INTERNET_DISCONNECT));
                    return;
                case TaskCommand.HANDLER_NOTIFY_SHEET: //отправлено на диск sheet
                    mBuilder.setSmallIcon(R.drawable.ic_stat_drive)
                            .setTicker(getString(R.string.Check_N) + ' ' + msg.arg1 + ' ' + getString(R.string.sent_to_the_server))
                            .setContentText(getString(R.string.Checks_send_count) + ' ' + ((ArrayList) msg.obj).size());
                    break;
                case TaskCommand.HANDLER_NOTIFY_PREF: //отправлено на диск sheet
                    mBuilder.setSmallIcon(R.drawable.ic_stat_drive)
                            .setTicker("Настройки отправлены")
                            .setContentText("Отправлено настроек кол-во: " + ((ArrayList) msg.obj).size());
                    notificationManager.notify(msg.what, generateNotification(new Intent(), mBuilder, msg.what));
                    handleRemoveEntry(TaskCommand.REMOVE_TASK_ENTRY, msg.arg2);
                    return;
                case TaskCommand.HANDLER_NOTIFY_MAIL: //отправлено на почту
                    mBuilder.setSmallIcon(R.drawable.ic_stat_mail)
                            .setTicker(getString(R.string.Check_N) + ' ' + msg.arg1 + ' ' + getString(R.string.Send_to_mail))
                            .setContentText(getString(R.string.Checks_send_count) + ' ' + ((ArrayList) msg.obj).size());
                    break;
                case TaskCommand.HANDLER_NOTIFY_CHECK_UNSEND: //не отправлен чек
                    mBuilder.setSmallIcon(R.drawable.ic_stat_information)
                            .setTicker(getString(R.string.Check_N) + ' ' + msg.arg1 + ' ' + getString(R.string.Warning_Error))
                            .setContentText(getString(R.string.Checks_not_send_count) + ' ' + ((ArrayList) msg.obj).size());
                    break;
                case TaskCommand.HANDLER_NOTIFY_MESSAGE: //отправлено сообщение на телефон
                    mBuilder.setSmallIcon(R.drawable.ic_stat_messages)
                            .setTicker(getString(R.string.Check_N) + ' ' + msg.arg1 + ' ' + getString(R.string.Message_sent))
                            .setContentText(getString(R.string.Checks_send_count) + ' ' + ((ArrayList) msg.obj).size());
                    break;
                case TaskCommand.HANDLER_NOTIFY_HTTP: //отправлено на http
                    mBuilder.setSmallIcon(R.drawable.ic_stat_cloud_comment)
                            .setTicker(getString(R.string.Check_N) + ' ' + msg.arg1 + ' ' + getString(R.string.sent_to_the_server))
                            .setContentText(getString(R.string.Checks_send_count) + ' ' + ((ArrayList) msg.obj).size());
                    //new CheckDBAdapter(getApplicationContext()).updateEntry(msg.arg1, CheckDBAdapter.KEY_CHECK_ON_SERVER, 1);
                    break;
                default:
                    return;
            }
            handleRemoveEntry(msg.what, msg.arg2);
            intent.setClass(ServiceProcessTask.this, ActivityListChecks.class).putParcelableArrayListExtra("listCheckNotify", (ArrayList) msg.obj);
            notificationManager.notify(msg.what, generateNotification(intent, mBuilder, msg.what));
        }

    };

    //==================================================================================================================

    private Notification generateNotification(Intent intent, NotificationCompat.Builder builder, int id) {

        builder.setContentTitle(getString(R.string.app_name))
                /*.setVibrate(new long[]{50, 100, 150})*/
                .setAutoCancel(true);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ActivityScales.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        return builder.build();
    }


}
