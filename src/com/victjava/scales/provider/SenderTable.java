package com.victjava.scales.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Collection;

/*
 * Created by Kostya on 11.04.2015.
 */
public class SenderTable {
    private final Context mContext;
    private final ContentResolver contentResolver;

    public static final String TABLE = "sender";

    static final String GO_FORM_HTTP = "https://docs.google.com/forms/d/11C5mq1Z-Syuw7ScsMlWgSnr9yB4L_eP-NhxnDdohtrw/formResponse"; // Форма движения
    static final String GO_DATE_HTTP = "entry.1974893725";                                  // Дата создания
    static final String GO_BT_HTTP = "entry.1465497317";                                    // Номер весов
    static final String GO_WEIGHT_HTTP = "entry.683315711";                                 // Вес
    static final String GO_TYPE_HTTP = "entry.138748566";                                   // Тип
    static final String GO_IS_READY_HTTP = "entry.1691625234";                              // Готов
    static final String GO_TIME_HTTP = "entry.1280991625";                                  //Время

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_TYPE = "type"; //TYPE_SENDER
    public static final String KEY_DATA1 = "data1";
    public static final String KEY_DATA2 = "data2";
    public static final String KEY_DATA3 = "data3";
    public static final String KEY_SYS = "system"; //  0 или 1

    public enum TypeSender {
        TYPE_GOOGLE_DISK,       //для google disk
        TYPE_HTTP_POST,         //на облако
        TYPE_SMS,               //для смс отправки боссу
        TYPE_EMAIL              //для електронной почты
    }

    public static final String TABLE_CREATE = "create table "
            + TABLE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_TYPE + " integer, "
            + KEY_DATA1 + " text, "
            + KEY_DATA2 + " text, "
            + KEY_DATA3 + " text, "
            + KEY_SYS + " integer );";

    private static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE);

    public SenderTable(Context context) {
        mContext = context;
        contentResolver = mContext.getContentResolver();
    }

    public Cursor getAllEntries() {
        return contentResolver.query(CONTENT_URI, new String[]{KEY_ID, KEY_TYPE}, null, null, null);
    }

    public Cursor getEntryItem(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = contentResolver.query(uri, null, null, null, null);
            result.moveToFirst();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public Cursor geSystemItem() {
        return contentResolver.query(CONTENT_URI, null, KEY_SYS + "= 1", null, null);
    }

    public Cursor getTypeItem(int type) {
        return contentResolver.query(CONTENT_URI, null, KEY_TYPE + "= " + type, null, null);
    }

    public boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        return uri != null && contentResolver.delete(uri, null, null) > 0;
    }

    public boolean updateEntry(int _rowIndex, String key, int in) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(key, in);
            return contentResolver.update(uri, newValues, null, null) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void addSystemHTTP(SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        //Resources res = mContext.getResources();
        contentValues.put(KEY_TYPE, TypeSender.TYPE_HTTP_POST.ordinal());
        contentValues.put(KEY_DATA1, GO_FORM_HTTP);

        Collection<BasicNameValuePair> results = new ArrayList<>();
        results.add(new BasicNameValuePair(GO_DATE_HTTP, CheckTable.KEY_DATE_CREATE));
        results.add(new BasicNameValuePair(GO_BT_HTTP, CheckTable.KEY_NUMBER_BT));
        results.add(new BasicNameValuePair(GO_WEIGHT_HTTP, CheckTable.KEY_WEIGHT_NETTO));
        results.add(new BasicNameValuePair(GO_TYPE_HTTP, CheckTable.KEY_TYPE));
        results.add(new BasicNameValuePair(GO_IS_READY_HTTP, CheckTable.KEY_IS_READY));
        results.add(new BasicNameValuePair(GO_TIME_HTTP, CheckTable.KEY_TIME_CREATE));
        String joined = TextUtils.join(" ", results);
        contentValues.put(KEY_DATA2, joined);
        contentValues.put(KEY_SYS, 0);
        db.insert(TABLE, null, contentValues);
    }

    public void addSystemSheet(SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        //Resources res = mContext.getResources();
        contentValues.put(KEY_TYPE, TypeSender.TYPE_GOOGLE_DISK.ordinal());
        contentValues.put(KEY_DATA1, "");
        contentValues.put(KEY_DATA2, "");
        contentValues.put(KEY_DATA3, "");
        contentValues.put(KEY_SYS, 1);
        db.insert(TABLE, null, contentValues);
    }

    public void addSystemMail(SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        //Resources res = mContext.getResources();
        contentValues.put(KEY_TYPE, TypeSender.TYPE_EMAIL.ordinal());
        contentValues.put(KEY_DATA1, "");
        contentValues.put(KEY_SYS, 0);
        db.insert(TABLE, null, contentValues);
    }

    public void addSystemSms(SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        //Resources res = mContext.getResources();
        contentValues.put(KEY_TYPE, TypeSender.TYPE_SMS.ordinal());
        contentValues.put(KEY_DATA1, "");
        contentValues.put(KEY_SYS, 1);
        db.insert(TABLE, null, contentValues);
    }
}
