package com.victjava.scales.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.victjava.scales.TaskCommand;

//import android.content.res.Resources;
//import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 24.09.13
 * Time: 12:27
 * To change this template use File | Settings | File Templates.
 */
public class TaskTable {
    private final Context context;
    public static final String TABLE = "taskTable";

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_MIME_TYPE = "mime_type";
    public static final String KEY_DOC = "id_doc";
    public static final String KEY_ID_DATA = "id_contact";
    public static final String KEY_DATA1 = "data1";
    public static final String KEY_NUM_ERROR = "num_error";

    private final int COUNT_ERROR = 5;

    /*public static final int TYPE_CHECK_MAIL_CONTACT = 1;    //для електронной почты
    public static final int TYPE_CHECK_DISK         = 2;    //для google disk
    public static final int TYPE_PREF_DISK          = 3;    //для google disk
    public static final int TYPE_CHECK_SMS_CONTACT  = 4;    //для смс отправки контакту
    public static final int TYPE_CHECK_SMS_SERVER   = 5;    //для смс отправки боссу*/

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
        context = cnt;
    }

    public Uri insertNewTask(TaskCommand.TaskType mimeType, long documentId, long dataId, String data1) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_MIME_TYPE, mimeType.ordinal());
        newTaskValues.put(KEY_DOC, documentId);
        newTaskValues.put(KEY_ID_DATA, dataId);
        newTaskValues.put(KEY_DATA1, data1);
        newTaskValues.put(KEY_NUM_ERROR, 0);
        return context.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public boolean isTaskReady() {
        try {
            boolean flag = false;
            Cursor result = context.getContentResolver().query(CONTENT_URI, null, null, null, null);
            if (result.getCount() > 0) {
                flag = true;
            }
            result.close();
            return flag;
        }catch (Exception e){return  false;}
    }

    public Cursor getAllEntries() {
        return context.getContentResolver().query(CONTENT_URI, null, null, null, null);
    }

    public int getKeyInt(int _rowIndex, String key) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = context.getContentResolver().query(uri, new String[]{KEY_ID, key}, null, null, null);
            result.moveToFirst();
            return result.getInt(result.getColumnIndex(key));
        } catch (Exception e) {
            return Integer.parseInt(null);
        }
    }

    public boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

    public boolean updateEntry(int _rowIndex, String key, int in) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(key, in);
            return context.getContentResolver().update(uri, newValues, null, null) > 0;
        }catch (Exception e){return false;}
    }

    public boolean removeEntryIfErrorOver(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        int err = getKeyInt(_rowIndex, KEY_NUM_ERROR);
        if (err++ < COUNT_ERROR) {
            return updateEntry(_rowIndex, KEY_NUM_ERROR, err);
        }
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

    public Cursor getTypeEntry(TaskCommand.TaskType type) {
        return context.getContentResolver().query(CONTENT_URI, null, KEY_MIME_TYPE + " = " + type.ordinal(), null, null);
    }

}
