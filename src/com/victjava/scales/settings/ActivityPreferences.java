//Активность настроек
package com.victjava.scales.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.*;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.Toast;
import com.konst.module.Commands;
import com.konst.module.ScaleModule;
import com.victjava.scales.*;
import com.victjava.scales.bootloader.ActivityBootloader;
import com.victjava.scales.provider.PreferencesTable;
import com.victjava.scales.provider.TaskTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityPreferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    static ScaleModule scaleModule;
    static Main main;
    private boolean flagChange;

    interface InterfacePreference {
        void setup(Preference name) throws Exception;
    }

    final Map<String, InterfacePreference> mapPreferences = new HashMap<>();

    void process() {
        for (Map.Entry<String, InterfacePreference> preferenceEntry : mapPreferences.entrySet()) {
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

    class PreferenceName implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            try {
                name.setSummary(scaleModule.getNameBluetoothDevice());
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty()) {
                            Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (scaleModule.setModuleName(o.toString())) {
                            preference.setSummary(o.toString());
                            Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + o.toString(), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
            } catch (Exception e) {
                name.setEnabled(false);
            }
        }
    }

    class PreferenceAddress implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setSummary(scaleModule.getAddressBluetoothDevice());
        }
    }

    class PreferenceNull implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setSummary(getString(R.string.sum_zeroing));
            if (!scaleModule.isAttach()) {
                name.setEnabled(false);
            }
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (scaleModule.setScaleNull()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
    }

    class PreferenceFilter implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.filter_adc) + ' ' + String.valueOf(scaleModule.getFilterADC()));
            name.setSummary(getString(R.string.sum_filter_adc) + ' ' + getString(R.string.The_range_is_from_0_to) + getResources().getInteger(R.integer.default_adc_filter));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_adc_filter)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    try {
                        if (scaleModule.setModuleFilterADC(Integer.valueOf(o.toString()))) {
                            scaleModule.setFilterADC(Integer.valueOf(o.toString()));
                            preference.setTitle(getString(R.string.filter_adc) + ' ' + String.valueOf(scaleModule.getFilterADC()));
                            Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + o.toString(), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            });
        }
    }

    class PreferenceUpdate implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            if (scaleModule.getVersion() != null) {
                if (scaleModule.getNumVersion() < main.microSoftware) {
                    name.setSummary(getString(R.string.Is_new_version));
                    name.setEnabled(true);
                } else {
                    name.setSummary(getString(R.string.Scale_update));
                    name.setEnabled(false);
                }

                name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        //Scales.vScale.backupPreference();
                        String hardware = scaleModule.getModuleHardware();
                        if (hardware.isEmpty()) {
                            hardware = "MBC04.36.2";
                        }
                        Intent intent = new Intent(ActivityPreferences.this, ActivityBootloader.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        else
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(getString(R.string.KEY_ADDRESS), scaleModule.getAddressBluetoothDevice());
                        intent.putExtra(Commands.CMD_HARDWARE.getName(), hardware);
                        intent.putExtra(Commands.CMD_VERSION.getName(), scaleModule.getNumVersion());
                        startActivity(intent);
                        return false;
                    }
                });
            }
        }
    }

    class PreferenceTimer implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.Timer_off) + ' ' + scaleModule.getTimeOff() + ' ' + getString(R.string.minute));
            name.setSummary(getString(R.string.sum_timer) + ' ' + getString(R.string.range) + getResources().getInteger(R.integer.default_min_time_off) + getString(R.string.to) + getResources().getInteger(R.integer.default_max_time_off));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString())
                            || Integer.valueOf(o.toString()) < getResources().getInteger(R.integer.default_min_time_off)
                            || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_max_time_off)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    try {
                        if (scaleModule.setModuleTimeOff(Integer.valueOf(o.toString()))) {
                            scaleModule.setTimeOff(Integer.valueOf(o.toString()));
                            preference.setTitle(getString(R.string.Timer_off) + ' ' + scaleModule.getTimeOff() + ' ' + getString(R.string.minute));
                            Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + scaleModule.getTimeOff() + ' ' + getString(R.string.minute), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            });
        }
    }

    class PreferenceTimerNull implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.Time) + ' ' + scaleModule.getTimerNull() + ' ' + getString(R.string.second));
            name.setSummary(getString(R.string.sum_time_auto_zero) + ' ' + getResources().getInteger(R.integer.default_max_time_auto_null) + ' ' + getString(R.string.second));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_max_time_auto_null)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    scaleModule.setTimerNull(Integer.valueOf(o.toString()));
                    preference.setTitle(getString(R.string.Time) + ' ' + scaleModule.getTimerNull() + ' ' + getString(R.string.second));
                    preference.getEditor().putInt(getString(R.string.KEY_TIMER_NULL), scaleModule.getTimerNull());
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + scaleModule.getTimerNull() + ' ' + getString(R.string.second), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    class PreferenceMaxNull implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.sum_weight) + ' ' + scaleModule.getWeightError() + ' ' + getString(R.string.scales_kg));
            name.setSummary(getString(R.string.sum_max_null) + ' ' + getResources().getInteger(R.integer.default_limit_auto_null) + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_limit_auto_null)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    scaleModule.setWeightError(Integer.valueOf(o.toString()));
                    preference.setTitle(getString(R.string.sum_weight) + ' ' + scaleModule.getWeightError() + ' ' + getString(R.string.scales_kg));
                    preference.getEditor().putInt(getString(R.string.KEY_TIMER_NULL), scaleModule.getWeightError());
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + scaleModule.getWeightError() + ' ' + getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    class PreferenceStep implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.measuring_step) + ' ' + main.getStepMeasuring() + ' ' + getString(R.string.scales_kg));
            name.setSummary(getString(R.string.The_range_is_from_1_to) + getResources().getInteger(R.integer.default_max_step_scale) + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_max_step_scale)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    main.setStepMeasuring(Integer.valueOf(o.toString()));
                    preference.setTitle(getString(R.string.measuring_step) + ' ' + main.getStepMeasuring() + ' ' + getString(R.string.scales_kg));
                    preference.getEditor().putInt(getString(R.string.KEY_STEP), main.getStepMeasuring());
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + main.getStepMeasuring() + ' ' + getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    class PreferenceAutoCapture implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.auto_capture) + ' ' + main.getAutoCapture() + ' ' + getString(R.string.scales_kg));
            name.setSummary(getString(R.string.Range_between) + (getResources().getInteger(R.integer.default_min_auto_capture) + getResources().getInteger(R.integer.default_delta_auto_capture)) + ' ' + getString(R.string.scales_kg) +
                    getString(R.string.and) + getResources().getInteger(R.integer.default_max_auto_capture) + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || Integer.valueOf(o.toString()) < getResources().getInteger(R.integer.default_min_auto_capture) || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_max_auto_capture)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    main.setAutoCapture(Integer.valueOf(o.toString()));
                    if (main.getAutoCapture() < getResources().getInteger(R.integer.default_min_auto_capture) + getResources().getInteger(R.integer.default_delta_auto_capture)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    preference.setTitle(getString(R.string.auto_capture) + ' ' + main.getAutoCapture() + ' ' + getString(R.string.scales_kg));
                    preference.getEditor().putInt(getString(R.string.KEY_AUTO_CAPTURE), main.getAutoCapture());
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + main.getAutoCapture() + ' ' + getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    class PreferenceDayClosedCheck implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.closed_checks) + ' ' + getResources().getInteger(R.integer.default_day_close_check) + ' ' + getString(R.string.day));
            name.setSummary(getString(R.string.sum_closed_checks));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_day_close_check)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    main.setDayClosedCheck(Integer.valueOf(o.toString()));
                    preference.setTitle(getString(R.string.closed_checks) + ' ' + getResources().getInteger(R.integer.default_day_close_check) + ' ' + getString(R.string.day));
                    preference.getEditor().putInt(getString(R.string.KEY_DAY_CLOSED_CHECK), getResources().getInteger(R.integer.default_day_close_check));
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + getResources().getInteger(R.integer.default_day_close_check) + ' ' + getString(R.string.day), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    class PreferenceDayCheckDelete implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.sum_delete_check) + ' ' + String.valueOf(getResources().getInteger(R.integer.default_day_delete_check)) + ' ' + getString(R.string.day));
            name.setSummary(getString(R.string.sum_removing_checks));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_day_delete_check)) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    main.setDayDeleteCheck(Integer.valueOf(o.toString()));
                    preference.setTitle(getString(R.string.sum_delete_check) + ' ' + String.valueOf(getResources().getInteger(R.integer.default_day_delete_check)) + ' ' + getString(R.string.day));
                    preference.getEditor().putInt(getString(R.string.KEY_DAY_CHECK_DELETE), getResources().getInteger(R.integer.default_day_delete_check));
                    Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    class PreferenceAbout implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setSummary(getString(R.string.version) + main.getVersionName() + ' ' + Integer.toString(main.getVersionNumber()));
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent().setClass(getApplicationContext(), ActivityAbout.class));
                    return false;
                }
            });
        }
    }

    class PreferenceEmptyCheckBox implements InterfacePreference {

        @Override
        public void setup(Preference name) throws Exception {
            name.setSummary(getString(R.string.checkbox_empty_summary));
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startDialog();
                    return true;
                }
            });
        }

        void startDialog(){
            AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityPreferences.this);
            dialog.setTitle(getString(R.string.Cleaning));
            dialog.setCancelable(false);
            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    clearCheckboxContact();
                }
            });
            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.setMessage(getString(R.string.Clear_checkbox_dialog_message));
            dialog.show();
        }

        void clearCheckboxContact(){
            Cursor data = getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                    new String[] {BaseColumns._ID, ContactsContract.Data.DATA5},
                    '(' + ContactsContract.Data.MIMETYPE+"='"+ ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE+'\''+" or "+ ContactsContract.Data.MIMETYPE+"='"+ ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE+'\''+')'
                            +" and "+ ContactsContract.Data.DATA5+" = 1", null, null);
            try {
                data.moveToFirst();
                if(!data.isAfterLast()){
                    do {
                        int id = data.getInt(data.getColumnIndex(BaseColumns._ID));
                        updateData5(id, ContactsContract.Data.DATA5, 0);
                    } while (data.moveToNext());
                }
                data.close();
            }catch (Exception e){}
        }

        public void updateData5(long _rowIndex, String key, int value) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(BaseColumns._ID + "=?", new String[]{String.valueOf(_rowIndex)})
                    .withValue(key, value)
                    .build());
            try {
                getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            }
        }
    }

    class Admin implements InterfacePreference{
        private EditText input;

        @Override
        public void setup(Preference name) throws Exception {
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startDialog();
                    return true;
                }
            });
        }

        void startDialog(){
            AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityPreferences.this);
            dialog.setTitle("ВВОД КОДА");
            input = new EditText(ActivityPreferences.this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            dialog.setView(input);
            dialog.setCancelable(false);
            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (input.getText() != null) {
                        String string = input.getText().toString();
                        if (!string.isEmpty()){
                            if("343434".equals(string) || string.equals(scaleModule.getModuleServiceCod())){
                                startActivityForResult(new Intent().setClass(getApplicationContext(),ActivityTuning.class),1);
                            }
                        }
                    }
                }
            });
            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.setMessage("Введи код доступа к административным настройкам");
            dialog.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        main = (Main)getApplication();
        scaleModule = ((Main)getApplication()).getScaleModule();

        /*mapPreferences.put(getString(R.string.KEY_NAME), new PreferenceName());
        mapPreferences.put(getString(R.string.KEY_ADDRESS), new PreferenceAddress());
        mapPreferences.put(getString(R.string.KEY_NULL), new PreferenceNull());
        mapPreferences.put(getString(R.string.KEY_FILTER), new PreferenceFilter());
        mapPreferences.put(getString(R.string.KEY_UPDATE), new PreferenceUpdate());
        mapPreferences.put(getString(R.string.KEY_TIMER), new PreferenceTimer());
        mapPreferences.put(getString(R.string.KEY_TIMER_NULL), new PreferenceTimerNull());
        mapPreferences.put(getString(R.string.KEY_MAX_NULL), new PreferenceMaxNull());
        mapPreferences.put(getString(R.string.KEY_STEP), new PreferenceStep());
        mapPreferences.put(getString(R.string.KEY_AUTO_CAPTURE), new PreferenceAutoCapture());
        mapPreferences.put(getString(R.string.KEY_DAY_CLOSED_CHECK), new PreferenceDayClosedCheck());
        mapPreferences.put(getString(R.string.KEY_DAY_CHECK_DELETE), new PreferenceDayCheckDelete());
        mapPreferences.put(getString(R.string.KEY_EMPTY_CHECKBOX), new PreferenceEmptyCheckBox());
        mapPreferences.put(getString(R.string.KEY_ABOUT), new PreferenceAbout());
        mapPreferences.put(getString(R.string.KEY_ADMIN), new Admin());*/

        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.preferences);
        //}else{
        //    getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragmentPreferences()).commit();
        //}


        //PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.camera, false);

        //addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        //getPreferenceManager().setSharedPreferencesName(Preferences.PREFERENCES);
        //process1();
        //process();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flagChange) {
            try {
                int entryID = Integer.valueOf(new PreferencesTable(this).insertAllEntry().getLastPathSegment());
                new TaskTable(this).setPreferenceReady(entryID);
                //new TaskTable(getApplicationContext()).insertNewTask(TaskCommand.TaskType.TYPE_PREF_SEND_SHEET_DISK, entryID, 0, "preferences");
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(s.equals(getString(R.string.KEY_FILTER)))
            flagChange = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_OK)
            flagChange = true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PrefsFragmentPreferences extends PreferenceFragment {

        enum EnumPreference{
            NULL(R.string.KEY_NULL){
                @Override
                void setup(Preference name)throws Exception {
                    name.setSummary( name.getContext().getString(R.string.sum_zeroing));
                    if (!scaleModule.isAttach()) {
                        name.setEnabled(false);
                    }
                    name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (scaleModule.setScaleNull()) {
                                Toast.makeText(preference.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                                return true;
                            }

                            Toast.makeText(preference.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });
                }
            },
            FILTER(R.string.KEY_FILTER){
                @Override
                void setup(Preference name)throws Exception {
                    Context context = name.getContext();
                    name.setTitle(context.getString(R.string.filter_adc) + ' ' + String.valueOf(scaleModule.getFilterADC()));
                    name.setSummary(context.getString(R.string.sum_filter_adc) + ' ' + context.getString(R.string.The_range_is_from_0_to) + context.getResources().getInteger(R.integer.default_adc_filter));
                    name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if (o.toString().isEmpty() || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_adc_filter)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            try {
                                if (scaleModule.setModuleFilterADC(Integer.valueOf(o.toString()))) {
                                    scaleModule.setFilterADC(Integer.valueOf(o.toString()));
                                    preference.setTitle(context.getString(R.string.filter_adc) + ' ' + String.valueOf(scaleModule.getFilterADC()));
                                    Toast.makeText(context, context.getString(R.string.preferences_yes) + ' ' + o.toString(), Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                            } catch (Exception e) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            }
                            return false;
                        }
                    });
                }
            },
            UPDATE(R.string.KEY_UPDATE){
                @Override
                void setup(Preference name)throws Exception {
                    if (scaleModule.getVersion() != null) {
                        if (scaleModule.getNumVersion() < main.microSoftware) {
                            name.setSummary(name.getContext().getString(R.string.Is_new_version));
                            name.setEnabled(true);
                        } else {
                            name.setSummary(name.getContext().getString(R.string.Scale_update));
                            name.setEnabled(false);
                        }

                        name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                //Scales.vScale.backupPreference();
                                String hardware = scaleModule.getModuleHardware();
                                if (hardware.isEmpty()) {
                                    hardware = "MBC04.36.2";
                                }
                                Intent intent = new Intent(preference.getContext(), ActivityBootloader.class);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                else
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra(preference.getContext().getString(R.string.KEY_ADDRESS), scaleModule.getAddressBluetoothDevice());
                                intent.putExtra(Commands.CMD_HARDWARE.getName(), hardware);
                                intent.putExtra(Commands.CMD_VERSION.getName(), scaleModule.getNumVersion());
                                preference.getContext().startActivity(intent);
                                return false;
                            }
                        });
                    }
                }
            },
            TIMER(R.string.KEY_TIMER){
                @Override
                void setup(Preference name)throws Exception {
                    Context context = name.getContext();
                    name.setTitle(context.getString(R.string.Timer_off) + ' ' + scaleModule.getTimeOff() + ' ' + context.getString(R.string.minute));
                    name.setSummary(context.getString(R.string.sum_timer) + ' ' + context.getString(R.string.range) + context.getResources().getInteger(R.integer.default_min_time_off) + context.getString(R.string.to) + context.getResources().getInteger(R.integer.default_max_time_off));
                    name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if (o.toString().isEmpty() || "0".equals(o.toString())
                                    || Integer.valueOf(o.toString()) < context.getResources().getInteger(R.integer.default_min_time_off)
                                    || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_max_time_off)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            try {
                                if (scaleModule.setModuleTimeOff(Integer.valueOf(o.toString()))) {
                                    scaleModule.setTimeOff(Integer.valueOf(o.toString()));
                                    preference.setTitle(context.getString(R.string.Timer_off) + ' ' + scaleModule.getTimeOff() + ' ' + context.getString(R.string.minute));
                                    Toast.makeText(context, context.getString(R.string.preferences_yes) + ' ' + scaleModule.getTimeOff() + ' ' + context.getString(R.string.minute), Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                            } catch (Exception e) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            }
                            return false;
                        }
                    });
                }
            },
            TIMER_NULL(R.string.KEY_TIMER_NULL){
                @Override
                void setup(Preference name)throws Exception {
                    Context context = name.getContext();
                    name.setTitle(context.getString(R.string.Time) + ' ' + scaleModule.getTimerNull() + ' ' + context.getString(R.string.second));
                    name.setSummary(context.getString(R.string.sum_time_auto_zero) + ' ' + context.getResources().getInteger(R.integer.default_max_time_auto_null) + ' ' + context.getString(R.string.second));
                    name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_max_time_auto_null)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            scaleModule.setTimerNull(Integer.valueOf(o.toString()));
                            preference.setTitle(context.getString(R.string.Time) + ' ' + scaleModule.getTimerNull() + ' ' + context.getString(R.string.second));
                            preference.getEditor().putInt(preference.getKey(), scaleModule.getTimerNull());
                            Toast.makeText(context, context.getString(R.string.preferences_yes) + ' ' + scaleModule.getTimerNull() + ' ' + context.getString(R.string.second), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
                }
            },
            MAX_NULL(R.string.KEY_MAX_NULL){
                @Override
                void setup(Preference name)throws Exception {
                    Context context = name.getContext();
                    name.setTitle(context.getString(R.string.sum_weight) + ' ' + scaleModule.getWeightError() + ' ' + context.getString(R.string.scales_kg));
                    name.setSummary(context.getString(R.string.sum_max_null) + ' ' + context.getResources().getInteger(R.integer.default_limit_auto_null) + ' ' + context.getString(R.string.scales_kg));
                    name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_limit_auto_null)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            scaleModule.setWeightError(Integer.valueOf(o.toString()));
                            preference.setTitle(context.getString(R.string.sum_weight) + ' ' + scaleModule.getWeightError() + ' ' + context.getString(R.string.scales_kg));
                            preference.getEditor().putInt(preference.getKey(), scaleModule.getWeightError());
                            Toast.makeText(context, context.getString(R.string.preferences_yes) + ' ' + scaleModule.getWeightError() + ' ' + context.getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
                }
            },
            STEP(R.string.KEY_STEP){
                @Override
                void setup(Preference name)throws Exception {
                    Context context = name.getContext();
                    name.setTitle(context.getString(R.string.measuring_step) + ' ' + main.getStepMeasuring() + ' ' + context.getString(R.string.scales_kg));
                    name.setSummary(context.getString(R.string.The_range_is_from_1_to) + context.getResources().getInteger(R.integer.default_max_step_scale) + ' ' + context.getString(R.string.scales_kg));
                    name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_max_step_scale)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            main.setStepMeasuring(Integer.valueOf(o.toString()));
                            preference.setTitle(context.getString(R.string.measuring_step) + ' ' + main.getStepMeasuring() + ' ' + context.getString(R.string.scales_kg));
                            preference.getEditor().putInt(preference.getKey(), main.getStepMeasuring());
                            Toast.makeText(context, context.getString(R.string.preferences_yes) + ' ' + main.getStepMeasuring() + ' ' + context.getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
                }
            },
            AUTO_CAPTURE(R.string.KEY_AUTO_CAPTURE){
                @Override
                void setup(Preference name)throws Exception {
                    Context context = name.getContext();
                    name.setTitle(context.getString(R.string.auto_capture) + ' ' + main.getAutoCapture() + ' ' + context.getString(R.string.scales_kg));
                    name.setSummary(context.getString(R.string.Range_between)
                            + (context.getResources().getInteger(R.integer.default_min_auto_capture)
                            + context.getResources().getInteger(R.integer.default_delta_auto_capture)) + ' ' + context.getString(R.string.scales_kg) +
                            context.getString(R.string.and) + context.getResources().getInteger(R.integer.default_max_auto_capture) + ' ' + context.getString(R.string.scales_kg));
                    name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if (o.toString().isEmpty()
                                    || Integer.valueOf(o.toString()) < context.getResources().getInteger(R.integer.default_min_auto_capture)
                                    || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_max_auto_capture)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            main.setAutoCapture(Integer.valueOf(o.toString()));
                            if (main.getAutoCapture() < context.getResources().getInteger(R.integer.default_min_auto_capture) + context.getResources().getInteger(R.integer.default_delta_auto_capture)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            preference.setTitle(context.getString(R.string.auto_capture) + ' ' + main.getAutoCapture() + ' ' + context.getString(R.string.scales_kg));
                            //preference.getEditor().putInt(preference.getKey(), 123/*main.getAutoCapture()*/);
                            //Main.preferencesScale.write(preference.getKey(), main.getAutoCapture());
                            Toast.makeText(context, context.getString(R.string.preferences_yes) + ' ' + main.getAutoCapture() + ' ' + context.getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
                }
            },
            DAY_CLOSE_CHECK(R.string.KEY_DAY_CLOSED_CHECK){
                @Override
                void setup(Preference name)throws Exception {
                    Context context = name.getContext();
                    name.setTitle(context.getString(R.string.closed_checks) + ' ' +  main.getDayClosedCheck() + ' ' + context.getString(R.string.day));
                    name.setSummary(context.getString(R.string.sum_closed_checks));
                    name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_day_close_check)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            main.setDayClosedCheck(Integer.valueOf(o.toString()));
                            preference.setTitle(context.getString(R.string.closed_checks) + ' ' + main.getDayClosedCheck() + ' ' + context.getString(R.string.day));
                            preference.getEditor().putInt(preference.getKey(), context.getResources().getInteger(R.integer.default_day_close_check));
                            Toast.makeText(context, context.getString(R.string.preferences_yes) + ' ' + main.getDayClosedCheck() + ' ' + context.getString(R.string.day), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
                }
            },
            DAY_DELETE_CHECK(R.string.KEY_DAY_CHECK_DELETE){
                @Override
                void setup(Preference name)throws Exception {
                    Context context = name.getContext();
                    name.setTitle(context.getString(R.string.sum_delete_check) + ' ' + main.getDayDeleteCheck() + ' ' + context.getString(R.string.day));
                    name.setSummary(context.getString(R.string.sum_removing_checks));
                    name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_day_delete_check)) {
                                Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            main.setDayDeleteCheck(Integer.valueOf(o.toString()));
                            preference.setTitle(context.getString(R.string.sum_delete_check) + ' ' + main.getDayDeleteCheck() + ' ' + context.getString(R.string.day));
                            preference.getEditor().putInt(preference.getKey(), context.getResources().getInteger(R.integer.default_day_delete_check));
                            Toast.makeText(context, R.string.preferences_yes + " " + main.getDayDeleteCheck(), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
                }
            },
            EMPTY_CHECK_BOX(R.string.KEY_EMPTY_CHECKBOX){
                Context context;
                @Override
                void setup(Preference name)throws Exception {
                    context = name.getContext();
                    name.setSummary(context.getString(R.string.checkbox_empty_summary));
                    name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startDialog();
                            return true;
                        }
                    });
                }

                void startDialog(){
                    AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                    dialog.setTitle(context.getString(R.string.Cleaning));
                    dialog.setCancelable(false);
                    dialog.setPositiveButton(context.getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            clearCheckboxContact();
                        }
                    });
                    dialog.setNegativeButton(context.getString(R.string.Close), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.setMessage(context.getString(R.string.Clear_checkbox_dialog_message));
                    dialog.show();
                }

                void clearCheckboxContact(){
                    Cursor data = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                            new String[] {BaseColumns._ID, ContactsContract.Data.DATA5},
                            '(' + ContactsContract.Data.MIMETYPE+"='"+ ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE+'\''+" or "+ ContactsContract.Data.MIMETYPE+"='"+ ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE+'\''+')'
                                    +" and "+ ContactsContract.Data.DATA5+" = 1", null, null);
                    try {
                        data.moveToFirst();
                        if(!data.isAfterLast()){
                            do {
                                int id = data.getInt(data.getColumnIndex(BaseColumns._ID));
                                updateData5(id, ContactsContract.Data.DATA5, 0);
                            } while (data.moveToNext());
                        }
                        data.close();
                    }catch (Exception e){}
                }

                public void updateData5(long _rowIndex, String key, int value) {
                    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                    ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(BaseColumns._ID + "=?", new String[]{String.valueOf(_rowIndex)})
                            .withValue(key, value)
                            .build());
                    try {
                        context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                    } catch (RemoteException | OperationApplicationException e) {
                        e.printStackTrace();
                    }
                }
            },
            ABOUT(R.string.KEY_ABOUT){
                @Override
                void setup(Preference name)throws Exception {
                    name.setSummary(name.getContext().getString(R.string.version) + main.getVersionName() + ' ' + Integer.toString(main.getVersionNumber()));
                    name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            preference.getContext().startActivity(new Intent().setClass(preference.getContext(), ActivityAbout.class));
                            return false;
                        }
                    });
                }
            },;
            /*ADMIN(R.string.KEY_ADMIN){
                @Override
                void setup(Preference name) {
                    name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startDialog();
                            return true;
                        }
                    });
                }

                void startDialog(){
                    AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityPreferences.this);
                    dialog.setTitle("ВВОД КОДА");
                    input = new EditText(ActivityPreferences.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    dialog.setView(input);
                    dialog.setCancelable(false);
                    dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (input.getText() != null) {
                                String string = input.getText().toString();
                                if (!string.isEmpty()){
                                    if("343434".equals(string) || string.equals(scaleModule.getModuleServiceCod())){
                                        startActivityForResult(new Intent().setClass(getApplicationContext(),ActivityTuning.class),1);
                                    }
                                }
                            }
                        }
                    });
                    dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.setMessage("Введи код доступа к административным настройкам");
                    dialog.show();
                }
            };*/

            private final int resId;
            abstract void setup(Preference name)throws Exception;

            EnumPreference(int key){
                resId = key;
            }

            public int getResId() { return resId; }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            //PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
            //getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            //getPreferenceManager().setSharedPreferencesName(Preferences.PREFERENCES);




            initCameraPreferences();

            /*Preference camera = findPreference(getString(R.string.KEY_ADMIN));
            camera.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getFragmentManager().beginTransaction().replace(android.R.id.content, new ActivityTuning.SettingsCamera()).commit();
                    return true;
                }
            });

            Preference admin = findPreference(getString(R.string.KEY_ADMIN));
            camera.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getFragmentManager().beginTransaction().replace(android.R.id.content, new ActivityTuning.SettingsCamera()).commit();
                    return true;
                }
            });*/
        }

        public void initCameraPreferences(){
            /*PreferenceManager preferenceManager = getPreferenceManager();
            preferenceManager.setSharedPreferencesName(Preferences.PREF_CAMERA);
            preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);*/
            for (EnumPreference enumPreference : EnumPreference.values()){
                Preference preference = findPreference(getString(enumPreference.getResId()));
                try {
                    enumPreference.setup(preference);
                } catch (Exception e) {
                    preference.setEnabled(false);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PrefFragmentAdmin extends PreferenceFragment{
        private EditText input;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            startDialog();
        }

        void startDialog(){
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setTitle("ВВОД КОДА");
            input = new EditText(getActivity());
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            dialog.setView(input);
            dialog.setCancelable(false);
            dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (input.getText() != null) {
                        String string = input.getText().toString();
                        if (!string.isEmpty()){
                            if("343434".equals(string) || string.equals(scaleModule.getModuleServiceCod())){
                                getPreferenceManager().setSharedPreferencesName(Preferences.PREFERENCES);
                                getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
                                addPreferencesFromResource(R.xml.admin_preferences);
                                return;
                            }
                        }

                    }
                    Toast.makeText(getActivity(), "Неверный код", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            });
            dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    getActivity().finish();
                }
            });
            dialog.setMessage("Введи код доступа к административным настройкам");
            dialog.show();
        }
    }

    /*@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
    }*/
}
