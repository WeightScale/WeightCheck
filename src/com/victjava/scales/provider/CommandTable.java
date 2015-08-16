package com.victjava.scales.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/*
 * Created by Kostya on 30.03.2015.
 */
public class CommandTable {
    private final Context context;

    public static final String TABLE = "commandTable";

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_DATE = "date";
    public static final String KEY_MIME = "mime";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_VALUE = "value";
    public static final String KEY_EXEC = "exec";

    public static final String MIME_SCALE = "scale";
    //public final String MIME_TERMINAL = "terminal";

    public static final String TABLE_CREATE = "create table "
            + TABLE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_DATE + " text,"
            + KEY_MIME + " text,"
            + KEY_COMMAND + " text,"
            + KEY_VALUE + " text,"
            + KEY_EXEC + " integer );";

    private static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE);

    public CommandTable(Context cnt) {
        context = cnt;
    }

    public Uri insertNewTask(ContentValues newTaskValues) {
        newTaskValues.put(KEY_EXEC, 0);
        return context.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public Cursor getAllEntries() {
        return context.getContentResolver().query(CONTENT_URI, null, null, null, null);
    }

    public boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

}
