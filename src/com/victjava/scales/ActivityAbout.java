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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setTitle(getString(R.string.About));

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        TextView textSoftVersion = (TextView) findViewById(R.id.textSoftVersion);
        textSoftVersion.setText(Main.versionName + ' ' + String.valueOf(Main.versionNumber));

        TextView textSettings = (TextView) findViewById(R.id.textSettings);
        textSettings.append(getString(R.string.Version_scale) + ScaleModule.getNumVersion() + '\n');
        try {
            textSettings.append(getString(R.string.Name_module_bluetooth) + ScaleModule.getNameBluetoothDevice() + '\n');
        } catch (Exception e) {
            textSettings.append(getString(R.string.Name_module_bluetooth) + '\n');
        }
        try {
            textSettings.append(getString(R.string.Address_bluetooth) + ScaleModule.getAddressBluetoothDevice() + '\n');
        } catch (Exception e) {
            textSettings.append(getString(R.string.Address_bluetooth) + '\n');
        }

        textSettings.append("\n");
        textSettings.append(getString(R.string.Operator) + Main.networkOperatorName + '\n');
        textSettings.append(getString(R.string.Number_phone) + Main.telephoneNumber + '\n');
        textSettings.append("\n");
        textSettings.append(getString(R.string.Battery) + ScaleModule.getBattery() + " %" + '\n');
        if (ScaleModule.isAttach()) {
            textSettings.append(getString(R.string.Temperature) + ScaleModule.getModuleTemperature() + '°' + 'C' + '\n');
        }
        textSettings.append(getString(R.string.Coefficient) + ScaleModule.getCoefficientA() + '\n');
        textSettings.append(getString(R.string.MLW) + ScaleModule.getWeightMax() + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append("\n");
        textSettings.append(getString(R.string.Table_google_disk) + ScaleModule.getSpreadSheet() + '\n');
        textSettings.append(getString(R.string.User_google_disk) + ScaleModule.getUserName() + '\n');
        textSettings.append(getString(R.string.Phone_for_sms) + ScaleModule.getPhone() + '\n');
        textSettings.append("\n");
        textSettings.append(getString(R.string.Off_timer) + ScaleModule.getTimeOff() + ' ' + getString(R.string.minute) + '\n');
        textSettings.append(getString(R.string.Step_capacity_scale) + Main.stepMeasuring + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append(getString(R.string.Capture_weight) + Main.autoCapture + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append("\n");

        TextView textAuthority = (TextView) findViewById(R.id.textAuthority);
        textAuthority.append(getString(R.string.Copyright) + '\n');
        textAuthority.append(getString(R.string.Reserved) + '\n');
    }
}
