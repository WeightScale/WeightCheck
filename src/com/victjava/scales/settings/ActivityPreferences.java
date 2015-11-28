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
import android.widget.Toast;
import com.konst.module.Commands;
import com.konst.module.ScaleModule;
import com.victjava.scales.*;
import com.victjava.scales.bootloader.ActivityBootloader;

import java.util.ArrayList;

public class ActivityPreferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    static ScaleModule scaleModule;
    static Main main;
    private boolean flagChange;
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
                        //preference.getEditor().putInt(preference.getKey(), scaleModule.getTimerNull());
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
                        //preference.getEditor().putInt(preference.getKey(), scaleModule.getWeightError());
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
                        //preference.getEditor().putInt(preference.getKey(), main.getStepMeasuring());
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
                        context.getString(R.string.and) + main.getAutoCapture() + ' ' + context.getString(R.string.scales_kg));
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty()
                                || Integer.valueOf(o.toString()) < context.getResources().getInteger(R.integer.default_min_auto_capture)
                                || Integer.valueOf(o.toString()) > main.getAutoCapture()) {
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
                        //preference.getEditor().putInt(preference.getKey(), context.getResources().getInteger(R.integer.default_day_close_check));
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
                        //preference.getEditor().putInt(preference.getKey(), context.getResources().getInteger(R.integer.default_day_delete_check));
                        Toast.makeText(context, context.getString(R.string.preferences_yes) + " " + main.getDayDeleteCheck(), Toast.LENGTH_SHORT).show();
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
        };

        private final int resId;
        abstract void setup(Preference name)throws Exception;

        EnumPreference(int key){
            resId = key;
        }

        public int getResId() { return resId; }
    }

    public void process(){
        for (EnumPreference enumPreference : EnumPreference.values()){
            Preference preference = findPreference(getString(enumPreference.getResId()));
            try {
                enumPreference.setup(preference);
            } catch (Exception e) {
                preference.setEnabled(false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        main = (Main)getApplication();
        scaleModule = ((Main)getApplication()).getScaleModule();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        String action = getIntent().getAction();
        int preferenceFile_toLoad=-1;
        //Manage single fragment with action parameter
        if (action != null && action.equals("com.victjava.scales.settings.GENERAL")) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                addPreferencesFromResource(R.xml.preferences);
                process();
            }else{
                getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragmentPreferences()).commit();
            }

        }else{
            addPreferencesFromResource(R.xml.preferences_legacy);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if (flagChange) {
            try {
                int entryID = Integer.valueOf(new PreferencesTable(this).insertAllEntry().getLastPathSegment());
                new TaskTable(this).setPreferenceReady(entryID);
                //new TaskTable(getApplicationContext()).insertNewTask(TaskCommand.TaskType.TYPE_PREF_SEND_SHEET_DISK, entryID, 0, "preferences");
            } catch (Exception e) {
            }
        }*/
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

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            initPreferences();
        }

        public void initPreferences(){
            for (EnumPreference enumPreference : EnumPreference.values()){
                Preference preference = findPreference(getString(enumPreference.getResId()));
                if(preference != null){
                    try {
                        enumPreference.setup(preference);
                    } catch (Exception e) {
                        preference.setEnabled(false);
                    }
                }
            }
        }

    }

    /*@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
    }*/
}
