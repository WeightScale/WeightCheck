package com.victjava.scales.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.konst.module.ScaleModule;
import com.victjava.scales.Main;
import com.victjava.scales.TaskCommand;

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

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_MIME_TYPE = "mime_type";
    public static final String KEY_DOC = "id_doc";
    public static final String KEY_ID_DATA = "id_contact";
    public static final String KEY_DATA1 = "data1";
    public static final String KEY_NUM_ERROR = "num_error";

    private final int COUNT_ERROR = 5;

    public static final String TABLE_CREATE = "create table "
            + TABLE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_MIME_TYPE + " integer,"
            + KEY_DOC + " integer,"
            + KEY_ID_DATA + " integer,"
            + KEY_DATA1 + " text,"
            + KEY_NUM_ERROR + " integer );";

    public static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE);

    public TaskTable(Context cnt) {
        mContext = cnt;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
    }

    public Uri insertNewTask(TaskCommand.TaskType mimeType, long documentId, long dataId, String data1) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_MIME_TYPE, mimeType.ordinal());
        newTaskValues.put(KEY_DOC, documentId);
        newTaskValues.put(KEY_ID_DATA, dataId);
        newTaskValues.put(KEY_DATA1, data1);
        newTaskValues.put(KEY_NUM_ERROR, 0);
        return mContext.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public boolean isTaskReady() {
        try {
            boolean flag = false;
            Cursor result = mContext.getContentResolver().query(CONTENT_URI, null, null, null, null);
            if (result.getCount() > 0) {
                flag = true;
            }
            result.close();
            return flag;
        } catch (Exception e) {
            return false;
        }
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

    public Cursor getTypeEntry(TaskCommand.TaskType type) {
        return mContext.getContentResolver().query(CONTENT_URI, null, KEY_MIME_TYPE + " = " + type.ordinal(), null, null);
    }

    public void setCheckReady(int _rowIndex) {
        Cursor cursor = new SenderTable(mContext).geSystemItem();
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    int senderId = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_ID));
                    SenderTable.TypeSender type_sender = SenderTable.TypeSender.values()[cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_TYPE))];
                    switch (type_sender) {
                        case TYPE_HTTP_POST:
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_HTTP_POST, _rowIndex, senderId, "");
                            break;
                        case TYPE_GOOGLE_DISK:
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_SHEET_DISK, _rowIndex, senderId, "");
                            break;
                        case TYPE_EMAIL:
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_MAIL, _rowIndex, senderId, scaleModule.getUserName());
                            break;
                        case TYPE_SMS:
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_SMS_ADMIN, _rowIndex, senderId, scaleModule.getPhone());
                            break;
                        default:
                    }
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {

        }
    }

    public void setPreferenceReady(int _rowIndex) {
        Cursor cursor = new SenderTable(mContext).geSystemItem();
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    int senderId = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_ID));
                    SenderTable.TypeSender type_sender = SenderTable.TypeSender.values()[cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_TYPE))];
                    switch (type_sender) {
                        /*case TYPE_HTTP_POST:
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_HTTP_POST, _rowIndex, senderId, "");
                            break;*/
                        case TYPE_GOOGLE_DISK:
                            insertNewTask(TaskCommand.TaskType.TYPE_PREF_SEND_SHEET_DISK, _rowIndex, senderId, "preferences");
                            break;
                        /*case TYPE_EMAIL:
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_MAIL, _rowIndex, senderId, scaleModule.getUserName());
                            break;*/
                        /*case TYPE_SMS:
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_SMS_ADMIN, _rowIndex, senderId, scaleModule.getPhone());
                            break;*/
                        default:
                    }
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {

        }
    }

}
