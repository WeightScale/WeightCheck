package com.victjava.scales;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.*;
import com.konst.module.ScaleModule;
import com.konst.sms_commander.SMS;
import com.victjava.scales.provider.CheckTable;
import com.victjava.scales.provider.PreferencesTable;
import com.victjava.scales.provider.SenderTable;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.*;

/*
 * Created by Kostya on 04.04.2015.
 */
public class TaskCommand {

    final CheckTable checkTable;
    final Context mContext;
    //String mMimeType;
    final HandlerTaskNotification mHandler;
    boolean cancel = true;

    public static final int HANDLER_FINISH_THREAD = 1;
    public static final int HANDLER_NOTIFY_GENERAL = 2;
    public static final int HANDLER_NOTIFY_SHEET = 3;
    public static final int HANDLER_NOTIFY_PREF = 4;
    public static final int HANDLER_NOTIFY_MAIL = 5;
    public static final int HANDLER_NOTIFY_MESSAGE = 6;
    public static final int HANDLER_NOTIFY_CHECK_UNSEND = 7;
    public static final int HANDLER_NOTIFY_HTTP = 8;
    public static final int REMOVE_TASK_ENTRY = 9;
    public static final int REMOVE_TASK_ENTRY_ERROR_OVER = 10;
    public static final int HANDLER_NOTIFY_ERROR = 11;
    public static final int ERROR = 12;

    public enum TaskType {

        TYPE_CHECK_SEND_MAIL_CONTACT,   //для електронной почты
        TYPE_CHECK_SEND_MAIL_ADMIN,     //для електронной боссу
        TYPE_CHECK_SEND_HTTP_POST,      //для чек для облака
        TYPE_PREF_SEND_HTTP_POST,       //для настройки для облака
        TYPE_CHECK_SEND_SHEET_DISK,     //для google disk
        TYPE_PREF_SEND_SHEET_DISK,      //для google disk
        TYPE_CHECK_SEND_SMS_CONTACT,    //для смс отправки контакту
        TYPE_CHECK_SEND_SMS_ADMIN       //для смс отправки боссу
    }

    public final Map<TaskType, InterfaceTaskCommand> cmdMap = new EnumMap<>(TaskType.class);

    {
        cmdMap.put(TaskType.TYPE_CHECK_SEND_HTTP_POST, new CheckTokHttpPost());
        cmdMap.put(TaskType.TYPE_CHECK_SEND_SHEET_DISK, new CheckTokDiskSheet());
        cmdMap.put(TaskType.TYPE_CHECK_SEND_MAIL_CONTACT, new CheckToMail());
        cmdMap.put(TaskType.TYPE_CHECK_SEND_MAIL_ADMIN, new CheckToMail());
        cmdMap.put(TaskType.TYPE_CHECK_SEND_SMS_CONTACT, new CheckToSms());
        cmdMap.put(TaskType.TYPE_CHECK_SEND_SMS_ADMIN, new CheckToSms());
        cmdMap.put(TaskType.TYPE_PREF_SEND_SHEET_DISK, new PrefToDiskSheet());
        //cmdMap = Collections.unmodifiableMap(cmdMap);
    }

    public interface InterfaceTaskCommand {
        void onExecTask(Map<String, ContentValues> map);
    }

    public TaskCommand(Context context, HandlerTaskNotification handler) {
        mContext = context;
        mHandler = handler;
        cancel = false;
        checkTable = new CheckTable(mContext);
    }

    public void execTask(TaskType type, Map<String, ContentValues> map) throws Exception {
        if (map.isEmpty())
            throw new Exception("map is empty");
        cmdMap.get(type).onExecTask(map);
    }

    boolean isCancelled() {
        return cancel;
    }

    private boolean getConnection(int timeout, int countConnect) {
        //int count = 0;
        while (!cancel && countConnect != 0) {
            mContext.sendBroadcast(new Intent(Internet.INTERNET_CONNECT));
            try { Thread.sleep(timeout); } catch (InterruptedException ignored) { }

            if (Internet.isOnline())
                return true;
            countConnect--;
        }
        return false;
    }

    //==================================================================================================================
    public class CheckTokDiskSheet implements InterfaceTaskCommand {
        private final GoogleSpreadsheets googleSpreadsheets;
        final String MAP_CHECKS_SEND = "send";
        final String MAP_CHECKS_UNSEND = "unsend";
        final Map<String, ArrayList<ObjParcel>> mapChecks = new HashMap<>();

        CheckTokDiskSheet() throws RuntimeException {
            try {
                googleSpreadsheets = new GoogleSpreadsheets(ScaleModule.getUserName(), ScaleModule.getPassword(), ScaleModule.getSpreadSheet(), Main.versionName);
            } catch (RuntimeException ignored) {
                mHandler.handleError(401, ignored.getMessage());
                throw new RuntimeException(ignored);
            }
            mapChecks.put(MAP_CHECKS_SEND, new ArrayList<ObjParcel>());
            mapChecks.put(MAP_CHECKS_UNSEND, new ArrayList<ObjParcel>());
        }

        @Override
        public void onExecTask(final Map<String, ContentValues> map) {

            new Thread(new Runnable() {
                @Override
                public void run() {

                    if (!getConnection(10000, 10)) {
                        mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
                        return;
                    }
                    //NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);
                    try {
                        googleSpreadsheets.login();
                        googleSpreadsheets.getSheetEntry(ScaleModule.getSpreadSheet());
                        googleSpreadsheets.UpdateListWorksheets();

                        for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                            int taskId = Integer.valueOf(entry.getKey());
                            int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                            Message msg;
                            try {
                                sendCheckToDisk(checkId);
                                mapChecks.get(MAP_CHECKS_SEND).add(new ObjParcel(checkId, mContext.getString(R.string.sent_to_the_server)));
                                msg = mHandler.obtainMessage(HANDLER_NOTIFY_SHEET, checkId, taskId, mapChecks.get(MAP_CHECKS_SEND));
                            } catch (Exception e) {
                                mapChecks.get(MAP_CHECKS_UNSEND).add(new ObjParcel(checkId, "Не отправлен " + e.getMessage()));
                                msg = mHandler.obtainMessage(HANDLER_NOTIFY_CHECK_UNSEND, checkId, taskId, mapChecks.get(MAP_CHECKS_UNSEND));
                                mHandler.handleError(401, e.getMessage());
                            }
                            mHandler.sendMessage(msg);
                        }
                    } catch (Exception e) {
                        mHandler.handleNotificationError(HANDLER_NOTIFY_ERROR, 505, new MsgNotify(MsgNotify.ID_NOTIFY_NO_SHEET, e.getMessage()));
                    }
                    mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
                }
            }).start();

        }

        private void sendCheckToDisk(int id) throws Exception {
            Cursor cursor = checkTable.getEntryItem(id);
            if (cursor == null)
                throw new Exception(mContext.getString(R.string.Check_N) + id + " null");

            if (cursor.moveToFirst()) {
                googleSpreadsheets.addRow(cursor, CheckTable.TABLE);
                checkTable.updateEntry(id, CheckTable.KEY_CHECK_ON_SERVER, 1);
            }
            cursor.close();
        }

    }

    public class CheckTokHttpPost implements InterfaceTaskCommand {

        final String MAP_CHECKS_SEND = "send";
        final String MAP_CHECKS_UNSEND = "unsend";
        final Map<String, ArrayList<ObjParcel>> mapChecks = new HashMap<>();

        {
            mapChecks.put(MAP_CHECKS_SEND, new ArrayList<ObjParcel>());
            mapChecks.put(MAP_CHECKS_UNSEND, new ArrayList<ObjParcel>());
        }

        @Override
        public void onExecTask(final Map<String, ContentValues> map) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!getConnection(10000, 10)) {
                        mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
                        return;
                    }
                    for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                        int taskId = Integer.valueOf(entry.getKey());
                        int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                        int senderId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_ID_DATA).toString());
                        Cursor sender = new SenderTable(mContext).getEntryItem(senderId);
                        String http = sender.getString(sender.getColumnIndex(SenderTable.KEY_DATA1));
                        String[] values = sender.getString(sender.getColumnIndex(SenderTable.KEY_DATA2)).split(" ");
                        Cursor check = checkTable.getEntryItem(checkId);
                        List<BasicNameValuePair> results = new ArrayList<>();
                        for (String postName : values) {
                            String[] pair = postName.split("=");
                            try {
                                results.add(new BasicNameValuePair(pair[0], check.getString(check.getColumnIndex(pair[1]))));
                            } catch (Exception e) { }
                        }
                        Message msg;
                        try {
                            submitData(http, results);
                            mapChecks.get(MAP_CHECKS_SEND).add(new ObjParcel(checkId, mContext.getString(R.string.sent_to_the_server)));
                            msg = mHandler.obtainMessage(HANDLER_NOTIFY_HTTP, checkId, taskId, mapChecks.get(MAP_CHECKS_SEND));
                        } catch (Exception e) {
                            mapChecks.get(MAP_CHECKS_UNSEND).add(new ObjParcel(checkId, "Не отправлен " + e.getMessage()));
                            msg = mHandler.obtainMessage(HANDLER_NOTIFY_CHECK_UNSEND, checkId, taskId, mapChecks.get(MAP_CHECKS_UNSEND));
                            mHandler.handleError(401, e.getMessage());
                        }
                        mHandler.sendMessage(msg);
                        sender.close();
                        check.close();
                    }
                    mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
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

    public class CheckToMail implements InterfaceTaskCommand {

        final String MAP_CHECKS_SEND = "send";
        final String MAP_CHECKS_UNSEND = "unsend";
        final Map<String, ArrayList<ObjParcel>> mapChecks = new HashMap<>();

        {
            mapChecks.put(MAP_CHECKS_SEND, new ArrayList<ObjParcel>());
            mapChecks.put(MAP_CHECKS_UNSEND, new ArrayList<ObjParcel>());
        }

        @Override
        public void onExecTask(final Map<String, ContentValues> map) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!getConnection(10000, 10)) {
                        mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
                        return;
                    }
                    for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                        int taskId = Integer.valueOf(entry.getKey());
                        int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                        String address = entry.getValue().get(TaskTable.KEY_DATA1).toString();
                        StringBuilder body = new StringBuilder(mContext.getString(R.string.WEIGHT_CHECK_N) + checkId + '\n' + '\n');
                        Cursor check = checkTable.getEntryItem(checkId);
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

                        Message msg;
                        try {
                            MailSend mail = new MailSend(mContext.getApplicationContext(), address, mContext.getString(R.string.Check_N) + checkId, body.toString());
                            mail.sendMail();
                            mapChecks.get(MAP_CHECKS_SEND).add(new ObjParcel(checkId, mContext.getString(R.string.Send_to_mail) + ": " + address));
                            msg = mHandler.obtainMessage(HANDLER_NOTIFY_MAIL, checkId, taskId, mapChecks.get(MAP_CHECKS_SEND));
                        } catch (MessagingException e) {
                            mapChecks.get(MAP_CHECKS_UNSEND).add(new ObjParcel(checkId, "Не отправлен " + e.getMessage() + ' ' + address));
                            msg = mHandler.obtainMessage(HANDLER_NOTIFY_CHECK_UNSEND, checkId, taskId, mapChecks.get(MAP_CHECKS_UNSEND));
                            mHandler.handleError(401, e.getMessage());
                        } catch (UnsupportedEncodingException e) {
                            continue;
                        }
                        mHandler.sendMessage(msg);

                    }
                    mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
                }
            }).start();
        }
    }

    public class CheckToSms implements InterfaceTaskCommand {

        final String MAP_CHECKS_SEND = "send";
        final String MAP_CHECKS_UNSEND = "unsend";
        final Map<String, ArrayList<ObjParcel>> mapChecks = new HashMap<>();

        {
            mapChecks.put(MAP_CHECKS_SEND, new ArrayList<ObjParcel>());
            mapChecks.put(MAP_CHECKS_UNSEND, new ArrayList<ObjParcel>());
        }

        @Override
        public void onExecTask(final Map<String, ContentValues> map) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                        int taskId = Integer.valueOf(entry.getKey());
                        int checkId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                        String address = entry.getValue().get(TaskTable.KEY_DATA1).toString();
                        StringBuilder body = new StringBuilder(mContext.getString(R.string.WEIGHT_CHECK_N) + checkId + '\n' + '\n');
                        Cursor check = checkTable.getEntryItem(checkId);
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
                            mapChecks.get(MAP_CHECKS_SEND).add(new ObjParcel(checkId, mContext.getString(R.string.Send_to_phone) + ": " + address));
                            msg = mHandler.obtainMessage(HANDLER_NOTIFY_MESSAGE, checkId, taskId, mapChecks.get(MAP_CHECKS_SEND));
                        } catch (Exception e) {
                            mapChecks.get(MAP_CHECKS_UNSEND).add(new ObjParcel(checkId, "Не отправлен " + e.getMessage() + ' ' + address));
                            msg = mHandler.obtainMessage(HANDLER_NOTIFY_CHECK_UNSEND, checkId, taskId, mapChecks.get(MAP_CHECKS_UNSEND));
                            mHandler.handleError(401, e.getMessage());
                        }
                        mHandler.sendMessage(msg);
                    }
                    mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
                }
            }).start();
        }
    }

    public class PrefToDiskSheet implements InterfaceTaskCommand {
        private GoogleSpreadsheets googleSpreadsheets;
        final String MAP_PREF_SEND = "send";
        final String MAP_PREF_UNSEND = "unsend";
        final Map<String, ArrayList<ObjParcel>> mapPrefs = new HashMap<>();

        {
            mapPrefs.put(MAP_PREF_SEND, new ArrayList<ObjParcel>());
            mapPrefs.put(MAP_PREF_UNSEND, new ArrayList<ObjParcel>());
        }

        PrefToDiskSheet() {
            try {
                googleSpreadsheets = new GoogleSpreadsheets(ScaleModule.getUserName(), ScaleModule.getPassword(), ScaleModule.getSpreadSheet(), Main.versionName);
            } catch (RuntimeException ignored) {
                mHandler.handleError(501, ignored.getMessage());
            }
        }

        @Override
        public void onExecTask(final Map<String, ContentValues> map) {

            new Thread(new Runnable() {
                @Override
                public void run() {

                    if (!getConnection(10000, 10)) {
                        mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
                        return;
                    }
                    //NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);
                    try {
                        googleSpreadsheets.login();
                        googleSpreadsheets.getSheetEntry(ScaleModule.getSpreadSheet());
                        googleSpreadsheets.UpdateListWorksheets();

                        Message msg = new Message();
                        for (Map.Entry<String, ContentValues> entry : map.entrySet()) {
                            int taskId = Integer.valueOf(entry.getKey());
                            int prefId = Integer.valueOf(entry.getValue().get(TaskTable.KEY_DOC).toString());
                            ObjParcel objParcel = new ObjParcel(prefId, mContext.getString(R.string.sent_to_the_server));
                            try {
                                sendPreferenceToDisk(prefId);
                                mapPrefs.get(MAP_PREF_SEND).add(objParcel);
                                msg = mHandler.obtainMessage(HANDLER_NOTIFY_PREF, prefId, taskId, mapPrefs.get(MAP_PREF_SEND));
                            } catch (Exception e) {
                                mHandler.handleNotificationError(HANDLER_NOTIFY_ERROR, 401, new MsgNotify(MsgNotify.ID_NOTIFY_NO_SHEET, "Настройки не отправлены " + e.getMessage()));
                            }
                            mHandler.sendMessage(msg);
                        }
                    } catch (Exception e) {
                        mHandler.handleNotificationError(HANDLER_NOTIFY_ERROR, 505, new MsgNotify(MsgNotify.ID_NOTIFY_NO_SHEET, e.getMessage()));
                    }
                    mHandler.sendEmptyMessage(HANDLER_FINISH_THREAD);
                }
            }).start();
        }

        private void sendPreferenceToDisk(int id) throws Exception {
            Cursor cursor = new PreferencesTable(mContext).getEntryItem(id);
            if (cursor == null) {
                throw new Exception(mContext.getString(R.string.Check_N) + id + " null");
            }
            if (cursor.moveToFirst()) {
                googleSpreadsheets.addRow(cursor, PreferencesTable.TABLE);
                new PreferencesTable(mContext).removeEntry(id);
            }
            cursor.close();
        }
    }

    public static class MsgNotify {
        int notifyId;
        final String message;

        public static final int ID_NOTIFY_SERVICE = 1;
        public static final int ID_NOTIFY_CLOUD = 2;
        public static final int ID_NOTIFY_MAIL = 3;
        public static final int ID_NOTIFY_MESSAGE = 4;
        public static final int ID_NOTIFY_NO_SHEET = 5;


        MsgNotify(int id, String message) {
            notifyId = id;
            this.message = message;
        }


        public int getNotifyId() {
            return notifyId;
        }
        //public int getIcon(){            return icon;        }

        //public String getTitle(){            return title;        }
        public String getMessage() {
            return message;
        }

        void setNotifyId(int id) {
            notifyId = id;
        }
        //void setIntent(Intent intent){        this.intent = intent;       }
        //public Intent getIntent(){         return intent;       }
        //public  NotificationCompat.Builder getBundle(){         return mBuilder;        }
        //public ArrayList<ObjParcel> getListObj(){        return listObj;       }
        //public int getArg1(){        return arg1;     }
    }

    public static class ObjParcel implements Parcelable {

        private String strValue;
        private Integer intValue;

        public ObjParcel(Integer value, String str) {
            intValue = value;
            strValue = str;
        }

        public ObjParcel(Parcel in) {
            readFromParcel(in);
        }

        public String getStrValue() {
            return strValue;
        }

        public void setStrValue(String strValue) {
            this.strValue = strValue;
        }

        public Integer getIntValue() {
            return intValue;
        }

        public void setIntValue(Integer intValue) {
            this.intValue = intValue;
        }

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
            public ObjParcel createFromParcel(Parcel in) {
                return new ObjParcel(in);
            }

            @Override
            public ObjParcel[] newArray(int size) {
                return new ObjParcel[size];
            }
        };

    }


}
