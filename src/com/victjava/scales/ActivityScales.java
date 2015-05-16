//Создаёт интерфейс управления весами
package com.victjava.scales;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.*;
import android.widget.*;
import com.konst.module.HandlerBatteryTemperature;
import com.konst.module.HandlerScaleConnect;
import com.konst.module.ScaleModule;
import com.victjava.scales.provider.CheckDBAdapter;
import com.victjava.scales.provider.ErrorDBAdapter;
import com.victjava.scales.service.ServiceProcessTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityScales extends Activity implements View.OnClickListener, View.OnLongClickListener {

    private SimpleCursorAdapter namesAdapter;
    CheckDBAdapter checkTable;
    private BroadcastReceiver broadcastReceiver; //приёмник намерений
    private BluetoothAdapter bluetooth; //блютуз адаптер
    private final ArrayList<BluetoothDevice> foundDevice = new ArrayList<>(); //чужие устройства
    private Vibrator vibrator; //вибратор
    private BatteryProgressBar progressBarBattery; //текст батареи
    private TemperatureProgressBar temperatureProgressBar;
    private ImageView imageViewRemote;
    private ImageView imageNewCheck;
    private ListView listView;

    //private TextView textViewPercentage;
    private LinearLayout linearBatteryTemp;              //лайаут для батарея температура
    public static final String PARAM_PENDING_INTENT = "pendingIntent";
    public static final String PARAM_DEVICE = "device";

    static final int REQUEST_CONNECT = 1;
    static final int REQUEST_SEARCH_SCALE = 2;
    public static final int STATUS_OK = 3;
    public static final int STATUS_CONNECT_ERROR = 4;
    public static final int STATUS_SCALE_ERROR = 5;
    public static final int STATUS_SCALE_UNKNOWN = 6;
    public static final int STATUS_TERMINAL_ERROR = 7;

    private boolean doubleBackToExitPressedOnce;
    public static boolean isScaleConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO лог всех событий внизу экрана
        super.onCreate(savedInstanceState);

        /*Configuration  config = new Configuration(getResources().getConfiguration());
        config.locale = Locale.ENGLISH ;
        getResources().updateConfiguration(config,getResources().getDisplayMetrics());*/
        checkTable = new CheckDBAdapter(this);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Main.telephoneNumber = telephonyManager.getLine1Number();
        Main.simNumber = telephonyManager.getSimSerialNumber();
        Main.networkOperatorName = telephonyManager.getNetworkOperatorName();
        Main.networkCountry = telephonyManager.getNetworkCountryIso();
        int state = telephonyManager.getSimState();

        //List<SMS.SmsObject> smsAllList = new SMS(this).getAllSms();

        //List<SMS.SmsObject> smsInboxList = new SMS(this).getInboxSms();
        //smsInboxList = new SMS(this).getInboxSms("Kyivstar");
        //Map<String, ContentValues> smsSentList = new SMS(this).getSentSms();
        //Map<String, ContentValues> smsDraftList = new SMS(this).getDraftSms();

        bluetooth = BluetoothAdapter.getDefaultAdapter();
        switch (state) {
            case TelephonyManager.SIM_STATE_READY:
                if (bluetooth == null) {
                    Toast.makeText(getBaseContext(), R.string.bluetooth_no, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(getBaseContext(), R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
                    bluetooth.enable();
                    assert bluetooth != null;
                    while (!bluetooth.isEnabled()) ;//ждем включения bluetooth
                    setupScale();
                }
                break;
            default:
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
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.imageNewCheck:
                vibrator.vibrate(100);
                //threadBattery.setAutoNull(); //todo надо стелать сброс автоноль
                startActivity(new Intent(getBaseContext(), ActivityContact.class).setAction("check"));
                break;
            case R.id.imageViewRemote:
                vibrator.vibrate(100);
                openSearch();
                break;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScaleModule.processBattery(true, handlerBatteryTemperature);
        namesAdapter.changeCursor(checkTable.getAllNoReadyCheck());
    }

    @Override
    protected void onPause() {
        super.onPause();
        ScaleModule.processBattery(false, handlerBatteryTemperature);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            //exit();
            return;
        }
        bluetooth.cancelDiscovery();
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
            /*case R.id.tuning:
                startActivity(new Intent(this, ActivityTuning.class));
            break;*/
            case R.id.search:
                openSearch();
                break;
            case R.id.exit:
                closeOptionsMenu();
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
        }
        return true;
    }

    void setupScale() {
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

        try {
            PackageManager packageManager = getPackageManager();
            if (packageManager != null) {
                PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
                Main.versionNumber = packageInfo.versionCode;
                Main.versionName = packageInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            new ErrorDBAdapter(this).insertNewEntry("100", e.getMessage());
        }

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            switch (bluetooth.getState()) {
                                case BluetoothAdapter.STATE_OFF:
                                    Toast.makeText(getBaseContext(), R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
                                    new Internet(getApplicationContext()).turnOnWiFiConnection(false);
                                    bluetooth.enable();
                                    break;
                                case BluetoothAdapter.STATE_TURNING_ON:
                                    Toast.makeText(getBaseContext(), R.string.bluetooth_turning_on, Toast.LENGTH_SHORT).show();
                                    break;
                                case BluetoothAdapter.STATE_ON:
                                    Toast.makeText(getBaseContext(), R.string.bluetooth_on, Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED://устройство отсоеденено
                            vibrator.vibrate(200);
                            listView.setOnItemClickListener(null);
                            linearBatteryTemp.setVisibility(View.INVISIBLE);
                            imageViewRemote.setImageDrawable(getResources().getDrawable(R.drawable.rss_off));
                            imageNewCheck.setEnabled(false);
                            isScaleConnect = false;
                            break;
                        case BluetoothDevice.ACTION_ACL_CONNECTED://найдено соеденено
                            vibrator.vibrate(200);
                            listView.setOnItemClickListener(onItemClickListener);
                            linearBatteryTemp.setVisibility(View.VISIBLE);
                            imageViewRemote.setImageDrawable(getResources().getDrawable(R.drawable.rss_on));
                            imageNewCheck.setEnabled(true);
                            isScaleConnect = true;
                            break;
                        default:
                    }
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
        connectBluetooth();

    }

    private void listCheckSetup() {
        listView = (ListView) findViewById(R.id.listViewWeights);
        /*listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startActivity(new Intent().setClass(getApplicationContext(), ActivityCheck.class).putExtra("id", String.valueOf(l)));
            }
        });*/
        try {
            setReadyOldChecks();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateList();
    }

    final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startActivity(new Intent().setClass(getApplicationContext(), ActivityCheck.class).putExtra("id", String.valueOf(id)));
        }
    };

    void updateList() {
        Cursor cursor = checkTable.getAllNoReadyCheck();
        if (cursor == null) {
            return;
        }
        String[] columns = {CheckDBAdapter.KEY_ID,
                CheckDBAdapter.KEY_DATE_CREATE,
                CheckDBAdapter.KEY_TIME_CREATE,
                CheckDBAdapter.KEY_VENDOR,
                CheckDBAdapter.KEY_WEIGHT_FIRST,
                CheckDBAdapter.KEY_WEIGHT_SECOND,
                CheckDBAdapter.KEY_WEIGHT_NETTO,
                CheckDBAdapter.KEY_PRICE_SUM, CheckDBAdapter.KEY_DIRECT, CheckDBAdapter.KEY_DIRECT, CheckDBAdapter.KEY_DIRECT};
        int[] to = {R.id.check_id, R.id.date, R.id.time, R.id.vendor, R.id.gross_row, R.id.tare_row, R.id.netto_row, R.id.sum_row, R.id.imageDirect, R.id.gross, R.id.tare};

        namesAdapter = new SimpleCursorAdapter(this, R.layout.item_check, cursor, columns, to);
        //namesAdapter = new MyCursorAdapter(this, R.layout.item_check, cursor, columns, to);
        namesAdapter.setViewBinder(new ListCheckViewBinder());
        listView.setAdapter(namesAdapter);
    }

    void connectBluetooth() {

        String define_device = Preferences.read(ActivityPreferences.KEY_LAST, "");
        scaleModule.init(Main.versionName, define_device/*, msgHandler*/);

    }

    private void exit() {
        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
        scaleModule.removeCallbacksAndMessages(null);
        scaleModule.dettach();
        bluetooth.disable();
        while (bluetooth.isEnabled()) ;
        //startService(new Intent(this, ServiceSentSheetServer.class));// Запускаем сервис для передачи данных на google disk//todo временно отключен
        startService(new Intent(this, ServiceProcessTask.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //AlertDialog.Builder dialog;
        setProgressBarIndeterminateVisibility(false);
        switch (resultCode) {
            case RESULT_OK:
                //scaleModule.obtainMessage(HandlerScaleConnect.Result.STATUS_LOAD_OK.ordinal()).sendToTarget();
                scaleModule.handleModuleConnect(HandlerScaleConnect.Result.STATUS_LOAD_OK);
                break;
            case RESULT_CANCELED:
                scaleModule.obtainMessage(RESULT_CANCELED, "Connect error").sendToTarget();
                //scaleModule.handleModuleConnectError(HandlerScaleConnect.Result.STATUS_CONNECT_ERROR, "Connect error");
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
                    direct = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_DIRECT));
                    if (direct == CheckDBAdapter.DIRECT_UP) {
                        setViewText((TextView) view, getString(R.string.Tape));
                    } else {
                        setViewText((TextView) view, getString(R.string.Gross));
                    }
                    break;
                case R.id.tare:
                    direct = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_DIRECT));
                    if (direct == CheckDBAdapter.DIRECT_DOWN) {
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

    void openSearch() {
        //setTitle(getString(R.string.app_name) + getString(R.string.search)); //установить заголовок

        //scaleSection.setVisibility(View.INVISIBLE);
        listView.setEnabled(false);
        scaleModule.dettach();
        startActivityForResult(new Intent(getBaseContext(), ActivitySearch.class), REQUEST_SEARCH_SCALE);
    }

    //==============================================================================================================
    private void setReadyOldChecks() throws Exception {
        Cursor cursor = checkTable.getNotReady();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    String date = cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE));
                    try {
                        long day = dayDiff(new Date(), new SimpleDateFormat("dd.MM.yy").parse(date));
                        if (day > Preferences.read(ActivityPreferences.KEY_DAY_CLOSED_CHECK, 5)) {
                            int id = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_ID));
                            checkTable.updateEntry(id, CheckDBAdapter.KEY_IS_READY, 1);
                            checkTable.setCheckReady(id);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
    }

    long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;//86400000
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return day1 - day2;
    }

    public final ScaleModule scaleModule = new ScaleModule() {
        AlertDialog.Builder dialog;
        ProgressDialog dialogSearch;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case RESULT_CANCELED:
                    listView.setEnabled(false);
                    break;
                default:
            }
        }

        @Override
        public void handleModuleConnect(final Result what) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (what) {
                        case STATUS_LOAD_OK:
                            try {
                                setTitle(getString(R.string.app_name) + " \"" + ScaleModule.getName() + "\", v." + ScaleModule.getNumVersion()); //установить заголовок
                            } catch (Exception e) {
                                setTitle(getString(R.string.app_name) + " , v." + ScaleModule.getNumVersion()); //установить заголовок
                            }
                            Preferences.write(ActivityPreferences.KEY_LAST, ScaleModule.getAddress());
                            listView.setEnabled(true);
                            processBattery(true, handlerBatteryTemperature);
                            break;
                        case STATUS_SETTINGS_UNCORRECTED:
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
                            dialog.setMessage("Запросите настройки у администратора. Настройки должен выполнять опытный пользователь");
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
                        case STATUS_SCALE_UNKNOWN:

                            break;
                        case STATUS_ATTACH_START:
                            dialogSearch = new ProgressDialog(ActivityScales.this);
                            dialogSearch.setCancelable(false);
                            dialogSearch.setIndeterminate(false);
                            dialogSearch.show();
                            dialogSearch.setContentView(R.layout.custom_progress_dialog);
                            TextView tv1 = (TextView) dialogSearch.findViewById(R.id.textView1);
                            tv1.setText(getString(R.string.Connecting) + '\n' + ScaleModule.getName());
                            break;
                        case STATUS_ATTACH_FINISH:
                            if (dialogSearch.isShowing()) {
                                dialogSearch.dismiss();
                            }
                        break;
                        default:
                    }
                }
            });
        }

        @Override
        public void handleModuleConnectError(final Result what, final String error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (what) {
                        case STATUS_TERMINAL_ERROR:
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
                            dialog.setMessage(error);
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
                        case STATUS_CONNECT_ERROR:
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

    final HandlerBatteryTemperature handlerBatteryTemperature = new HandlerBatteryTemperature() {
        @Override
        public int handlerBatteryTemperature(final int battery, final int temperature) {
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