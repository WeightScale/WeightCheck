/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.victjava.scales;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.webkit.URLUtil;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.konst.module.ScaleModule;
import com.konst.sms_commander.SMS;
import com.victjava.scales.provider.CheckTable;
import com.victjava.scales.provider.PreferencesTable;
import com.victjava.scales.provider.TaskTable.*;
import com.victjava.scales.provider.TaskTable;
import com.victjava.scales.TaskCommand.*;
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.*;

/**
 * Класс задач.
 *
 * @author Kostya
 */
public class TaskPoolCommand extends CheckTable {
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
        HANDLER_NOTIFY_PHOTO,
        HANDLER_NOTIFY_PROCESS,
        REMOVE_TASK_ENTRY,
        REMOVE_TASK_ENTRY_ERROR_OVER,
        HANDLER_NOTIFY_ERROR,
        ERROR
    }

    /** Контейнер команд. */
    public final Map<TaskType, InterfaceTaskCommand> mapTasks = new EnumMap<>(TaskType.class);

    public interface InterfaceTaskCommand {
        void onExecuteTask(Map<String, ContentValues> map);
    }

    public TaskPoolCommand(Context context, HandlerTaskNotification handler) {
        super(context);
        mContext = context;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
        mHandler = handler;
        cancel = false;

        mapTasks.put(TaskType.TYPE_CHECK_SEND_HTTP_POST, new CheckTokHttpPost());
        mapTasks.put(TaskType.TYPE_CHECK_SEND_SHEET_DISK, new CheckToSpreadsheet(((Main)mContext.getApplicationContext()).getVersionName()));
        mapTasks.put(TaskType.TYPE_CHECK_SEND_MAIL, new CheckToMail());
        //mapTasks.put(TaskType.TYPE_CHECK_SEND_MAIL_ADMIN, new CheckToMail());
        mapTasks.put(TaskType.TYPE_CHECK_SEND_SMS_CONTACT, new CheckToSmsContact());
        mapTasks.put(TaskType.TYPE_CHECK_SEND_SMS_ADMIN, new CheckToSmsAdmin());
        mapTasks.put(TaskType.TYPE_PREF_SEND_HTTP_POST, new PreferenceTokHttpPost());
        mapTasks.put(TaskType.TYPE_PREF_SEND_SHEET_DISK, new PreferenceToSpreadsheet(((Main)mContext.getApplicationContext()).getVersionName()));
        mapTasks.put(TaskType.TYPE_DATA_SEND_TO_DISK, new DataToGoogleDisk());
    }

    public InterfaceTaskCommand getTask(TaskType type){
        return mapTasks.get(type);
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
    public class CheckToSpreadsheet extends GoogleSpreadsheets implements InterfaceTaskCommand, Callable<String> {
        ExecutorService executorService;
        /** Контейнер для обратных сообщений.
         * какие чеки отправлены или не отправлены
         */
        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();
        /** Контейнер чеков для отправки. */
        Map<String, ContentValues> mapChecks;

        /** Конструктор экземпляра класса CheckToSpreadsheet.
         * @param service Имя сервиса SpreadsheetService.
         */
        public CheckToSpreadsheet(String service) {
            super(service);
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());     /** Лист чеков отправленых */
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());   /** Лист чеков не отправленых */
            executorService = Executors.newFixedThreadPool(5);
        }

        /** Вызывается когда токен получен. */
        @Override
        protected void tokenIsReceived() {
            List<Future<String>> futures = new ArrayList<>();
            try {
                if (!getConnection(10000, 10)) {
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                    return;
                }
                if (executorService.isShutdown())
                    executorService = Executors.newFixedThreadPool(5);

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

                    try {
                        futures.add(executorService.submit(new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                sendCheckToDisk(checkId);
                                mapChecksProcessed.get(MAP_CHECKS_SEND).add(new ObjectParcel(checkId, mContext.getString(R.string.sent_to_the_server)));
                                mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_SHEET.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_SEND)).sendToTarget();
                                return "";
                            }
                        }));
                    } catch (Exception e) {
                        mapChecksProcessed.get(MAP_CHECKS_UNSEND).add(new ObjectParcel(checkId, "Не отправлен " + e.getMessage()));
                        mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_CHECK_UNSEND.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_UNSEND)).sendToTarget();
                        mHandler.handleError(401, e.getMessage());
                    }
                }

            } catch (Exception e) {
                mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, e.getMessage()));
            }
            for (Future<String> f : futures){
                /** ждем выполнения задачи. */
                while (!f.isDone());
            }
            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
        }

        /** Вызываем если ошибка получения токена. */
        @Override
        protected void tokenIsFalse(String error) {
            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
            mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, "Ошибка получения токена " + error));
        }

        /** Вызывается при получении токена.
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

        /** Выполнить задачу отправки чеков.
         * @param map Контейнер чеков для отправки.
         */
        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
            /** Сохраняем контейнер локально. */
            mapChecks = map;
            /** Процесс получения доступа к SpreadsheetService. */
            try {
                spreadsheetService.setAuthSubToken(fetchToken());
                tokenIsReceived();
            } catch (Exception e) {
                tokenIsFalse(e.getMessage());
            }
            //super.execute();
        }

        /** Отослать данные чека в таблицу.
         * @param id Индекс чека.
         * @throws Exception Ошибка отправки чека.
         */
        private void sendCheckToDisk(int id) throws Exception {
            Cursor cursor = getEntryItem(id, COLUMNS_SHEET);
            if (cursor == null)
                throw new Exception(mContext.getString(R.string.Check_N) + id + " null");

            if (cursor.moveToFirst()) {
                addRow(cursor, CheckTable.TABLE);
                updateEntry(id, CheckTable.KEY_CHECK_STATE, State.CHECK_ON_SERVER.ordinal());
            }
            cursor.close();
        }

        @Override
        public String call() throws Exception {
            return "CheckToSpreadsheet";
        }
    }

    /** Класс для отправки чека http post. */
    public class CheckTokHttpPost implements InterfaceTaskCommand, Callable<String> {
        ExecutorService executorService;
        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();

        public CheckTokHttpPost() {
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());
            executorService = Executors.newFixedThreadPool(5);
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
            if (!getConnection(10000, 10)) {
                mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                return;
            }
            if (executorService.isShutdown())
                executorService = Executors.newFixedThreadPool(5);
            List<Future<String>> futures = new ArrayList<>();
            for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                int taskId = Integer.valueOf(entry.getKey());
                int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                String user = entry.getValue().get(TaskTable.KEY_DATA2).toString();
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
                    futures.add(executorService.submit(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            submitData(http, results);
                            mapChecksProcessed.get(MAP_CHECKS_SEND).add(new ObjectParcel(checkId, mContext.getString(R.string.sent_to_the_server)));
                            mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_HTTP.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_SEND)).sendToTarget();
                            return "";
                        }
                    }));
                } catch (Exception e) {
                    mapChecksProcessed.get(MAP_CHECKS_UNSEND).add(new ObjectParcel(checkId, "Ошибка " + e));
                    mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_CHECK_UNSEND.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_UNSEND)).sendToTarget();
                    mHandler.handleError(401, e.getMessage());
                }
                check.close();
            }
            for (Future<String> f : futures){
                /** ждем выполнения задачи. */
                while (!f.isDone());
            }
            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
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

        @Override
        public String call() throws Exception {
            return "CheckTokHttpPost";
        }
    }

    /** Класс для отправки чека email почтой. */
    public class CheckToMail implements InterfaceTaskCommand, Callable<String> {
        ExecutorService executorService;
        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();

        public CheckToMail() {
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());
            executorService = Executors.newFixedThreadPool(5);
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
            if (!getConnection(10000, 10)) {
                mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                return;
            }
            if (executorService.isShutdown())
                executorService = Executors.newFixedThreadPool(5);
            List<Future<String>> futures = new ArrayList<>();
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
                //try {
                    futures.add(executorService.submit(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            MailSend mail = new MailSend(mContext, mailObject);
                            mail.sendMail();
                            mapChecksProcessed.get(MAP_CHECKS_SEND).add(new ObjectParcel(checkId, mContext.getString(R.string.Send_to_mail) + ": " + mailObject.getEmail()));
                            mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_MAIL.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_SEND)).sendToTarget();
                            return "";
                        }
                    }));
                /*} catch (MessagingException e) {
                    mapChecksProcessed.get(MAP_CHECKS_UNSEND).add(new ObjectParcel(checkId, "Не отправлен " + e.getMessage() + ' ' + mailObject.getEmail()));
                    mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_CHECK_UNSEND.ordinal(), checkId, taskId, mapChecksProcessed.get(MAP_CHECKS_UNSEND)).sendToTarget();
                    mHandler.handleError(401, e.getMessage());
                } */
            }
            for (Future<String> f : futures){
                /** ждем выполнения задачи. */
                while (!f.isDone());
            }
            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
        }

        @Override
        public String call() throws Exception {
            return "CheckToMail";
        }
    }

    /** Класс для отправки чека смс сообщением. */
    public class CheckToSmsContact implements InterfaceTaskCommand, Callable<String> {

        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();

        public CheckToSmsContact() {
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
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

        @Override
        public String call() throws Exception {
            return "CheckToSmsContact";
        }
    }

    /** Класс для отправки чека смс сообщением. */
    public class CheckToSmsAdmin implements InterfaceTaskCommand, Callable<String> {

        final Map<String, ArrayList<ObjectParcel>> mapChecksProcessed = new HashMap<>();

        public CheckToSmsAdmin() {
            mapChecksProcessed.put(MAP_CHECKS_SEND, new ArrayList<>());
            mapChecksProcessed.put(MAP_CHECKS_UNSEND, new ArrayList<>());
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
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

        @Override
        public String call() throws Exception {
            return "CheckToSmsAdmin";
        }
    }

    /** Отправляем настройки Google disk spreadsheet таблицу. */
    public class PreferenceToSpreadsheet extends GoogleSpreadsheets implements InterfaceTaskCommand, Callable<String> {
        ExecutorService executorService;
        final String MAP_PREF_SEND = "send";
        final String MAP_PREF_UNSEND = "unsend";
        final Map<String, ArrayList<ObjectParcel>> mapPrefsProcessed = new HashMap<>();
        Map<String, ContentValues> map;

        PreferenceToSpreadsheet(String service) {
            super(service);
            mapPrefsProcessed.put(MAP_PREF_SEND, new ArrayList<>());
            mapPrefsProcessed.put(MAP_PREF_UNSEND, new ArrayList<>());
            executorService = Executors.newFixedThreadPool(5);
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
            this.map = map;
            /** Процесс получения доступа к SpreadsheetService. */
            try {
                spreadsheetService.setAuthSubToken(fetchToken());
                tokenIsReceived();
            } catch (Exception e) {
                tokenIsFalse(e.getMessage());
            }
            //super.execute();
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
            if (!getConnection(10000, 10)) {
                mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                return;
            }
            if (executorService.isShutdown())
                executorService = Executors.newFixedThreadPool(5);
            List<Future<String>> futures = new ArrayList<>();
            try {
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
                        futures.add(executorService.submit(new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                sendPreferenceToDisk(prefId);
                                mapPrefsProcessed.get(MAP_PREF_SEND).add(objParcel);
                                mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_PREF.ordinal(), prefId, taskId, mapPrefsProcessed.get(MAP_PREF_SEND)).sendToTarget();
                                return "";
                            }
                        }));
                    } catch (Exception e) {
                        mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 401, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, "Настройки не отправлены " + e.getMessage()));
                    }
                }

            } catch (Exception e) {
                mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 505, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, e.getMessage()));
            }
            for (Future<String> f : futures){
                /** ждем выполнения задачи. */
                while (!f.isDone());
            }
            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
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
        public String call() throws Exception {
            return "PreferenceToSpreadsheet";
        }
    }

    /** Класс для отправки чека http post. */
    public class PreferenceTokHttpPost implements InterfaceTaskCommand, Callable<String> {
        ExecutorService executorService;
        final String MAP_PREF_SEND = "send";
        final String MAP_PREF_UNSEND = "unsend";

        final Map<String, ArrayList<ObjectParcel>> mapPrefProcessed = new HashMap<>();

        public PreferenceTokHttpPost() {
            mapPrefProcessed.put(MAP_PREF_SEND, new ArrayList<>());
            mapPrefProcessed.put(MAP_PREF_UNSEND, new ArrayList<>());
            executorService = Executors.newFixedThreadPool(5);
        }

        @Override
        public void onExecuteTask(final Map<String, ContentValues> map) {
            if (!getConnection(10000, 10)) {
                mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                return;
            }
            if (executorService.isShutdown())
                executorService = Executors.newFixedThreadPool(5);
            List<Future<String>> futures = new ArrayList<>();
            for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                //Message msg = new Message();
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

                    futures.add(executorService.submit(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            /** Отправляем данные на сервер. */
                            submitData(http, results);
                            /** Удаляем запить в базе если отправили на сервер. */
                            new PreferencesTable(mContext).removeEntry(prefId);
                            mapPrefProcessed.get(MAP_PREF_SEND).add(new ObjectParcel(prefId, mContext.getString(R.string.sent_to_the_server)));
                            mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_PREF.ordinal(), prefId, taskId, mapPrefProcessed.get(MAP_PREF_SEND)).sendToTarget();
                            return "";
                        }
                    }));

                } catch (Exception e) {
                    mHandler.handleNotificationError(NotifyType.HANDLER_NOTIFY_ERROR.ordinal(), 401, new MessageNotify(MessageNotify.ID_NOTIFY_NO_SHEET, "Настройки не отправлены " + e.getMessage()));
                }
                pref.close();
            }
            for (Future<String> f : futures){
                /** ждем выполнения задачи. */
                while (!f.isDone());
            }
            executorService.shutdown();
            mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
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

        @Override
        public String call() throws Exception {
            return "PreferenceTokHttpPost";
        }
    }

    /** Класс для отправки фото на google disk. */
    public class DataToGoogleDisk implements InterfaceTaskCommand, Callable{
        ExecutorService executorService;
        //int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        private UtilityDriver utilityDriver = null;
        final String MAP_PHOTO_SEND = "send";
        final String MAP_PHOTO_UNSEND = "unsend";
        final String[] SCOPE_ARRAY = {"https://www.googleapis.com/auth/drive.file ", "https://www.googleapis.com/auth/drive "};
        final List<String> SCOPES_LIST = Arrays.asList(SCOPE_ARRAY);
        protected final String SCOPE = SCOPE_ARRAY[0] + SCOPE_ARRAY[1];

        final Map<String, ArrayList<ObjectParcel>> mapPhotoProcessed = new HashMap<>();

        public DataToGoogleDisk() {
            mapPhotoProcessed.put(MAP_PHOTO_SEND, new ArrayList<>());
            mapPhotoProcessed.put(MAP_PHOTO_UNSEND, new ArrayList<>());
            executorService = Executors.newFixedThreadPool(5);
        }


        @Override
        public void onExecuteTask(Map<String, ContentValues> map) {
            if (!map.isEmpty()){
                if (!getConnection(10000, 10)) {
                    mHandler.sendEmptyMessage(NotifyType.HANDLER_FINISH_THREAD.ordinal());
                    return;
                }

                try {
                    String user = ((Main)mContext.getApplicationContext()).preferencesScale.read(mContext.getString(R.string.KEY_LAST_USER), "");
                    /** Экземпляр для работы с google drive */
                    utilityDriver = new UtilityDriver(mContext, user);
                } catch (NullPointerException e) {
                    return;
                }
                if (executorService.isShutdown())
                    executorService = Executors.newFixedThreadPool(5);
                List<Future<String>> futures = new ArrayList<>();

                for (Map.Entry<String, ContentValues> entry : map.entrySet()) {

                    int taskId = Integer.valueOf(entry.getKey());
                    int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                    String folder = entry.getValue().get(TaskTable.KEY_DATA0).toString();

                    Cursor cursor = getEntryItem(checkId, KEY_PHOTO_FIRST, KEY_PHOTO_SECOND);
                    String[] columnNames = cursor.getColumnNames();
                    for (String column : columnNames){
                        String value = cursor.getString(cursor.getColumnIndex(column));
                        if (value != null){
                            if(!URLUtil.isHttpsUrl(value)){
                                java.io.File file = new java.io.File(value);
                                try {
                                    /** Получаем доступ к папке рут на google drive. */
                                    File folderRoot = utilityDriver.getFolder(folder, null);
                                    Future<String> future = executorService.submit(new Callable<String>() {
                                        @Override
                                        public String call() throws Exception {
                                            /** Получаем индекс папки. */
                                            final String parentId = folderRoot.getId();
                                            /** Сохраняем фаил в папку на google disc. */
                                            String link = saveFileToDrive(file, parentId);
                                            updateEntry(checkId, column, link);
                                            mapPhotoProcessed.get(MAP_PHOTO_SEND).add(new ObjectParcel(0, mContext.getString(R.string.sent_to_the_server)));
                                            mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_PHOTO.ordinal(), 0, taskId, mapPhotoProcessed.get(MAP_PHOTO_SEND)).sendToTarget();
                                            return String.valueOf(taskId);
                                        }
                                    });
                                    futures.add(future);
                                    /** Исключение если не добавлено разрешение для программы в google service play. */
                                } catch (UserRecoverableAuthIOException e) {
                                    Intent intent = new Intent(mContext, ActivityGoogleDrivePreference.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setAction("UserRecoverableAuthIOException");
                                    intent.putExtra("request_authorization", e.getIntent());
                                    mContext.startActivity(intent);
                                    return;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }else{
                                mapPhotoProcessed.get(MAP_PHOTO_SEND).add(new ObjectParcel(0, mContext.getString(R.string.sent_to_the_server)));
                                mHandler.obtainMessage(NotifyType.HANDLER_NOTIFY_PHOTO.ordinal(), 0, taskId, mapPhotoProcessed.get(MAP_PHOTO_SEND)).sendToTarget();
                            }
                        }
                    }
                }
                for (Future<String> f : futures){
                    /** ждем выполнения задачи. */
                    while (!f.isDone());
                }
                executorService.shutdown();
            }
        }

        /** Сохраняем фаил на Google drive.
         *
         * @param fileContent Фаил который нужно сохранить.
         * @param parentId    Родительский индекс папки Google disk.
         * @return string  - ссылку на загруженый фаил.
         * @throws Exception Ошибки сохранения файла.
         */
        private String saveFileToDrive(final java.io.File fileContent, String parentId) throws Exception {

            try {
                /** Проверяе фаил */
                if (!fileContent.exists())
                    throw new Exception("Нет файла " + fileContent.getPath());
                /** Создаем контента экземпляр файла для закрузки*/
                FileContent mediaContent = new FileContent("image/jpeg", fileContent);
                /**Не содержит контент */
                if (mediaContent.getLength() == 0) {
                    /** Фаил удаляем */
                    if (!fileContent.delete()) {
                        throw new Exception("Невозможно удалить медиоконтент " + fileContent.getPath());
                    }
                }
                /** Загружаем фаил на диск */
                File file = utilityDriver.uploadFile(fileContent.getName(), parentId, "image/jpeg", fileContent);
                return file.getAlternateLink();
            } catch (UserRecoverableAuthIOException e) {
                Intent intent = new Intent(mContext, ActivityGoogleDrivePreference.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction("UserRecoverableAuthIOException");
                intent.putExtra("request_authorization", e.getIntent());
                mContext.startActivity(intent);
                throw new Exception(e.getMessage());
            } catch (IOException e) {
                throw new Exception("Возможно нужно установить Сервисы Google Play" + e.getMessage());
            }
        }

        @Override
        public Object call() throws Exception {
            mHandler.sendEmptyMessage(NotifyType.HANDLER_NOTIFY_PROCESS.ordinal());
            return null;
        }
    }

}
