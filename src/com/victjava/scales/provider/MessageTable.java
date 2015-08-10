package com.victjava.scales.provider;


import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 *@author Kostya
 */
public class MessageTable {
    private final Context mContext;
    public static final String TABLE = "messageTable";

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_ADDRESS = "address";

    public static final String TABLE_CREATE = "create table "
            + TABLE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_ADDRESS + " text );";

    private static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE);

    public MessageTable(Context context) {
        mContext = context;
    }

}
