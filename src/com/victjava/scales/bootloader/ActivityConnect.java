//���� ����
package com.victjava.scales.bootloader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.konst.module.HandlerScaleConnect;
import com.konst.module.ScaleModule;
import com.victjava.scales.*;

public class ActivityConnect extends Activity implements View.OnClickListener {

    //==================================================================================================================

    private Vibrator vibrator; //��������

    private BroadcastReceiver broadcastReceiver; //������� ���������
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //������ �������
    private BluetoothDevice bluetoothDevice;
    private TextView textViewLog; //��� �������
    private LinearLayout linearScreen;//������ ��� ������ ���������� ����� ����������� ���������

    public static int versionNumber;
    public static String versionName;

    private boolean doubleBackToExitPressedOnce;

    //==================================================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothDevice = bluetoothAdapter.getRemoteDevice(getIntent().getStringExtra("address"));

        setupScale();
    }

    //==================================================================================================================
    private void exit() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit();
    }

    //==================================================================================================================
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            //exit();
            return;
        }
        bluetoothAdapter.cancelDiscovery();
        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_again_to_exit /*Please click BACK again to exit*/, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    //==================================================================================================================
    void log(int resource) { //��� ��������
        textViewLog.setText(getString(resource) + '\n' + textViewLog.getText());
    }

    //==================================================================================================================
    public void log(String string) { //��� ������
        textViewLog.setText(string + '\n' + textViewLog.getText());
    }

    //==================================================================================================================
    void log(int resource, boolean toast) { //��� ������
        textViewLog.setText(getString(resource) + '\n' + textViewLog.getText());
        if (toast) {
            Toast.makeText(getBaseContext(), resource, Toast.LENGTH_SHORT).show();
        }
    }

    //==================================================================================================================
    void log(int resource, String str) { //��� �������� � ��������� �����������
        textViewLog.setText(getString(resource) + ' ' + str + '\n' + textViewLog.getText());
    }

    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void setupScale() {
        /*Window window = getWindow();
        window.requestFeature(Window.FEATURE_CUSTOM_TITLE);*/
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.connect);

        setProgressBarIndeterminateVisibility(true);

        linearScreen = (LinearLayout) findViewById(R.id.searchScreen);
        //linearScreen.setVisibility(View.INVISIBLE);

        textViewLog = (TextView) findViewById(R.id.textLog);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null) {
            if (networkInfo.isAvailable()) //���� ������������
            {
                new Internet(this).turnOnWiFiConnection(false); // ��� ��������� � ������� ���� ������ wifi � bluetooth
            }
        }

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) { //���������� Bluetooth
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                                log(R.string.bluetooth_off);
                                bluetoothAdapter.enable();
                            } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                                log(R.string.bluetooth_turning_on, true);
                            } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                                log(R.string.bluetooth_on, true);
                            }
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:  //���������� �����������
                            vibrator.vibrate(200);
                            log(R.string.bluetooth_disconnected);
                            break;
                        case BluetoothDevice.ACTION_ACL_CONNECTED:  //������� ���������
                            vibrator.vibrate(200);
                            log(R.string.bluetooth_connected);
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:  //����� ��������
                            setTitle(getString(R.string.app_name) + " \"" + versionName + "\", v." + versionNumber); //���������� ���������

                            setProgressBarIndeterminateVisibility(false);
                            break;
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, intentFilter);

        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                log(R.string.bluetooth_off, true);
                bluetoothAdapter.enable();
            } else {
                log(R.string.bluetooth_on, true);
            }
        }
        //}

        PackageInfo packageInfo = null;
        try {
            PackageManager packageManager = getPackageManager();
            if (packageManager != null) {
                packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            log(e.getMessage());
        }

        if (packageInfo != null) {
            versionNumber = packageInfo.versionCode;
        }
        if (packageInfo != null) {
            versionName = packageInfo.versionName;
        }

        setTitle(getString(R.string.app_name) + " \"" + versionName + "\", v." + versionNumber);                        //���������� ���������
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        findViewById(R.id.buttonSearchBluetooth).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);

        //scaleModule.initBoot("bootloader", bluetoothDevice.getAddress());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonBack:
                onBackPressed();
                break;
            case R.id.buttonSearchBluetooth:
                scaleModule.initBoot("bootloader", bluetoothDevice.getAddress());
            break;
            default:
        }
    }

    final ScaleModule scaleModule = new ScaleModule() {

        /*@Override
        public void handleMessage(Message msg) {
            setResult(RESULT_OK, new Intent());
            finish();
        }*/

        @Override
        public void handleModuleConnect(Result what) {
            switch (what){
                case STATUS_LOAD_OK:
                    setResult(RESULT_OK, new Intent());
                    finish();
                break;
            }
        }

        @Override
        public void handleModuleConnectError(Result result, String s) {}

    };

}