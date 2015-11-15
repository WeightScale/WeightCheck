package com.victjava.scales;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.konst.module.BootModule;
import com.konst.module.ScaleModule;
import com.victjava.scales.provider.ErrorTable;
import com.victjava.scales.service.ServiceSmsCommand;

/**
 * @author Kostya
 */
public class Main extends Application {
    ScaleModule scaleModule;
    BootModule bootModule;
    /** Класс формы для передачи данных весового чека. */
    //private GoogleForms.Form formWeightCheck;
    //private GoogleForms.Form formSettings;
    /**
     * Настройки для весов.
     */
    public static Preferences preferencesScale;
    /**
     * Настройки для обновления весов.
     */
    public static Preferences preferencesUpdate;

    PackageInfo packageInfo;

    /**
     * Версия пограммы весового модуля.
     */
    public final int microSoftware = 4;
    protected String networkOperatorName;
    protected String simNumber;
    protected String telephoneNumber;
    protected String networkCountry;
    protected int versionNumber;
    public String versionName = "";

    /** Шаг измерения (округление). */
    public int stepMeasuring;

    /** Шаг захвата (округление). */
    public int autoCapture;

    /** Время задержки для авто захвата после которого начинается захват в секундах. */
    public int timeDelayDetectCapture;
    public int dayClosedCheck;
    public int dayDeleteCheck;

    /**
     * Вес максимальный по умолчанию килограммы.
     */
    public static final int default_max_weight = 1000;

    /**
     * Максимальный заряд батареи проценты.
     */
    public static final int default_max_battery = 100;

    /**
     * Максимальное время бездействия весов в минутах.
     */
    public static final int default_max_time_off = 60;

    /**
     * Минимальное время бездействия весов в минутах.
     */
    public static final int default_min_time_off = 10;

    /**
     * Максимальное время срабатывания авто ноль секундах.
     */
    public static final int default_max_time_auto_null = 120;

    /**
     * Предел ошибки при котором срабатывает авто ноль килограммы.
     */
    public static final int default_limit_auto_null = 50;

    /**
     * Максимальный шаг измерения весов килограммы.
     */
    public static final int default_max_step_scale = 20;

    /**
     * Максимальный значение авто захвата веса килограммы.
     */
    public static final int default_max_auto_capture = 100;

    /**
     * Дельта значение авто захвата веса килограммы.
     */
    public static final int default_delta_auto_capture = 10;

    /**
     * Минимальное значение авто захвата веса килограммы.
     */
    public static final int default_min_auto_capture = 20;

    /**
     * Максимальное количество дней для закрытия не закрытых чеков дней.
     */
    public static final int default_day_close_check = 10;

    /**
     * Максимальное количество дней для удвления чеков дней.
     */
    public static final int default_day_delete_check = 10;

    /**
     * Максимальное значение фильтра ацп.
     */
    public static final int default_adc_filter = 15;

    public ScaleModule getScaleModule() {
        return scaleModule;
    }

    public BootModule getBootModule() {
        return bootModule;
    }

    public void setScaleModule(ScaleModule scaleModule) {
        this.scaleModule = scaleModule;
    }

    public void setBootModule(BootModule bootModule) {
        this.bootModule = bootModule;
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setStepMeasuring(int stepMeasuring) {
        this.stepMeasuring = stepMeasuring;
    }

    public int getStepMeasuring() {
        return stepMeasuring;
    }

    public int getAutoCapture() {
        return autoCapture;
    }

    public void setAutoCapture(int autoCapture) {
        this.autoCapture = autoCapture;
    }

    public String getNetworkOperatorName() {
        return networkOperatorName;
    }

    public void setNetworkOperatorName(String networkOperatorName) {
        this.networkOperatorName = networkOperatorName;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    /*public String getNetworkCountry() {
        return networkCountry;
    }*/

    /*public String getSimNumber() {
        return simNumber;
    }*/

    public int getDayClosedCheck() {
        return dayClosedCheck;
    }

    public int getDayDeleteCheck() {
        return dayDeleteCheck;
    }

    public void setDayClosedCheck(int dayClosedCheck) {
        this.dayClosedCheck = dayClosedCheck;
    }

    public void setDayDeleteCheck(int dayDeleteCheck) {
        this.dayDeleteCheck = dayDeleteCheck;
    }

    public void setSimNumber(String simNumber) {
        this.simNumber = simNumber;
    }

    public void setNetworkCountry(String networkCountry) {
        this.networkCountry = networkCountry;
    }

    //public GoogleForms.Form getFormWeightCheck() { return formWeightCheck; }

    //public GoogleForms.Form getFormSettings() { return formSettings; }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            PackageManager packageManager = getPackageManager();
            packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            versionNumber = packageInfo.versionCode;
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            new ErrorTable(this).insertNewEntry("100", e.getMessage());
        }

        preferencesScale = new Preferences(getApplicationContext(), Preferences.PREFERENCES);
        preferencesUpdate = new Preferences(getApplicationContext(), Preferences.PREF_UPDATE);
        Preferences.load(getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE)); //загрузить настройки

        stepMeasuring = Preferences.read(getString(R.string.KEY_STEP), default_max_step_scale);
        autoCapture = Preferences.read(getString(R.string.KEY_AUTO_CAPTURE), default_max_auto_capture);
        dayDeleteCheck = Preferences.read(getString(R.string.KEY_DAY_CHECK_DELETE), default_day_delete_check);
        dayClosedCheck = Preferences.read(getString(R.string.KEY_DAY_CLOSED_CHECK), default_day_close_check);
        //ScaleModule.setTimerNull(Preferences.read(getString(R.string.KEY_TIMER_NULL), default_max_time_auto_null));
        //ScaleModule.setWeightError(Preferences.read(getString(R.string.KEY_MAX_NULL), default_limit_auto_null));
        timeDelayDetectCapture = Preferences.read(getString(R.string.KEY_TIME_DELAY_DETECT_CAPTURE), 1);



        /*try {
            GoogleForms googleForms = new GoogleForms(getApplicationContext(), R.raw.forms);
            //GoogleForms forms = new GoogleForms(getAssets().open("forms/forms.xml"));
            formWeightCheck =googleForms.createForm("WeightCheck");
            formSettings = googleForms.createForm("Settings");
            //formSettings = forms.createForm("Settings");
            //formWeightCheck =forms.createForm("WeightCheck");
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        /** Запускаем сервис для приемеа смс команд. */
        getApplicationContext().startService(new Intent(getApplicationContext(), ServiceSmsCommand.class));

    }

}
