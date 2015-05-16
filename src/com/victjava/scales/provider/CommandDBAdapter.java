package com.victjava.scales.provider;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

/*
 * Created by Kostya on 30.03.2015.
 */
public class CommandDBAdapter {
    private final Context context;

    public static final String TABLE_COMMAND = "commandTable";

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_DATE = "date";
    public static final String KEY_MIME = "mime";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_VALUE = "value";
    public static final String KEY_EXEC = "exec";

    public static final String MIME_SCALE = "scale";
    public final String MIME_TERMINAL = "terminal";

    public static final String TABLE_CREATE_COMMAND = "create table "
            + TABLE_COMMAND + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_DATE + " text,"
            + KEY_MIME + " text,"
            + KEY_COMMAND + " text,"
            + KEY_VALUE + " text,"
            + KEY_EXEC + " integer );";

    private static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE_COMMAND);

    public CommandDBAdapter(Context cnt) {
        context = cnt;
    }

    public Uri insertNewTask(ContentValues newTaskValues) {
        newTaskValues.put(KEY_EXEC, 0);
        return context.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

}
