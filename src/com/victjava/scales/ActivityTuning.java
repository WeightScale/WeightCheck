//Активность для стартовой настройки весов
package com.victjava.scales;

//import android.content.SharedPreferences;

import android.graphics.Point;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;
import com.konst.module.ScaleModule;

//import android.preference.PreferenceManager;

public class ActivityTuning extends PreferenceActivity {
    private final Point point1 = new Point(Integer.MIN_VALUE, 0);
    private final Point point2 = new Point(Integer.MIN_VALUE, 0);
    private boolean flag_restore;
    //boolean flag_change = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tuning);
        String KEY_POINT1 = "point1";
        Preference name = findPreference(KEY_POINT1);
        if (name != null) {
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String str = ScaleModule.getModuleSensor();
                    if (str.isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    //Scales.sensor=Integer.valueOf(str);
                    ScaleModule.setSensorTenzo(Integer.valueOf(str));
                    point1.x = Integer.valueOf(str);
                    point1.y = 0;
                    Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    flag_restore = true;
                    return true;
                }
            });
        }
        if (name != null) {
            String KEY_POINT2 = "point2";
            name = findPreference(KEY_POINT2);
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String str = ScaleModule.getModuleSensor();
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
                }
            });
        }
        if (name != null) {
            String KEY_WEIGHT_MAX = "weightMax";
            name = findPreference(KEY_WEIGHT_MAX);
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
        /*if (name != null) {
            name = findPreference("speed");
            name.setSummary(String.valueOf(ScaleModule.getModuleSpeedPort()));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("") || Integer.valueOf(o.toString()) > 5 || Integer.valueOf(o.toString()) < 1) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (Scales.command(ScaleInterface.CMD_SPEED + o.toString()).equals(ScaleInterface.CMD_SPEED)) {
                        Scales.speed = Integer.valueOf(o.toString());
                        preference.setSummary(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }*/
        if (name != null) {
            String KEY_COEFFICIENT_A = "coefficientA";
            name = findPreference(KEY_COEFFICIENT_A);
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
                        return false;
                    }
                }
            });
        }
        if (name != null) {
            String KEY_CALL_BATTERY = "call_battery";
            name = findPreference(KEY_CALL_BATTERY);
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
        if (name != null) {
            String KEY_SHEET = "sheet";
            name = findPreference(KEY_SHEET);
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
        if (name != null) {
            String KEY_NAME = "name";
            name = findPreference(KEY_NAME);
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
        if (name != null) {
            String KEY_PASSWORD = "password";
            name = findPreference(KEY_PASSWORD);
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
        if (name != null) {
            String KEY_PHONE = "phone_msg";
            name = findPreference(KEY_PHONE);
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
