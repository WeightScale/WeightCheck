package com.victjava.scales.provider;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import com.konst.module.ScaleModule;
import com.victjava.scales.Main;
import com.victjava.scales.R;
import org.apache.http.message.BasicNameValuePair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CheckTable {
    final ScaleModule scaleModule;
    private final Context mContext;
    final ContentResolver contentResolver;

    public static final String TABLE = "checkTable";

    public static final String KEY_ID               = BaseColumns._ID;
    public static final String KEY_DATE_CREATE      = "dateCreate";
    public static final String KEY_TIME_CREATE      = "timeCreate";
    public static final String KEY_NUMBER_BT        = "numberBt";
    public static final String KEY_WEIGHT_FIRST     = "weightFirst";
    public static final String KEY_WEIGHT_SECOND    = "weightSecond";
    public static final String KEY_WEIGHT_NETTO     = "weightNetto";
    public static final String KEY_VENDOR           = "vendor";
    public static final String KEY_VENDOR_ID        = "vendorId";
    public static final String KEY_TYPE             = "type";
    public static final String KEY_TYPE_ID          = "typeId";
    public static final String KEY_PRICE            = "price";
    public static final String KEY_PRICE_SUM        = "priceSum";
    //public static final String KEY_CHECK_ON_SERVER  = "checkOnServer";
    public static final String KEY_CHECK_STATE      = "state";
    //public static final String KEY_IS_READY         = "checkIsReady";
    public static final String KEY_VISIBILITY       = "visibility";
    public static final String KEY_DIRECT           = "direct";
    public static final String KEY_PHOTO_FIRST      = "photoFirst";
    public static final String KEY_PHOTO_SECOND     = "photoSecond";
    public static final String KEY_WEB_FIRST        = "webFirst";
    public static final String KEY_WEB_SECOND       = "webSecond";

    public static final int INVISIBLE = 0;
    public static final int VISIBLE = 1;

    public enum State{
        CHECK_FIRST,
        CHECK_SECOND,
        /*CHECK_UNCLOSED,*/
        CHECK_PRELIMINARY,
        CHECK_READY,
        CHECK_ON_SERVER
    }

    public static final int DIRECT_DOWN = R.drawable.ic_action_down;
    public static final int DIRECT_UP = R.drawable.ic_action_up;

    private static final String[] All_COLUMN_TABLE = {
            KEY_ID,
            KEY_DATE_CREATE,
            KEY_TIME_CREATE,
            KEY_NUMBER_BT,
            KEY_WEIGHT_FIRST,
            KEY_WEIGHT_SECOND,
            KEY_WEIGHT_NETTO,
            KEY_VENDOR,
            KEY_VENDOR_ID,
            KEY_TYPE,
            KEY_TYPE_ID,
            KEY_PRICE,
            KEY_PRICE_SUM,
            //KEY_CHECK_ON_SERVER,
            KEY_CHECK_STATE,
            //KEY_IS_READY,
            KEY_VISIBILITY,
            KEY_DIRECT,
            KEY_PHOTO_FIRST,
            KEY_PHOTO_SECOND,
            KEY_WEB_FIRST,
            KEY_WEB_SECOND};

    public static final String[] COLUMNS_SMS_ADMIN = {
            KEY_DATE_CREATE,
            KEY_TIME_CREATE,
            KEY_NUMBER_BT,
            KEY_WEIGHT_FIRST,
            KEY_WEIGHT_SECOND,
            KEY_WEIGHT_NETTO,
            KEY_VENDOR,
            KEY_TYPE,
            //KEY_IS_READY,
            KEY_DIRECT,
            KEY_WEB_FIRST,
            KEY_WEB_SECOND};

    public static final String[] COLUMNS_SMS_CONTACT = {
            KEY_DATE_CREATE,
            KEY_TIME_CREATE,
            KEY_WEIGHT_FIRST,
            KEY_WEIGHT_SECOND,
            KEY_WEIGHT_NETTO,
            KEY_VENDOR,
            KEY_TYPE,
            KEY_PRICE,
            KEY_PRICE_SUM,
            KEY_WEB_FIRST,
            KEY_WEB_SECOND};

    public static final String[] COLUMNS_SHEET = {
            /*KEY_ID,*/
            KEY_DATE_CREATE,
            KEY_TIME_CREATE,
            KEY_NUMBER_BT,
            KEY_WEIGHT_FIRST,
            KEY_WEIGHT_SECOND,
            KEY_WEIGHT_NETTO,
            KEY_VENDOR,
            KEY_TYPE,
            KEY_PRICE,
            KEY_PRICE_SUM,
            //KEY_IS_READY,
            KEY_DIRECT,
            KEY_PHOTO_FIRST,
            KEY_PHOTO_SECOND};

    public static final String TABLE_CREATE = "create table "
            + TABLE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_DATE_CREATE + " text,"
            + KEY_TIME_CREATE + " text,"
            + KEY_NUMBER_BT + " text,"
            + KEY_WEIGHT_FIRST + " integer,"
            + KEY_WEIGHT_SECOND + " integer,"
            + KEY_WEIGHT_NETTO + " integer,"
            + KEY_VENDOR + " text,"
            + KEY_VENDOR_ID + " integer,"
            + KEY_TYPE + " text,"
            + KEY_TYPE_ID + " integer,"
            + KEY_PRICE + " integer,"
            + KEY_PRICE_SUM + " real,"
            //+ KEY_CHECK_ON_SERVER + " integer,"
            + KEY_CHECK_STATE + " integer,"
            //+ KEY_IS_READY + " integer,"
            + KEY_VISIBILITY + " integer,"
            + KEY_DIRECT + " integer,"
            + KEY_PHOTO_FIRST + " text,"
            + KEY_PHOTO_SECOND + " text,"
            + KEY_WEB_FIRST + " text,"
            + KEY_WEB_SECOND + " text );";


    private static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE);

    public CheckTable(Context context) {
        mContext = context;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
        contentResolver = mContext.getContentResolver();
    }

    /*public CheckTable(Context context, int d) {
        mContext = context;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
        contentResolver = mContext.getContentResolver();
        day = d;
    }*/

    public Uri insertNewEntry(String vendor, int vendorId, int direct) {
        ContentValues newTaskValues = new ContentValues();
        Date date = new Date();
        newTaskValues.put(KEY_DATE_CREATE, new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date));
        newTaskValues.put(KEY_TIME_CREATE, new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date));
        newTaskValues.put(KEY_NUMBER_BT, scaleModule.getAddressBluetoothDevice());
        newTaskValues.put(KEY_VENDOR, vendor);
        newTaskValues.put(KEY_VENDOR_ID, vendorId);
        newTaskValues.put(KEY_CHECK_STATE, State.CHECK_FIRST.ordinal());
        //newTaskValues.put(KEY_IS_READY, false);
        newTaskValues.put(KEY_WEIGHT_FIRST, 0);
        newTaskValues.put(KEY_WEIGHT_NETTO, 0);
        newTaskValues.put(KEY_WEIGHT_SECOND, 0);
        newTaskValues.put(KEY_PRICE_SUM, 0);
        newTaskValues.put(KEY_TYPE_ID, 1);
        newTaskValues.put(KEY_VISIBILITY, VISIBLE);
        newTaskValues.put(KEY_DIRECT, direct);
        return contentResolver.insert(CONTENT_URI, newTaskValues);
    }

    void removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        contentResolver.delete(uri, null, null);
    }

    public void deleteCheckIsServer(/*long  dayAfter*/) {
        try {
            Cursor result = contentResolver.query(CONTENT_URI, new String[]{KEY_ID, KEY_DATE_CREATE},
                    KEY_CHECK_STATE + "= " + State.CHECK_ON_SERVER.ordinal() + " and " + KEY_VISIBILITY + "= " + INVISIBLE, null, null);
            if (result.getCount() > 0) {
                result.moveToFirst();
                if (!result.isAfterLast()) {
                    do {
                        int id = result.getInt(result.getColumnIndex(KEY_ID));
                        removeEntry(id);
                    } while (result.moveToNext());
                }
            }
            result.close();
        } catch (Exception e) {
        }
    }

    public void invisibleCheckIsReady(long dayAfter) {
        try {
            Cursor result = contentResolver.query(CONTENT_URI, new String[]{KEY_ID, KEY_DATE_CREATE},
                    KEY_CHECK_STATE + "= " + State.CHECK_FIRST.ordinal()+ " or " + KEY_CHECK_STATE + "= " + State.CHECK_SECOND.ordinal() /*and " + KEY_VISIBILITY + "= " + VISIBLE*/, null, null);
            result.moveToFirst();
            if (!result.isAfterLast()) {
                do {
                    int id = result.getInt(result.getColumnIndex(KEY_ID));
                    String date = result.getString(result.getColumnIndex(KEY_DATE_CREATE));
                    long day = 0;
                    try {
                        day = dayDiff(new Date(), new SimpleDateFormat("dd.MM.yy", Locale.getDefault()).parse(date));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if (day > dayAfter) {
                        updateEntry(id, KEY_VISIBILITY, INVISIBLE);
                    } else {
                        updateEntry(id, KEY_VISIBILITY, VISIBLE);
                    }
                } while (result.moveToNext());
            }
            result.close();
        } catch (Exception e) {
        }
    }

    long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return day1 - day2;
    }

    /*private String getKeyString(int _rowIndex, String key) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = contentResolver.query(uri, new String[]{KEY_ID, key}, null, null, null);
            result.moveToFirst();
            String str = result.getString(result.getColumnIndex(key));
            result.close();
            return str;
        } catch (Exception e) {
            return "";
        }
    }*/

    public Cursor getAllEntries(int view) {
        return contentResolver.query(CONTENT_URI, All_COLUMN_TABLE,
                "( "+KEY_CHECK_STATE + "!= " + State.CHECK_FIRST.ordinal() + " or " + KEY_CHECK_STATE + "!= " + State.CHECK_SECOND.ordinal() + " )"
                        + " and " + KEY_VISIBILITY + "= " + view, null, null);
    }

    public Cursor getUnclosedCheck() {
        //return contentResolver.query(CONTENT_URI, All_COLUMN_TABLE, KEY_CHECK_ON_SERVER + "= 0" + " and " + KEY_IS_READY + "= 0", null, null);
        return contentResolver.query(CONTENT_URI, All_COLUMN_TABLE, KEY_CHECK_STATE + "= " + State.CHECK_FIRST.ordinal() + " or " + KEY_CHECK_STATE + "= " + State.CHECK_SECOND.ordinal(), null, null);
    }

    public Cursor getNotReady() {
        return contentResolver.query(CONTENT_URI, All_COLUMN_TABLE, KEY_CHECK_STATE + "= " + State.CHECK_FIRST.ordinal() + " or " + KEY_CHECK_STATE + "= " + State.CHECK_SECOND.ordinal(), null, null);
    }

    public Cursor getEntryItem(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = contentResolver.query(uri, All_COLUMN_TABLE, null, null, null);
            result.moveToFirst();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public Cursor getEntryItem(int _rowIndex, String... columns) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = contentResolver.query(uri, columns, null, null, null);
            result.moveToFirst();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public ContentValues getValuesItem(int _rowIndex) throws Exception {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = contentResolver.query(uri, All_COLUMN_TABLE, null, null, null);
            result.moveToFirst();
            ContentQueryMap mQueryMap = new ContentQueryMap(result, BaseColumns._ID, true, null);
            Map<String, ContentValues> map = mQueryMap.getRows();
            result.close();
            return map.get(String.valueOf(_rowIndex));
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public boolean updateEntry(int _rowIndex, String key, int in) {
        //boolean b;
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(key, in);
            return contentResolver.update(uri, newValues, null, null) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateEntry(int _rowIndex, String key, String value) {
        //boolean b;
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(key, value);
            return contentResolver.update(uri, newValues, null, null) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateEntry(int _rowIndex, ContentValues values) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            return contentResolver.update(uri, values, null, null) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /*public void updateEntry(int _rowIndex, String key, float fl) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(key, fl);
            contentResolver.update(uri, newValues, null, null);
        } catch (Exception e) {
        }
    }*/

    /*public boolean updateEntry(int _rowIndex, String key, String st) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(key, st);
            return contentResolver.update(uri, newValues, null, null) > 0;
        } catch (Exception e) {
            return false;
        }
    }*/

    /*public static String getGoFormHttp(){
        return GO_FORM_HTTP;
    }*/

    /*public static String geGoParamHttp(){
        Collection<BasicNameValuePair> results = new ArrayList<>();
        results.add(new BasicNameValuePair(GO_DATE_HTTP, KEY_DATE_CREATE));
        results.add(new BasicNameValuePair(GO_BT_HTTP, KEY_NUMBER_BT));
        results.add(new BasicNameValuePair(GO_WEIGHT_HTTP, KEY_WEIGHT_NETTO));
        results.add(new BasicNameValuePair(GO_TYPE_HTTP, KEY_TYPE));
        results.add(new BasicNameValuePair(GO_IS_READY_HTTP, KEY_IS_READY));
        results.add(new BasicNameValuePair(GO_TIME_HTTP, KEY_TIME_CREATE));
        return TextUtils.join(" ", results);
    }*/
}
