package com.victjava.scales.service;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import com.victjava.scales.ActivityCheck;
import com.victjava.scales.Main;
import com.victjava.scales.Preferences;
import com.victjava.scales.R;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Сервис сьемки фото.
 *
 * @author Kostya
 */
public class ServiceTake extends Service {
    /**
     * Таймер для периода сьемки
     */
    Timer timer;
    /**
     * Камера
     */
    private Camera camera;
    /**
     * Настройки
     */
    Preferences preferences;
    /**
     * Временный фаил изображения
     */
    private File tempFileTake;
    /**
     * Флаг делается изображение
     */
    private boolean flagWaitTake;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //super.onStartCommand(intent, flags, startId);
        /** Есть цель */
        if (intent != null) {
            /** Есть действие цели */
            if (intent.getAction() != null) {
                /** Действие цели сделать одно фото */
                if ("com.victjava.scales.TAKE".equals(intent.getAction())) {
                    /** Цель имеет параметры */
                    if (intent.getExtras() != null) {
                        /** Номер весового чека. */
                        int checkId = intent.getIntExtra("com.victjava.scales.CHECK_ID", -1);
                        PendingIntent pi = intent.getParcelableExtra(ActivityCheck.PENDING_TAKE);
                        /** Запустить процесс сьемки. */
                        new Thread(new TakePicture(checkId, pi)).start();
                    }
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        /** Экземпляр настроек камеры */
        preferences = new Preferences(getApplicationContext());
        /** Качество фото в процентах*/
        int temp = Integer.parseInt(preferences.read(getString(R.string.key_quality_pic), "50"));
        /** Должно быть в диапазоне в процентах */
        if (temp < 10 || temp > 100)
            preferences.write(getString(R.string.key_quality_pic), "50");
        /** Открываем главную камеру*/
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        /** Получить параметры камеры */
        Main.parameters = camera.getParameters();
        /** Сбрасываем настройки */
        camera.release();
        /** Загружаем сохраненные настройки. */
        loadParametersToCamera();
    }

    /** Загружаем параметры камеры в настройки программы.  */
    void loadParametersToCamera() {

        Preferences preferences = new Preferences(getApplicationContext());

        List<String> colorEffects = Main.parameters.getSupportedColorEffects();
        if (colorEffects != null) {
            String color = preferences.read(getString(R.string.key_color_effect), Main.parameters.getColorEffect());
            if (colorEffects.contains(color))
                Main.parameters.setColorEffect(color);
        }

        List<String> antiBanding = Main.parameters.getSupportedAntibanding();
        if (antiBanding != null) {
            String banding = preferences.read(getString(R.string.key_anti_banding), Main.parameters.getAntibanding());
            if (antiBanding.contains(banding))
                Main.parameters.setAntibanding(banding);
        }

        List<String> flashModes = Main.parameters.getSupportedFlashModes();
        if (flashModes != null) {
            String flash = preferences.read(getString(R.string.key_flash_mode), Main.parameters.getFlashMode());
            if (flashModes.contains(flash))
                Main.parameters.setFlashMode(flash);
        }

        List<String> focusModes = Main.parameters.getSupportedFocusModes();
        if (focusModes != null) {
            String focus = preferences.read(getString(R.string.key_focus_mode), Main.parameters.getFocusMode());
            if (focusModes.contains(focus))
                Main.parameters.setFocusMode(focus);
        }

        List<String> sceneModes = Main.parameters.getSupportedSceneModes();
        if (sceneModes != null) {
            String scene = preferences.read(getString(R.string.key_scene_mode), Main.parameters.getSceneMode());
            if (sceneModes.contains(scene))
                Main.parameters.setSceneMode(scene);
        }

        List<String> whiteBalance = Main.parameters.getSupportedWhiteBalance();
        if (whiteBalance != null) {
            String white = preferences.read(getString(R.string.key_white_mode), Main.parameters.getWhiteBalance());
            if (whiteBalance.contains(white))
                Main.parameters.setWhiteBalance(white);
        }

        int max_exp = Main.parameters.getMaxExposureCompensation();
        int min_exp = Main.parameters.getMinExposureCompensation();
        int exposure = Integer.parseInt(preferences.read(getString(R.string.key_exposure), String.valueOf(Main.parameters.getExposureCompensation())));
        if (exposure >= min_exp && exposure <= max_exp)
            Main.parameters.setExposureCompensation(exposure);

        //List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        int width = Integer.parseInt(preferences.read(getString(R.string.key_pic_size_width), String.valueOf(Main.parameters.getPictureSize().width)));
        int height = Integer.parseInt(preferences.read(getString(R.string.key_pic_size_height), String.valueOf(Main.parameters.getPictureSize().height)));
        Main.parameters.setPictureSize(width, height);

        int rotation = Integer.parseInt(preferences.read(getString(R.string.key_rotation), "90"));
        if (rotation >= 0 && rotation <= 270)
            Main.parameters.setRotation(rotation);
    }

    /** Сжатие и поворот изибражения.
     * @param input Входящии данные.
     * @param camera Экземпляр камеры.
     * @return Сжатые данные.
     * @throws Exception Исключение при ошибки преобразования данных.
     */
    byte[] compressImage(byte[] input, Camera camera) throws Exception{
        //Preferences preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
        Bitmap original;
        try {

            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(input, 0, input.length, options);
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPictureSize();
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, size.width, size.height);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            //return BitmapFactory.decodeResource(res, resId, options);
            /** Создаем битовую карту из входящих данных */
            //original = BitmapFactory.decodeByteArray(input, 0, input.length);
            original = BitmapFactory.decodeByteArray(input, 0, input.length, options);
            //original.recycle();
            /** Исключение если память выходит за пределы. */
        } catch (OutOfMemoryError e) {
            /** Создаем опции битовой карты */
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inJustDecodeBounds = true;
            /** Временное хранилище */
            bitmapOptions.inTempStorage = new byte[32 * 1024];
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPictureSize();
            /** Получить высоту */
            int height11 = size.height;
            /** Получить ширину */
            int width11 = size.width;
            /** Размер картинки в мб */
            float mb = (float)(width11 * height11) / 1024000;

            if (mb > 4.0f)
                bitmapOptions.inSampleSize = 4;
            else if (mb > 3.0f)
                bitmapOptions.inSampleSize = 2;

            bitmapOptions.inJustDecodeBounds = false;
            /** Создаем битовую карту из опций */
            original = BitmapFactory.decodeByteArray(input, 0, input.length, bitmapOptions);
        }
        /** Создаем матрикс обьект */
        Matrix matrix = new Matrix();
        /** Поворот изображения в градусах против часовой стрелки*/
        matrix.postRotate(Integer.parseInt(preferences.read(getString(R.string.key_rotation), "90"))); // anti-clockwise by 90 degrees
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        try {
            Bitmap bitmapRotate = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            bitmapRotate.compress(Bitmap.CompressFormat.JPEG, Integer.parseInt(preferences.read(getString(R.string.key_quality_pic), "50")), blob);
            original.recycle();
            original = null;
            bitmapRotate.recycle();
            bitmapRotate = null;
        } catch (OutOfMemoryError e) {
            original.recycle();
            original = null;
            e.printStackTrace();
        }
        return blob.toByteArray();
    }

    public static int calculateInSampleSize( BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void shootSound() {
        /*AudioManager meng = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);
        MediaPlayer _shootMP=null;

        if (volume != 0) {
            if (_shootMP == null)
                _shootMP = MediaPlayer.create(getApplicationContext(), Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (_shootMP != null)
                _shootMP.start();
        }*/

        SoundPool soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        int shutterSound = soundPool.load(this, R.raw.camera_click, 0);
        soundPool.play(shutterSound, 1f, 1f, 0, 0, 1);
    }

    /** Класс запуска зделать фото. */
    class TakePicture implements Runnable {
        final int checkId;
        PendingIntent pendingIntent;

        /** Конструктор класса зделать фото.
         * @param id Номер весового чека к которому относиться фото.
         * @param pi Намерение обратной отправки откуда запущен сервис.
         */
        TakePicture(int id, PendingIntent pi){
            checkId = id;
            pendingIntent = pi;
        }

        @Override
        public void run() {
            /** Пока обрабатывается фото. */
            while (flagWaitTake) ;
            /** Сделать фото*/
            takeImage();
        }

        private void openCamera(int cameraId, Camera.Parameters parameters){
            /** Экземпляр камеры существует. */
            if (camera != null) {
                /** Сбрасываем настройки. */
                camera.release();
            }
            /** Открываем главную камеру. */
            camera = Camera.open(cameraId);
            /** Загружаем настройки из программы. */
            camera.setParameters(parameters);
        }

        private void closeCamera(){
            /** Экземпляр камеры существует. */
            if (camera != null){
                /** Удаляем обратный вызовю. */
                camera.setPreviewCallback(null);
                /** Выключаем отображение картинки. */
                camera.stopPreview();
                /** Завершаем работу скамеро. */
                camera.release();
                /** Удаляем экземплярк камеры. */
                camera = null;
            }
        }

        /** Сделать фотографию. */
        private void takeImage() {
            /** Установливаем флаг делаем фото. */
            flagWaitTake = true;
            /** Экземпляр камеры существует. */
            if (camera != null) {
                /** Сбрасываем настройки. */
                camera.release();
            }
            /** Открываем главную камеру. */
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            /** Загружаем настройки из программы. */
            camera.setParameters(Main.parameters);
            /** Начать сьемку изображения. */
            camera.startPreview();
            /** Задержка  2 секунды для стабилизации камеры. */
            try {TimeUnit.SECONDS.sleep(2);} catch (InterruptedException e) {}
            try {
                /** Сделать сьемку изображения. */
                camera.takePicture(null, null, null, jpegCallback);
                //shootSound();
            } catch (Exception e) {
                try {
                    /** При ошибке сделать пересоединение. */
                    camera.reconnect();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                /** Остановить сьемку. */
                camera.stopPreview();
                /** Сбросить настройки. */
                camera.release();
                flagWaitTake = false;
            }
        }

        /** Обратный вызов камеры. */
        final Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            /** Фото сделано.
             * @param data Данные изображения.
             * @param camera Камера которая сделала изображение.
             */
            @Override
            public void onPictureTaken(final byte[] data, final Camera camera) {
                /** Процесс обработки сделаного фото */
                //new Thread(new Runnable() {
                    //@Override
                    //public /*synchronized*/ void run() {
                        try {
                            /** Сжимаем данные изображения. */
                            byte[] compressImage = compressImage(data, camera);
                            /** Создаем штамп времени */
                            String timeStamp = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
                            /** Создаем имя папки по дате */
                            String folderStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
                            /** Сохраняем фаил. */
                            String path = saveExternalMemory(Main.path.getAbsolutePath() + File.separator + folderStamp, "№" + String.valueOf(checkId) + "(" + timeStamp + ").jpg", compressImage);
                            //String path = saveInternalMemory(Main.FOLDER_LOCAL /*+ File.separator + folderStamp*/, folderStamp + "_" + timeStamp + "(" + String.valueOf(checkId) + ").jpg", compressImage);
                            Intent intent = new Intent();
                            intent.putExtra("com.victjava.scales.PHOTO_PATH", path);
                            pendingIntent.send(ServiceTake.this, 0, intent);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        /** Закрываем камеру */
                        camera.stopPreview();
                        /** Сбрасываем настройки */
                        camera.release();
                        /** Сбрасываем флаг фото сделано */
                        flagWaitTake = false;
                    //}
                //}).start();
            }
        };

        /** Процедура сохранения фото во внутренюю память приложения.
         * @param folderStamp Имя папки.
         * @param file Имя фаила.
         * @param data Массив данных файла
         * @throws IOException Исключение при ошибки записи данных.
         */
        String saveInternalMemory(String folderStamp, String file, byte[] data) throws IOException {
            /** Создаем папку с именем штампа даты. */
            File folderPath = new File(getFilesDir()+File.separator+folderStamp);
            /** Делаем папку. */
            folderPath.mkdirs();
            /** Создаем фаил с именем штампа времени. */
            File fileTake = new File(folderPath, file);
            /** Создаем поток для записи фаила в папку временного хранения. */
            FileOutputStream fileOutputStream = new FileOutputStream(fileTake);
            /** Записываем фаил в папку. */
            fileOutputStream.write(data);
            /** Закрываем поток. */
            fileOutputStream.close();
            /** Возвращяем путь к файлу. */
            return fileTake.getPath();
        }

        /** Процедура сохранения файла во внешней памяти.
         * @param folder Имя папки.
         * @param file Имя файла.
         * @param data Данные файла.
         * @return Возвращяет путь к сохраненному файлу.
         * @throws IOException Ошибка сохранения фаила.
         */
        String saveExternalMemory(String folder, String file, byte[] data) throws IOException {
            /** Создаем папку с именем штампа даты. */
            File folderPath = new File(folder);
            /** Делаем папку. */
            folderPath.mkdirs();
            /** Создаем фаил с именем штампа времени. */
            File fileTake = new File(folderPath.getPath(), file);
            /** Создаем поток для записи фаила в папку временного хранения. */
            FileOutputStream fileOutputStream = new FileOutputStream(fileTake.getPath());
            /** Записываем фаил в папку. */
            fileOutputStream.write(data);
            /** Закрываем поток. */
            fileOutputStream.close();
            /** Возвращяем путь к файлу. */
            return fileTake.getPath();
        }
    }

}
