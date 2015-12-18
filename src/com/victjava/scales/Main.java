package com.victjava.scales;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.konst.module.BootModule;
import com.konst.module.ScaleModule;
import com.victjava.scales.provider.ErrorTable;
import com.victjava.scales.service.ServiceSmsCommand;

import java.io.File;
import java.util.List;

/**
 * @author Kostya
 */
public class Main extends Application {
    ScaleModule scaleModule;
    BootModule bootModule;
    public static Camera.Parameters parameters;
    public static File path;
    public static String FOLDER_LOCAL = "CheckPhoto";
    /** Класс формы для передачи данных весового чека. */
    /** Настройки для весов. */
    public static Preferences preferencesScale;
    /** Настройки для обновления весов. */
    public static Preferences preferencesUpdate;
    /** Настройки для камеры. */
    public static Preferences preferencesCamera;

    PackageInfo packageInfo;

    /** Версия пограммы весового модуля. */
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

        preferencesScale = new Preferences(getApplicationContext()); //загрузить настройки

        stepMeasuring = preferencesScale.read(getString(R.string.KEY_STEP), getResources().getInteger(R.integer.default_step_scale));
        autoCapture = preferencesScale.read(getString(R.string.KEY_AUTO_CAPTURE), getResources().getInteger(R.integer.default_max_auto_capture));
        dayDeleteCheck = preferencesScale.read(getString(R.string.KEY_DAY_CHECK_DELETE), getResources().getInteger(R.integer.default_day_delete_check));
        dayClosedCheck = preferencesScale.read(getString(R.string.KEY_DAY_CLOSED_CHECK), getResources().getInteger(R.integer.default_day_close_check));
        //ScaleModule.setTimerNull(Preferences.read(getString(R.string.KEY_TIMER_NULL), default_max_time_auto_null));
        //ScaleModule.setWeightError(Preferences.read(getString(R.string.KEY_MAX_NULL), default_limit_auto_null));
        timeDelayDetectCapture = getResources().getInteger(R.integer.time_delay_detect_capture);

        /** Создаем путь к временной папке для для хранения файлов. */
        path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + FOLDER_LOCAL);
        /** Если нет папки тогда создаем. */
        if (!path.exists()) {
            if (!path.mkdirs()) {
                new ErrorTable(this).insertNewEntry("500", "Path no create: " + path.getPath());
                //todo что зделать если не создали папку
            }
        }
        /** Окрываем экземпляр основной камеры. */
        Camera camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        /** Получаем параметры камеры */
        parameters = camera.getParameters();
        camera.release();
        loadParametersToCamera();
        /** Запускаем сервис для приемеа смс команд. */
        getApplicationContext().startService(new Intent(getApplicationContext(), ServiceSmsCommand.class));

    }

    /**
     * Загружаем параметры камеры в настройки программы
     */
    public void loadParametersToCamera() {

        preferencesCamera = new Preferences(getApplicationContext());

        List<String> colorEffects = parameters.getSupportedColorEffects();
        if (colorEffects != null) {
            String color = preferencesCamera.read(getString(R.string.key_color_effect), parameters.getColorEffect());
            if (colorEffects.contains(color))
                parameters.setColorEffect(color);
        }

        List<String> antiBanding = parameters.getSupportedAntibanding();
        if (antiBanding != null) {
            String banding = preferencesCamera.read(getString(R.string.key_anti_banding), parameters.getAntibanding());
            if (antiBanding.contains(banding))
                parameters.setAntibanding(banding);
        }

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null) {
            String flash = preferencesCamera.read(getString(R.string.key_flash_mode), parameters.getFlashMode());
            if (flashModes.contains(flash))
                parameters.setFlashMode(flash);
        }

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            String focus = preferencesCamera.read(getString(R.string.key_focus_mode), parameters.getFocusMode());
            if (focusModes.contains(focus))
                parameters.setFocusMode(focus);
        }

        List<String> sceneModes = parameters.getSupportedSceneModes();
        if (sceneModes != null) {
            String scene = preferencesCamera.read(getString(R.string.key_scene_mode), parameters.getSceneMode());
            if (sceneModes.contains(scene))
                parameters.setSceneMode(scene);
        }

        List<String> whiteBalance = parameters.getSupportedWhiteBalance();
        if (whiteBalance != null) {
            String white = preferencesCamera.read(getString(R.string.key_white_mode), parameters.getWhiteBalance());
            if (sceneModes != null ? sceneModes.contains(white) : false)
                parameters.setWhiteBalance(white);
        }

        int max_exp = parameters.getMaxExposureCompensation();
        int min_exp = parameters.getMinExposureCompensation();
        int exposure = Integer.parseInt(preferencesCamera.read(getString(R.string.key_exposure), String.valueOf(parameters.getExposureCompensation())));
        if (exposure >= min_exp && exposure <= max_exp)
            parameters.setExposureCompensation(exposure);

        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        String str = preferencesCamera.read(getString(R.string.key_pic_size), " ");
        int width = Integer.parseInt(preferencesCamera.read(getString(R.string.key_pic_size_width), String.valueOf(parameters.getPictureSize().width)));
        int height = Integer.parseInt(preferencesCamera.read(getString(R.string.key_pic_size_height), String.valueOf(parameters.getPictureSize().height)));
        parameters.setPictureSize(width, height);

        int rotation = Integer.parseInt(preferencesCamera.read(getString(R.string.key_rotation), "90"));
        if (rotation >= 0 && rotation <= 270)
            parameters.setRotation(rotation);

    }

}
