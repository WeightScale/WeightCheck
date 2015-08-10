package com.victjava.scales.provider;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/*
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 08.12.13
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
public class WeightCheckBaseProvider extends ContentProvider {

    private static final String DATABASE_NAME = "weightCheck.db";
    private static final int DATABASE_VERSION = 8;
    static final String AUTHORITY = "com.victjava.scale.weightCheck";
    private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";

    private static final int ALL_ROWS = 1;
    private static final int SINGLE_ROWS = 2;

    private enum TableList {
        CHECK_LIST,
        CHECK_ID,
        PREFERENCES_LIST,
        PREFERENCES_ID,
        TYPE_LIST,
        TYPE_ID,
        TASK_LIST,
        TASK_ID,
        ERROR_LIST,
        ERROR_ID,
        COMMAND_LIST,
        COMMAND_ID,
        ADMIN_SENDER_LIST,
        ADMIN_SENDER_ID,
        MESSAGE_LIST,
        MESSAGE_ID
    }

    private static final UriMatcher uriMatcher;
    private SQLiteDatabase db;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, CheckTable.TABLE, TableList.CHECK_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, CheckTable.TABLE + "/#", TableList.CHECK_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, PreferencesTable.TABLE, TableList.PREFERENCES_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, PreferencesTable.TABLE + "/#", TableList.PREFERENCES_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, TypeTable.TABLE, TableList.TYPE_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, TypeTable.TABLE + "/#", TableList.TYPE_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, TaskTable.TABLE, TableList.TASK_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, TaskTable.TABLE + "/#", TableList.TASK_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, ErrorTable.TABLE, TableList.ERROR_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, ErrorTable.TABLE + "/#", TableList.ERROR_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, CommandTable.TABLE, TableList.COMMAND_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, CommandTable.TABLE + "/#", TableList.COMMAND_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, SenderTable.TABLE, TableList.ADMIN_SENDER_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, SenderTable.TABLE + "/#", TableList.ADMIN_SENDER_ID.ordinal());
    }

    /*public void vacuum(){
        db.execSQL("VACUUM");
    }*/

    private String getTable(Uri uri) {
        switch (TableList.values()[uriMatcher.match(uri)]) {
            case CHECK_LIST:
            case CHECK_ID:
                return CheckTable.TABLE; // return
            case PREFERENCES_LIST:
            case PREFERENCES_ID:
                return PreferencesTable.TABLE; // return
            case TYPE_LIST:
            case TYPE_ID:
                return TypeTable.TABLE; // return
            case TASK_LIST:
            case TASK_ID:
                return TaskTable.TABLE; // return
            case ERROR_LIST:
            case ERROR_ID:
                return ErrorTable.TABLE; // return
            case COMMAND_LIST:
            case COMMAND_ID:
                return CommandTable.TABLE; // return
            case ADMIN_SENDER_LIST:
            case ADMIN_SENDER_ID:
                return SenderTable.TABLE; // return
            case MESSAGE_LIST:
            case MESSAGE_ID:
                return MessageTable.TABLE; // return
            /** PROVIDE A DEFAULT CASE HERE **/
            default:
                // If the URI doesn't match any of the known patterns, throw an exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        DBHelper dbHelper = new DBHelper(getContext());
        //db = dbHelper.getWritableDatabase();
        db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.setLockingEnabled(false);
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (TableList.values()[uriMatcher.match(uri)]) {
            case CHECK_LIST: // общий Uri
                queryBuilder.setTables(CheckTable.TABLE);
                break;
            case CHECK_ID: // Uri с ID
                queryBuilder.setTables(CheckTable.TABLE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case PREFERENCES_LIST: // общий Uri
                queryBuilder.setTables(PreferencesTable.TABLE);
                break;
            case PREFERENCES_ID: // Uri с ID
                queryBuilder.setTables(PreferencesTable.TABLE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case TYPE_LIST: // общий Uri
                queryBuilder.setTables(TypeTable.TABLE);
                break;
            case TYPE_ID: // Uri с ID
                queryBuilder.setTables(TypeTable.TABLE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case TASK_LIST: // общий Uri
                queryBuilder.setTables(TaskTable.TABLE);
                break;
            case TASK_ID: // Uri с ID
                queryBuilder.setTables(TaskTable.TABLE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case ERROR_LIST: // общий Uri
                queryBuilder.setTables(ErrorTable.TABLE);
                break;
            case ERROR_ID: // Uri с ID
                queryBuilder.setTables(ErrorTable.TABLE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case COMMAND_LIST: // общий Uri
                queryBuilder.setTables(CommandTable.TABLE);
                break;
            case COMMAND_ID: // Uri с ID
                queryBuilder.setTables(CommandTable.TABLE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case ADMIN_SENDER_LIST: // общий Uri
                queryBuilder.setTables(SenderTable.TABLE);
                break;
            case ADMIN_SENDER_ID: // Uri с ID
                queryBuilder.setTables(SenderTable.TABLE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case MESSAGE_LIST: // общий Uri
                queryBuilder.setTables(MessageTable.TABLE);
                break;
            case MESSAGE_ID: // Uri с ID
                queryBuilder.setTables(MessageTable.TABLE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sort);
        if (cursor == null) {
            return null;
        }
        Context context = getContext();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                cursor.setNotificationUri(contentResolver, uri);
            }
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                return "vnd.android.cursor.dir/vnd.";
            case SINGLE_ROWS:
                return "vnd.android.cursor.item/vnd.";
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {

        long rowID = db.insert(getTable(uri), null, contentValues);
        if (rowID > 0L) {
            Uri resultUri = ContentUris.withAppendedId(uri, rowID);
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(resultUri, null);
                return resultUri;
            }
        }
        throw new SQLiteException("Ошибка добавления записи " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArg) {
        int delCount;
        String id;


        switch (TableList.values()[uriMatcher.match(uri)]) {
            case CHECK_LIST: // общий Uri
                delCount = db.delete(CheckTable.TABLE, where, whereArg);
                break;
            case CHECK_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(CheckTable.TABLE, where, whereArg);
                break;
            case PREFERENCES_LIST: // общий Uri
                delCount = db.delete(PreferencesTable.TABLE, where, whereArg);
                break;
            case PREFERENCES_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(PreferencesTable.TABLE, where, whereArg);
                break;
            case TYPE_LIST:
                delCount = db.delete(TypeTable.TABLE, where, whereArg);
                break;
            case TYPE_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(TypeTable.TABLE, where, whereArg);
                break;
            case TASK_LIST:
                delCount = db.delete(TaskTable.TABLE, where, whereArg);
                break;
            case TASK_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(TaskTable.TABLE, where, whereArg);
                break;
            case ERROR_LIST:
                delCount = db.delete(ErrorTable.TABLE, where, whereArg);
                break;
            case ERROR_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(ErrorTable.TABLE, where, whereArg);
                break;
            case COMMAND_LIST:
                delCount = db.delete(CommandTable.TABLE, where, whereArg);
                break;
            case COMMAND_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(CommandTable.TABLE, where, whereArg);
                break;
            case ADMIN_SENDER_LIST:
                delCount = db.delete(SenderTable.TABLE, where, whereArg);
                break;
            case ADMIN_SENDER_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(SenderTable.TABLE, where, whereArg);
                break;
            case MESSAGE_LIST:
                delCount = db.delete(MessageTable.TABLE, where, whereArg);
                break;
            case MESSAGE_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(MessageTable.TABLE, where, whereArg);
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db.execSQL("VACUUM");
        if (delCount > 0) {
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
        }

        return delCount;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String where, String[] whereArg) {
        int updateCount;
        String id;
        switch (TableList.values()[uriMatcher.match(uri)]) {
            case CHECK_LIST: // общий Uri
                updateCount = db.update(CheckTable.TABLE, contentValues, where, whereArg);
                break;
            case CHECK_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(CheckTable.TABLE, contentValues, where, whereArg);
                break;
            case PREFERENCES_LIST: // общий Uri
                updateCount = db.update(PreferencesTable.TABLE, contentValues, where, whereArg);
                break;
            case PREFERENCES_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(PreferencesTable.TABLE, contentValues, where, whereArg);
                break;
            case TYPE_LIST: // общий Uri
                updateCount = db.update(TypeTable.TABLE, contentValues, where, whereArg);
                break;
            case TYPE_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(TypeTable.TABLE, contentValues, where, whereArg);
                break;
            case TASK_LIST: // общий Uri
                updateCount = db.update(TaskTable.TABLE, contentValues, where, whereArg);
                break;
            case TASK_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(TaskTable.TABLE, contentValues, where, whereArg);
                break;
            case ERROR_LIST: // общий Uri
                updateCount = db.update(ErrorTable.TABLE, contentValues, where, whereArg);
                break;
            case ERROR_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(ErrorTable.TABLE, contentValues, where, whereArg);
                break;
            case COMMAND_LIST: // общий Uri
                updateCount = db.update(CommandTable.TABLE, contentValues, where, whereArg);
                break;
            case COMMAND_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(CommandTable.TABLE, contentValues, where, whereArg);
                break;
            case ADMIN_SENDER_LIST: // общий Uri
                updateCount = db.update(SenderTable.TABLE, contentValues, where, whereArg);
                break;
            case ADMIN_SENDER_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(SenderTable.TABLE, contentValues, where, whereArg);
                break;
            case MESSAGE_LIST: // общий Uri
                updateCount = db.update(MessageTable.TABLE, contentValues, where, whereArg);
                break;
            case MESSAGE_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(MessageTable.TABLE, contentValues, where, whereArg);
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        if (updateCount > 0) {
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
        }

        return updateCount;
    }

    private static class DBHelper extends SQLiteOpenHelper {
        final SenderTable senderTable;

        DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            senderTable = new SenderTable(context);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CheckTable.TABLE_CREATE);
            db.execSQL(TypeTable.TABLE_CREATE);
            db.execSQL(PreferencesTable.TABLE_CREATE);
            db.execSQL(TaskTable.TABLE_CREATE);
            db.execSQL(ErrorTable.TABLE_CREATE);
            db.execSQL(CommandTable.TABLE_CREATE);
            db.execSQL(SenderTable.TABLE_CREATE);
            db.execSQL(MessageTable.TABLE_CREATE);

            //Add default record to my table
            //new TypeTable(getContext()).addSystemRow(db);
            /*-----------------------------------------------*/
            senderTable.addSystemSheet(db);
            //senderTable.addSystemHTTP(db);
            //senderTable.addSystemMail(db);
            //senderTable.addSystemSms(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            db.execSQL(DROP_TABLE_IF_EXISTS + CheckTable.TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + TypeTable.TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + PreferencesTable.TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + TaskTable.TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + ErrorTable.TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + CommandTable.TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + SenderTable.TABLE);
            db.execSQL(DROP_TABLE_IF_EXISTS + MessageTable.TABLE);
            onCreate(db);
        }
    }
}
