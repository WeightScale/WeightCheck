package com.victjava.scales.bootloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.konst.bootloader.*;
import com.konst.module.*;
import com.victjava.scales.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 25.12.13
 * Time: 21:49
 * To change this template use File | Settings | File Templates.
 */
public class ActivityBootloader extends Activity implements View.OnClickListener {
    private ImageView startBoot, buttonBack;
    private TextView textViewLog;
    private ProgressDialog progressDialog;
    private BootModule bootModule;

    private AVRProgrammer programmer;
    private String addressDevice = "";
    private String hardware = "362";
    private static final String dirDeviceFiles = "device";
    private static final String dirBootFiles = "bootfiles";

    protected boolean flagProgramsFinish = true;

    static final int REQUEST_CONNECT_BOOT = 1;
    static final int REQUEST_CONNECT_SCALE = 2;

    private static final SparseArray<String> mapCodeDevice = new SparseArray<>();

    static {
        mapCodeDevice.put(0x9514, "atmega328.xml");
        mapCodeDevice.put(0x9406, "atmega168.xml");
        mapCodeDevice.put(0x930a, "atmega88.xml");
    }

    private class ThreadDoDeviceDependent extends AsyncTask<Void, Void, Boolean> {
        protected AlertDialog.Builder dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            buttonBack.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                programmer.doDeviceDependent();
            } catch (Exception e) {
                handlerProgrammed.obtainMessage(HandlerBootloader.Result.MSG_LOG.ordinal(), e.getMessage() + " \r\n").sendToTarget();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            flagProgramsFinish = true;
            buttonBack.setEnabled(true);
            dialog = new AlertDialog.Builder(ActivityBootloader.this);
            dialog.setCancelable(false);

            if (b) {
                dialog.setTitle(getString(R.string.Warning_Loading_settings));
                dialog.setMessage(getString(R.string.TEXT_MESSAGE1));
                dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case DialogInterface.BUTTON_POSITIVE:
                                Intent intent = new Intent(getBaseContext(), ActivityConnect.class);
                                intent.putExtra("address", addressDevice);
                                startActivityForResult(intent, REQUEST_CONNECT_SCALE);
                                break;
                            default:
                        }
                    }
                });
                dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
            } else {
                dialog.setTitle(getString(R.string.Warning_Error));
                dialog.setMessage(getString(R.string.TEXT_MESSAGE2));
                dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            }
            dialog.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bootloder);

        addressDevice = getIntent().getStringExtra(ActivityPreferences.KEY_ADDRESS);
        hardware = getIntent().getStringExtra(InterfaceVersions.CMD_HARDWARE);

        //Spinner spinnerField = (Spinner) findViewById(R.id.spinnerField);
        textViewLog = (TextView) findViewById(R.id.textLog);
        startBoot = (ImageView) findViewById(R.id.buttonBoot);
        startBoot.setOnClickListener(this);
        startBoot.setEnabled(false);
        startBoot.setAlpha(128);
        buttonBack = (ImageView) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(this);

        progressDialog = new ProgressDialog(this);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.Warning_Connect));
        dialog.setCancelable(false);
        dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        try {
                            bootModule.init( addressDevice);
                            bootModule.attach();
                        } catch (Exception e) {
                            onEventConnectResult.handleConnectError(Module.ResultError.CONNECT_ERROR, e.getMessage());
                        }
                        break;
                    default:
                }
            }
        });
        dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        dialog.setMessage(getString(R.string.TEXT_MESSAGE));
        dialog.show();

        try {
            bootModule = new BootModule("bootloader", onEventConnectResult);
            log(getString(R.string.bluetooth_off));
        } catch (Exception e) {
            log(e.getMessage());
            finish();
        }
    }

    final OnEventConnectResult onEventConnectResult = new OnEventConnectResult() {
        private AlertDialog.Builder dialog;

        @Override
        public void handleResultConnect(final Module.ResultConnect result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (result) {
                        case STATUS_LOAD_OK:
                            startBoot.setEnabled(true);
                            startBoot.setAlpha(255);
                            dialog = new AlertDialog.Builder(ActivityBootloader.this);
                            dialog.setTitle(getString(R.string.Warning_update));
                            dialog.setCancelable(false);
                            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    switch (i) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            if (!startProgramed()) {
                                                flagProgramsFinish = true;
                                            }
                                            break;
                                        default:
                                    }
                                }
                            });
                            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            });
                            dialog.setMessage(getString(R.string.TEXT_MESSAGE5));
                            dialog.show();
                            break;
                        default:
                    }
                }
            });
        }

        @Override
        public void handleConnectError(Module.ResultError error, String s) {
            switch (error) {
                case CONNECT_ERROR:
                    Intent intent = new Intent(getBaseContext(), ActivityConnect.class);
                    intent.putExtra("address", addressDevice);
                    startActivityForResult(intent, REQUEST_CONNECT_BOOT);
                    break;
                default:
            }
        }
    };

    final HandlerBootloader handlerProgrammed = new HandlerBootloader() {

        @Override
        public void handleMessage(Message msg) {
            switch (HandlerBootloader.Result.values()[msg.what]) {
                case MSG_LOG:
                    log(msg.obj.toString());// обновляем TextView
                    break;
                case MSG_SHOW_DIALOG:
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMessage(msg.obj.toString());
                    progressDialog.setMax(msg.arg1);
                    progressDialog.setProgress(0);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    break;
                case MSG_UPDATE_DIALOG:
                    progressDialog.setProgress(msg.arg1);
                    break;
                case MSG_CLOSE_DIALOG:
                    progressDialog.dismiss();
                    break;
                default:
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonBack:
                exit();
                break;
            case R.id.buttonBoot:
                if (!startProgramed()) {
                    flagProgramsFinish = true;
                }
                break;
            default:
        }
    }

    @Override
    public void onBackPressed() {
        exit();
        //return;
    }

    void log(String string) { //для текста
        //textViewLog.append(string);
        textViewLog.setText(string + '\n' + textViewLog.getText());
    }

    void exit() {
        if (flagProgramsFinish) {
            //Preferences.load(getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE));
            Preferences.write(ActivityPreferences.KEY_FLAG_UPDATE, true);
            bootModule.dettach();
            BluetoothAdapter.getDefaultAdapter().disable();
            while (BluetoothAdapter.getDefaultAdapter().isEnabled()) ;
            finish();
        }
    }

    static boolean isBootloader() { //Является ли весами и какой версии
        String vrs = Module.getModuleVersion(); //Получаем версию весов
        return vrs.startsWith("BOOT");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            log("Connected...");
            switch (requestCode) {
                case REQUEST_CONNECT_BOOT:
                    //scaleModule.obtainMessage(HandlerScaleConnect.Result.STATUS_LOAD_OK.ordinal()).sendToTarget();
                    onEventConnectResult.handleResultConnect(Module.ResultConnect.STATUS_LOAD_OK);
                    break;
                case REQUEST_CONNECT_SCALE:
                    log(getString(R.string.Loading_settings));
                    if (ScaleModule.isScales()) {
                        //restorePreferences(); //todo сделать загрузку настроек которые сохранены пере перепрограммированием.
                        log(getString(R.string.Settings_loaded));
                        break;
                    }
                    log(getString(R.string.Scale_no_defined));
                    log(getString(R.string.Setting_no_loaded));
                    break;
                default:
            }
        } else {
            log("Not connected...");
        }
    }

    boolean startProgramed() {
        programmer = new AVRProgrammer(handlerProgrammed) {
            @Override
            public void sendByte(byte b) {
                Module.sendByte(b);
            }

            @Override
            public int getByte() {
                return Module.getByte();
            }
        };
        if (!programmer.isProgrammerId()) {
            log(getString(R.string.Not_programmer));
            return false;
        }
        flagProgramsFinish = false;
        log(getString(R.string.Programmer_defined));
        try {

            int desc = programmer.getDescriptor();

            if (mapCodeDevice.get(desc) == null) {
                throw new Exception("Фаил с дескриптором " + desc + " не найден! ");
            }

            String deviceFileName = mapCodeDevice.get(desc);
            if (deviceFileName.isEmpty()) {
                throw new Exception("Device name not specified!");
            }

            log("Device " + deviceFileName);

            /*37894_mbc04.36.2_4.hex пример имени файла прошивки
            |desc||hardware ||version     desc- это сигнатура 1 и сигнатура 2     микроконтролера 0x94 ## 0x06
                                        hardware- это версия платы              mbc04.36.2
                                        version- этоверсия программы платы      4                   */
            String constructBootFile = new StringBuilder()
                    .append(desc).append('_')               //дескриптор сигнатура 1 и сигнатура 2
                    .append(hardware.toLowerCase())         //hardware- это версия платы
                    .append('_')
                    .append(Main.microSoftware)             //version- этоверсия программы платы
                    .append(".hex").toString();
            log(getString(R.string.TEXT_MESSAGE3) + constructBootFile);
            String[] bootFiles = getAssets().list(dirBootFiles);
            String bootFileName = "";
            if (Arrays.asList(bootFiles).contains(constructBootFile)) {
                bootFileName = constructBootFile;
            }

            if (bootFileName.isEmpty()) {
                throw new Exception("Boot фаил отсутствует для этого устройства!\r\n");
            }

            InputStream inputDeviceFile = getAssets().open(dirDeviceFiles + '/' + deviceFileName);
            InputStream inputHexFile = getAssets().open(dirBootFiles + '/' + bootFileName);


            startBoot.setEnabled(false);
            startBoot.setAlpha(128);
            programmer.doJob(inputDeviceFile, inputHexFile);
            new ThreadDoDeviceDependent().execute();
        } catch (IOException e) {
            handlerProgrammed.obtainMessage(HandlerBootloader.Result.MSG_LOG.ordinal(), e.getMessage()).sendToTarget();
            return false;
        } catch (Exception e) {
            handlerProgrammed.obtainMessage(HandlerBootloader.Result.MSG_LOG.ordinal(), e.getMessage()).sendToTarget();
            return false;
        }
        return true;
    }

    public boolean backupPreference() {
        Preferences.load(getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE));

        Preferences.write(InterfaceVersions.CMD_FILTER, ScaleModule.getFilterADC());
        Preferences.write(InterfaceVersions.CMD_TIMER, ScaleModule.getTimeOff());
        Preferences.write(InterfaceVersions.CMD_BATTERY, ScaleModule.getBattery());
        //Main.preferencesUpdate.write(InterfaceVersions.CMD_CALL_TEMP, String.valueOf(coefficientTemp));
        Preferences.write(InterfaceVersions.CMD_SPREADSHEET, ScaleModule.getSpreadSheet());
        Preferences.write(InterfaceVersions.CMD_G_USER, ScaleModule.getUserName());
        Preferences.write(InterfaceVersions.CMD_G_PASS, ScaleModule.getPassword());
        Preferences.write(InterfaceVersions.CMD_DATA_CFA, ScaleModule.getCoefficientA());
        Preferences.write(InterfaceVersions.CMD_DATA_WGM, ScaleModule.getWeightMax());

        //editor.apply();
        return true;
    }

    /*public boolean restorePreferences() {
        if (ScaleModule.isScales()) {
            log("Соединились");
            Preferences.load(getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE));
            ScaleModule.setModuleFilterADC(Preferences.read(InterfaceVersions.CMD_FILTER, Main.default_adc_filter));
            log("Фмльтер "+ BootModule.getFilterADC());
            ScaleModule.setModuleTimeOff(Preferences.read(InterfaceVersions.CMD_TIMER, Main.default_max_time_off));
            log("Время отключения "+ BootModule.getTimeOff());
            ScaleModule.setModuleBatteryCharge(Preferences.read(InterfaceVersions.CMD_BATTERY, Main.default_max_battery));
            log("Заряд батареи "+ BootModule.getBattery());
            //command(InterfaceScaleModule.CMD_CALL_TEMP + Main.preferencesUpdate.read(InterfaceScaleModule.CMD_CALL_TEMP, "0"));
            ScaleModule.setModuleSpreadsheet(Preferences.read(InterfaceVersions.CMD_SPREADSHEET, "weightscale"));
            log("Имя таблици "+ BootModule.getSpreadSheet());
            ScaleModule.setModuleUserName(Preferences.read(InterfaceVersions.CMD_G_USER, ""));
            log("Имя пользователя "+ BootModule.getUserName());
            ScaleModule.setModulePassword(Preferences.read(InterfaceVersions.CMD_G_PASS, ""));
            log("Пароль");
            ScaleModule.setCoefficientA(Preferences.read(InterfaceVersions.CMD_DATA_CFA, 0.0f));
            log("Коэффициент А "+ ScaleModule.getCoefficientA());
            ScaleModule.setWeightMax(Preferences.read(InterfaceVersions.CMD_DATA_WGM, Main.default_max_weight));
            log("Максимальный вес "+ ScaleModule.getWeightMax());
            ScaleModule.setLimitTenzo((int) (ScaleModule.getWeightMax() / ScaleModule.getCoefficientA()));
            log("Лимит датчика "+ ScaleModule.getLimitTenzo());
            ScaleModule.writeData();
        }
        return true;
    }*/


}
