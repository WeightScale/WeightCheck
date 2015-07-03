//Ищет весы
package com.victjava.scales;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.konst.module.ScaleModule;

import java.util.ArrayList;

public class ActivitySearch extends Activity implements View.OnClickListener {

    private BroadcastReceiver broadcastReceiver; //приёмник намерений
    private BluetoothAdapter bluetooth; //блютуз адаптер
    private ArrayList<BluetoothDevice> foundDevice; //чужие устройства
    private ArrayAdapter<BluetoothDevice> bluetoothAdapter; //адаптер имён
    private IntentFilter intentFilter; //фильтр намерений
    private ListView listView; //список весов
    private TextView textViewLog; //лог событий

    //private LinearLayout linearScreen;//лайаут для экрана показывать когда загрузились настройки

    //public static int versionNumber;
    //public static String versionName = "";
    //static boolean flag_connect = false;

    //private boolean doubleBackToExitPressedOnce;
    //==================================================================================================================
    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            if (bluetooth.isDiscovering()) {
                bluetooth.cancelDiscovery();
            }
            scaleModule.init(Main.versionName, (BluetoothDevice) foundDevice.toArray()[i]);
        }
    };

    //==================================================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.search);

        setTitle(getString(R.string.Search_scale)); //установить заголовок

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        //Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        textViewLog = (TextView) findViewById(R.id.textLog);
        bluetooth = BluetoothAdapter.getDefaultAdapter();

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case BluetoothAdapter.ACTION_DISCOVERY_STARTED: //поиск начался
                            log(R.string.discovery_started);
                            foundDevice.clear();
                            bluetoothAdapter.notifyDataSetChanged();
                            setTitle(getString(R.string.discovery_started)); //установить заголовок

                            setProgressBarIndeterminateVisibility(true);
                            break;
                        case BluetoothDevice.ACTION_FOUND:  //найдено устройство
                            BluetoothDevice bd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            foundDevice.add(bd);
                            bluetoothAdapter.notifyDataSetChanged();
                            //BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            String name = null;
                            if (bd != null) {
                                name = bd.getName();
                            }
                            if (name != null) {
                                log(R.string.device_found, name);
                            }
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:  //поиск завершён
                            log("Поиск завершён");
                            setProgressBarIndeterminateVisibility(false);
                            break;
                        case BluetoothDevice.ACTION_ACL_CONNECTED:
                            setProgressBarIndeterminateVisibility(false);
                            try {
                                setTitle(" \"" + ScaleModule.getNameBluetoothDevice() + "\", v." + ScaleModule.getNumVersion()); //установить заголовок
                            } catch (Exception e) {
                                setTitle(" \"" + e.getMessage() + "\", v." + ScaleModule.getNumVersion()); //установить заголовок      }
                            }
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                            setTitle(getString(R.string.Search_scale)); //установить заголовок
                            break;
                        default:
                    }
                }
            }
        };

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, intentFilter);

        if (bluetooth != null) {
            if (bluetooth.isEnabled()) {
                log(R.string.bluetooth_on, true);
            } else {
                log(R.string.bluetooth_off, true);
                bluetooth.enable();
            }
        }

        foundDevice = new ArrayList<>();

        for (int i = 0; Preferences.contains(ActivityPreferences.KEY_ADDRESS + i); i++) { //заполнение списка
            foundDevice.add(bluetooth.getRemoteDevice(Preferences.read(ActivityPreferences.KEY_ADDRESS + i, "")));
        }
        bluetoothAdapter = new BluetoothListAdapter(this, foundDevice);

        findViewById(R.id.buttonSearchBluetooth).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);

        listView = (ListView) findViewById(R.id.listViewDevices);  //список весов
        listView.setAdapter(bluetoothAdapter);
        listView.setOnItemClickListener(onItemClickListener);

        if (foundDevice.isEmpty()) {
            bluetooth.startDiscovery();
        }
        /*String msg = "0503285426 coffa=0.25687 coffb gogusr=kreogen.lg@gmail.com gogpsw=htcehc25";
        String str = encodeMessage(msg);
        decodeMessage("+380503285426",str);
        byte[] pdu = fromHexString("079183503082456201000C9183503082456200004A33DCCC56DBE16EB5DCC82C4FA7C98059AC86CBED7423B33C9D2E8FD47235DE5E07B8EB68B91A1D8FBDD543359CCC7EC7CC72F8482D57CFED7AC0FA6E46AFCD351C");

        Intent intent = new Intent(IncomingSMSReceiver.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", new Object[] { pdu });
        sendBroadcast(intent);*/
    }

    //==================================================================================================================
    private void exit() {
        if (bluetooth.isDiscovering()) {
            bluetooth.cancelDiscovery();
        }
        unregisterReceiver(broadcastReceiver);

        for (int i = 0; Preferences.contains(ActivityPreferences.KEY_ADDRESS + i); i++) { //стереть прошлый список
            Preferences.remove(ActivityPreferences.KEY_ADDRESS + i);
        }
        for (int i = 0; i < foundDevice.size(); i++) { //сохранить новый список
            Preferences.write(ActivityPreferences.KEY_ADDRESS + i, ((BluetoothDevice) foundDevice.toArray()[i]).getAddress());
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit();
    }

    //==================================================================================================================
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    //==================================================================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_search, menu);
        return true;
    }

    //==================================================================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.checks_all:
                startActivity(new Intent(this, ActivityListChecks.class));
                break;
            case R.id.search:
                registerReceiver(broadcastReceiver, new IntentFilter());
                unregisterReceiver(broadcastReceiver);
                registerReceiver(broadcastReceiver, intentFilter);
                bluetooth.startDiscovery();
                break;
            case R.id.exit:
                //onDestroy();
                finish();
                break;
            default:
        }
        return true;
    }

    //==================================================================================================================
    void log(int resource) { //для ресурсов
        textViewLog.setText(getString(resource) + '\n' + textViewLog.getText());
    }

    //==================================================================================================================
    public void log(String string) { //для текста
        textViewLog.setText(string + '\n' + textViewLog.getText());
    }

    //==================================================================================================================
    void log(int resource, boolean toast) { //для текста
        textViewLog.setText(getString(resource) + '\n' + textViewLog.getText());
        if (toast) {
            Toast.makeText(getBaseContext(), resource, Toast.LENGTH_SHORT).show();
        }
    }

    //==================================================================================================================
    void log(int resource, String str) { //для ресурсов с текстовым дополнением
        textViewLog.setText(getString(resource) + ' ' + str + '\n' + textViewLog.getText());
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
            case R.id.buttonSearchBluetooth:
                registerReceiver(broadcastReceiver, new IntentFilter());
                unregisterReceiver(broadcastReceiver);
                registerReceiver(broadcastReceiver, intentFilter);
                bluetooth.startDiscovery();
                break;
            default:
        }
    }

    public final ScaleModule scaleModule = new ScaleModule() {
        AlertDialog.Builder dialog;
        private ProgressDialog dialogSearch;

        @Override
        public void handleResultConnect(final ResultConnect result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (result) {
                        case STATUS_LOAD_OK:
                            setResult(RESULT_OK, new Intent());
                            finish();
                            break;
                        case STATUS_SCALE_UNKNOWN:
                            log(ScaleModule.getNameBluetoothDevice() + ' ' + getString(R.string.not_scale));
                            break;
                        case STATUS_ATTACH_START:
                            listView.setEnabled(false);
                            dialogSearch = new ProgressDialog(ActivitySearch.this);
                            dialogSearch.setCancelable(false);
                            dialogSearch.setIndeterminate(false);
                            dialogSearch.show();
                            dialogSearch.setContentView(R.layout.custom_progress_dialog);
                            TextView tv1 = (TextView) dialogSearch.findViewById(R.id.textView1);
                            tv1.setText(getString(R.string.Connecting) + '\n' + ScaleModule.getNameBluetoothDevice());

                            setProgressBarIndeterminateVisibility(true);
                            setTitle(getString(R.string.Connecting) + getString(R.string.app_name) + ' ' + ScaleModule.getNameBluetoothDevice()); //установить заголовок
                            break;
                        case STATUS_ATTACH_FINISH:
                            listView.setEnabled(true);
                            setProgressBarIndeterminateVisibility(false);
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
        public void handleConnectError(final ResultError error, final String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (error) {
                        case TERMINAL_ERROR:
                            dialog = new AlertDialog.Builder(ActivitySearch.this);
                            dialog.setTitle(getString(R.string.preferences_error));
                            dialog.setCancelable(false);
                            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    //doubleBackToExitPressedOnce = true;
                                    onBackPressed();
                                }
                            });
                            dialog.setMessage(s);
                            Toast.makeText(getBaseContext(), R.string.preferences_error, Toast.LENGTH_SHORT).show();
                            setTitle(getString(R.string.app_name) + ": " + getString(R.string.preferences_error));
                            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(ActivitySearch.this, ActivityPreferences.class));
                                    dialogInterface.dismiss();
                                }
                            });
                            dialog.show();
                            log(s);
                            break;
                        case MODULE_ERROR:
                            dialog = new AlertDialog.Builder(ActivitySearch.this);
                            dialog.setTitle("Ошибка в настройках");
                            dialog.setCancelable(false);
                            dialog.setNegativeButton("Закрыть", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    onBackPressed();
                                }
                            });
                            dialog.setMessage("Запросите настройки у администратора. Настройки должен выполнять опытный пользователь. Ошибка("+s+ ')');
                            Toast.makeText(getBaseContext(), R.string.preferences_error, Toast.LENGTH_SHORT).show();
                            setTitle(getString(R.string.app_name) + ": админ настройки неправельные");
                            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(ActivitySearch.this, ActivityTuning.class));
                                    dialogInterface.dismiss();
                                }
                            });
                            dialog.show();
                            break;
                        case CONNECT_ERROR:
                            //setTitle(getString(R.string.app_name) + getString(R.string.error_connect)); //установить заголовок
                            log(getString(R.string.Error_connect)+s);
                            break;
                        default:
                    }
                }
            });
        }

    };

}