//Простой класс настроек
package com.victjava.scales;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.Set;

public class Preferences {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public static final String PREFERENCES = "preferences"; //настройки общии для весов.
    public static final String PREF_UPDATE = "pref_update"; //настройки сохраненные при обновлении прошивки.
    public static final String PREF_CAMERA = "pref_camera"; //настройки для камеры.

    public static final String KEY_NUMBER_SMS = "number_sms";
    //public static final String KEY_SENT_SERVICE = "sent_service";

    public Preferences(Context context, String name) {
        load(context.getSharedPreferences(name, Context.MODE_PRIVATE)); //загрузить настройки
    }

    public Preferences(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        editor.apply();
    }

    public void load(SharedPreferences sp) {
        sharedPreferences = sp;
        editor = sp.edit();
        editor.apply();
    }

    public void write(String key, String value) {
        editor.putString(key, value);
        editor.commit();
    }

    public void write(String key, int value) {
        editor.putInt(key, value);
        editor.commit();
    }

    /*public static void write(String key, float value) {
        editor.putFloat(key, value);
        editor.commit();
    }*/

    public void write(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }

    /*@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    static void write(String key, Set<String> value) {
        editor.putStringSet(key, value);
        editor.commit();
    }*/

    public String read(String key, String def) {
        return sharedPreferences.getString(key, def);
    }

    public boolean read(String key, boolean def) {
        return sharedPreferences.getBoolean(key, def);
    }

    public int read(String key, int in) {
        return sharedPreferences.getInt(key, in);
    }

    public float read(String key, float in) {
        return sharedPreferences.getFloat(key, in);
    }

    /*@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    static Set<String> read(String key, Set<String> def) {
        return sharedPreferences.getStringSet(key, def);
    }*/

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    public void remove(String key) {
        editor.remove(key);
        editor.commit();
    }
}