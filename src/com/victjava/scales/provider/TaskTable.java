package com.victjava.scales.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.konst.module.ScaleModule;
import com.victjava.scales.Main;
import com.victjava.scales.TaskCommand.*;
import com.victjava.scales.provider.SenderTable.TypeSender;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 24.09.13
 * Time: 12:27
 * To change this template use File | Settings | File Templates.
 */
public class TaskTable {
    private final Context mContext;
    final ScaleModule scaleModule;
    public static final String TABLE = "taskTable";

    public static final String KEY_ID           = BaseColumns._ID;
    public static final String KEY_MIME_TYPE    = "mime_type";
    public static final String KEY_DOC          = "id_doc";
    public static final String KEY_ID_DATA      = "id_contact";
    public static final String KEY_DATA0        = "data0";
    public static final String KEY_DATA1        = "data1";
    public static final String KEY_DATA2        = "data2";
    public static final String KEY_DATA3        = "data3";
    public static final String KEY_DATA4        = "data4";
    public static final String KEY_NUM_ERROR    = "num_error";

    private final int COUNT_ERROR = 5;

    public static final String TABLE_CREATE = "create table "
            + TABLE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_MIME_TYPE + " integer,"
            + KEY_DOC + " integer,"
            + KEY_ID_DATA + " integer,"
            + KEY_DATA0 + " text,"
            + KEY_DATA1 + " text,"
            + KEY_DATA2 + " text,"
            + KEY_DATA3 + " text,"
            + KEY_DATA4 + " text,"
            + KEY_NUM_ERROR + " integer );";

    public static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE);

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
        TYPE_CHECK_SEND_SMS_ADMIN,
        /** фото чека на google disk. */
        TYPE_DATA_SEND_TO_DISK
    }

    public TaskTable(Context cnt) {
        mContext = cnt;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
    }

    public Uri insertNewTask(TaskType mimeType, long documentId, long dataId, String ... data) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_MIME_TYPE, mimeType.ordinal());
        newTaskValues.put(KEY_DOC, documentId);
        newTaskValues.put(KEY_ID_DATA, dataId);
        if(data.length < 4){
            for (int i=0; i < data.length; i++){
                newTaskValues.put("data"+i, data[i]);
            }
        }else {
            for (int i=0; i < 3; i++){
                newTaskValues.put("data"+i, data[i]);
            }
        }
        newTaskValues.put(KEY_NUM_ERROR, 0);
        return mContext.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public Uri insertNewTask(TaskType mimeType, long documentId, String ... data) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_MIME_TYPE, mimeType.ordinal());
        newTaskValues.put(KEY_DOC, documentId);
        if(data.length < 4){
            for (int i=0; i < data.length; i++){
                newTaskValues.put("data"+i, data[i]);
            }
        }else {
            for (int i=0; i < 3; i++){
                newTaskValues.put("data"+i, data[i]);
            }
        }
        newTaskValues.put(KEY_NUM_ERROR, 0);
        return mContext.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public Uri insertNewTaskPhone(TaskType mimeType, long documentId, long dataId, String phone) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_MIME_TYPE, mimeType.ordinal());
        newTaskValues.put(KEY_DOC, documentId);
        newTaskValues.put(KEY_ID_DATA, dataId);
        newTaskValues.put(KEY_DATA0, phone);
        newTaskValues.put(KEY_NUM_ERROR, 0);
        return mContext.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public Uri insertNewTaskEmail(TaskType mimeType, long documentId, long dataId, String address) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_MIME_TYPE, mimeType.ordinal());
        newTaskValues.put(KEY_DOC, documentId);
        newTaskValues.put(KEY_ID_DATA, dataId);
        newTaskValues.put(KEY_DATA0, address);
        newTaskValues.put(KEY_DATA1, getUser());
        newTaskValues.put(KEY_DATA2, getPassword());
        newTaskValues.put(KEY_NUM_ERROR, 0);
        return mContext.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public Cursor getAllEntries() {
        return mContext.getContentResolver().query(CONTENT_URI, null, null, null, null);
    }

    public int getKeyInt(int _rowIndex, String key) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = mContext.getContentResolver().query(uri, new String[]{KEY_ID, key}, null, null, null);
            result.moveToFirst();
            return result.getInt(result.getColumnIndex(key));
        } catch (Exception e) {
            return Integer.parseInt(null);
        }
    }

    public boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        return uri != null && mContext.getContentResolver().delete(uri, null, null) > 0;
    }

    public boolean updateEntry(int _rowIndex, String key, int in) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(key, in);
            return mContext.getContentResolver().update(uri, newValues, null, null) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeEntryIfErrorOver(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        int err = getKeyInt(_rowIndex, KEY_NUM_ERROR);
        if (err++ < COUNT_ERROR) {
            return updateEntry(_rowIndex, KEY_NUM_ERROR, err);
        }
        return uri != null && mContext.getContentResolver().delete(uri, null, null) > 0;
    }

    public Cursor getTypeEntry(TaskType type) {
        return mContext.getContentResolver().query(CONTENT_URI, null, KEY_MIME_TYPE + " = " + type.ordinal(), null, null);
    }

    public void setCheckReady(int _rowIndex, ContentValues values) {

        Cursor checks = new CheckTable(mContext).getEntryItem(_rowIndex, CheckTable.KEY_PHOTO_FIRST, CheckTable.KEY_PHOTO_SECOND);
        checks.moveToFirst();
        if(checks.isNull(checks.getColumnIndex(CheckTable.KEY_PHOTO_FIRST)) && checks.isNull(checks.getColumnIndex(CheckTable.KEY_PHOTO_SECOND))){
            new CheckTable(mContext).updateEntry(_rowIndex, CheckTable.KEY_CHECK_STATE, CheckTable.State.CHECK_READY.ordinal());
        }else {
            setDataReady(_rowIndex, values);
        }

        Cursor cursor = new SenderTable(mContext).geSystemItem();
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    int senderId = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_ID));
                    TypeSender type_sender = TypeSender.values()[cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_TYPE))];
                    switch (type_sender) {
                        case TYPE_HTTP_POST:
                            insertNewTask(TaskType.TYPE_CHECK_SEND_HTTP_POST, _rowIndex, senderId, "forms/disk.xml", "WeightCheckForm", getUser());
                        break;
                        case TYPE_GOOGLE_DISK:
                            insertNewTask(TaskType.TYPE_CHECK_SEND_SHEET_DISK, _rowIndex, senderId, getSpreadSheet(), getUser(), getPassword());
                        break;
                        case TYPE_EMAIL:
                            insertNewTaskEmail(TaskType.TYPE_CHECK_SEND_MAIL, _rowIndex, senderId, getUser());
                        break;
                        case TYPE_SMS:
                            insertNewTaskPhone(TaskType.TYPE_CHECK_SEND_SMS_ADMIN, _rowIndex, senderId, scaleModule.getPhone());
                        break;
                        default:
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {}
    }

    public void setPreferenceReady(int _rowIndex) {
        Cursor cursor = new SenderTable(mContext).geSystemItem();
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    int senderId = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_ID));
                    TypeSender type_sender = TypeSender.values()[cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_TYPE))];
                    switch (type_sender) {
                        case TYPE_HTTP_POST:
                            insertNewTask(TaskType.TYPE_PREF_SEND_HTTP_POST, _rowIndex, senderId, "forms/disk.xml", "SettingsForm");
                        break;
                        case TYPE_GOOGLE_DISK:
                            insertNewTask(TaskType.TYPE_PREF_SEND_SHEET_DISK, _rowIndex, senderId, getSpreadSheet(), getUser(), getPassword());
                        break;
                        default:
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {}
    }

    public void setDataReady(int _rowIndex, ContentValues values){
        Cursor cursor = new SenderTable(mContext).geSystemItem();
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    //int senderId = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_ID));
                    //TypeSender type_sender = TypeSender.values()[cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_TYPE))];
                    switch (TypeSender.values()[cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_TYPE))]) {
                        case TYPE_HTTP_POST:
                        case TYPE_GOOGLE_DISK:
                            if(values.containsKey(CheckTable.KEY_PHOTO_FIRST))
                                if (values.get(CheckTable.KEY_PHOTO_FIRST)!= null)
                                    insertNewTask(TaskType.TYPE_DATA_SEND_TO_DISK, _rowIndex, values.getAsString(CheckTable.KEY_PHOTO_FIRST),CheckTable.KEY_PHOTO_FIRST,scaleModule.getAddressBluetoothDevice());
                            if (values.containsKey(CheckTable.KEY_PHOTO_SECOND))
                                if (values.get(CheckTable.KEY_PHOTO_SECOND)!=null)
                                    insertNewTask(TaskType.TYPE_DATA_SEND_TO_DISK, _rowIndex, values.getAsString(CheckTable.KEY_PHOTO_SECOND),CheckTable.KEY_PHOTO_SECOND,scaleModule.getAddressBluetoothDevice());
                        return;
                        default:
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {}
    }

    public String getUser(){
        return scaleModule.getUserName();
    }

    public String getPassword(){
        return scaleModule.getPassword();
    }

    public String getSpreadSheet(){
        return scaleModule.getSpreadSheet();
    }

}
