package com.victjava.scales;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.*;
import android.provider.BaseColumns;
import android.provider.ContactsContract.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.*;
import android.widget.*;
import com.konst.module.scale.ScaleModule;
import com.victjava.scales.camera.CameraCallback;
import com.victjava.scales.camera.CameraSurface;
import com.victjava.scales.provider.CheckTable;
import com.victjava.scales.provider.TaskTable;
import com.victjava.scales.provider.TaskTable.*;


import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ActivityCheck extends FragmentActivity implements View.OnClickListener, Runnable, CameraCallback {
    private SpannableStringBuilder textKg;
    private FrameLayout cameraHolder;
    private SlidingDrawer slidingDrawer;
    private CameraSurface cameraSurface;
    private Thread threadAutoWeight;
    private ScaleModule scaleModule;
    private Globals globals;
    private TakingTimeout takingTimeout;
    private CheckTable checkTable;
    private Vibrator vibrator; //вибратор
    private ProgressBar progressBarSensor, progressBarStable;
    //private WeightView weightTextView;
    TextView textViewWeight;
    private TabHost mTabHost;
    private TabsAdapter mTabsAdapter;
    private ImageView buttonFinish, buttonPhoto;
    private SimpleGestureFilter detectorWeightView;
    private Drawable dProgressWeight, dWeightDanger;
    protected ContentValues values = new ContentValues();
    private Dialog dialogCamera;
    //private WeightCallback weightCallback = null;
    /** Количество стабильных показаний веса для авто сохранения. */
    public static final int COUNT_STABLE = 64;
    //public final static int START_TAKE = 1;
    public static final String PENDING_TAKE = "com.victjava.scales.PENDING_TAKE";

    public int entryID;
    public int numStable;
    private int moduleWeight;
    protected int tempWeight;

    protected boolean isStable;
    private boolean running;
    private boolean taking;
    private boolean flagExit = true;
    private boolean touchWeightView;
    private boolean weightViewIsSwipe;

    public enum Action {
        /** Остановка взвешивания. */
        STOP_WEIGHTING,
        /** Пуск взвешивания */
        START_WEIGHTING,
        /** Сохранить результат взвешивания. */
        STORE_WEIGHTING,
        /** Обновить данные веса. */
        UPDATE_PROGRESS,
        /** Показать диалог сохраняем */
        DIALOG_SAVE
    }
    protected interface OnCheckEventListener {
        void someEvent();
    }

    @Override
    public void run() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        //handler.obtainMessage(Action.FREEZE_SCREEN.ordinal(), true).sendToTarget();                                   //Экран делаем видимым
        while (running) {

            weightViewIsSwipe = false;
            numStable = 0;

            while (running && !isCapture() && !weightViewIsSwipe) {                                                     //ждём начала нагружения
                try { Thread.sleep(50);} catch (InterruptedException ignored) {}
            }
            if (!running) {
                break;
            }
            handler.obtainMessage(Action.START_WEIGHTING.ordinal()).sendToTarget();
            isStable = false;
            while (running && !(isStable || weightViewIsSwipe)) {                                                       //ждем стабилизации веса или нажатием выбора веса
                try { Thread.sleep(50);} catch (InterruptedException ignored) {}
                if (!touchWeightView) {                                                                                 //если не прикасаемся к индикатору тогда стабилизируем вес
                    isStable = processStable(moduleWeight);
                    handler.obtainMessage(Action.UPDATE_PROGRESS.ordinal(), numStable, 0).sendToTarget();
                }
            }
            numStable = COUNT_STABLE;
            if (!running) {
                break;
            }
            //if (isStable) {
                handler.obtainMessage(Action.STORE_WEIGHTING.ordinal(), moduleWeight, 0).sendToTarget();                //сохраняем стабильный вес
            //}

            weightViewIsSwipe = false;

            while (running && moduleWeight >= getResources().getInteger(R.integer.default_min_auto_capture)) {
                try {Thread.sleep(50); } catch (InterruptedException ignored) {}                                        // ждем разгрузки весов
            }
            vibrator.vibrate(100);
            handler.obtainMessage(Action.UPDATE_PROGRESS.ordinal(), 0, 0).sendToTarget();

            if (!running) {
                if (isStable && values.getAsInteger(CheckTable.KEY_CHECK_STATE) == CheckTable.State.CHECK_SECOND.ordinal()) { //Если тара зафоксирована и выход через кнопку назад
                    values.put(CheckTable.KEY_CHECK_STATE, CheckTable.State.CHECK_PRELIMINARY.ordinal());
                }
                break;
            }
            /*if (!running) {
                if (isStable && weightType == WeightType.SECOND) {                                                      //Если тара зафоксирована и выход через кнопку назад
                    weightType = WeightType.NETTO;
                }
                break;
            }*/
            handler.obtainMessage(Action.DIALOG_SAVE.ordinal(), false).sendToTarget();                                  // Диалог открываем сохраняем данные
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ignored) {
            }                            //задержка
            handler.obtainMessage(Action.DIALOG_SAVE.ordinal(), true).sendToTarget();                                   // Диалог закрываем
            if (values.getAsInteger(CheckTable.KEY_CHECK_STATE) == CheckTable.State.CHECK_SECOND.ordinal()) {
                stopThread(); //running = true;
            }
            /*if (weightType == WeightType.SECOND) {
                stopThread(); //running = true;
            }*/

            handler.obtainMessage(Action.STOP_WEIGHTING.ordinal()).sendToTarget();
        }
        //start = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Thread.setDefaultUncaughtExceptionHandler(new ReportHelper(this));
        setContentView(R.layout.check);

        globals = Globals.getInstance();
        scaleModule = globals.getScaleModule();
        //scaleModule.setWeightCallback(resultWeightCallback);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        checkTable = new CheckTable(this);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        entryID = Integer.valueOf(getIntent().getStringExtra("id"));
        try {
            values = checkTable.getValuesItem(entryID);
        } catch (Exception e) {
            exit();
        }

        setTitle(getString(R.string.Check_N) + entryID + ' ' + ": " + values.getAsString(CheckTable.KEY_VENDOR)); //установить заголовок
        setupTabHost(savedInstanceState);
        setupWeightView();

        textKg = new SpannableStringBuilder(getResources().getString(R.string.scales_kg));
        textKg.setSpan(new TextAppearanceSpan(this, R.style.SpanTextKg),0,textKg.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        progressBarSensor = (ProgressBar) findViewById(R.id.progressBarSensor);
        progressBarSensor.setMax(scaleModule.getMarginTenzo());
        progressBarSensor.setSecondaryProgress(scaleModule.getLimitTenzo());



        buttonFinish = (ImageView) findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(this);

        findViewById(R.id.imageViewPage).setOnClickListener(this);

        setupSliding();
        if(globals.getPreferencesCamera().read(getString(R.string.KEY_PHOTO_CHECK), false))
            setupPictureMode();

        /*if (values.getAsInteger(CheckTable.KEY_WEIGHT_FIRST) == 0 || values.getAsInteger(CheckTable.KEY_WEIGHT_SECOND) == 0) {
            weightCallback = new WeightCallback();
            scaleModule.startMeasuringWeight(weightCallback);
        }*/
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startThread();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonFinish:
                exit();
                break;
            case R.id.imageViewPage:
                vibrator.vibrate(100);
                startActivity(new Intent(getBaseContext(), ActivityViewCheck.class).putExtra("id", entryID));
                exit();
                break;
            case R.id.takePhotoDialog:
                weightViewIsSwipe = true;
                //handler.obtainMessage(Action.STORE_WEIGHTING.ordinal(), moduleWeight, 0).sendToTarget();
                /*new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cameraSurface.startTakePicture(values.getAsInteger(CheckTable.KEY_CHECK_STATE));
                    }
                }).start();*/
                break;
            default:
        }
    }

    @Override
    public void onBackPressed() {
        if (flagExit) {
            super.onBackPressed();
            exit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        scaleModule.startMeasuringWeight(weightCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scaleModule.stopMeasuringWeight();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String path = data.getStringExtra("com.victjava.scales.PHOTO_PATH");
        if(path != null){
            switch (CheckTable.State.values()[requestCode]){
                case CHECK_FIRST:
                    values.put(CheckTable.KEY_PHOTO_FIRST, path);
                    break;
                case CHECK_SECOND:
                    values.put(CheckTable.KEY_PHOTO_SECOND, path);
                    break;
                default:
            }
        }
        taking = false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    @Override
    public void onShutter() {
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

    @Override
    public void onJpegPictureTaken(byte[] data, Camera camera, int id) {
        /** Задвигаем окно камеры */
        slidingDrawer.animateClose();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    /** Сжимаем данные изображения. */
                    byte[] compressImage = compressImage(data, camera.getParameters().getPictureSize().width, camera.getParameters().getPictureSize().height);
                    /** Создаем штамп времени */
                    String timeStamp = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
                    /** Создаем имя папки по дате */
                    String dateStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
                    /** Сохраняем фаил. */
                    String path;
                    /** Проверяем куда сохранять фаил. */
                    if (globals.getPreferencesScale().read(getString(R.string.KEY_MEMORY_PHOTO), false))
                        path = saveInternalMemory(globals.FOLDER_LOCAL, dateStamp + "_" + timeStamp + ".jpg", compressImage);
                    else
                        path = saveExternalMemory(globals.path.getAbsolutePath(), dateStamp + "_" +timeStamp + ".jpg", compressImage);
                    if(path != null){
                        switch (CheckTable.State.values()[id]){
                            case CHECK_FIRST:
                                values.put(CheckTable.KEY_PHOTO_FIRST, path);
                                break;
                            case CHECK_SECOND:
                                values.put(CheckTable.KEY_PHOTO_SECOND, path);
                                break;
                            default:
                        }
                    }
                } catch (FileNotFoundException e) {}
                catch (IOException e) {}
                catch (Exception e) {}
                cameraSurface.startPreview();
                taking = false;
            }
        }).start();
    }

    private void setupPictureMode(){
        cameraHolder = (FrameLayout)findViewById(R.id.camera_preview);
        buttonPhoto = (ImageView) findViewById(R.id.takePhotoDialog);
        buttonPhoto.setOnClickListener(this);
        cameraSurface = new CameraSurface(this);
        cameraHolder.addView(cameraSurface);
        cameraSurface.setCallback(this);
    }

    private void setupSliding() {
        final ImageView ibHandle = (ImageView) findViewById(R.id.handle);
        slidingDrawer = (SlidingDrawer) findViewById(R.id.drawer);
        slidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            public void onDrawerOpened() {
                ibHandle.setImageResource(R.drawable.ic_action_sliding_right);
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            public void onDrawerClosed() {
                ibHandle.setImageResource(R.drawable.ic_action_sliding_up);
            }
        });
        slidingDrawer.setOnDrawerScrollListener(new SlidingDrawer.OnDrawerScrollListener() {

            public void onScrollEnded() {
                return;
                // TODO Auto-generated method stub

            }

            public void onScrollStarted() {
                return;
                // TODO Auto-generated method stub
            }
        });
    }

    private void setupDialogCamera(){
        /*dialogCamera = new Dialog(this);
        dialogCamera.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogCamera.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogCamera.setCancelable(false);
        dialogCamera.show();
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = layoutInflater.inflate(R.layout.dialog_camera1, null);
        dialogCamera.setContentView(convertView);
        cameraHolder = (FrameLayout) convertView.findViewById(R.id.camera_preview4);
        TextView dialogTitle = (TextView) convertView.findViewById(R.id.dialog_title);
        dialogTitle.setText("Фото");
        Button positiveButton = (Button)convertView.findViewById(R.id.buttonClosedDialog);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialogCamera.dismiss();
            }
        });*/


        dialogCamera = new Dialog(this);
        dialogCamera.setCancelable(false);
        dialogCamera.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogCamera.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogCamera.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialogCamera.show();
        dialogCamera.setContentView(R.layout.dialog_camera);
        cameraHolder = (FrameLayout)dialogCamera.findViewById(R.id.camera_preview);
        cameraHolder.setOnClickListener(this);
    }

    private void setupTabHost(Bundle savedInstanceState) {
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
        mTabsAdapter.addTab(mTabHost.newTabSpec("input").setIndicator(createTabView(this, getString(R.string.incoming))), InputFragment.class);
        mTabsAdapter.addTab(mTabHost.newTabSpec("output").setIndicator(createTabView(this, getString(R.string.outgo))), OutputFragment.class);
        switch (values.getAsInteger(CheckTable.KEY_DIRECT)) {
            case CheckTable.DIRECT_DOWN:
                mTabHost.setCurrentTab(0);
                break;
            case CheckTable.DIRECT_UP:
                mTabHost.setCurrentTab(1);
                break;
            default:
        }

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

    /**
     * Устоновка индикатора веса.
     */
    private void setupWeightView() {

        //weightTextView = new WeightView(this);
        textViewWeight = (TextView) findViewById(R.id.textViewWeight);
        progressBarStable = (ProgressBar)findViewById(R.id.progressBarStable);
        progressBarStable.setMax(COUNT_STABLE);
        progressBarStable.setProgress(numStable = 0);
        dProgressWeight = getResources().getDrawable(R.drawable.progress_weight);
        dWeightDanger = getResources().getDrawable(R.drawable.progress_weight_danger);

        SimpleGestureFilter.SimpleGestureListener weightViewGestureListener = new SimpleGestureFilter.SimpleGestureListener() {
            @Override
            public void onSwipe(int direction) {

                switch (direction) {
                    case SimpleGestureFilter.SWIPE_RIGHT:
                    case SimpleGestureFilter.SWIPE_LEFT:
                        if (saveWeight(moduleWeight)) {
                            weightViewIsSwipe = true;
                            buttonFinish.setEnabled(true);
                            buttonFinish.setAlpha(255);
                            flagExit = true;
                        }
                        /*if (values.getAsInteger(CheckTable.KEY_CHECK_STATE) == CheckTable.State.CHECK_SECOND.ordinal()) {
                            weightTypeUpdate();
                        }*/
                        /*if (weightType == WeightType.SECOND) {
                            weightTypeUpdate();
                        }*/
                        break;
                    default:
                }
            }

            @Override
            public void onDoubleTap() {
                progressBarStable.setProgress(0);
                vibrator.vibrate(100);
                new ZeroThread(ActivityCheck.this).start();
            }
        };

        detectorWeightView = new SimpleGestureFilter(this, weightViewGestureListener);
        detectorWeightView.setSwipeMinVelocity(50);
        textViewWeight.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detectorWeightView.setSwipeMaxDistance(v.getMeasuredWidth());
                detectorWeightView.setSwipeMinDistance(detectorWeightView.getSwipeMaxDistance() / 3);
                detectorWeightView.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        touchWeightView = true;
                        vibrator.vibrate(5);
                        int progress = (int) (event.getX() / (detectorWeightView.getSwipeMaxDistance() / progressBarStable.getMax()));
                        progressBarStable.setProgress(progress);
                        break;
                    //case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        touchWeightView = false;
                        break;
                    default:
                }
                return false;
            }
        });
    }

    protected void exit() {
        scaleModule.stopMeasuringWeight();
        stopThread();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(taking){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            takingTimeout = new TakingTimeout(10000,10000);
                            takingTimeout.start();
                        }
                    });
                    while (taking){ try { TimeUnit.MILLISECONDS.sleep(10); } catch (InterruptedException e) { } };
                }
                //takingTimeout.cancel();
                checkTable.updateEntry(entryID, values);
                if (values.getAsInteger(CheckTable.KEY_CHECK_STATE) == CheckTable.State.CHECK_PRELIMINARY.ordinal()) {
                    new TaskTable(ActivityCheck.this).setCheckReady(entryID, values);
                    taskToContact();
                    startActivity(new Intent(getBaseContext(), ActivityViewCheck.class).putExtra("id", entryID));
                }
                finish();
            }
        }).start();
    }

    /** Добавляет задачи для контакта в весовом чеке.
     * Если для контакта установлены галочки для телефона и почты.
     */
    private void taskToContact(){
        int contactId = values.getAsInteger(CheckTable.KEY_VENDOR_ID);
        Cursor data = getContentResolver().query(Data.CONTENT_URI,
                new String[] {BaseColumns._ID, Data.DATA1, Data.DATA5, Data.MIMETYPE},
                Data.CONTACT_ID+"=?"+" and ("+ Data.MIMETYPE+"='"+ CommonDataKinds.Phone.CONTENT_ITEM_TYPE+'\''+" or "+ Data.MIMETYPE+"='"+ CommonDataKinds.Email.CONTENT_ITEM_TYPE+'\''+')'
                        +" and "+Data.DATA5+" = 1",
                new String[] {String.valueOf(contactId)}, null);
        try {
            data.moveToFirst();
            if(!data.isAfterLast()){
                do {
                    String type = data.getString(data.getColumnIndex(Data.MIMETYPE));
                    String address = data.getString(data.getColumnIndex(Data.DATA1));
                    if(CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(type))
                        new TaskTable(this).insertNewTaskPhone(TaskType.TYPE_CHECK_SEND_SMS_CONTACT, entryID, contactId, address);
                    else if(CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(type))
                        new TaskTable(this).insertNewTaskEmail(TaskType.TYPE_CHECK_SEND_MAIL, entryID, contactId, address);
                } while (data.moveToNext());
            }
            data.close();
        }catch (Exception e){}


    }

    private static View createTabView(final Context context, final CharSequence text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        return view;
    }

    /**
     * Захват веса для авто сохранения веса.
     * Задержка захвата от ложных срабатываний. Устанавливается значения в настройках.
     *
     * @return true - Условия захвата истины.
     */
    public boolean isCapture() {
        boolean capture = false;
        while (moduleWeight > globals.getAutoCapture()) {
            if (capture) {
                return true;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(globals.getTimeDelayDetectCapture());
                } catch (InterruptedException ignored) {}
                capture = true;
            }
        }
        return false;
    }

    public boolean processStable(int weight) {
        if (tempWeight - globals.getStepMeasuring() <= weight && tempWeight + globals.getStepMeasuring() >= weight) {
            if (++numStable >= COUNT_STABLE) {
                return true;
            }
        } else {
            numStable = 0;
        }
        tempWeight = weight;
        return false;
    }

    /**
     * Преобразовать вес в шкалу шага веса.
     * Шаг измерения установливается в настройках.
     *
     * @param weight Вес для преобразования.
     * @return Преобразованый вес.
     */
    private int getWeightToStepMeasuring(int weight) {
        return weight / globals.getStepMeasuring() * globals.getStepMeasuring();
    }

    private boolean saveWeight(int weight/*, WeightType type*/) {
        boolean flag = false;
        switch (CheckTable.State.values()[values.getAsInteger(CheckTable.KEY_CHECK_STATE)]) {
            case CHECK_FIRST:
                if (weight > 0) {
                    values.put(CheckTable.KEY_WEIGHT_FIRST, weight);
                    vibrator.vibrate(100); //вибрация
                    flag = true;
                }
                break;
            case CHECK_SECOND:
                values.put(CheckTable.KEY_WEIGHT_SECOND, weight);
                int total = sumNetto();
                values.put(CheckTable.KEY_PRICE_SUM, total);
                vibrator.vibrate(100); //вибрация
                flag = true;
                break;
            case CHECK_PRELIMINARY:
                //scaleModule.stopMeasuringWeight(false);
                exit();
                break;
        }
        if (flag) {
            ((OnCheckEventListener) mTabsAdapter.getCurrentFragment()).someEvent();
            buttonFinish.setEnabled(true);
            buttonFinish.setAlpha(255);
            flagExit = true;
        }
        return flag;
    }

    /**
     * Расчет веса нетто.
     *
     * @return Вес нетто.
     */
    public int sumNetto() {
        int netto = values.getAsInteger(CheckTable.KEY_WEIGHT_FIRST) - values.getAsInteger(CheckTable.KEY_WEIGHT_SECOND);
        values.put(CheckTable.KEY_WEIGHT_NETTO, netto);
        return netto;
    }

    /**
     * Засчет стоимости
     *
     * @param netto Вес нетто.
     * @return Сумма.
     */
    public float sumTotal(int netto) {
        return (float) netto * values.getAsInteger(CheckTable.KEY_PRICE) / 1000;
    }

    private void weightTypeUpdate() {
        switch (CheckTable.State.values()[values.getAsInteger(CheckTable.KEY_CHECK_STATE)]) {
            case CHECK_FIRST:
                values.put(CheckTable.KEY_CHECK_STATE, CheckTable.State.CHECK_SECOND.ordinal());
            break;
            case CHECK_SECOND:
                values.put(CheckTable.KEY_CHECK_STATE, CheckTable.State.CHECK_PRELIMINARY.ordinal());
                saveWeight(0);
            break;
            /*default:
                values.put(CheckTable.KEY_CHECK_STATE, CheckTable.State.CHECK_FIRST.ordinal());*/
        }
        buttonFinish.setEnabled(true);
        buttonFinish.setAlpha(255);
        flagExit = true;
    }

    private class TabsAdapter extends FragmentPagerAdapter implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {

        //private final FragmentManager fragmentManager;
        private Fragment mCurrentFragment;
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<>();

        private class TabInfo {
            private final Class<?> mClass;
            private final Bundle args;

            TabInfo(final Class<?> _class, final Bundle _args) {
                mClass = _class;
                args = _args;
            }

            public Class<?> getMClass() {
                return mClass;
            }

            public Bundle getArgs() {
                return args;
            }
        }

        private class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(final Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(final String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(final FragmentActivity activity, final TabHost tabHost, final ViewPager pager) {
            super(activity.getSupportFragmentManager());

            //fragmentManager = activity.getSupportFragmentManager();
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(final TabHost.TabSpec tabSpec, final Class<?> _class) {

            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();
            TabInfo info = new TabInfo(_class, null);

            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(final int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.getMClass().getName(), info.getArgs());
        }

        @Override
        public void onTabChanged(final String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(final int position) {

            if (position == 0) {
                values.put(CheckTable.KEY_DIRECT, CheckTable.DIRECT_DOWN);
            } else if (position == 1) {
                values.put(CheckTable.KEY_DIRECT, CheckTable.DIRECT_UP);
            }
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
        }

        public Fragment getCurrentFragment() {
            return mCurrentFragment;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (!object.equals(mCurrentFragment)) {
                mCurrentFragment = (Fragment) object;
            }
            super.setPrimaryItem(container, position, object);
        }

    }

    /** Класс обработчик показаний веса
     * Возвращяем время обновления показаний веса в милисекундах.
     */
    ScaleModule.WeightCallback weightCallback = new  ScaleModule.WeightCallback() {
        @Override
        public void weight(final ScaleModule.ResultWeight what, final int weight, final int sensor) {
            moduleWeight = getWeightToStepMeasuring(weight);
            runOnUiThread(new Runnable() {
                Rect bounds;
                SpannableStringBuilder w;
                @Override
                public void run() {
                    switch (what) {
                        case WEIGHT_NORMAL:
                            w = new SpannableStringBuilder(String.valueOf(moduleWeight));
                            w.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.setSpan(new ForegroundColorSpan(Color.BLACK), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.append(textKg);
                            progressBarSensor.setProgress(sensor);
                            bounds = progressBarSensor.getProgressDrawable().getBounds();
                            progressBarSensor.setProgressDrawable(dProgressWeight);
                            progressBarSensor.getProgressDrawable().setBounds(bounds);
                            break;
                        case WEIGHT_LIMIT:
                            w = new SpannableStringBuilder(String.valueOf(moduleWeight));
                            w.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.setSpan(new ForegroundColorSpan(Color.RED), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.append(textKg);
                            progressBarSensor.setProgress(sensor);
                            bounds = progressBarSensor.getProgressDrawable().getBounds();
                            progressBarSensor.setProgressDrawable(dWeightDanger);
                            progressBarSensor.getProgressDrawable().setBounds(bounds);
                            break;
                        case WEIGHT_MARGIN:
                            w = new SpannableStringBuilder(String.valueOf(moduleWeight));
                            w.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.setSpan(new ForegroundColorSpan(Color.RED), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            progressBarSensor.setProgress(sensor);
                            vibrator.vibrate(100);
                            break;
                        case WEIGHT_ERROR:
                            w = new SpannableStringBuilder("---");
                            w.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.setSpan(new ForegroundColorSpan(Color.BLACK), 0, w.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            w.append(textKg);
                            progressBarSensor.setProgress(0);
                            break;
                        default:
                    }
                    textViewWeight.setText(w, TextView.BufferType.SPANNABLE);
                    /*switch (what) {
                        case WEIGHT_NORMAL:
                            moduleWeight = getWeightToStepMeasuring(weight);
                            progressBarWeight.setProgress(sensor);
                            bounds = progressBarWeight.getProgressDrawable().getBounds();
                            weightTextView.updateProgress(moduleWeight, Color.BLACK, getResources().getDimension(R.dimen.text_big));
                            progressBarWeight.setProgressDrawable(dProgressWeight);
                            progressBarWeight.getProgressDrawable().setBounds(bounds);
                            break;
                        case WEIGHT_LIMIT:
                            moduleWeight = getWeightToStepMeasuring(weight);
                            progressBarWeight.setProgress(sensor);
                            bounds = progressBarWeight.getProgressDrawable().getBounds();
                            weightTextView.updateProgress(moduleWeight, Color.RED, getResources().getDimension(R.dimen.text_big));
                            progressBarWeight.setProgressDrawable(dWeightDanger);
                            progressBarWeight.getProgressDrawable().setBounds(bounds);
                            break;
                        case WEIGHT_MARGIN:
                            moduleWeight = getWeightToStepMeasuring(weight);
                            progressBarWeight.setProgress(sensor);
                            weightTextView.updateProgress(getString(R.string.OVER_LOAD), Color.RED, getResources().getDimension(R.dimen.text_large_xx));
                            vibrator.vibrate(100);
                            break;
                        case WEIGHT_ERROR:
                            weightTextView.updateProgress(getString(R.string.NO_CONNECT), Color.BLACK, getResources().getDimension(R.dimen.text_large_xx));
                            progressBarWeight.setProgress(0);
                            break;
                        default:
                    }*/
                }
            });
        }
    };

    /** Обработчик сообщений. */
    final Handler handler = new Handler() {
        private ProgressDialog dialogSave;
        /** Сообщение от обработчика авто сохранения.
         * @param msg Данные сообщения.
         */
        @Override
        public void handleMessage(Message msg) {
            switch (Action.values()[msg.what] ) {
                case STORE_WEIGHTING:
                    if(globals.getPreferencesCamera().read(getString(R.string.KEY_PHOTO_CHECK), false)){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (taking);
                                taking = true;
                                cameraSurface.startTakePicture(values.getAsInteger(CheckTable.KEY_CHECK_STATE));
                            }
                        }).start();
                        //taking = true;
                    }
                    saveWeight(msg.arg1);
                    break;
                case STOP_WEIGHTING:
                    weightTypeUpdate();
                    buttonFinish.setEnabled(true);
                    buttonFinish.setAlpha(255);
                    ((OnCheckEventListener) mTabsAdapter.getCurrentFragment()).someEvent();
                    flagExit = true;
                    break;
                case START_WEIGHTING:
                    buttonFinish.setEnabled(false);
                    buttonFinish.setAlpha(100);
                    flagExit = false;
                    if(globals.getPreferencesCamera().read(getString(R.string.KEY_PHOTO_CHECK), false))
                        slidingDrawer.animateOpen();
                    break;
                case UPDATE_PROGRESS:
                    progressBarStable.setProgress(msg.arg1);
                    break;
                case DIALOG_SAVE:
                    if ((Boolean)msg.obj){
                        if (dialogSave.isShowing()) {
                            dialogSave.dismiss();
                        }
                    } else{
                        dialogSave = new ProgressDialog(ActivityCheck.this);
                        dialogSave.setCancelable(false);
                        dialogSave.setIndeterminate(false);
                        dialogSave.show();
                        dialogSave.setContentView(R.layout.custom_progress_dialog);
                        TextView tv1 = (TextView) dialogSave.findViewById(R.id.textView1);
                        tv1.setText("Сохранение...");
                    }
                    break;
                default:
            }
        }
    };

    public void startThread(){
        running = true;
        if(threadAutoWeight == null){
            threadAutoWeight = new Thread(this);
            //threadAutoWeight.setPriority(Thread.MIN_PRIORITY);
            //threadAutoWeight.setDaemon(true);
            threadAutoWeight.start();

        }else {
            if(!threadAutoWeight.isAlive()){
                threadAutoWeight.start();
            }
        }
    }

    public void stopThread(){
        running = false;
        boolean retry = true;
        while(retry){
            try {
                threadAutoWeight.join(3000);
            } catch (InterruptedException | NullPointerException e) {}
            retry = false;
        }
    }

    /** Сжатие и поворот изибражения.
     * @param input Входящии данные.
     * @param width Ширина картинки.
     * @param height Высота картинки.
     * @return Сжатые данные.
     * @throws Exception Исключение при ошибки преобразования данных.
     */
    byte[] compressImage(byte[] input, int width, int height) throws Exception{
        //Preferences preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
        Bitmap original;
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(input, 0, input.length, options);
            //Camera.Parameters parameters = camera.getParameters();
            //Camera.Size size = parameters.getPictureSize();
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, width, height);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            /** Создаем битовую карту из входящих данных */
            original = BitmapFactory.decodeByteArray(input, 0, input.length, options);
            /** Исключение если память выходит за пределы. */
        } catch (OutOfMemoryError e) {
            /** Создаем опции битовой карты */
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inJustDecodeBounds = true;
            /** Временное хранилище */
            bitmapOptions.inTempStorage = new byte[32 * 1024];
            //Camera.Parameters parameters = camera.getParameters();
            //Camera.Size size = parameters.getPictureSize();
            /** Получить высоту */
            //int height11 = size.height;
            /** Получить ширину */
            //int width11 = size.width;
            /** Размер картинки в мб */
            float mb = (float)(width * height) / 1024000;
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
        matrix.postRotate(Integer.parseInt(new Preferences(getApplicationContext()).read(getString(R.string.key_rotation), "90"))); // anti-clockwise by 90 degrees
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        try {
            Bitmap bitmapRotate = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            bitmapRotate.compress(Bitmap.CompressFormat.JPEG, Integer.parseInt(new Preferences(getApplicationContext()).read(getString(R.string.key_quality_pic), "50")), blob);
            original.recycle();
            original = null;
            bitmapRotate.recycle();
            bitmapRotate = null;
        } catch (OutOfMemoryError e) {
            original.recycle();
            original = null;
        }
        byte[] b = blob.toByteArray();
        Bitmap src = createByteToBitmap(b, width, height); // the original file is cuty.jpg i added in resources
        Bitmap dest = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        //original = createByteToBitmap(b, width, height);
        //original = Bitmap.createScaledBitmap(original, original.getWidth(), original.getHeight(), true);
        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        paint.setColor(Color.MAGENTA);
        paint.setTextSize(getResources().getDimension(R.dimen.text_micro));
        canvas.drawBitmap(src, 0f, 0f, null);
        int x = 2/*(canvas.getWidth() / 2) - 2*/;     //-2 is for regulating the x position offset
        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
        int y = (int) (paint.getTextSize())/*(int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2))*/;
        StringBuilder stringBuilder = new StringBuilder(getString(R.string.app_name)+"#" + entryID);
        stringBuilder.append('\n');
        /** Создаем штамп времени */
        String timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        /** Создаем имя папки по дате */
        String dateStamp = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(new Date());
        stringBuilder.append(dateStamp).append(' ').append(timeStamp).append('\n');
        drawMultiLineText(stringBuilder.toString(), x, y, paint, canvas);
        //canvas.drawText(stringBuilder.toString(), x, y, paint);
        blob = new ByteArrayOutputStream();
        dest.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        dest.recycle();
        dest = null;
        return blob.toByteArray();
    }

    /** Сжатие и поворот изибражения.
     * @param input Входящии данные.
     * @param width Ширина картинки.
     * @param height Высота картинки.
     * @return Сжатые данные.
     * @throws Exception Исключение при ошибки преобразования данных.
     */
//    byte[] compressImage(byte[] input, int width, int height) throws Exception{
//        //Preferences preferences = new Preferences(getSharedPreferences(Preferences.PREF_SETTINGS,Context.MODE_PRIVATE));
//        Bitmap original = createByteToBitmap(input, width, height);
//        //BitmapFactory.Options options;
//        //Camera.Size size = camera.getParameters().getPictureSize();
//        /*try {
//            // First decode with inJustDecodeBounds=true to check dimensions
//            options = new BitmapFactory.Options();
//            options.inPurgeable = true;
//            options.inJustDecodeBounds = true;
//            BitmapFactory.decodeByteArray(input, 0, input.length, options);
//            // Calculate inSampleSize
//            options.inSampleSize = calculateInSampleSize(options, width, height);
//            // Decode bitmap with inSampleSize set
//            options.inJustDecodeBounds = false;
//            *//**//** Создаем битовую карту из входящих данных. *//**//*
//            original = BitmapFactory.decodeByteArray(input, 0, input.length, options);
//            *//**//** Исключение если память выходит за пределы.*//**//*
//        } catch (OutOfMemoryError e) {
//            *//**//** Создаем опции битовой карты.*//**//*
//            options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//            *//**//** Временное хранилище.*//**//*
//            options.inTempStorage = new byte[32 * 1024];
//            *//**//** Размер картинки в мб.*//**//*
//            float mb = (float)(width * height) / 1024000;
//            if (mb > 4.0f)
//                options.inSampleSize = 4;
//            else if (mb > 3.0f)
//                options.inSampleSize = 2;
//            options.inJustDecodeBounds = false;
//            *//**//** Создаем битовую карту из опций. *//**//*
//            original = BitmapFactory.decodeByteArray(input, 0, input.length, options);
//        }*/
//        /** Создаем матрикс обьект. */
//        Matrix matrix = new Matrix();
//        /** Поворот изображения в градусах против часовой стрелки. */
//        matrix.postRotate(Integer.parseInt(new Preferences(getApplicationContext()).read(getString(R.string.key_rotation), "90"))); // anti-clockwise by 90 degrees
//        ByteArrayOutputStream blob = new ByteArrayOutputStream();
//        try {
//            Bitmap bitmapRotate = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
//            bitmapRotate.compress(Bitmap.CompressFormat.JPEG, Integer.parseInt(new Preferences(getApplicationContext()).read(getString(R.string.key_quality_pic), "50")), blob);
//            original.recycle();
//            original = null;
//            bitmapRotate.recycle();
//            bitmapRotate = null;
//        } catch (OutOfMemoryError e) {
//            original.recycle();
//            original = null;
//        }
//        //BitmapFactory.Options options = new BitmapFactory.Options();
//        /*original = createByteToBitmap(blob.toByteArray(), width, height); //BitmapFactory.decodeByteArray(blob.toByteArray() , 0, blob.toByteArray().length);
//        original = Bitmap.createScaledBitmap(original, width, height, true);
//        Canvas canvas = new Canvas(original);
//        Paint paint = new Paint();
//        paint.setColor(Color.MAGENTA);
//        paint.setTextSize(getResources().getDimension(R.dimen.text_micro));
//        int x = 2*//**//*(canvas.getWidth() / 2) - 2*//**//*;     //-2 is for regulating the x position offset
//        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
//        int y = (int) (paint.getTextSize())*//**//*(int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2))*//**//* ;
//        StringBuilder stringBuilder = new StringBuilder("Hello");
//        stringBuilder.append('\n');
//        stringBuilder.append("Some Text here");
//        drawMultiLineText(stringBuilder.toString(), x, y, paint, canvas);
//        //canvas.drawText(stringBuilder.toString(), x, y, paint);
//        blob = new ByteArrayOutputStream();
//        original.compress(Bitmap.CompressFormat.JPEG, 100, blob);
//        original.recycle();
//        original = null;*/
//        return blob.toByteArray();
//    }

    private Bitmap createByteToBitmap(byte[] input, int width, int height) throws Exception{
        //Bitmap original;
        BitmapFactory.Options options;
        //Camera.Size size = camera.getParameters().getPictureSize();
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(input, 0, input.length, options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, width, height);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            /** Создаем битовую карту из входящих данных. */
            return BitmapFactory.decodeByteArray(input, 0, input.length, options);
            /** Исключение если память выходит за пределы.*/
        } catch (OutOfMemoryError e) {
            /** Создаем опции битовой карты.*/
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            /** Временное хранилище.*/
            options.inTempStorage = new byte[32 * 1024];
            /** Размер картинки в мб.*/
            float mb = (float)(width * height) / 1024000;
            if (mb > 4.0f)
                options.inSampleSize = 4;
            else if (mb > 3.0f)
                options.inSampleSize = 2;
            options.inJustDecodeBounds = false;
            /** Создаем битовую карту из опций. */
            return BitmapFactory.decodeByteArray(input, 0, input.length, options);
        }
    }

    public String getRotation(Context context){
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return "0";
            case Surface.ROTATION_90:
                return "90";
            case Surface.ROTATION_180:
                return "180";
            case Surface.ROTATION_270:
                return "270";
            default:
                return "0";
        }
    }

    void drawMultiLineText(String str, float x, float y, Paint paint, Canvas canvas) {
        String[] lines = str.split("\n");
        float txtSize = -paint.ascent() + paint.descent();

        if (paint.getStyle() == Paint.Style.FILL_AND_STROKE || paint.getStyle() == Paint.Style.STROKE){
            txtSize += paint.getStrokeWidth(); //add stroke width to the text size
        }
        float lineSpace = txtSize * 0.2f;  //default line spacing

        for (int i = 0; i < lines.length; ++i) {
            canvas.drawText(lines[i], x, y + (txtSize + lineSpace) * i, paint);
        }
    }

    public int calculateInSampleSize( BitmapFactory.Options options, int reqWidth, int reqHeight) {
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

    class TakingTimeout extends CountDownTimer{

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public TakingTimeout(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            taking = false;
        }
    }

    class SavePhotoTask extends Thread {
        byte[] data;
        Camera camera;
        SavePhotoTask(byte[]data, Camera camera){
            this.data = data;
            this.camera = camera;
        }

        @Override
        public void run() {
            try {
                /** Сжимаем данные изображения. */
                byte[] compressImage = compressImage(data, camera.getParameters().getPictureSize().width, camera.getParameters().getPictureSize().height);
                /** Создаем штамп времени */
                String timeStamp = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
                /** Создаем имя папки по дате */
                String folderStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
                /** Сохраняем фаил. */
                String path = saveExternalMemory(globals.path.getAbsolutePath() + File.separator + folderStamp, folderStamp + timeStamp + ".jpg", compressImage);
                //String path = saveInternalMemory(Main.FOLDER_LOCAL /*+ File.separator + folderStamp*/, folderStamp + "_" + timeStamp + "(" + String.valueOf(checkId) + ").jpg", compressImage);
                /*Intent intent = new Intent();
                intent.putExtra("com.victjava.scales.PHOTO_PATH", path);
                pendingIntent.send(ServiceTake.this, 0, intent);*/
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }










    }

    /**
     * Обработка обнуления весов.
     */
    private class ZeroThread extends Thread {
        private final ProgressDialog dialog;

        ZeroThread(Context context) {
            // Создаём новый поток
            super(getString(R.string.Zeroing));
            dialog = new ProgressDialog(context);
            dialog.setCancelable(false);
            dialog.setIndeterminate(false);
            dialog.show();
            dialog.setContentView(R.layout.custom_progress_dialog);
            TextView tv1 = (TextView) dialog.findViewById(R.id.textView1);
            tv1.setText(R.string.Zeroing);
            //start(); // Запускаем поток
        }

        @Override
        public void run() {
            scaleModule.setOffsetScale();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    public class ReportHelper implements Thread.UncaughtExceptionHandler {
        private AlertDialog dialog;
        private Context context;

        public ReportHelper(Context context) {
            this.context = context;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            String text = ex.getCause().getMessage();
            if(text == null){
                text = "";
            }
            showToastInThread(text);
        }

        public void showToastInThread(final String str){
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(str)
                            .setTitle("Ошибка приложения")
                            .setCancelable(false)
                            .setNegativeButton("Выход", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    exit();
                                }
                            });
                    dialog = builder.create();

                    //Toast.makeText(context, str, Toast.LENGTH_LONG).show();
                    if(!dialog.isShowing())
                        dialog.show();
                    Looper.loop();
                }
            }.start();
        }
    }
}

