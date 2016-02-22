/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.victjava.scales;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;
import com.konst.module.boot.BootModule;
import com.konst.module.scale.ScaleModule;
import com.victjava.scales.provider.ErrorTable;
import com.victjava.scales.service.ServiceSmsCommand;

import java.io.File;
import java.util.List;

/**
 * Created by Kostya on 23.01.2016.
 */
public class Globals {
    private static Globals instance = new Globals();
    private ScaleModule scaleModule;
    private BootModule bootModule;
    public Camera.Parameters parameters;
    public static File path;
    public static String FOLDER_LOCAL = "CheckPhoto";
    /** Класс формы для передачи данных весового чека. */
    /** Настройки для весов. */
    protected Preferences preferencesScale;
    /** Настройки для обновления весов. */
    private Preferences preferencesUpdate;
    /** Настройки для камеры. */
    private Preferences preferencesCamera;

    protected PackageInfo packageInfo;

    /** Версия пограммы весового модуля. */
    private final int microSoftware = 4;

    protected String networkOperatorName;
    protected String simNumber;
    protected String telephoneNumber;
    protected String networkCountry;
    protected int versionNumber;
    protected String versionName = "";
    /** Шаг измерения (округление). */
    private int stepMeasuring;
    /** Шаг захвата (округление). */
    private int autoCapture;
    /** Время задержки для авто захвата после которого начинается захват в секундах. */
    private int timeDelayDetectCapture;
    private int dayClosedCheck;
    private int dayDeleteCheck;
    /** Процент заряда батареи (0-100%). */
    private int battery;
    private int temperature;

    public int getTemperature() { return temperature;  }

    public void setTemperature(int temperature) { this.temperature = temperature; }

    /** Получаем заряд батареи раннее загруженый в процентах.
     * @return заряд батареи в процентах.
     */
    public int getBattery() { return battery; }

    public void setBattery(int battery) { this.battery = battery; }

    /** Флаг есть соединение */
    private boolean isScaleConnect;

    public void initialize(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionNumber = packageInfo.versionCode;
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            new ErrorTable(context).insertNewEntry("100", e.getMessage());
        }

        preferencesScale = new Preferences(context.getApplicationContext()); //загрузить настройки

        stepMeasuring = preferencesScale.read(context.getString(R.string.KEY_STEP), context.getResources().getInteger(R.integer.default_step_scale));
        autoCapture = preferencesScale.read(context.getString(R.string.KEY_AUTO_CAPTURE), context.getResources().getInteger(R.integer.default_max_auto_capture));
        dayDeleteCheck = preferencesScale.read(context.getString(R.string.KEY_DAY_CHECK_DELETE), context.getResources().getInteger(R.integer.default_day_delete_check));
        dayClosedCheck = preferencesScale.read(context.getString(R.string.KEY_DAY_CLOSED_CHECK), context.getResources().getInteger(R.integer.default_day_close_check));
        //ScaleModule.setTimerNull(Preferences.read(getString(R.string.KEY_TIMER_NULL), default_max_time_auto_null));
        //ScaleModule.setWeightError(Preferences.read(getString(R.string.KEY_MAX_NULL), default_limit_auto_null));
        timeDelayDetectCapture = context.getResources().getInteger(R.integer.time_delay_detect_capture);

        /** Создаем путь к временной папке для для хранения файлов. */
        path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + FOLDER_LOCAL);
        /** Если нет папки тогда создаем. */
        if (!path.exists()) {
            if (!path.mkdirs()) {
                new ErrorTable(context).insertNewEntry("500", "Path no create: " + path.getPath());
                //todo что зделать если не создали папку
            }
        }
        /** Окрываем экземпляр основной камеры. */
        Camera camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        /** Получаем параметры камеры */
        parameters = camera.getParameters();
        camera.release();
        loadParametersToCamera(context);
        /** Запускаем сервис для приемеа смс команд. */
        context.startService(new Intent(context, ServiceSmsCommand.class));

    }

    public boolean isScaleConnect() {
        return isScaleConnect;
    }

    public void setScaleConnect(boolean scaleConnect) {
        isScaleConnect = scaleConnect;
    }

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

    public int getDayClosedCheck() { return dayClosedCheck;  }

    public int getDayDeleteCheck() {  return dayDeleteCheck; }

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

    public int getTimeDelayDetectCapture() { return timeDelayDetectCapture;  }

    public Preferences getPreferencesScale() { return preferencesScale; }

    public Preferences getPreferencesCamera() { return preferencesCamera; }

    public int getMicroSoftware() { return microSoftware; }

    public static Globals getInstance() { return instance; }

    public static void setInstance(Globals instance) { Globals.instance = instance; }

    /**
     * Загружаем параметры камеры в настройки программы
     */
    public void loadParametersToCamera(Context context) {

        preferencesCamera = new Preferences(context);

        List<String> colorEffects = parameters.getSupportedColorEffects();
        if (colorEffects != null) {
            String color = preferencesCamera.read(context.getString(R.string.key_color_effect), parameters.getColorEffect());
            if (colorEffects.contains(color))
                parameters.setColorEffect(color);
        }

        List<String> antiBanding = parameters.getSupportedAntibanding();
        if (antiBanding != null) {
            String banding = preferencesCamera.read(context.getString(R.string.key_anti_banding), parameters.getAntibanding());
            if (antiBanding.contains(banding))
                parameters.setAntibanding(banding);
        }

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null) {
            String flash = preferencesCamera.read(context.getString(R.string.key_flash_mode), parameters.getFlashMode());
            if (flashModes.contains(flash))
                parameters.setFlashMode(flash);
        }

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            String focus = preferencesCamera.read(context.getString(R.string.key_focus_mode), parameters.getFocusMode());
            if (focusModes.contains(focus))
                parameters.setFocusMode(focus);
        }

        List<String> sceneModes = parameters.getSupportedSceneModes();
        if (sceneModes != null) {
            String scene = preferencesCamera.read(context.getString(R.string.key_scene_mode), parameters.getSceneMode());
            if (sceneModes.contains(scene))
                parameters.setSceneMode(scene);
        }

        List<String> whiteBalance = parameters.getSupportedWhiteBalance();
        if (whiteBalance != null) {
            String white = preferencesCamera.read(context.getString(R.string.key_white_mode), parameters.getWhiteBalance());
            if (sceneModes != null ? sceneModes.contains(white) : false)
                parameters.setWhiteBalance(white);
        }

        int max_exp = parameters.getMaxExposureCompensation();
        int min_exp = parameters.getMinExposureCompensation();
        int exposure = Integer.parseInt(preferencesCamera.read(context.getString(R.string.key_exposure), String.valueOf(parameters.getExposureCompensation())));
        if (exposure >= min_exp && exposure <= max_exp)
            parameters.setExposureCompensation(exposure);

        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        String str = preferencesCamera.read(context.getString(R.string.key_pic_size), " ");
        int width = Integer.parseInt(preferencesCamera.read(context.getString(R.string.key_pic_size_width), String.valueOf(parameters.getPictureSize().width)));
        int height = Integer.parseInt(preferencesCamera.read(context.getString(R.string.key_pic_size_height), String.valueOf(parameters.getPictureSize().height)));
        parameters.setPictureSize(width, height);

        int rotation = Integer.parseInt(preferencesCamera.read(context.getString(R.string.key_rotation), "90"));
        if (rotation >= 0 && rotation <= 270)
            parameters.setRotation(rotation);

    }

}
