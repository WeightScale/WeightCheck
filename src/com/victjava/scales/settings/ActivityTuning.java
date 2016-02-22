//Активность для стартовой настройки весов
package com.victjava.scales.settings;

//import android.content.SharedPreferences;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.provider.BaseColumns;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.widget.*;
import com.konst.module.Commands;
import com.konst.module.scale.ScaleModule;
import com.victjava.scales.Globals;
import com.victjava.scales.Preferences;
import com.victjava.scales.R;
import com.victjava.scales.bootloader.ActivityBootloader;
import com.victjava.scales.provider.PreferencesTable;
import com.victjava.scales.provider.SenderTable;
import com.victjava.scales.provider.TaskTable;

import java.util.ArrayList;
import java.util.List;

//import android.preference.PreferenceManager;

public class ActivityTuning extends PreferenceActivity {
    public static Preferences preferencesCamera;
    protected static Dialog dialog;
    private static ScaleModule scaleModule;
    private static Globals globals;
    private EditText input;

    private static final Point point1 = new Point(Integer.MIN_VALUE, 0);
    private static final Point point2 = new Point(Integer.MIN_VALUE, 0);
    private static boolean flag_restore;
    //final Map<String, InterfacePreference> mapTuning = new HashMap<>();

    enum EnumPreferenceAdmin{
        NAME(R.string.KEY_NAME){
            @Override
            void setup(Preference name) throws Exception {
                name.setSummary("Имя модуля: " + scaleModule.getNameBluetoothDevice());
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue.toString().isEmpty()) {
                            Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        if(scaleModule.setModuleName(newValue.toString())){
                            preference.setSummary("Имя модуля: " + newValue);
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        return false;
                    }
                });
            }
        },
        SPEED_PORT(R.string.KEY_SPEED_PORT){
            @Override
            void setup(Preference name) throws Exception {
                String[] speed = name.getContext().getResources().getStringArray(R.array.array_speed_port);
                name.setSummary("Скорость порта: " + speed[Integer.valueOf(scaleModule.getModuleSpeedPort())-1]);
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue.toString().isEmpty()) {
                            Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        int temp = Integer.valueOf(newValue.toString());

                        if (scaleModule.setModuleSpeedPort(temp) ){
                            preference.setSummary("Скорость порта: " + newValue);
                            //scaleModule.setPhone(o.toString());
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        preference.setSummary("Скорость порта: ???");
                        Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                        return false;
                    }
                });
            }
        },
        /*POWER(R.string.KEY_POWER){
            @Override
            void setup(Preference name) throws Exception {

                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue.toString().isEmpty()) {
                            Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        int temp = Integer.valueOf(newValue.toString());

                        if (scaleModule.setModulePower(temp) ){
                            preference.setSummary("Скорость порта: " + newValue);
                            //scaleModule.setPhone(o.toString());
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        preference.setSummary("Скорость порта: ???");
                        Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                        return false;
                    }
                });
            }
        },*/
        POINT1(R.string.KEY_POINT1){
            @Override
            void setup(Preference name) throws Exception {
                if(!globals.isScaleConnect())
                    throw new Exception(" ");
                name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            String str = scaleModule.feelWeightSensor();
                            scaleModule.setSensorTenzo(Integer.valueOf(str));
                            point1.x = Integer.valueOf(str);
                            point1.y = 0;
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            flag_restore = true;
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(name.getContext(), R.string.preferences_no + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                });
            }
        },
        POINT2(R.string.KEY_POINT2){
            @Override
            void setup(Preference name) throws Exception {
                if(!globals.isScaleConnect())
                    throw new Exception(" ");
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        try {
                            String str = scaleModule.feelWeightSensor();
                            if (str.isEmpty()) {
                                Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            scaleModule.setSensorTenzo(Integer.valueOf(str));
                            point2.x = Integer.valueOf(str);
                            point2.y = Integer.valueOf(o.toString());
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            flag_restore = true;
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(name.getContext(), R.string.preferences_no + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                });
            }
        },
        WEIGHT_MAX(R.string.KEY_WEIGHT_MAX){
            @Override
            void setup(Preference name) throws Exception {
                Context context = name.getContext();
                name.setTitle(context.getString(R.string.Max_weight) + scaleModule.getWeightMax() + context.getString(R.string.scales_kg));
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty() || Integer.valueOf(o.toString()) < context.getResources().getInteger(R.integer.default_max_weight)) {
                            Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        scaleModule.setWeightMax(Integer.valueOf(o.toString()));
                        scaleModule.setWeightMargin((int) (scaleModule.getWeightMax() * 1.2));
                        preference.setTitle(context.getString(R.string.Max_weight) + scaleModule.getWeightMax() + context.getString(R.string.scales_kg));
                        Toast.makeText(context, R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        flag_restore = true;
                        return true;
                    }
                });
            }
        },
        COEFFICIENT_A(R.string.KEY_COEFFICIENT_A){
            @Override
            void setup(Preference name) throws Exception {
                Context context = name.getContext();
                name.setTitle(context.getString(R.string.ConstantA) + Float.toString(scaleModule.getCoefficientA()));
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        try {
                            scaleModule.setCoefficientA(Float.valueOf(o.toString()));
                            preference.setTitle(context.getString(R.string.ConstantA) + Float.toString(scaleModule.getCoefficientA()));
                            Toast.makeText(context, R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            flag_restore = true;
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                });
            }
        },
        CALL_BATTERY(R.string.KEY_CALL_BATTERY){
            @Override
            void setup(Preference name) throws Exception {
                Context context = name.getContext();
                name.setTitle(context.getString(R.string.Battery) + globals.getBattery() + '%');
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > context.getResources().getInteger(R.integer.default_max_battery)) {
                            Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (scaleModule.setModuleBatteryCharge(Integer.valueOf(o.toString()))) {
                            preference.setTitle(context.getString(R.string.Battery) + globals.getBattery() + '%');
                            Toast.makeText(context, R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
            }
        },
        KEY_SHEET(R.string.KEY_SHEET){
            @Override
            void setup(Preference name) throws Exception {
                Context context = name.getContext();
                name.setTitle(context.getString(R.string.Table) + '"' + scaleModule.getSpreadSheet() + '"');
                name.setSummary(context.getString(R.string.TEXT_MESSAGE7));
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty()) {
                            Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (scaleModule.setModuleSpreadsheet(o.toString())) {
                            preference.setTitle(context.getString(R.string.Table) + '"' + o + '"');
                            Toast.makeText(context, R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        preference.setTitle(context.getString(R.string.Table) + "???");
                        Toast.makeText(context, R.string.preferences_no, Toast.LENGTH_SHORT).show();

                        return false;
                    }
                });
            }
        },
        USER(R.string.KEY_USER){
            @Override
            void setup(Preference name) throws Exception {
                name.setSummary("Account Google: " + scaleModule.getUserName());
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty()) {
                            Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (scaleModule.setModuleUserName(o.toString())) {
                            preference.setSummary("Account Google: " + o);
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        preference.setSummary("Account Google: ???");
                        Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
            }
        },
        PASSWORD(R.string.KEY_PASSWORD){
            @Override
            void setup(Preference name) throws Exception {
                name.setSummary("Password account Google - " + scaleModule.getPassword());
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty()) {
                            Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        if (scaleModule.setModulePassword(o.toString())) {
                            preference.setSummary("Password account Google: " + o);
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        preference.setSummary("Password account Google: ???");
                        Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                        return false;
                    }
                });
            }
        },
        PHONE(R.string.KEY_PHONE){
            @Override
            void setup(Preference name) throws Exception {
                name.setSummary("Номер телефона для смс - " + scaleModule.getPhone());
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o.toString().isEmpty()) {
                            Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        if (scaleModule.setModulePhone(o.toString())) {
                            preference.setSummary("Номер телефона для смс: " + o);
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        preference.setSummary("Номер телефона для смс: ???");
                        Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                        return false;
                    }
                });
            }
        },
        SENDER(R.string.KEY_SENDER){
            Context mContext;
            SenderTable senderTable;

            @Override
            void setup(Preference name) throws Exception {
                mContext = name.getContext();
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

            class ListBinder implements SimpleCursorAdapter.ViewBinder {
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
        },
        CAMERA(R.string.key_camera_settings){
            @Override
            void setup(Preference name) throws Exception {
                boolean check = name.getSharedPreferences().getBoolean(name.getContext().getString(R.string.KEY_PHOTO_CHECK), false);
                name.setEnabled(check);
                name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        return true;
                    }
                });
            }
        },
        CHECK_PHOTO(R.string.KEY_PHOTO_CHECK){
            @Override
            void setup(Preference name) throws Exception {
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean check = (boolean)newValue;
                        Preference photo = preference.getPreferenceManager().findPreference(preference.getContext().getString(R.string.key_camera_settings));
                        photo.setEnabled(check);
                        return true;
                    }
                });
            }
        },
        SERVICE_COD(R.string.KEY_SERVICE_COD){
            @Override
            void setup(Preference name) throws Exception {
                if(!globals.isScaleConnect())
                    throw new Exception(" ");
                name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue.toString().length() > 32 || newValue.toString().length() < 4) {
                            Toast.makeText(name.getContext(), "Длина кода больше 32 или меньше 4 знаков", Toast.LENGTH_LONG).show();
                            return false;
                        }

                        try {
                            scaleModule.setModuleServiceCod(newValue.toString());
                            Toast.makeText(name.getContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(name.getContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                });
            }
        },
        UPDATE(R.string.KEY_UPDATE){
            @Override
            void setup(Preference name) throws Exception {
                Context context = name.getContext();
                try {
                    if (scaleModule.getVersion() != null) {
                        if (scaleModule.getNumVersion() < globals.getMicroSoftware()) {
                            name.setSummary(context.getString(R.string.Is_new_version));
                        } else {
                            name.setSummary(context.getString(R.string.Scale_update));
                        }
                    }
                }catch (Exception e){
                    name.setSummary(context.getString(R.string.TEXT_MESSAGE14));
                }
                name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(context, ActivityBootloader.class);
                        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        else
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);*/
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            intent.putExtra(context.getString(R.string.KEY_ADDRESS), globals.isScaleConnect()? scaleModule.getAddressBluetoothDevice():"");
                            intent.putExtra(Commands.CMD_HARDWARE.getName(), scaleModule.getModuleHardware());
                            intent.putExtra(Commands.CMD_VERSION.getName(), scaleModule.getNumVersion());
                            if (globals.isScaleConnect()){
                                if(scaleModule.powerOff())
                                    intent.putExtra(context.getString(R.string.KEY_POWER), true);
                            }
                            scaleModule.dettach();
                        }catch (Exception e){}
                        context.startActivity(intent);
                        return false;
                    }
                });
            }
        };

        private final int resId;
        abstract void setup(Preference name)throws Exception;

        EnumPreferenceAdmin(int key){
            resId = key;
        }

        public int getResId() { return resId; }
    }

    enum CameraPreferences{
        COLOR_EFFECT(R.string.key_color_effect) {
            @Override
            void setup( Preference listPreference) {
                listPreference.setSummary(preferencesCamera.read(listPreference.getKey(),""));
                List<String> parameters = globals.parameters.getSupportedColorEffects();
                if (parameters != null) {
                    CharSequence[] entries = new CharSequence[0];
                    entries = parameters.toArray(entries);
                    ((ListPreference)listPreference).setEntries(entries);
                    ((ListPreference)listPreference).setEntryValues(entries);
                    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            preference.setSummary(o.toString());
                            globals.parameters.setColorEffect(o.toString());
                            return true;
                        }
                    });
                } else {
                    listPreference.setEnabled(false);
                    listPreference.setSummary("Неподдерживает");
                }
            }
        },
        ANTI_BANDING(R.string.key_anti_banding) {
            @Override
            void setup(Preference listPreference) {
                listPreference.setSummary(((ListPreference)listPreference).getValue());
                List<String> parameters = globals.parameters.getSupportedAntibanding();
                if(parameters != null){
                    CharSequence[] entries = new CharSequence[0];
                    entries = parameters.toArray(entries);
                    ((ListPreference)listPreference).setEntries(entries);
                    ((ListPreference)listPreference).setEntryValues(entries);
                    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            preference.setSummary(o.toString());
                            globals.parameters.setAntibanding(o.toString());
                            return true;
                        }
                    });
                }else {
                    listPreference.setEnabled(false);
                    listPreference.setSummary("Неподдерживает");
                }
            }
        },
        FLASH_MODE(R.string.key_flash_mode) {
            @Override
            void setup(Preference listPreference) {
                listPreference.setSummary(((ListPreference)listPreference).getValue());
                List<String> parameters = globals.parameters.getSupportedFlashModes();
                if(parameters != null){
                    CharSequence[] entries = new CharSequence[0];
                    entries = parameters.toArray(entries);
                    ((ListPreference)listPreference).setEntries(entries);
                    ((ListPreference)listPreference).setEntryValues(entries);
                    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            preference.setSummary(o.toString());
                            globals.parameters.setFlashMode(o.toString());
                            return true;
                        }
                    });
                }else {
                    listPreference.setEnabled(false);
                    listPreference.setSummary("Неподдерживает");
                }
            }
        },
        FOCUS_MODE(R.string.key_focus_mode) {
            @Override
            void setup(Preference listPreference) {
                listPreference.setSummary(((ListPreference)listPreference).getValue());
                List<String> parameters = globals.parameters.getSupportedFocusModes();
                if(parameters != null){
                    CharSequence[] entries = new CharSequence[0];
                    entries = parameters.toArray(entries);
                    ((ListPreference)listPreference).setEntries(entries);
                    ((ListPreference)listPreference).setEntryValues(entries);
                    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            preference.setSummary(o.toString());
                            globals.parameters.setFocusMode(o.toString());
                            return true;
                        }
                    });
                }else {
                    listPreference.setEnabled(false);
                    listPreference.setSummary("Неподдерживает");
                }
            }
        },
        SCENE_MODE(R.string.key_scene_mode) {
            @Override
            void setup(Preference listPreference) {
                listPreference.setSummary(((ListPreference)listPreference).getValue());
                List<String> parameters = globals.parameters.getSupportedSceneModes();
                if(parameters != null){
                    CharSequence[] entries = new CharSequence[0];
                    entries = parameters.toArray(entries);
                    ((ListPreference)listPreference).setEntries(entries);
                    ((ListPreference)listPreference).setEntryValues(entries);
                    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            preference.setSummary(o.toString());
                            globals.parameters.setSceneMode(o.toString());
                            return true;
                        }
                    });
                }else {
                    listPreference.setEnabled(false);
                    listPreference.setSummary("Неподдерживает");
                }
            }
        },
        WHITE_MODE(R.string.key_white_mode) {
            @Override
            void setup(Preference listPreference) {
                listPreference.setSummary(((ListPreference)listPreference).getValue());
                List<String> parameters = globals.parameters.getSupportedWhiteBalance();
                if(parameters != null){
                    CharSequence[] entries = new CharSequence[0];
                    entries = parameters.toArray(entries);
                    ((ListPreference)listPreference).setEntries(entries);
                    ((ListPreference)listPreference).setEntryValues(entries);
                    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            preference.setSummary(o.toString());
                            globals.parameters.setWhiteBalance(o.toString());
                            return true;
                        }
                    });
                }else {
                    listPreference.setEnabled(false);
                    listPreference.setSummary("Неподдерживает");
                }
            }
        },
        EXPOSURE(R.string.key_exposure) {
            @Override
            void setup(Preference listPreference) {
                listPreference.setSummary(((ListPreference)listPreference).getValue());
                int max = globals.parameters.getMaxExposureCompensation();
                int min = globals.parameters.getMinExposureCompensation();
                int step = (int) globals.parameters.getExposureCompensationStep();
                List<String> exposure = new ArrayList<>();
                for (; max >= min; max -= step) {
                    exposure.add(String.valueOf(max));
                }
                if (exposure != null) {
                    CharSequence[] entries = new CharSequence[0];
                    entries = exposure.toArray(entries);
                    ((ListPreference)listPreference).setEntries(entries);
                    ((ListPreference)listPreference).setEntryValues(entries);
                    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            preference.setSummary(o.toString());
                            globals.parameters.setExposureCompensation(Integer.parseInt(o.toString()));
                            return true;
                        }
                    });
                } else {
                    listPreference.setEnabled(false);
                    listPreference.setSummary("Неподдерживает");
                }
            }
        },
        PIC_SIZE(R.string.key_pic_size) {
            @Override
            void setup(Preference listPreference) {
                Context context = listPreference.getContext();
                listPreference.setSummary(((ListPreference)listPreference).getValue());
                List<Camera.Size> pictureSizes = globals.parameters.getSupportedPictureSizes();
                if (pictureSizes != null) {
                    CharSequence[] entries = new CharSequence[0];
                    List<String> sizeList = new ArrayList<>();
                    for (Camera.Size size : pictureSizes) {
                        int w = size.width;
                        int h = size.height;
                        sizeList.add(w + "x" + h);
                    }
                    entries = sizeList.toArray(entries);
                    ((ListPreference)listPreference).setEntries(entries);
                    ((ListPreference)listPreference).setEntryValues(entries);
                    listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            //preference.getEditor().putString(preference.getKey(), o.toString());
                            String[] str = o.toString().split("x");
                            SharedPreferences.Editor editor = preference.getEditor();
                            editor.putString(preference.getContext().getString(R.string.key_pic_size_width), str[0]);
                            editor.putString(preference.getContext().getString(R.string.key_pic_size_height), str[1]);
                            editor.commit();
                            preference.getPreferenceManager().findPreference(preference.getContext().getString(R.string.key_pic_size_width)).setSummary(str[0]);
                            preference.getPreferenceManager().findPreference(preference.getContext().getString(R.string.key_pic_size_height)).setSummary(str[1]);
                            preference.setSummary(o.toString());
                            globals.parameters.setPictureSize(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
                            return true;
                        }
                    });
                    listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            return false;
                        }

                    });
                } else {
                    listPreference.setEnabled(false);
                    listPreference.setSummary("Неподдерживает");
                }
            }
        },
        ROTATION(R.string.key_rotation) {
            @Override
            void setup(Preference listPreference) {
                listPreference.setSummary(listPreference.getSharedPreferences().getString(listPreference.getKey(), "0"));
                listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        int rotation = Integer.parseInt(o.toString());
                        if (rotation >= 0 && rotation <= 270) {
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            preference.setSummary(o.toString());
                            globals.parameters.setRotation(rotation);
                            return true;
                        }
                        return false;
                    }
                });
            }
        },
        PIC_QUALITY(R.string.key_quality_pic) {
            @Override
            void setup(Preference quality) {
                quality.setSummary("Качество фото: " + quality.getSharedPreferences().getString(quality.getKey(), ""));
                quality.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        int time = Integer.parseInt(o.toString());
                        if (time > 0 && time <= 100) {
                            preference.setSummary("Качество фото: " + o);
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            return true;
                        }
                        return false;
                    }
                });
            }
        };

        private final int resId;
        abstract void setup(Preference listPreference);

        CameraPreferences(int key){
            resId = key;
        }

        public int getResId() { return resId; }
    }

    public void initCameraPreferences(){
        for (CameraPreferences cameraPreferences : CameraPreferences.values()){
            Preference preference = findPreference(getString(cameraPreferences.getResId()));
            if(preference != null){
                try{
                    cameraPreferences.setup(preference);
                }catch (Exception e){
                    preference.setEnabled(false);
                }
            }
        }
    }

    void process(){
        for (EnumPreferenceAdmin enumPreferenceAdmin : EnumPreferenceAdmin.values()){
            Preference preference = findPreference(getString(enumPreferenceAdmin.getResId()));
            if(preference != null){
                try {
                    enumPreferenceAdmin.setup(preference);
                } catch (Exception e) {
                    preference.setEnabled(false);
                }
            }
        }
    }

    void startDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("ВВОД КОДА");
        input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input.setGravity(Gravity.CENTER);
        dialog.setView(input);
        dialog.setCancelable(false);
        dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (input.getText() != null) {
                    boolean key = false;
                    String string = input.getText().toString();
                    if (!string.isEmpty()){
                        try{
                            if ("343434".equals(string))
                                key = true;
                            else if (string.equals(scaleModule.getModuleServiceCod()))
                                key = true;
                            if (key){
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                                    addPreferencesFromResource(R.xml.admin_preferences);
                                    process();
                                    initCameraPreferences();
                                }else {
                                    getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefFragmentAdmin()).commit();
                                }

                                /*Preference camera = findPreference(getString(R.string.key_camera_settings));
                                boolean check = getPreferenceManager().getSharedPreferences().getBoolean(getString(R.string.KEY_PHOTO_CHECK), false);
                                camera.setEnabled(check);
                                camera.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                    @Override
                                    public boolean onPreferenceClick(Preference preference) {
                                        //getFragmentManager().beginTransaction().replace(android.R.id.content, new ActivityTuning.SettingsCamera()).commit();
                                        return true;
                                    }
                                });*/

                                return;
                            }
                        }catch (Exception e){}

                    }

                }
                Toast.makeText(ActivityTuning.this, "Неверный код", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        dialog.setMessage("Введи код доступа к административным настройкам");
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        flag_restore = false;
        globals = Globals.getInstance();
        scaleModule = globals.getScaleModule();
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        startDialog();
        preferencesCamera = new Preferences(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flag_restore) {
            if (point1.x != Integer.MIN_VALUE && point2.x != Integer.MIN_VALUE) {
                //scaleModule.setCoefficientA((float) (point1.x - point2.x)/(point1.y - point2.y));
                scaleModule.setCoefficientA((float) (point1.y - point2.y) / (point1.x - point2.x));
                //scaleModule.setCoefficientB(point1.y - point1.x/scaleModule.getCoefficientA() );
                scaleModule.setCoefficientB(point1.y - scaleModule.getCoefficientA() * point1.x);
            }
            //scaleModule.setLimitTenzo((int) (scaleModule.getWeightMax() * scaleModule.getCoefficientA()));
            scaleModule.setLimitTenzo((int) (scaleModule.getWeightMax() / scaleModule.getCoefficientA()));
            if (scaleModule.getLimitTenzo() > 0xffffff) {
                scaleModule.setLimitTenzo(0xffffff);
                scaleModule.setWeightMax((int) (0xffffff / scaleModule.getCoefficientA()));
                //scaleModule.setWeightMax((int) (0xffffff * scaleModule.getCoefficientA()));
            }
            if (scaleModule.writeData()) {
                Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
            }
            try {
                int entryID = Integer.valueOf(new PreferencesTable(this).insertAllEntry().getLastPathSegment());
                new TaskTable(this).setPreferenceReady(entryID);
            } catch (Exception e) {
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PrefFragmentAdmin extends PreferenceFragment{

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.admin_preferences);
            initAdminPreference();
            initCameraPreferences();
        }

        void initAdminPreference(){
            for (EnumPreferenceAdmin enumPreferenceAdmin : EnumPreferenceAdmin.values()){
                Preference preference = findPreference(getString(enumPreferenceAdmin.getResId()));
                if(preference != null){
                    try {
                        enumPreferenceAdmin.setup(preference);
                    } catch (Exception e) {
                        preference.setEnabled(false);
                    }
                }
            }
        }

        public void initCameraPreferences(){
            for (CameraPreferences cameraPreferences : CameraPreferences.values()){
                Preference preference = findPreference(getString(cameraPreferences.getResId()));
                if(preference != null){
                    try{
                        cameraPreferences.setup(preference);
                    }catch (Exception e){
                        preference.setEnabled(false);
                    }
                }
            }
        }

    }
}
