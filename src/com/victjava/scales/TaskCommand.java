package com.victjava.scales;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.*;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.konst.module.ScaleModule;
import com.konst.sms_commander.SMS;
import com.victjava.scales.provider.CheckTable;
import com.victjava.scales.provider.PreferencesTable;
import com.victjava.scales.provider.TaskTable;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * Класс задач.
 *
 * @author Kostya
 */
public class TaskCommand extends CheckTable {

    final Context mContext;
    ScaleModule scaleModule;
    final HandlerTaskNotification mHandler;
    boolean cancel = true;
    /** Чек отправлен. */
    static final String MAP_CHECKS_SEND = "send";
    /** Чек не отправлен. */
    static final String MAP_CHECKS_UNSEND = "unsend";
    /** Кодовое слово для дешифрации сообщения. */
    final String codeword = "weightcheck";

    /** Энумератор типа сообщений. */
    public enum NotifyType{
        HANDLER_TASK_START,
        HANDLER_FINISH_THREAD,
        HANDLER_NOTIFY_GENERAL,
        HANDLER_NOTIFY_SHEET,
        HANDLER_NOTIFY_PREF,
        HANDLER_NOTIFY_MAIL,
        HANDLER_NOTIFY_MESSAGE,
        HANDLER_NOTIFY_CHECK_UNSEND,
        HANDLER_NOTIFY_HTTP,
        REMOVE_TASK_ENTRY,
        REMOVE_TASK_ENTRY_ERROR_OVER,
        HANDLER_NOTIFY_ERROR,
        ERROR
    }

    /** Энумератор типа задачи. */
    public enum TaskType {
        /** чек для електронной почты. */
        TYPE_CHECK_SEND_MAIL,
        /** чек для облака. */
        TYPE_CHECK_SEND_HTTP_POST,
        /** настройки для для облака. */
        TYPE_PREF_SEND_HTTP_POST,
        /** чек для google disk. */
        TYPE_CHECK_SEND_SHEET_DISK,
        /** настройки для google disk. */
        TYPE_PREF_SEND_SHEET_DISK,
        /** чек для смс отправки контакту. */
        TYPE_CHECK_SEND_SMS_CONTACT,
        /** чек для смс отправки администратору. */
        TYPE_CHECK_SEND_SMS_ADMIN
    }

    /** Контейнер команд. */
    public final Map<TaskType, InterfaceTaskCommand> mapTasks = new EnumMap<>(TaskType.class);

    public interface InterfaceTaskCommand {
        void onExecuteTask(Map<String, ContentValues> map);
    }

    public TaskCommand(Context context, HandlerTaskNotification handler) {
        super(context);
        mContext = context;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
        mHandler = handler;
        cancel = false;

        mapTasks.put(TaskType.TYPE_CHECK_SEND_HTTP_POST, new CheckTokHttpPost());
        mapTasks.put(TaskType.TYPE_CHECK_SEND_SHEET_DISK, new CheckToSpreadsheet(((Main)mContext.getApplicationContext()).getVersionName()));
        mapTasks.put(TaskType.TYPE_CHECK_SEND_MAIL, new CheckToMail());
        /*mapTasks.put(TaskType.TYPE_CHECK_SEND_MAIL_ADMIN, new CheckToMail());*/
        mapTasks.put(TaskType.TYPE_CHECK_SEND_SMS_CONTACT, new CheckToSmsContact());
        mapTasks.put(TaskType.TYPE_CHECK_SEND_SMS_ADMIN, new CheckToSmsAdmin());
        mapTasks.put(TaskType.TYPE_PREF_SEND_HTTP_POST, new PreferenceTokHttpPost());
        mapTasks.put(TaskType.TYPE_PREF_SEND_SHEET_DISK, new PreferenceToSpreadsheet(((Main)mContext.getApplicationContext()).getVersionName()));
    }

    public void execute(TaskType type, Map<String, ContentValues> map) throws Exception {
        if (map.isEmpty())
            throw new Exception("map is empty");
        mapTasks.get(type).onExecuteTask(map);
    }

    /** Получить интернет соединение.
     *
     * @param timeout      Задержка между попытками.
     * @param countAttempt Количество попыток.
     * @return true - интернет соединение установлено.
     */
    private boolean getConnection(int timeout, int countAttempt) {
        while (!cancel && countAttempt != 0) {
            if (Internet.isOnline())
                return true;
            mContext.sendBroadcast(new Intent(Internet.INTERNET_CONNECT));
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException ignored) {
            }
            countAttempt--;
        }
        return false;
    }

    /** Отправляем весовой чек Google disk spreadsheet таблицу. */
    public class CheckToSpreadsheet extends GoogleSpreadsheets implements TaskCommand.InterfaceTaskCommand {
        /**
         * Контейнер для обратных сообщений
         * какие чеки отправлены или не отправлены
         */
        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();
        /**
         * Контейнер чеков для отправки
         */
        Map<String, ContentValues> mapChecks;


        /**
         * Конструктор экземпляра класса CheckToSpreadsheet.
         *
         * @param service Имя сервиса SpreadsheetService.
         */
        public CheckToSpreadsheet(String service) {
            super(service);
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());     /** Лист чеков отправленых */
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());   /** Лист чеков не отправленых */}

        /**
         * Вызывается когда токен получен
         */
        @Override
        protected void tokenIsReceived() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!getConnection(10000, 10)) {
                            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                            return;
                        }

                        for (Map.Entry<String, ContentValues> entry : mapChecks.entrySet()) {
                            int taskId = Integer.valueOf(entry.getKey());
                            int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());

                            try {
                                getSheetEntry(entry.getValue().get(TaskTable.KEY_DATA0).toString());
                                UpdateListWorksheets();
                            }catch (Exception e){
                                mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, e.getMessage()));
                                continue;
                            }

                            Message msg;
                            try {
                                sendCheckToDisk(checkId);
                                mapChecksProcessed.get(MAP_CHECKS_SEND).add(new ObjectParcel(checkId, mContext.getString(R.string.sent_to_the_server)));
                                msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_SHEET.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_SEND));
                            } catch (Exception e) {
                                mapChecksProcessed.get(MAP_CHECKS_UNSEND).add(new ObjectParcel(checkId, "Не отправлен " + e.getMessage()));
                                msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_CHECK_UNSEND.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_UNSEND));
                                mHandler.handleError(401, e.getMessage());
                            }
                            mHandler.sendMessage(msg);
                        }

                    } catch (Exception e) {
                        mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, e.getMessage()));
                    }
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                }
            }).start();
        }

        /**
         * Вызываем если ошибка получения токена
         */
        @Override
        protected void tokenIsFalse(String error) {
            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
            mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, "Ошибка получения токена " + error));
        }

        /**
         * Вызывается при получении токена.
         *
         * @return Возвращяет полученый токен.
         * @throws IOException
         * @throws GoogleAuthException
         */
        @Override
        protected String fetchToken() throws IOException, GoogleAuthException, IllegalArgumentException {
            if (!getConnection(10000, 10)) {
                mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                return null;
            }
            String user = ((Main)mContext.getApplicationContext()).preferencesScale.read(mContext.getString(R.string.KEY_LAST_USER), "");
            return GoogleAuthUtil.getTokenWithNotification(mContext, user /*ScaleModule.getUserName()*/, "oauth2:" + SCOPE, null, makeCallback());
        }

        /**
         * Вызывается если разрешение для получения токена получено
         */
        @Override
        protected void permissionIsObtained() {
            /** Процесс получения доступа к SpreadsheetService */
            super.execute();
        }

        /**
         * Выполнить задачу отправки чеков.
         *
         * @param map Контейнер чеков для отправки.
         */
        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
            /** Сохраняем контейнер локально */
            mapChecks = map;
            /** Процесс получения доступа к SpreadsheetService */
            super.execute();
        }

        /**
         * Отослать данные чека в таблицу
         *
         * @param id Индекс чека.
         * @throws Exception
         */
        private void sendCheckToDisk(int id) throws Exception {
            Cursor cursor = getEntryItem(id);
            if (cursor == null)
                throw new Exception(mContext.getString(R.string.Check_N) + id + " null");

            if (cursor.moveToFirst()) {
                addRow(cursor, CheckTable.TABLE);
                updateEntry(id, CheckTable.KEY_CHECK_ON_SERVER, 1);
            }
            cursor.close();
        }

    }

    /** Класс для отправки чека http post. */
    public class CheckTokHttpPost implements InterfaceTaskCommand {
        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();

        public CheckTokHttpPost() {
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!getConnection(10000, 10)) {
                        mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                        return;
                    }
                    for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                        Message msg;
                        int taskId = Integer.valueOf(entry.getKey());
                        int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                        Cursor check = getEntryItem(checkId);
                        try {
                            String pathFile = entry.getValue().get(TaskTable.KEY_DATA0).toString();
                            String nameForm = entry.getValue().get(TaskTable.KEY_DATA1).toString();
                            /** Класс формы для передачи данных весового чека. */
                            GoogleForms.Form form = new GoogleForms(mContext.getAssets().open(pathFile)).createForm(nameForm);
                            String http = form.getHttp();
                            Collection<BasicNameValuePair> values = form.getEntrys();
                            List<BasicNameValuePair> results = new ArrayList<>();
                            for (BasicNameValuePair valuePair : values){
                                try {
                                    results.add(new BasicNameValuePair(valuePair.getName(), check.getString(check.getColumnIndex(valuePair.getValue()))));
                                } catch (Exception e) {}
                            }
                            submitData(http, results);
                            mapChecksProcessed.get(MAP_CHECKS_SEND).add(new ObjectParcel(checkId, mContext.getString(R.string.sent_to_the_server)));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_HTTP.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_SEND));
                        } catch (Exception e) {
                            mapChecksProcessed.get(MAP_CHECKS_UNSEND).add(new ObjectParcel(checkId, "Ошибка " + e));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_CHECK_UNSEND.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_UNSEND));
                            mHandler.handleError(401, e.getMessage());
                        }
                        mHandler.sendMessage(msg);
                        check.close();
                    }
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                }
            }).start();
        }

        public void submitData(String http_post, List<BasicNameValuePair> results) throws Exception {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(http_post);
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 15000);
            HttpConnectionParams.setSoTimeout(httpParameters, 30000);
            post.setParams(httpParameters);
            post.setEntity(new UrlEncodedFormEntity(results, "UTF-8"));
            HttpResponse httpResponse = client.execute(post);
            if (httpResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK)
                throw new Exception(httpResponse.toString());
        }
    }

    /** Класс для отправки чека email почтой. */
    public class CheckToMail implements InterfaceTaskCommand {

        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();

        public CheckToMail() {
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!getConnection(10000, 10)) {
                        mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                        return;
                    }
                    for (Map.Entry<String, ContentValues> entry : map.entrySet()) {

                        int taskId = Integer.valueOf(entry.getKey());
                        int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                        MailSend.MailObject mailObject = new MailSend.MailObject(entry.getValue().get(TaskTable.KEY_DATA0).toString());
                        StringBuilder body = new StringBuilder(mContext.getString(R.string.WEIGHT_CHECK_N) + checkId + '\n' + '\n');
                        Cursor check = getEntryItem(checkId);
                        if (check == null) {
                            body.append(mContext.getString(R.string.No_data_check)).append(checkId).append(mContext.getString(R.string.delete));
                        } else {
                            if (check.moveToFirst()) {
                                body.append(mContext.getString(R.string.Date)).append('_').append(check.getString(check.getColumnIndex(CheckTable.KEY_DATE_CREATE))).append("__").append(check.getString(check.getColumnIndex(CheckTable.KEY_TIME_CREATE))).append('\n');
                                body.append(mContext.getString(R.string.Contact)).append("__").append(check.getString(check.getColumnIndex(CheckTable.KEY_VENDOR))).append('\n');
                                body.append(mContext.getString(R.string.GROSS)).append("___").append(check.getString(check.getColumnIndex(CheckTable.KEY_WEIGHT_FIRST))).append('\n');
                                body.append(mContext.getString(R.string.TAPE)).append("_____").append(check.getString(check.getColumnIndex(CheckTable.KEY_WEIGHT_SECOND))).append('\n');
                                body.append(mContext.getString(R.string.Netto)).append(":____").append(check.getString(check.getColumnIndex(CheckTable.KEY_WEIGHT_NETTO))).append('\n');
                                body.append(mContext.getString(R.string.Goods)).append("____").append(check.getString(check.getColumnIndex(CheckTable.KEY_TYPE))).append('\n');
                                body.append(mContext.getString(R.string.Price)).append("_____").append(check.getString(check.getColumnIndex(CheckTable.KEY_PRICE))).append('\n');
                                body.append(mContext.getString(R.string.Sum)).append(":____").append(check.getString(check.getColumnIndex(CheckTable.KEY_PRICE_SUM))).append('\n');
                            } else {
                                body.append(mContext.getString(R.string.No_data_check)).append(checkId).append(mContext.getString(R.string.delete));
                            }
                            check.close();
                        }
                        mailObject.setBody(body.toString());
                        mailObject.setSubject(mContext.getString(R.string.Check_N) + checkId);
                        try {
                            mailObject.setPersonal(mContext.getString(R.string.app_name) + " \"" + scaleModule.getNameBluetoothDevice());
                        } catch (Exception e) {
                            mailObject.setPersonal(mContext.getString(R.string.app_name) + " \"");
                        }
                        mailObject.setUser(entry.getValue().get(TaskTable.KEY_DATA1).toString());
                        mailObject.setPassword(entry.getValue().get(TaskTable.KEY_DATA2).toString());
                        Message msg;
                        try {
                            MailSend mail = new MailSend(mContext, mailObject);
                            mail.sendMail();
                            mapChecksProcessed.get(MAP_CHECKS_SEND).add(new ObjectParcel(checkId, mContext.getString(R.string.Send_to_mail) + ": " + mailObject.getEmail()));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_MAIL.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_SEND));
                        } catch (MessagingException e) {
                            mapChecksProcessed.get(MAP_CHECKS_UNSEND).add(new ObjectParcel(checkId, "Не отправлен " + e.getMessage() + ' ' + mailObject.getEmail()));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_CHECK_UNSEND.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_UNSEND));
                            mHandler.handleError(401, e.getMessage());
                        } catch (UnsupportedEncodingException e) {
                            continue;
                        }
                        mHandler.sendMessage(msg);

                    }
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                }
            }).start();
        }
    }

    /** Класс для отправки чека смс сообщением. */
    public class CheckToSmsContact implements InterfaceTaskCommand {

        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();

        public CheckToSmsContact() {
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                        int taskId = Integer.valueOf(entry.getKey());
                        int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                        String address = entry.getValue().get(TaskTable.KEY_DATA0).toString();
                        StringBuilder body = new StringBuilder(mContext.getString(R.string.WEIGHT_CHECK_N) + checkId + '\n' + '\n');
                        Cursor check = getEntryItem(checkId, CheckTable.COLUMNS_SMS_CONTACT);
                        if (check == null) {
                            body.append(mContext.getString(R.string.No_data_check)).append(checkId).append(mContext.getString(R.string.delete));
                        } else {
                            if (check.moveToFirst()) {
                                body.append(mContext.getString(R.string.Date)).append('=').append(check.getString(check.getColumnIndex(CheckTable.KEY_DATE_CREATE))).append('_').append(check.getString(check.getColumnIndex(CheckTable.KEY_TIME_CREATE))).append('\n');
                                body.append(mContext.getString(R.string.Contact)).append('=').append(check.getString(check.getColumnIndex(CheckTable.KEY_VENDOR))).append('\n');
                                body.append(mContext.getString(R.string.GROSS)).append('=').append(check.getString(check.getColumnIndex(CheckTable.KEY_WEIGHT_FIRST))).append('\n');
                                body.append(mContext.getString(R.string.TAPE)).append('=').append(check.getString(check.getColumnIndex(CheckTable.KEY_WEIGHT_SECOND))).append('\n');
                                body.append(mContext.getString(R.string.Netto)).append(":=").append(check.getString(check.getColumnIndex(CheckTable.KEY_WEIGHT_NETTO))).append('\n');
                                body.append(mContext.getString(R.string.Goods)).append('=').append(check.getString(check.getColumnIndex(CheckTable.KEY_TYPE))).append('\n');
                                body.append(mContext.getString(R.string.Price)).append('=').append(check.getString(check.getColumnIndex(CheckTable.KEY_PRICE))).append('\n');
                                body.append(mContext.getString(R.string.Sum)).append(":=").append(check.getString(check.getColumnIndex(CheckTable.KEY_PRICE_SUM))).append('\n');
                            } else {
                                body.append(mContext.getString(R.string.No_data_check)).append(checkId).append(mContext.getString(R.string.delete));
                            }
                            check.close();
                        }

                        Message msg;
                        try {
                            SMS.sendSMS(address, body.toString());
                            mapChecksProcessed.get(MAP_CHECKS_SEND).add(new ObjectParcel(checkId, mContext.getString(R.string.Send_to_phone) + ": " + address));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_MESSAGE.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_SEND));
                        } catch (Exception e) {
                            mapChecksProcessed.get(MAP_CHECKS_UNSEND).add(new ObjectParcel(checkId, "Не отправлен " + e.getMessage() + ' ' + address));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_CHECK_UNSEND.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_UNSEND));
                            mHandler.handleError(401, e.getMessage());
                        }
                        mHandler.sendMessage(msg);
                    }
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                }
            }).start();
        }
    }

    /** Класс для отправки чека смс сообщением.  */
    public class CheckToSmsAdmin implements InterfaceTaskCommand {

        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();

        public CheckToSmsAdmin() {
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                        int taskId = Integer.valueOf(entry.getKey());
                        int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                        String address = entry.getValue().get(TaskTable.KEY_DATA0).toString();
                        StringBuilder body = new StringBuilder();
                        Cursor check = getEntryItem(checkId,CheckTable.COLUMNS_SMS_ADMIN);
                        if (check == null) {
                            body.append(mContext.getString(R.string.No_data_check)).append(checkId).append(mContext.getString(R.string.delete));
                        } else {
                            if (check.moveToFirst()) {
                                body.append("check(");
                                String[] columns = check.getColumnNames();
                                for (String column : columns){
                                    try {
                                        body.append(column).append('=').append(check.getString(check.getColumnIndex(column)).replaceAll(" ","")).append(' ');
                                    }catch (NullPointerException e){}
                                }
                                body.append(')');
                            } else {
                                body.append(mContext.getString(R.string.No_data_check)).append(checkId).append(mContext.getString(R.string.delete));
                            }
                            check.close();
                        }

                        Message msg;
                        try {
                            SMS.sendSMS(address, SMS.encrypt(codeword, body.toString()));
                            mapChecksProcessed.get(MAP_CHECKS_SEND).add(new ObjectParcel(checkId, mContext.getString(R.string.Send_to_phone) + ": " + address));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_MESSAGE.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_SEND));
                        } catch (Exception e) {
                            mapChecksProcessed.get(MAP_CHECKS_UNSEND).add(new ObjectParcel(checkId, "Не отправлен " + e.getMessage() + ' ' + address));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_CHECK_UNSEND.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_UNSEND));
                            mHandler.handleError(401, e.getMessage());
                        }
                        mHandler.sendMessage(msg);
                    }
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                }
            }).start();
        }
    }

    /** Отправляем настройки Google disk spreadsheet таблицу.  */
    public class PreferenceToSpreadsheet extends GoogleSpreadsheets implements InterfaceTaskCommand {

        final String MAP_PREF_SEND = "send";
        final String MAP_PREF_UNSEND = "unsend";
        final Map<String, ArrayList<ObjectParcel>> mapPrefsProcessed = new HashMap<>();
        Map<String, ContentValues> map;

        PreferenceToSpreadsheet(String service) {
            super(service);
            mapPrefsProcessed.put(MAP_PREF_SEND, new ArrayList<>());
            mapPrefsProcessed.put(MAP_PREF_UNSEND, new ArrayList<>());
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
            this.map = map;
            super.execute();
        }

        private void sendPreferenceToDisk(int id) throws Exception {
            Cursor cursor = new PreferencesTable(mContext).getEntryItem(id);
            if (cursor == null) {
                throw new Exception(mContext.getString(R.string.Check_N) + id + " null");
            }
            if (cursor.moveToFirst()) {
                addRow(cursor, PreferencesTable.TABLE);
                new PreferencesTable(mContext).removeEntry(id);
            }
            cursor.close();
        }

        @Override
        protected void tokenIsReceived() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!getConnection(10000, 10)) {
                        mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                        return;
                    }
                    try {

                        Message msg = new Message();
                        for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                            try {
                                getSheetEntry(entry.getValue().get(TaskTable.KEY_DATA0).toString());
                                UpdateListWorksheets();
                            }catch (Exception e){
                                mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, e.getMessage()));
                                continue;
                            }

                            int taskId = Integer.valueOf(entry.getKey());
                            int prefId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                            ObjectParcel objParcel = new ObjectParcel(prefId, mContext.getString(R.string.sent_to_the_server));
                            try {
                                sendPreferenceToDisk(prefId);
                                mapPrefsProcessed.get(MAP_PREF_SEND).add(objParcel);
                                msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_PREF.ordinal(), prefId, taskId, mapPrefsProcessed.get(MAP_PREF_SEND));
                            } catch (Exception e) {
                                mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 401, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, "Настройки не отправлены " + e.getMessage()));
                            }
                            mHandler.sendMessage(msg);
                        }
                    } catch (Exception e) {
                        mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, e.getMessage()));
                    }
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                }
            }).start();
        }

        @Override
        protected void tokenIsFalse(String error) {
            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
            mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, "Ошибка получения токена " + error));
        }

        @Override
        protected String fetchToken() throws IOException, GoogleAuthException {
            if (!getConnection(10000, 10)) {
                mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                return null;
            }
            String user = ((Main)mContext.getApplicationContext()).preferencesScale.read(mContext.getString(R.string.KEY_LAST_USER), "");
            return GoogleAuthUtil.getTokenWithNotification(mContext, user /*ScaleModule.getUserName()*/, "oauth2:" + SCOPE, null, makeCallback());
        }

        @Override
        protected void permissionIsObtained() {
            super.execute();
        }


    }

    /** Класс для отправки чека http post. */
    public class PreferenceTokHttpPost implements InterfaceTaskCommand {
        final String MAP_PREF_SEND = "send";
        final String MAP_PREF_UNSEND = "unsend";

        final Map<String, ArrayList<ObjectParcel>> mapPrefProcessed = new HashMap<>();

        public PreferenceTokHttpPost() {
            mapPrefProcessed.put(MAP_PREF_SEND, new ArrayList<>());
            mapPrefProcessed.put(MAP_PREF_UNSEND, new ArrayList<>());
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!getConnection(10000, 10)) {
                        mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                        return;
                    }

                    for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                        Message msg = new Message();
                        int taskId = Integer.valueOf(entry.getKey());
                        int prefId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                        Cursor pref = new PreferencesTable(mContext).getEntryItem(prefId);
                        try {
                            String pathFile = entry.getValue().get(TaskTable.KEY_DATA0).toString();
                            String nameForm = entry.getValue().get(TaskTable.KEY_DATA1).toString();
                            /** Класс формы для передачи данных весового чека. */
                            GoogleForms.Form form = new GoogleForms(mContext.getAssets().open(pathFile)).createForm(nameForm);
                            String http = form.getHttp();
                            Collection<BasicNameValuePair> values = form.getEntrys();
                            List<BasicNameValuePair> results = new ArrayList<>();
                            for (BasicNameValuePair valuePair : values){
                                try {
                                    results.add(new BasicNameValuePair(valuePair.getName(), pref.getString(pref.getColumnIndex(valuePair.getValue()))));
                                } catch (Exception e) {}
                            }
                            /** Отправляем данные на сервер. */
                            submitData(http, results);
                            /** Удаляем запить в базе если отправили на сервер. */
                            new PreferencesTable(mContext).removeEntry(prefId);
                            mapPrefProcessed.get(MAP_PREF_SEND).add(new ObjectParcel(prefId, mContext.getString(R.string.sent_to_the_server)));
                            msg = mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_PREF.ordinal(), prefId, taskId, mapPrefProcessed.get(MAP_PREF_SEND));
                        } catch (Exception e) {
                            mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 401, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, "Настройки не отправлены " + e.getMessage()));
                        }
                        mHandler.sendMessage(msg);
                        pref.close();
                    }
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                }
            }).start();
        }

        public void submitData(String http_post, List<BasicNameValuePair> results) throws Exception {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(http_post);
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 15000);
            HttpConnectionParams.setSoTimeout(httpParameters, 30000);
            post.setParams(httpParameters);
            post.setEntity(new UrlEncodedFormEntity(results, "UTF-8"));
            HttpResponse httpResponse = client.execute(post);
            if (httpResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK)
                throw new Exception(httpResponse.toString());
            //return httpResponse.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK;
        }
    }

    public static class MessageNotify {
        int notifyId;
        final String message;

        public static final int ID_NOTIFY_SERVICE = 1;
        public static final int ID_NOTIFY_CLOUD = 2;
        public static final int ID_NOTIFY_MAIL = 3;
        public static final int ID_NOTIFY_MESSAGE = 4;
        public static final int ID_NOTIFY_NO_SHEET = 5;


        MessageNotify(int id, String message) {
            notifyId = id;
            this.message = message;
        }

        /*public int getNotifyId() {
            return notifyId;
        }*/

        public String getMessage() {
            return message;
        }

        /*void setNotifyId(int id) {
            notifyId = id;
        }*/
    }

    public static class ObjectParcel implements Parcelable {

        private String strValue;
        private Integer intValue;

        public ObjectParcel(Integer value, String str) {
            intValue = value;
            strValue = str;
        }

        public ObjectParcel(Parcel in) {
            readFromParcel(in);
        }

        public String getStrValue() {
            return strValue;
        }

        /*public void setStrValue(String strValue) {
            this.strValue = strValue;
        }*/

        public Integer getIntValue() {
            return intValue;
        }

        /*public void setIntValue(Integer intValue) {
            this.intValue = intValue;
        }*/

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

            dest.writeString(strValue);
            dest.writeInt(intValue);
        }

        private void readFromParcel(Parcel in) {

            strValue = in.readString();
            intValue = in.readInt();

        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            @Override
            public ObjectParcel createFromParcel(Parcel in) {
                return new ObjectParcel(in);
            }

            @Override
            public ObjectParcel[] newArray(int size) {
                return new ObjectParcel[size];
            }
        };

    }

}
