//Активность для стартовой настройки весов
package com.victjava.scales;

//import android.content.SharedPreferences;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.konst.module.InterfaceVersions;
import com.konst.module.ScaleModule;
import com.victjava.scales.bootloader.ActivityBootloader;
import com.victjava.scales.provider.SenderTable;

import java.util.HashMap;
import java.util.Map;

//import android.preference.PreferenceManager;

public class ActivityTuning extends PreferenceActivity {
    protected Dialog dialog;

    public static final String KEY_POINT1           = "key_point1";
    public static final String KEY_POINT2           = "key_point2";
    public static final String KEY_WEIGHT_MAX       = "key_weight_max";
    public static final String KEY_COEFFICIENT_A    = "key_coefficient_a";
    public static final String KEY_CALL_BATTERY     = "key_call_battery";
    public static final String KEY_SHEET            = "key_sheet";
    public static final String KEY_USER             = "key_user";
    public static final String KEY_PASSWORD         = "key_password";
    public static final String KEY_PHONE            = "key_phone";
    public static final String KEY_SENDER           = "key_sender";
    public static final String KEY_UPDATE           = "key_update";

    private final Point point1 = new Point(Integer.MIN_VALUE, 0);
    private final Point point2 = new Point(Integer.MIN_VALUE, 0);
    private boolean flag_restore;
    final Map<String, InterfacePreference> mapTuning = new HashMap<>();

    public ActivityTuning(){
        mapTuning.put(KEY_POINT1, new Point1());
        mapTuning.put(KEY_POINT2, new Point2());
        mapTuning.put(KEY_WEIGHT_MAX, new WeightMax());
        mapTuning.put(KEY_COEFFICIENT_A, new CoefficientA());
        mapTuning.put(KEY_CALL_BATTERY, new CallBattery());
        mapTuning.put(KEY_SHEET, new Sheet());
        mapTuning.put(KEY_USER, new User());
        mapTuning.put(KEY_PASSWORD, new Password());
        mapTuning.put(KEY_PHONE, new Phone());
        mapTuning.put(KEY_SENDER, new Sender());
        mapTuning.put(KEY_UPDATE, new Update());
    }

    interface InterfacePreference {
        void setup(Preference name) throws Exception;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName("my_preferences");
        preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
        addPreferencesFromResource(R.xml.tuning);

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        process();
    }

    void process() {
        for (Map.Entry<String, InterfacePreference> preferenceEntry : mapTuning.entrySet()) {
            Preference name = findPreference(preferenceEntry.getKey());
            if (name != null) {
                try {
                    preferenceEntry.getValue().setup(name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class Point1 implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        String str = ScaleModule.feelWeightSensor();
                        ScaleModule.setSensorTenzo(Integer.valueOf(str));
                        point1.x = Integer.valueOf(str);
                        point1.y = 0;
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        flag_restore = true;
                        return true;
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            });
        }
    }

    class Point2 implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    try {
                        String str = ScaleModule.feelWeightSensor();
                        if (str.isEmpty()) {
                            Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        ScaleModule.setSensorTenzo(Integer.valueOf(str));
                        point2.x = Integer.valueOf(str);
                        point2.y = Integer.valueOf(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        flag_restore = true;
                        return true;
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            });
        }
    }

    class WeightMax implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.Max_weight) + ScaleModule.getWeightMax() + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || Integer.valueOf(o.toString()) < Main.default_max_weight) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    ScaleModule.setWeightMax(Integer.valueOf(o.toString()));
                    ScaleModule.setWeightMargin((int) (ScaleModule.getWeightMax() * 1.2));
                    preference.setTitle(getString(R.string.Max_weight) + ScaleModule.getWeightMax() + getString(R.string.scales_kg));
                    Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    flag_restore = true;
                    return true;
                }
            });
        }
    }

    class CoefficientA implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.ConstantA) + Float.toString(ScaleModule.getCoefficientA()));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    try {
                        ScaleModule.setCoefficientA(Float.valueOf(o.toString()));
                        preference.setTitle(getString(R.string.ConstantA) + Float.toString(ScaleModule.getCoefficientA()));
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        flag_restore = true;
                        return true;
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            });
        }
    }

    class CallBattery implements InterfacePreference{

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.Battery) + ScaleModule.getBattery() + '%');
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > Main.default_max_battery) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (ScaleModule.setModuleBatteryCharge(0)) {
                        ScaleModule.setBattery(Integer.valueOf(o.toString()));
                        preference.setTitle(getString(R.string.Battery) + ScaleModule.getBattery() + '%');
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
    }

    class Sheet implements InterfacePreference{

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.Table) + '"' + ScaleModule.getSpreadSheet() + '"');
            name.setSummary(getString(R.string.TEXT_MESSAGE7));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (ScaleModule.setModuleSpreadsheet(o.toString())) {
                        preference.setTitle(getString(R.string.Table) + '"' + o + '"');
                        ScaleModule.setSpreadSheet(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    preference.setTitle(getString(R.string.Table) + "???");
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                    return false;
                }
            });
        }
    }

    class User implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setSummary("Account Google: " + ScaleModule.getUserName());
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (ScaleModule.setModuleUserName(o.toString())) {
                        preference.setSummary("Account Google: " + o);
                        ScaleModule.setUserName(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    preference.setSummary("Account Google: ???");
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
    }

    class Password implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setSummary("Password account Google - " + ScaleModule.getPassword());
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (ScaleModule.setModulePassword(o.toString())) {
                        preference.setSummary("Password account Google: " + o);
                        ScaleModule.setPassword(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    preference.setSummary("Password account Google: ???");
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                    return false;
                }
            });
        }
    }

    class Phone implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setSummary("Phone for Boss - " + ScaleModule.getPhone());
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (ScaleModule.setModulePhone(o.toString())) {
                        preference.setSummary("Phone for Boss: " + o);
                        ScaleModule.setPhone(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    preference.setSummary("Phone for Boss: ???");
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                    return false;
                }
            });
        }
    }

    class Sender implements InterfacePreference{
        Context mContext;
        SenderTable senderTable;

        @Override
        public void setup(Preference name) throws Exception {
            mContext = getApplicationContext();
            senderTable = new SenderTable(mContext);

            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    openListDialog();
                    return false;
                }
            });
        }

        public void openListDialog() {
            final Cursor senders = senderTable.getAllEntries();
            //final Cursor emails = contentResolver.query(CommonDataKinds.Email.CONTENT_URI, null,CommonDataKinds.Email.CONTACT_ID + " = " + mContactId, null, null);
            if (senders == null) {
                return;
            }
            if (senders.moveToFirst()) {
                String[] columns = {SenderTable.KEY_TYPE};
                int[] to = {R.id.text1};
                SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(mContext, R.layout.item_list_sender, senders, columns, to);
                cursorAdapter.setViewBinder(new ListBinder());
                //LayoutInflater layoutInflater = mContext.getLayoutInflater();
                LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View convertView = layoutInflater.inflate(R.layout.dialog_sender, null);
                ListView listView = (ListView) convertView.findViewById(R.id.component_list);
                TextView dialogTitle = (TextView) convertView.findViewById(R.id.dialog_title);
                dialogTitle.setText("Выбрать отсылатель");
                listView.setAdapter(cursorAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Checkable v = (Checkable) view;
                        v.toggle();
                        if (v.isChecked())
                            senderTable.updateEntry((int)id, SenderTable.KEY_SYS, 1);
                        else
                            senderTable.updateEntry((int) id, SenderTable.KEY_SYS, 0);
                    }
                });
                dialog.setContentView(convertView);
                dialog.setCancelable(false);
                ImageButton buttonSelectAll = (ImageButton) dialog.findViewById(R.id.buttonSelectAll);
                buttonSelectAll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        selectedAll();
                    }
                });
                ImageButton buttonUnSelect = (ImageButton) dialog.findViewById(R.id.buttonUnselect);
                buttonUnSelect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        unselectedAll();
                    }
                });
                ImageButton buttonBack = (ImageButton) dialog.findViewById(R.id.buttonBack);
                buttonBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        }

        private void selectedAll(){
            Cursor cursor = senderTable.getAllEntries();
            try {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    do {
                        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
                        senderTable.updateEntry(id,SenderTable.KEY_SYS, 1);
                    } while (cursor.moveToNext());
                }
            }catch (Exception e){ }
        }

        private void unselectedAll(){
            Cursor cursor = senderTable.getAllEntries();
            try {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    do {
                        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
                        senderTable.updateEntry(id, SenderTable.KEY_SYS, 0);
                    } while (cursor.moveToNext());
                }
            }catch (Exception e){ }
        }

        private class ListBinder implements SimpleCursorAdapter.ViewBinder {
            int enable;
            int type;
            String text;

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

                switch (view.getId()) {
                    case R.id.text1:
                        enable = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_SYS));
                        type = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_TYPE));
                        text = SenderTable.TypeSender.values()[type].toString();
                        //text = cursor.getString(cursor.getColumnIndex(SenderTable.KEY_TYPE));
                        setViewText((TextView) view, text);
                        if(enable > 0)
                            ((Checkable) view).setChecked(true);
                        else
                            ((Checkable) view).setChecked(false);
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

    }

    class Update implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            if (ScaleModule.getVersion() != null) {
                if (ScaleModule.getNumVersion() < Main.microSoftware) {
                    name.setSummary(getString(R.string.Is_new_version));
                    //name.setEnabled(true);
                } else {
                    name.setSummary(getString(R.string.Scale_update));
                    //name.setEnabled(false);
                }

                name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        //Scales.vScale.backupPreference();
                        String hardware = ScaleModule.getModuleHardware();
                        if (hardware.isEmpty()) {
                            hardware = "MBC04.36.2";
                        }
                        Intent intent = new Intent(ActivityTuning.this, ActivityBootloader.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        else
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(ActivityPreferences.KEY_ADDRESS, ScaleModule.getAddressBluetoothDevice());
                        intent.putExtra(InterfaceVersions.CMD_HARDWARE, hardware);
                        intent.putExtra(InterfaceVersions.CMD_VERSION, ScaleModule.getNumVersion());
                        startActivity(intent);
                        return false;
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flag_restore) {
            if (point1.x != Integer.MIN_VALUE && point2.x != Integer.MIN_VALUE) {
                ScaleModule.setCoefficientA((float) (point1.y - point2.y) / (point1.x - point2.x));
                ScaleModule.setCoefficientB(point1.y - ScaleModule.getCoefficientA() * point1.x);
            }
            ScaleModule.setLimitTenzo((int) (ScaleModule.getWeightMax() / ScaleModule.getCoefficientA()));
            if (ScaleModule.getLimitTenzo() > 0xffffff) {
                ScaleModule.setLimitTenzo(0xffffff);
                ScaleModule.setWeightMax((int) (0xffffff * ScaleModule.getCoefficientA()));
            }
            if (ScaleModule.writeData()) {
                Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
            }
        }

    }
}
