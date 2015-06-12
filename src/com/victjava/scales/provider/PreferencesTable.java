package com.victjava.scales.provider;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.konst.module.ScaleModule;
import com.victjava.scales.Main;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 11.11.13
 * Time: 14:15
 * To change this template use File | Settings | File Templates.
 */
public class PreferencesTable {
    private final Context context;
    public static final String TABLE = "preferencesTable";

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_DATE_CREATE = "dateCreate";
    public static final String KEY_TIME_CREATE = "timeCreate";
    public static final String KEY_NUMBER_BT = "numberBt";
    public static final String KEY_COEFFICIENT_A = "coefficientA";
    public static final String KEY_COEFFICIENT_B = "coefficientB";
    public static final String KEY_MAX_WEIGHT = "maxWeight";
    public static final String KEY_FILTER_ADC = "filterADC";
    public static final String KEY_STEP_SCALE = "stepScale";
    public static final String KEY_STEP_CAPTURE = "stepCapture";
    public static final String KEY_TIME_OFF = "timeOff";
    public static final String KEY_NUMBER_BT_TERMINAL = "numberBtTerminal";
    private static final String KEY_CHECK_ON_SERVER = "checkOnServer";

    public static final String TABLE_CREATE = "create table "
            + TABLE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_DATE_CREATE + " text,"
            + KEY_TIME_CREATE + " text,"
            + KEY_NUMBER_BT + " text,"
            + KEY_COEFFICIENT_A + " float,"
            + KEY_COEFFICIENT_B + " float,"
            + KEY_MAX_WEIGHT + " integer,"
            + KEY_FILTER_ADC + " integer,"
            + KEY_STEP_SCALE + " integer,"
            + KEY_STEP_CAPTURE + " integer,"
            + KEY_TIME_OFF + " integer,"
            + KEY_NUMBER_BT_TERMINAL + " text,"
            + KEY_CHECK_ON_SERVER + " integer );";

    //static final String TABLE_PREFERENCES_PATH = TABLE;
    private static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE);

    public PreferencesTable(Context cnt) {
        context = cnt;
    }

    public Uri insertAllEntry() {
        ContentValues newTaskValues = new ContentValues();
        Date date = new Date();
        newTaskValues.put(KEY_DATE_CREATE, new SimpleDateFormat("dd.MM.yyyy").format(date));
        newTaskValues.put(KEY_TIME_CREATE, new SimpleDateFormat("HH:mm:ss").format(date));
        newTaskValues.put(KEY_NUMBER_BT, ScaleModule.getAddressBluetoothDevice());
        newTaskValues.put(KEY_COEFFICIENT_A, ScaleModule.getCoefficientA());
        newTaskValues.put(KEY_COEFFICIENT_B, ScaleModule.getCoefficientB());
        newTaskValues.put(KEY_MAX_WEIGHT, ScaleModule.getWeightMax());
        newTaskValues.put(KEY_FILTER_ADC, ScaleModule.getFilterADC());
        newTaskValues.put(KEY_STEP_SCALE, Main.stepMeasuring);
        newTaskValues.put(KEY_STEP_CAPTURE, Main.autoCapture);
        newTaskValues.put(KEY_TIME_OFF, ScaleModule.getTimeOff());
        newTaskValues.put(KEY_NUMBER_BT_TERMINAL, BluetoothAdapter.getDefaultAdapter().getAddress());
        newTaskValues.put(KEY_CHECK_ON_SERVER, 0);
        return context.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public void removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            context.getContentResolver().delete(uri, null, null);
        } catch (Exception e) {}
    }

    public Cursor getEntryItem(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = context.getContentResolver().query(uri, null, null, null, null);
            result.moveToFirst();
            return result;
        }catch (Exception e){return null;}

    }

}
