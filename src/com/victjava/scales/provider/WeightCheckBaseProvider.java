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

    /*private static final int CHECK_LIST         = 1;
    private static final int CHECK_ID           = 2;
    private static final int PREFERENCES_LIST   = 3;
    private static final int PREFERENCES_ID     = 4;
    private static final int TYPE_LIST          = 5;
    private static final int TYPE_ID            = 6;
    private static final int TASK_LIST          = 7;
    private static final int TASK_ID            = 8;
    private static final int ERROR_LIST         = 9;
    private static final int ERROR_ID           = 10;
    private static final int COMMAND_LIST       = 11;
    private static final int COMMAND_ID         = 12;*/

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
        ADMIN_SENDER_ID
    }

    private static final UriMatcher uriMatcher;
    private SQLiteDatabase db;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, CheckDBAdapter.TABLE_CHECKS, TableList.CHECK_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, CheckDBAdapter.TABLE_CHECKS + "/#", TableList.CHECK_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, PreferencesDBAdapter.TABLE_PREFERENCES, TableList.PREFERENCES_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, PreferencesDBAdapter.TABLE_PREFERENCES + "/#", TableList.PREFERENCES_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, TypeDBAdapter.TABLE_TYPE, TableList.TYPE_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, TypeDBAdapter.TABLE_TYPE + "/#", TableList.TYPE_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, TaskDBAdapter.TABLE_TASK, TableList.TASK_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, TaskDBAdapter.TABLE_TASK + "/#", TableList.TASK_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, ErrorDBAdapter.TABLE_ERROR, TableList.ERROR_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, ErrorDBAdapter.TABLE_ERROR + "/#", TableList.ERROR_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, CommandDBAdapter.TABLE_COMMAND, TableList.COMMAND_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, CommandDBAdapter.TABLE_COMMAND + "/#", TableList.COMMAND_ID.ordinal());
        uriMatcher.addURI(AUTHORITY, SenderDBAdapter.TABLE_SENDER, TableList.ADMIN_SENDER_LIST.ordinal());
        uriMatcher.addURI(AUTHORITY, SenderDBAdapter.TABLE_SENDER + "/#", TableList.ADMIN_SENDER_ID.ordinal());
    }

    /*public void vacuum(){
        db.execSQL("VACUUM");
    }*/

    private String getTable(Uri uri) {
        switch (TableList.values()[uriMatcher.match(uri)]) {
            case CHECK_LIST:
            case CHECK_ID:
                return CheckDBAdapter.TABLE_CHECKS; // return
            case PREFERENCES_LIST:
            case PREFERENCES_ID:
                return PreferencesDBAdapter.TABLE_PREFERENCES; // return
            case TYPE_LIST:
            case TYPE_ID:
                return TypeDBAdapter.TABLE_TYPE; // return
            case TASK_LIST:
            case TASK_ID:
                return TaskDBAdapter.TABLE_TASK; // return
            case ERROR_LIST:
            case ERROR_ID:
                return ErrorDBAdapter.TABLE_ERROR; // return
            case COMMAND_LIST:
            case COMMAND_ID:
                return CommandDBAdapter.TABLE_COMMAND; // return
            case ADMIN_SENDER_LIST:
            case ADMIN_SENDER_ID:
                return SenderDBAdapter.TABLE_SENDER; // return
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
                queryBuilder.setTables(CheckDBAdapter.TABLE_CHECKS);
                break;
            case CHECK_ID: // Uri с ID
                queryBuilder.setTables(CheckDBAdapter.TABLE_CHECKS);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case PREFERENCES_LIST: // общий Uri
                queryBuilder.setTables(PreferencesDBAdapter.TABLE_PREFERENCES);
                break;
            case PREFERENCES_ID: // Uri с ID
                queryBuilder.setTables(PreferencesDBAdapter.TABLE_PREFERENCES);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case TYPE_LIST: // общий Uri
                queryBuilder.setTables(TypeDBAdapter.TABLE_TYPE);
                break;
            case TYPE_ID: // Uri с ID
                queryBuilder.setTables(TypeDBAdapter.TABLE_TYPE);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case TASK_LIST: // общий Uri
                queryBuilder.setTables(TaskDBAdapter.TABLE_TASK);
                break;
            case TASK_ID: // Uri с ID
                queryBuilder.setTables(TaskDBAdapter.TABLE_TASK);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case ERROR_LIST: // общий Uri
                queryBuilder.setTables(ErrorDBAdapter.TABLE_ERROR);
                break;
            case ERROR_ID: // Uri с ID
                queryBuilder.setTables(ErrorDBAdapter.TABLE_ERROR);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case COMMAND_LIST: // общий Uri
                queryBuilder.setTables(CommandDBAdapter.TABLE_COMMAND);
                break;
            case COMMAND_ID: // Uri с ID
                queryBuilder.setTables(CommandDBAdapter.TABLE_COMMAND);
                queryBuilder.appendWhere(BaseColumns._ID + '=' + uri.getLastPathSegment());
                break;
            case ADMIN_SENDER_LIST: // общий Uri
                queryBuilder.setTables(SenderDBAdapter.TABLE_SENDER);
                break;
            case ADMIN_SENDER_ID: // Uri с ID
                queryBuilder.setTables(SenderDBAdapter.TABLE_SENDER);
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
                delCount = db.delete(CheckDBAdapter.TABLE_CHECKS, where, whereArg);
                break;
            case CHECK_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(CheckDBAdapter.TABLE_CHECKS, where, whereArg);
                break;
            case PREFERENCES_LIST: // общий Uri
                delCount = db.delete(PreferencesDBAdapter.TABLE_PREFERENCES, where, whereArg);
                break;
            case PREFERENCES_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(PreferencesDBAdapter.TABLE_PREFERENCES, where, whereArg);
                break;
            case TYPE_LIST:
                delCount = db.delete(TypeDBAdapter.TABLE_TYPE, where, whereArg);
                break;
            case TYPE_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(TypeDBAdapter.TABLE_TYPE, where, whereArg);
                break;
            case TASK_LIST:
                delCount = db.delete(TaskDBAdapter.TABLE_TASK, where, whereArg);
                break;
            case TASK_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(TaskDBAdapter.TABLE_TASK, where, whereArg);
                break;
            case ERROR_LIST:
                delCount = db.delete(ErrorDBAdapter.TABLE_ERROR, where, whereArg);
                break;
            case ERROR_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(ErrorDBAdapter.TABLE_ERROR, where, whereArg);
                break;
            case COMMAND_LIST:
                delCount = db.delete(CommandDBAdapter.TABLE_COMMAND, where, whereArg);
                break;
            case COMMAND_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(CommandDBAdapter.TABLE_COMMAND, where, whereArg);
                break;
            case ADMIN_SENDER_LIST:
                delCount = db.delete(SenderDBAdapter.TABLE_SENDER, where, whereArg);
                break;
            case ADMIN_SENDER_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                delCount = db.delete(SenderDBAdapter.TABLE_SENDER, where, whereArg);
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
                updateCount = db.update(CheckDBAdapter.TABLE_CHECKS, contentValues, where, whereArg);
                break;
            case CHECK_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(CheckDBAdapter.TABLE_CHECKS, contentValues, where, whereArg);
                break;
            case PREFERENCES_LIST: // общий Uri
                updateCount = db.update(PreferencesDBAdapter.TABLE_PREFERENCES, contentValues, where, whereArg);
                break;
            case PREFERENCES_ID:
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(PreferencesDBAdapter.TABLE_PREFERENCES, contentValues, where, whereArg);
                break;
            case TYPE_LIST: // общий Uri
                updateCount = db.update(TypeDBAdapter.TABLE_TYPE, contentValues, where, whereArg);
                break;
            case TYPE_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(TypeDBAdapter.TABLE_TYPE, contentValues, where, whereArg);
                break;
            case TASK_LIST: // общий Uri
                updateCount = db.update(TaskDBAdapter.TABLE_TASK, contentValues, where, whereArg);
                break;
            case TASK_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(TaskDBAdapter.TABLE_TASK, contentValues, where, whereArg);
                break;
            case ERROR_LIST: // общий Uri
                updateCount = db.update(ErrorDBAdapter.TABLE_ERROR, contentValues, where, whereArg);
                break;
            case ERROR_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(ErrorDBAdapter.TABLE_ERROR, contentValues, where, whereArg);
                break;
            case COMMAND_LIST: // общий Uri
                updateCount = db.update(CommandDBAdapter.TABLE_COMMAND, contentValues, where, whereArg);
                break;
            case COMMAND_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(CommandDBAdapter.TABLE_COMMAND, contentValues, where, whereArg);
                break;
            case ADMIN_SENDER_LIST: // общий Uri
                updateCount = db.update(SenderDBAdapter.TABLE_SENDER, contentValues, where, whereArg);
                break;
            case ADMIN_SENDER_ID: // Uri с ID
                id = uri.getLastPathSegment();
                where = TextUtils.isEmpty(where) ? BaseColumns._ID + " = " + id : where + " AND " + BaseColumns._ID + " = " + id;
                updateCount = db.update(SenderDBAdapter.TABLE_SENDER, contentValues, where, whereArg);
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

    private class DBHelper extends SQLiteOpenHelper {
        final SenderDBAdapter senderTable;
        DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            senderTable = new SenderDBAdapter(context);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CheckDBAdapter.TABLE_CREATE_CHECKS);
            db.execSQL(TypeDBAdapter.TABLE_CREATE_TYPE);
            db.execSQL(PreferencesDBAdapter.TABLE_CREATE_PREFERENCES);
            db.execSQL(TaskDBAdapter.TABLE_CREATE_TASK);
            db.execSQL(ErrorDBAdapter.TABLE_CREATE_ERROR);
            db.execSQL(CommandDBAdapter.TABLE_CREATE_COMMAND);
            db.execSQL(SenderDBAdapter.TABLE_CREATE_TYPE);

            //Add default record to my table
            new TypeDBAdapter(getContext()).addSystemRow(db);
            /*-----------------------------------------------*/
            senderTable.addSystemSheet(db);
            senderTable.addSystemHTTP(db);
            senderTable.addSystemMail(db);
            senderTable.addSystemSms(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            db.execSQL(DROP_TABLE_IF_EXISTS + CheckDBAdapter.TABLE_CHECKS);
            db.execSQL(DROP_TABLE_IF_EXISTS + TypeDBAdapter.TABLE_TYPE);
            db.execSQL(DROP_TABLE_IF_EXISTS + PreferencesDBAdapter.TABLE_PREFERENCES);
            db.execSQL(DROP_TABLE_IF_EXISTS + TaskDBAdapter.TABLE_TASK);
            db.execSQL(DROP_TABLE_IF_EXISTS + ErrorDBAdapter.TABLE_ERROR);
            db.execSQL(DROP_TABLE_IF_EXISTS + CommandDBAdapter.TABLE_COMMAND);
            db.execSQL(DROP_TABLE_IF_EXISTS + SenderDBAdapter.TABLE_SENDER);
            onCreate(db);
        }
    }
}
