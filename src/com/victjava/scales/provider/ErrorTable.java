package com.victjava.scales.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.telephony.TelephonyManager;
import com.konst.module.scale.ScaleModule;
import com.victjava.scales.Globals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ErrorTable {

    private final Context context;
    final ScaleModule scaleModule;

    public static final String TABLE = "errorTable";

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_DATE_CREATE = "dateCreate";
    public static final String KEY_NUMBER_ERROR = "numberError";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_NUMBER_BT = "bluetooth";

    private static final String[] All_COLUMN_TABLE = {KEY_ID, KEY_DATE_CREATE, KEY_NUMBER_ERROR, KEY_DESCRIPTION, KEY_NUMBER_BT};

    public static final String TABLE_CREATE = "create table "
            + TABLE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_DATE_CREATE + " text,"
            + KEY_NUMBER_ERROR + " text,"
            + KEY_DESCRIPTION + " text,"
            + KEY_NUMBER_BT + " text );";

    private static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE);

    public ErrorTable(Context cnt) {
        context = cnt;
        scaleModule = Globals.getInstance().getScaleModule();
    }

    public Uri insertNewEntry(String number, String des) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.getDefault());
        ContentValues newTaskValues = new ContentValues();
        TelephonyManager mngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //mngr.getDeviceId();
        String bt_number = mngr.getDeviceId();
        newTaskValues.put(KEY_DATE_CREATE, sdf.format(date));
        if (bt_number != null) {
            newTaskValues.put(KEY_NUMBER_BT, bt_number /*+ ' ' + scaleModule.getAddressBluetoothDevice()*/);
        }
        newTaskValues.put(KEY_NUMBER_ERROR, number);
        newTaskValues.put(KEY_DESCRIPTION, des);
        return context.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public boolean removeEntry(long _rowIndex) {
        return context.getContentResolver().delete(ContentUris.withAppendedId(CONTENT_URI, _rowIndex), null, null) > 0;
    }

    public boolean deleteRows(int count) {
        try {
            Cursor cursor = context.getContentResolver().query(CONTENT_URI, new String[]{KEY_ID, KEY_NUMBER_ERROR}, null, null, KEY_ID + " DESC " + " LIMIT " + count);
            boolean flag = false;
            if (cursor.moveToFirst()) {
                if (!cursor.isAfterLast()) {
                    do {
                        int id = cursor.getInt(cursor.getColumnIndex(KEY_ID));
                        removeEntry(id);
                    } while (cursor.moveToNext());
                    flag = true;
                }
            }
            cursor.close();
            return flag;
        } catch (Exception e) {
            return false;
        }
    }

    public int deleteAll() {
        return context.getContentResolver().delete(CONTENT_URI, null, null);
    }

    /*public Cursor getAllEntries() {
        return context.getContentResolver().query(CONTENT_URI, All_COLUMN_TABLE, null, null, null);
    }*/

    /*public Cursor getErrorCodeCounts(int count) {
        return context.getContentResolver().query(CONTENT_URI, new String[]{KEY_ID, KEY_NUMBER_ERROR}, null, null, KEY_ID + " DESC " + " LIMIT " + count);
    }*/

    public String getErrorToString(int count) {
        try {
            Cursor cursor = context.getContentResolver().query(CONTENT_URI, null, null, null, KEY_ID + " DESC " + " LIMIT " + count);
            StringBuilder stringBuilder = new StringBuilder("");
            if (cursor.moveToFirst()) {
                if (!cursor.isAfterLast()) {
                    do {
                        stringBuilder.append(cursor.getString(cursor.getColumnIndex(KEY_DATE_CREATE))).append('_');
                        stringBuilder.append(cursor.getString(cursor.getColumnIndex(KEY_NUMBER_ERROR))).append('|');
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
            return stringBuilder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /*public boolean updateEntry(long _rowIndex, String key, int in) {
        //boolean b;
        ContentValues newValues = new ContentValues();
        newValues.put(key, in);
        return context.getContentResolver().update(ContentUris.withAppendedId(CONTENT_URI, _rowIndex), newValues, null, null) > 0;
    }*/

   /* public boolean updateEntry(long _rowIndex, String key, float fl) {
        ContentValues newValues = new ContentValues();
        newValues.put(key, fl);
        return context.getContentResolver().update(ContentUris.withAppendedId(CONTENT_URI, _rowIndex), newValues, null, null) > 0;
    }*/

    /*public boolean updateEntry(long _rowIndex, String key, String st) {
        ContentValues newValues = new ContentValues();
        newValues.put(key, st);
        return context.getContentResolver().update(ContentUris.withAppendedId(CONTENT_URI, _rowIndex), newValues, null, null) > 0;
    }*/

}
