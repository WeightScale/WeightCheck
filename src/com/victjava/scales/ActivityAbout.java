package com.victjava.scales;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import com.konst.module.ScaleModule;

/*
 * Created by Kostya on 26.04.14.
 */
public class ActivityAbout extends Activity {
    ScaleModule scaleModule;
    Main main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setTitle(getString(R.string.About));

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        main = (Main)getApplication();
        scaleModule = main.getScaleModule();

        TextView textSoftVersion = (TextView) findViewById(R.id.textSoftVersion);
        textSoftVersion.setText(main.getVersionName() + ' ' + String.valueOf(main.getVersionNumber()));

        TextView textSettings = (TextView) findViewById(R.id.textSettings);
        textSettings.append(getString(R.string.Version_scale) + scaleModule.getNumVersion() + '\n');
        try {
            textSettings.append(getString(R.string.Name_module_bluetooth) + scaleModule.getNameBluetoothDevice() + '\n');
        } catch (Exception e) {
            textSettings.append(getString(R.string.Name_module_bluetooth) + '\n');
        }
        try {
            textSettings.append(getString(R.string.Address_bluetooth) + scaleModule.getAddressBluetoothDevice() + '\n');
        } catch (Exception e) {
            textSettings.append(getString(R.string.Address_bluetooth) + '\n');
        }

        textSettings.append("\n");
        textSettings.append(getString(R.string.Operator) + main.getNetworkOperatorName() + '\n');
        textSettings.append(getString(R.string.Number_phone) + main.getTelephoneNumber() + '\n');
        textSettings.append("\n");
        textSettings.append(getString(R.string.Battery) + scaleModule.getBattery() + " %" + '\n');
        if (scaleModule.isAttach()) {
            textSettings.append(getString(R.string.Temperature) + scaleModule.getModuleTemperature() + '°' + 'C' + '\n');
        }
        textSettings.append(getString(R.string.Coefficient) + scaleModule.getCoefficientA() + '\n');
        textSettings.append(getString(R.string.MLW) + scaleModule.getWeightMax() + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append("\n");
        textSettings.append(getString(R.string.Table_google_disk) + scaleModule.getSpreadSheet() + '\n');
        textSettings.append(getString(R.string.User_google_disk) + scaleModule.getUserName() + '\n');
        textSettings.append(getString(R.string.Phone_for_sms) + scaleModule.getPhone() + '\n');
        textSettings.append("\n");
        textSettings.append(getString(R.string.Off_timer) + scaleModule.getTimeOff() + ' ' + getString(R.string.minute) + '\n');
        textSettings.append(getString(R.string.Step_capacity_scale) + main.getStepMeasuring() + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append(getString(R.string.Capture_weight) + main.getAutoCapture() + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append("\n");

        TextView textAuthority = (TextView) findViewById(R.id.textAuthority);
        textAuthority.append(getString(R.string.Copyright) + '\n');
        textAuthority.append(getString(R.string.Reserved) + '\n');
    }
}
