package com.victjava.scales;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import com.konst.module.ScaleModule;
import com.victjava.scales.service.ServiceSmsCommand;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 30.10.13
 * Time: 15:04
 * To change this template use File | Settings | File Templates.
 */
public class Main extends Application {
    /** Настройки для весов */
    public static Preferences preferencesScale;
    /** Настройки для обновления весов */
    public static Preferences preferencesUpdate;

    /** Версия пограммы весового модуля */
    public static final int microSoftware = 4;
    protected static String networkOperatorName;
    protected static String simNumber;
    protected static String telephoneNumber;
    protected static String networkCountry;
    protected static int versionNumber;
    public static String versionName = "";

    /** шаг измерения (округление) */
    public static int stepMeasuring;

    /** шаг захвата (округление) */
    public static int autoCapture;

    /** Время задержки для авто захвата после которого начинается захват в секундах  */
    public static int timeDelayDetectCapture;
    public static int day_closed;
    public static int day_delete;

    /**вес максимальный по умолчанию килограммы*/
    public static final int default_max_weight = 1000;

    /**максимальный заряд батареи проценты */
    public static final int default_max_battery = 100;

    /**максимальное время бездействия весов в минутах*/
    public static final int default_max_time_off = 60;

    /**минимальное время бездействия весов в минутах*/
    protected static final int default_min_time_off = 10;

    /**максимальное время срабатывания авто ноль секундах*/
    protected static final int default_max_time_auto_null = 120;

    /**предел ошибки при котором срабатывает авто ноль килограммы*/
    protected static final int default_limit_auto_null = 50;

    /**максимальный шаг измерения весов килограммы*/
    protected static final int default_max_step_scale = 20;

    /**максимальный значение авто захвата веса килограммы*/
    protected static final int default_max_auto_capture = 100;

    /**дельта значение авто захвата веса килограммы*/
    protected static final int default_delta_auto_capture = 10;

    /**минимальное значение авто захвата веса килограммы*/
    protected static final int default_min_auto_capture = 20;

    /**максимальное количество дней для закрытия не закрытых чеков дней*/
    protected static final int default_day_close_check = 10;

    /**максимальное количество дней для удвления чеков дней*/
    protected static final int default_day_delete_check = 10;

    /**максимальное значение фильтра ацп*/
    public static final int default_adc_filter = 15;

    @Override
    public void onCreate() {
        super.onCreate();
        preferencesScale = new Preferences(getApplicationContext(), Preferences.PREFERENCES);
        preferencesUpdate = new Preferences(getApplicationContext(), Preferences.PREF_UPDATE);
        Preferences.load(getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE)); //загрузить настройки

        stepMeasuring = Preferences.read(ActivityPreferences.KEY_STEP, default_max_step_scale);
        autoCapture = Preferences.read(ActivityPreferences.KEY_AUTO_CAPTURE, default_max_auto_capture);
        day_delete = Preferences.read(ActivityPreferences.KEY_DAY_CHECK_DELETE, default_day_delete_check);
        day_closed = Preferences.read(ActivityPreferences.KEY_DAY_CLOSED_CHECK, default_day_close_check);
        ScaleModule.setTimerNull(Preferences.read(ActivityPreferences.KEY_TIMER_NULL, default_max_time_auto_null));
        ScaleModule.setWeightError(Preferences.read(ActivityPreferences.KEY_MAX_NULL, default_limit_auto_null));
        timeDelayDetectCapture = Preferences.read(ActivityPreferences.KEY_TIME_DELAY_DETECT_CAPTURE, 1);

        /** Запускаем сервис для приемеа смс команд */
        getApplicationContext().startService(new Intent(getApplicationContext(), ServiceSmsCommand.class));
    }

    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}
