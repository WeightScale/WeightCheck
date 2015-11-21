//Активность для стартовой настройки весов
package com.victjava.scales.settings;

//import android.content.SharedPreferences;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import com.konst.module.Commands;
import com.konst.module.ScaleModule;
import com.victjava.scales.Main;
import com.victjava.scales.Preferences;
import com.victjava.scales.R;
import com.victjava.scales.bootloader.ActivityBootloader;
import com.victjava.scales.provider.SenderTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import android.preference.PreferenceManager;

public class ActivityTuning extends PreferenceActivity {
    public static Preferences preferencesCamera;
    protected Dialog dialog;
    ScaleModule scaleModule;
    Main main;

    private final Point point1 = new Point(Integer.MIN_VALUE, 0);
    private final Point point2 = new Point(Integer.MIN_VALUE, 0);
    private boolean flag_restore;
    final Map<String, InterfacePreference> mapTuning = new HashMap<>();

    interface InterfacePreference {
        void setup(Preference name) throws Exception;
    }

    enum CameraPreferences{
        COLOR_EFFECT(R.string.key_color_effect) {
            @Override
            void setup( Preference listPreference) {
                listPreference.setSummary(preferencesCamera.read(listPreference.getKey(),""));
                List<String> parameters = Main.parameters.getSupportedColorEffects();
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
                            Main.parameters.setColorEffect(o.toString());
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
                List<String> parameters = Main.parameters.getSupportedAntibanding();
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
                            Main.parameters.setAntibanding(o.toString());
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
                List<String> parameters = Main.parameters.getSupportedFlashModes();
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
                            Main.parameters.setFlashMode(o.toString());
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
                List<String> parameters = Main.parameters.getSupportedFocusModes();
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
                            Main.parameters.setFocusMode(o.toString());
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
                List<String> parameters = Main.parameters.getSupportedSceneModes();
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
                            Main.parameters.setSceneMode(o.toString());
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
                List<String> parameters = Main.parameters.getSupportedWhiteBalance();
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
                            Main.parameters.setWhiteBalance(o.toString());
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
                int max = Main.parameters.getMaxExposureCompensation();
                int min = Main.parameters.getMinExposureCompensation();
                int step = (int) Main.parameters.getExposureCompensationStep();
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
                            Main.parameters.setExposureCompensation(Integer.parseInt(o.toString()));
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
                listPreference.setSummary(((ListPreference)listPreference).getValue());
                List<Camera.Size> pictureSizes = Main.parameters.getSupportedPictureSizes();
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
                            preference.getEditor().putString(preference.getKey(), o.toString());
                            String[] str = o.toString().split("x");
                            /*preferences.write(getString(R.string.key_pic_size_width), str[0]);
                            preferences.write(getString(R.string.key_pic_size_height), str[1]);
                            findPreference(getString(R.string.key_pic_size_width)).setSummary(str[0]);
                            findPreference(getString(R.string.key_pic_size_height)).setSummary(str[1]);*/
                            preference.setSummary(o.toString());
                            Main.parameters.setPictureSize(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
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
                            Main.parameters.setRotation(rotation);
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
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(Preferences.PREF_CAMERA);
        preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
        for (CameraPreferences cameraPreferences : CameraPreferences.values()){
            Preference preference = preferenceManager.findPreference(getString(cameraPreferences.getResId()));
            cameraPreferences.setup(preference);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        main = (Main)getApplication();
        scaleModule = main.getScaleModule();

        mapTuning.put(getString(R.string.KEY_NAME), new Name());
        mapTuning.put(getString(R.string.KEY_SPEED_PORT), new SpeedPort());
        mapTuning.put(getString(R.string.KEY_POINT1), new Point1());
        mapTuning.put(getString(R.string.KEY_POINT2), new Point2());
        mapTuning.put(getString(R.string.KEY_WEIGHT_MAX), new WeightMax());
        mapTuning.put(getString(R.string.KEY_COEFFICIENT_A), new CoefficientA());
        mapTuning.put(getString(R.string.KEY_CALL_BATTERY), new CallBattery());
        mapTuning.put(getString(R.string.KEY_SHEET), new Sheet());
        mapTuning.put(getString(R.string.KEY_USER), new User());
        mapTuning.put(getString(R.string.KEY_PASSWORD), new Password());
        mapTuning.put(getString(R.string.KEY_PHONE), new Phone());
        mapTuning.put(getString(R.string.KEY_SENDER), new Sender());
        mapTuning.put(getString(R.string.KEY_SERVICE_COD), new ServiceCod());
        mapTuning.put(getString(R.string.KEY_UPDATE), new Update());

        //PreferenceManager preferenceManager = getPreferenceManager();
        //preferenceManager.setSharedPreferencesName(Preferences.PREFERENCES);
        //preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
        addPreferencesFromResource(R.xml.tuning);
        getPreferenceManager().setSharedPreferencesName(Preferences.PREFERENCES);

        preferencesCamera = new Preferences(getApplicationContext(), Preferences.PREF_CAMERA);
        PreferenceManager.setDefaultValues(this, R.xml.camera, false);

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        process();
        //initCameraPreferences();
        Preference camera = findPreference(getString(R.string.key_camera_settings));
        camera.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsCamera()).commit();
                return true;
            }
        });
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

    class Name implements InterfacePreference{

        @Override
        public void setup(Preference name) throws Exception {
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if(scaleModule.setModuleName(newValue.toString())){
                        preference.setSummary("Имя модуля: " + newValue);
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    class SpeedPort implements InterfacePreference{

        @Override
        public void setup(Preference name) throws Exception {
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    int temp = Integer.valueOf(newValue.toString());

                    if (scaleModule.setModuleSpeedPort(temp) ){
                        preference.setSummary("Скорость порта: " + newValue);
                        //scaleModule.setPhone(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    preference.setSummary("Скорость порта: ???");
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                    return false;
                }
            });
        }
    }

    class Point1 implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        String str = scaleModule.feelWeightSensor();
                        scaleModule.setSensorTenzo(Integer.valueOf(str));
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
        public void setup(final Preference name) throws Exception {
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    try {
                        String str = scaleModule.feelWeightSensor();
                        if (str.isEmpty()) {
                            Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        scaleModule.setSensorTenzo(Integer.valueOf(str));
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

            /*name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //scaleModule.setOnEventResultWeight(onEventResultWeight);
                    return false;
                }
            });*/
            /*name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    scaleModule.setOnEventResultWeight(onEventResultWeight);
                    openDialog();
                    return false;
                }
            });*/
        }
    }

    class WeightMax implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setTitle(getString(R.string.Max_weight) + scaleModule.getWeightMax() + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || Integer.valueOf(o.toString()) < getResources().getInteger(R.integer.default_max_weight)) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    scaleModule.setWeightMax(Integer.valueOf(o.toString()));
                    scaleModule.setWeightMargin((int) (scaleModule.getWeightMax() * 1.2));
                    preference.setTitle(getString(R.string.Max_weight) + scaleModule.getWeightMax() + getString(R.string.scales_kg));
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
            name.setTitle(getString(R.string.ConstantA) + Float.toString(scaleModule.getCoefficientA()));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    try {
                        scaleModule.setCoefficientA(Float.valueOf(o.toString()));
                        preference.setTitle(getString(R.string.ConstantA) + Float.toString(scaleModule.getCoefficientA()));
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
            name.setTitle(getString(R.string.Battery) + scaleModule.getBattery() + '%');
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > getResources().getInteger(R.integer.default_max_battery)) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (scaleModule.setModuleBatteryCharge(0)) {
                        scaleModule.setBattery(Integer.valueOf(o.toString()));
                        preference.setTitle(getString(R.string.Battery) + scaleModule.getBattery() + '%');
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
            name.setTitle(getString(R.string.Table) + '"' + scaleModule.getSpreadSheet() + '"');
            name.setSummary(getString(R.string.TEXT_MESSAGE7));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (scaleModule.setModuleSpreadsheet(o.toString())) {
                        preference.setTitle(getString(R.string.Table) + '"' + o + '"');
                        scaleModule.setSpreadSheet(o.toString());
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
            name.setSummary("Account Google: " + scaleModule.getUserName());
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (scaleModule.setModuleUserName(o.toString())) {
                        preference.setSummary("Account Google: " + o);
                        scaleModule.setUserName(o.toString());
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
            name.setSummary("Password account Google - " + scaleModule.getPassword());
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (scaleModule.setModulePassword(o.toString())) {
                        preference.setSummary("Password account Google: " + o);
                        scaleModule.setPassword(o.toString());
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
            name.setSummary("Номер телефона для смс - " + scaleModule.getPhone());
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (scaleModule.setModulePhone(o.toString())) {
                        preference.setSummary("Номер телефона для смс: " + o);
                        scaleModule.setPhone(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    preference.setSummary("Номер телефона для смс: ???");
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

    class ServiceCod implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().length() > 32 || newValue.toString().length() < 4) {
                        Toast.makeText(getApplicationContext(), "Длина кода больше 32 или меньше 4 знаков", Toast.LENGTH_LONG).show();
                        return false;
                    }

                    try {
                        scaleModule.setModuleServiceCod(newValue.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            });

        }
    }

    class Update implements InterfacePreference{
        @Override
        public void setup(Preference name) throws Exception {
            if (scaleModule.getVersion() != null) {
                if (scaleModule.getNumVersion() < main.microSoftware) {
                    name.setSummary(getString(R.string.Is_new_version));
                    //name.setEnabled(true);
                } else {
                    name.setSummary(getString(R.string.Scale_update));
                    //name.setEnabled(false);
                }
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
                    Intent intent = new Intent(ActivityTuning.this, ActivityBootloader.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    else
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(getString(R.string.KEY_ADDRESS), scaleModule.isAttach()? scaleModule.getAddressBluetoothDevice():"");
                    intent.putExtra(Commands.CMD_HARDWARE.getName(), hardware);
                    intent.putExtra(Commands.CMD_VERSION.getName(), scaleModule.getNumVersion());
                    startActivity(intent);
                    return false;
                }
            });
        }
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
        }
    }

    @Override
    public void onBackPressed() {
        if(flag_restore)
            setResult(RESULT_OK, new Intent());
        super.onBackPressed();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsCamera extends PreferenceFragment {
        public static Preferences preferencesCamera;
        enum CameraPreferences{
            COLOR_EFFECT(R.string.key_color_effect) {
                @Override
                void setup( Preference listPreference) {
                    listPreference.setSummary(preferencesCamera.read(listPreference.getKey(),""));
                    List<String> parameters = Main.parameters.getSupportedColorEffects();
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
                                Main.parameters.setColorEffect(o.toString());
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
                    List<String> parameters = Main.parameters.getSupportedAntibanding();
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
                                Main.parameters.setAntibanding(o.toString());
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
                    List<String> parameters = Main.parameters.getSupportedFlashModes();
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
                                Main.parameters.setFlashMode(o.toString());
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
                    List<String> parameters = Main.parameters.getSupportedFocusModes();
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
                                Main.parameters.setFocusMode(o.toString());
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
                    List<String> parameters = Main.parameters.getSupportedSceneModes();
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
                                Main.parameters.setSceneMode(o.toString());
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
                    List<String> parameters = Main.parameters.getSupportedWhiteBalance();
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
                                Main.parameters.setWhiteBalance(o.toString());
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
                    int max = Main.parameters.getMaxExposureCompensation();
                    int min = Main.parameters.getMinExposureCompensation();
                    int step = (int) Main.parameters.getExposureCompensationStep();
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
                                Main.parameters.setExposureCompensation(Integer.parseInt(o.toString()));
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
                    listPreference.setSummary(((ListPreference)listPreference).getValue());
                    List<Camera.Size> pictureSizes = Main.parameters.getSupportedPictureSizes();
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
                                preference.getEditor().putString(preference.getKey(), o.toString());
                                String[] str = o.toString().split("x");
                            /*preferences.write(getString(R.string.key_pic_size_width), str[0]);
                            preferences.write(getString(R.string.key_pic_size_height), str[1]);
                            findPreference(getString(R.string.key_pic_size_width)).setSummary(str[0]);
                            findPreference(getString(R.string.key_pic_size_height)).setSummary(str[1]);*/
                                preference.setSummary(o.toString());
                                Main.parameters.setPictureSize(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
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
                                Main.parameters.setRotation(rotation);
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
            PreferenceManager preferenceManager = getPreferenceManager();
            preferenceManager.setSharedPreferencesName(Preferences.PREF_CAMERA);
            preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
            for (CameraPreferences cameraPreferences : CameraPreferences.values()){
                Preference preference = preferenceManager.findPreference(getString(cameraPreferences.getResId()));
                cameraPreferences.setup(preference);
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource

            getPreferenceManager().setSharedPreferencesName(Preferences.PREF_CAMERA);
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            addPreferencesFromResource(R.xml.camera);
            preferencesCamera = Main.preferencesCamera;
            //PreferenceManager.setDefaultValues(this, R.xml.camera, false);
            initCameraPreferences();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            view.setBackgroundColor(Color.WHITE);
            return view;
        }
    }
}
