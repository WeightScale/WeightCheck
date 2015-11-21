//
package com.victjava.scales;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.*;
import android.widget.*;
import com.konst.module.Module;
import com.konst.module.OnEventConnectResult;
import com.konst.module.ScaleModule;
import com.konst.module.ScaleModule.*;
import com.victjava.scales.provider.CheckTable;
import com.victjava.scales.provider.CommandTable;
import com.victjava.scales.provider.TaskTable;
import com.victjava.scales.service.ServiceProcessTask;
import com.victjava.scales.settings.ActivityPreferences;
import com.victjava.scales.settings.ActivityTuning;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Активность управления весами
 *
 * @author Kostya
 */
public class ActivityScales extends Activity implements View.OnClickListener, View.OnLongClickListener {
    private SimpleCursorAdapter namesAdapter;
    private CheckTable checkTable;
    private BroadcastReceiver broadcastReceiver; //приёмник намерений
    private Vibrator vibrator; //вибратор
    private BatteryProgressBar progressBarBattery; //текст батареи
    private TemperatureProgressBar temperatureProgressBar;
    private ImageView imageViewRemote;
    private ImageView imageNewCheck;
    private ListView listView;
    private ScaleModule scaleModule;
    private Main main;

    /** лайаут для батарея температура */
    private LinearLayout linearBatteryTemp;
    static final int REQUEST_SEARCH_SCALE = 2;

    private boolean doubleBackToExitPressedOnce;
    public static boolean isScaleConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        main = (Main)getApplicationContext();

        checkTable = new CheckTable(this);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        main.setTelephoneNumber(telephonyManager.getLine1Number());
        main.setSimNumber(telephonyManager.getSimSerialNumber());
        main.setNetworkOperatorName(telephonyManager.getNetworkOperatorName());
        main.setNetworkCountry(telephonyManager.getNetworkCountryIso());
        int state = telephonyManager.getSimState();

        if (state == TelephonyManager.SIM_STATE_READY) {
            try {
                scaleModule = new ScaleModule(main.getPackageInfo().versionName, onEventConnectResult);
                main.setScaleModule(scaleModule);
                scaleModule.setTimerNull(main.preferencesScale.read(getString(R.string.KEY_TIMER_NULL), getResources().getInteger(R.integer.default_max_time_auto_null)));
                scaleModule.setWeightError(main.preferencesScale.read(getString(R.string.KEY_MAX_NULL), getResources().getInteger(R.integer.default_limit_auto_null)));
                Toast.makeText(getBaseContext(), R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
                setupScale();
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }

        } else {
            Toast.makeText(getBaseContext(), R.string.telephony_sim_no, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonMenu:
                openOptionsMenu();
                break;
            case R.id.buttonBack:
                onBackPressed();
                break;
            case R.id.imageViewRemote:
                vibrator.vibrate(200);
                break;
            default:
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.imageNewCheck:
                vibrator.vibrate(100);
                /** сбрасываем в ноль счетчик автоноль*/
                scaleModule.resetAutoNull();
                startActivity(new Intent(getBaseContext(), ActivityContact.class).setAction("check"));
                break;
            case R.id.imageViewRemote:
                vibrator.vibrate(100);
                openSearch();
                break;
            default:
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(scaleModule.isAttach())
            scaleModule.startMeasuringBatteryTemperature();
        namesAdapter.changeCursor(checkTable.getAllNoReadyCheck());
    }

    @Override
    protected void onPause() {
        super.onPause();
        scaleModule.stopMeasuringBatteryTemperature(false);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            //exit();
            return;
        }
        scaleModule.getAdapter().cancelDiscovery();
        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_again_to_exit /*Please click BACK again to exit*/, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    @Override
    public void onDestroy() { //при разрушении активности
        super.onDestroy();
        exit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_scales, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences:
                startActivity(new Intent(this, ActivityPreferences.class));
                break;
            case R.id.type:
                startActivity(new Intent(getBaseContext(), ActivityType.class));
                break;
            case R.id.checks:
                startActivity(new Intent(getBaseContext(), ActivityListChecks.class));
                break;
            case R.id.contact:
                startActivity(new Intent(getBaseContext(), ActivityContact.class).setAction("contact"));
                break;
            case R.id.power_off:
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(getString(R.string.Scale_off));
                dialog.setIcon(getResources().getDrawable(R.drawable.ic_action_power_off));
                dialog.setCancelable(false);
                dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            if (scaleModule.isAttach())
                                scaleModule.setModulePowerOff();
                        }
                    }
                });
                dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //finish();
                    }
                });
                dialog.setMessage(getString(R.string.TEXT_MESSAGE15));
                dialog.show();
                break;
            default:

        }
        return true;
    }

    /**
     * Установка настроек весов.
     */
    private void setupScale() {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.scales);

        linearBatteryTemp = (LinearLayout) findViewById(R.id.linearSectionScale);
        linearBatteryTemp.setVisibility(View.INVISIBLE);
        //LinearLayout scaleSection = (LinearLayout) findViewById(R.id.scaleSection);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = 1.0f;
        getWindow().setAttributes(layoutParams);

        Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME, 1);       //Включаем автообновления дата время
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME_ZONE, 1);  //Включаем автообновления дата время

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null) {
            if (networkInfo.isAvailable()) {//Если используется
                new Internet(this).turnOnWiFiConnection(false); // для телефонов у которых один модуль wifi и bluetooth
            }
        }

        broadcastReceiver = new BroadcastReceiver() {
            private ProgressDialog dialog;

            @Override
            public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
                String action = intent.getAction();
                if (action != null) {
                    //switch (action) {
                        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {//case BluetoothAdapter.ACTION_STATE_CHANGED:
                            switch (scaleModule.getAdapter().getState()) {
                                case BluetoothAdapter.STATE_OFF:
                                    dialog = new ProgressDialog(context);
                                    dialog.setCancelable(false);
                                    dialog.setIndeterminate(false);
                                    dialog.show();
                                    dialog.setContentView(R.layout.custom_progress_dialog);
                                    TextView tv1 = (TextView) dialog.findViewById(R.id.textView1);
                                    tv1.setText(R.string.bluetooth_turning_on);
                                    //Toast.makeText(getBaseContext(), R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
                                    new Internet(getApplicationContext()).turnOnWiFiConnection(false);
                                    scaleModule.getAdapter().enable();
                                    break;
                                case BluetoothAdapter.STATE_TURNING_ON:
                                    //Toast.makeText(getBaseContext(), R.string.bluetooth_turning_on, Toast.LENGTH_SHORT).show();
                                    break;
                                case BluetoothAdapter.STATE_ON:
                                    if (dialog.isShowing()) {
                                        dialog.dismiss();
                                    }
                                    //Toast.makeText(getBaseContext(), R.string.bluetooth_on, Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                        }//break;
                        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {//case BluetoothDevice.ACTION_ACL_DISCONNECTED://устройство отсоеденено
                            vibrator.vibrate(200);
                            listView.setOnItemClickListener(null);
                            linearBatteryTemp.setVisibility(View.INVISIBLE);
                            imageViewRemote.setImageDrawable(getResources().getDrawable(R.drawable.rss_off));
                            imageNewCheck.setEnabled(false);
                            isScaleConnect = false;
                        }//break;
                        else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {//case BluetoothDevice.ACTION_ACL_CONNECTED://найдено соеденено
                            vibrator.vibrate(200);
                            listView.setOnItemClickListener(onItemClickListener);
                            linearBatteryTemp.setVisibility(View.VISIBLE);
                            imageViewRemote.setImageDrawable(getResources().getDrawable(R.drawable.rss_on));
                            imageNewCheck.setEnabled(true);
                            isScaleConnect = true;
                        }//break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(broadcastReceiver, intentFilter);

        imageViewRemote = (ImageView) findViewById(R.id.imageViewRemote);
        //imageViewRemote.setOnClickListener(this);
        imageViewRemote.setOnLongClickListener(this);

        imageNewCheck = (ImageView) findViewById(R.id.imageNewCheck);
        imageNewCheck.setOnLongClickListener(this);

        //findViewById(R.id.imageButtonUp).setOnLongClickListener(this);
        findViewById(R.id.buttonMenu).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);

        //progressBarBattery = new BatteryProgressBar(this);
        progressBarBattery = (BatteryProgressBar) findViewById(R.id.progressBarBattery);
        temperatureProgressBar = (TemperatureProgressBar) findViewById(R.id.progressBarTemperature);

        progressBarBattery.updateProgress(0);
        temperatureProgressBar.updateProgress(0);

        listCheckSetup();
        connectScaleModule(Main.preferencesScale.read(getString(R.string.KEY_LAST_SCALES), ""));

    }

    /**
     * Настройка листа весовых чеков.
     */
    private void listCheckSetup() {
        listView = (ListView) findViewById(R.id.listViewWeights);
        try {
            setReadyOldChecks();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateList();
    }

    /** Слушатель нажания чека в листе.
     * Запускаем Активность работы с чеком.
     */
    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startActivity(new Intent().setClass(getApplicationContext(), ActivityCheck.class).putExtra("id", String.valueOf(id)));
        }
    };

    /** Обновляем данные листа непроведенных чеков.
     */
    private void updateList() {
        Cursor cursor = checkTable.getAllNoReadyCheck();
        if (cursor == null) {
            return;
        }
        String[] columns = {CheckTable.KEY_ID,
                CheckTable.KEY_DATE_CREATE,
                CheckTable.KEY_TIME_CREATE,
                CheckTable.KEY_VENDOR,
                CheckTable.KEY_WEIGHT_FIRST,
                CheckTable.KEY_WEIGHT_SECOND,
                CheckTable.KEY_WEIGHT_NETTO,
                CheckTable.KEY_PRICE_SUM, CheckTable.KEY_DIRECT, CheckTable.KEY_DIRECT, CheckTable.KEY_DIRECT};
        int[] to = {R.id.check_id, R.id.date, R.id.time, R.id.vendor, R.id.gross_row, R.id.tare_row, R.id.netto_row, R.id.sum_row, R.id.imageDirect, R.id.gross, R.id.tare};

        namesAdapter = new SimpleCursorAdapter(this, R.layout.item_check, cursor, columns, to);
        //namesAdapter = new MyCursorAdapter(this, R.layout.item_check, cursor, columns, to);
        namesAdapter.setViewBinder(new ListCheckViewBinder());
        listView.setAdapter(namesAdapter);
    }

    /** Соеденяемся с Весовым модулем.
     * Инициализируем созданый экземпляр модуля.
     */
    private void connectScaleModule(String address) {
        try {
            scaleModule.init(address);
            scaleModule.attach();
        } catch (Exception e) {
            openSearch();
        }

    }

    /**
     * Выход.
     */
    private void exit() {
        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
        if(scaleModule != null){
            scaleModule.dettach();
            scaleModule.getAdapter().disable();
            while (scaleModule.getAdapter().isEnabled()) ;
        }
        startService(new Intent(this, ServiceProcessTask.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //AlertDialog.Builder dialog;
        setProgressBarIndeterminateVisibility(false);
        switch (resultCode) {
            case RESULT_OK:
                onEventConnectResult.handleResultConnect(Module.ResultConnect.STATUS_LOAD_OK);
                break;
            case RESULT_CANCELED:
                listView.setEnabled(false);
                break;
            default:

        }
    }

    private class ListCheckViewBinder implements SimpleCursorAdapter.ViewBinder {
        private int direct;

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

            switch (view.getId()) {
                case R.id.gross:
                    direct = cursor.getInt(cursor.getColumnIndex(CheckTable.KEY_DIRECT));
                    if (direct == CheckTable.DIRECT_UP) {
                        setViewText((TextView) view, getString(R.string.Tape));
                    } else {
                        setViewText((TextView) view, getString(R.string.Gross));
                    }
                    break;
                case R.id.tare:
                    direct = cursor.getInt(cursor.getColumnIndex(CheckTable.KEY_DIRECT));
                    if (direct == CheckTable.DIRECT_DOWN) {
                        setViewText((TextView) view, getString(R.string.Tape));
                    } else {
                        setViewText((TextView) view, getString(R.string.Gross));
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }

        public void setViewText(TextView v, CharSequence text) {
            v.setText(text);
        }
    }

    /**
     * Открыть активность поиска весов.
     */
    private void openSearch() {
        listView.setEnabled(false);
        scaleModule.dettach();
        startActivityForResult(new Intent(getBaseContext(), ActivitySearch.class), REQUEST_SEARCH_SCALE);
    }

    /**
     * Устанавливаем старые чеки в состояние готовые для отправки.
     * Условие проверки по дате создания и даты хранения не готовых чеков.
     *
     * @throws Exception
     */
    private void setReadyOldChecks() throws Exception {
        Cursor cursor = checkTable.getNotReady();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    String date = cursor.getString(cursor.getColumnIndex(CheckTable.KEY_DATE_CREATE));
                    try {
                        long day = dayDiff(new Date(), new SimpleDateFormat("dd.MM.yy",Locale.getDefault()).parse(date));
                        if (day > ((Main)getApplication()).preferencesScale.read(getString(R.string.KEY_DAY_CLOSED_CHECK), 5)) {
                            int id = cursor.getInt(cursor.getColumnIndex(CheckTable.KEY_ID));
                            checkTable.updateEntry(id, CheckTable.KEY_IS_READY, 1);
                            new TaskTable(this).setCheckReady(id);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
    }

    /**
     * Вычисляем разницу между датами
     *
     * @param d1 Дата которуя проверяем
     * @param d2 Дата сравнения
     * @return Разница между d1 и d2 в днях.
     */
    private long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;//86400000
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return day1 - day2;
    }

    /** Экземпляр Весового модуля.
     * Обработсик сообщений.
     */
    final OnEventConnectResult onEventConnectResult = new OnEventConnectResult() {
        AlertDialog.Builder dialog;
        //ProgressDialog dialogSearch;

        /** Сообщение о результате соединения
         * @param result Результат соединения энкмератор ResultConnect.
         */
        @Override
        public void handleResultConnect(final Module.ResultConnect result) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (result) {
                        case STATUS_LOAD_OK:
                            scaleModule.setOnEventResultBatteryTemperature(onEventResultBatteryTemperature);
                            try {
                                setTitle(getString(R.string.app_name) + " \"" + scaleModule.getNameBluetoothDevice() + "\", v." + scaleModule.getNumVersion()); //установить заголовок
                            } catch (Exception e) {
                                setTitle(getString(R.string.app_name) + " , v." + scaleModule.getNumVersion()); //установить заголовок
                            }
                            main.preferencesScale.write(getString(R.string.KEY_LAST_SCALES), scaleModule.getAddressBluetoothDevice());
                            main.preferencesScale.write(getString(R.string.KEY_LAST_USER), scaleModule.getUserName());
                            listView.setEnabled(true);
                            scaleModule.startMeasuringBatteryTemperature();
                            getSettingPostponed();
                        break;
                        case STATUS_VERSION_UNKNOWN:

                            break;
                        case STATUS_ATTACH_START:
                            setTitle(getString(R.string.app_name) + " >> " + scaleModule.getNameBluetoothDevice());
                            /*dialogSearch = new ProgressDialog(ActivityScales.this);
                            dialogSearch.setCancelable(false);
                            dialogSearch.setIndeterminate(false);
                            dialogSearch.show();
                            dialogSearch.setContentView(R.layout.custom_progress_dialog);
                            TextView tv1 = (TextView) dialogSearch.findViewById(R.id.textView1);
                            tv1.setText(getString(R.string.Connecting) + '\n' + ScaleModule.getNameBluetoothDevice());*/
                        break;
                        case STATUS_ATTACH_FINISH:
                            setProgressBarIndeterminateVisibility(false);
                            /*if (dialogSearch.isShowing()) {
                                dialogSearch.dismiss();
                            }*/
                        break;
                        default:
                    }
                }
            });
        }

        /** Сообщение о ошибки соединения
         * @param error Тип ошибки энумератор Error.
         * @param s Описание ошибки.
         */
        @Override
        public void handleConnectError(final Module.ResultError error, final String s) {
            //setProgressBarIndeterminateVisibility(false);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (error) {
                        case TERMINAL_ERROR:
                            dialog = new AlertDialog.Builder(ActivityScales.this);
                            dialog.setTitle(getString(R.string.preferences_error));
                            dialog.setCancelable(false);
                            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    doubleBackToExitPressedOnce = true;
                                    onBackPressed();
                                }
                            });
                            dialog.setMessage(s);
                            Toast.makeText(getBaseContext(), R.string.preferences_error, Toast.LENGTH_SHORT).show();
                            setTitle(getString(R.string.app_name) + ": " + getString(R.string.preferences_error));
                            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(ActivityScales.this, ActivityPreferences.class));
                                    dialogInterface.dismiss();
                                }
                            });
                            dialog.show();
                        break;
                        case MODULE_ERROR:
                            dialog = new AlertDialog.Builder(ActivityScales.this);
                            dialog.setTitle("Ошибка в настройках");
                            dialog.setCancelable(false);
                            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    onBackPressed();
                                }
                            });
                            dialog.setMessage("Запросите настройки у администратора. Настройки должен выполнять опытный пользователь. Ошибка(" + s + ')');
                            Toast.makeText(getBaseContext(), R.string.preferences_error, Toast.LENGTH_SHORT).show();
                            setTitle(getString(R.string.app_name) + ": админ настройки неправельные");
                            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(ActivityScales.this, ActivityTuning.class));
                                    dialogInterface.dismiss();
                                }
                            });
                            dialog.show();
                        break;
                        case CONNECT_ERROR:
                            setTitle(getString(R.string.app_name) + getString(R.string.NO_CONNECT)); //установить заголовок
                            listView.setEnabled(false);
                            imageNewCheck.setEnabled(false);
                            imageViewRemote.setImageDrawable(getResources().getDrawable(R.drawable.rss_off));
                            Intent intent = new Intent(getBaseContext(), ActivitySearch.class);
                            startActivityForResult(intent, REQUEST_SEARCH_SCALE);
                        break;
                        default:
                    }
                }
            });
        }

    };

    /** Получаем отложеные настройки.
     * Смс команды которые пришли но не были отработаны.
     */
    void getSettingPostponed(){
        Cursor cursor = new CommandTable(this).getAllEntries();
        ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
        Map<String, ContentValues> map = mQueryMap.getRows();
        cursor.close();

        for (Map.Entry<String,ContentValues> entry : map.entrySet()){
            new SmsCommand(getApplicationContext()).execute(entry.getValue().getAsString(CommandTable.KEY_COMMAND), entry.getValue().getAsString(CommandTable.KEY_VALUE));
            new CommandTable(this).removeEntry(Integer.valueOf(entry.getKey()));
        }
    }

    /** Обработчик показаний заряда батареи и температуры.
     * Возвращяет время обновления в секундах.
     */
    final OnEventResultBatteryTemperature onEventResultBatteryTemperature = new OnEventResultBatteryTemperature() {
        /** Сообщение
         * @param battery Заряд батареи в процентах.
         * @param temperature Температура в градусах.
         * @return Время обновления показаний заряда батареи и температуры в секундах.*/
        @Override
        public int batteryTemperature(final int battery, final int temperature) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBarBattery.updateProgress(battery);
                    temperatureProgressBar.updateProgress(temperature);
                }
            });
            return 5; //Обновляется через секунд
        }
    };
}