package com.victjava.scales;


import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.provider.BaseColumns;
import android.provider.ContactsContract.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import com.konst.module.ScaleModule;
import com.victjava.scales.provider.CheckTable;
import com.victjava.scales.provider.TaskTable;


import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ActivityCheck extends FragmentActivity implements View.OnClickListener, Runnable {
    Thread threadAutoWeight;
    ScaleModule scaleModule;
    Main main;
    boolean running;

    public enum Action {
        /** Остановка взвешивания. */
        STOP_WEIGHTING,
        /** Пуск взвешивания */
        START_WEIGHTING,
        /** Сохранить результат взвешивания. */
        STORE_WEIGHTING,
        /** Обновить данные веса. */
        UPDATE_PROGRESS,
        DIALOG_SAVE
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
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
            handler.obtainMessage(Action.START_WEIGHTING.ordinal()).sendToTarget();
            isStable = false;
            while (running && !(isStable || weightViewIsSwipe)) {                                                       //ждем стабилизации веса или нажатием выбора веса
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
                if (!touchWeightView) {                                                                                 //если не прикасаемся к индикатору тогда стабилизируем вес
                    isStable = processStable(moduleWeight);
                    handler.obtainMessage(Action.UPDATE_PROGRESS.ordinal(), numStable, 0).sendToTarget();
                }
            }
            numStable = COUNT_STABLE;
            if (!running) {
                break;
            }
            if (isStable) {
                handler.obtainMessage(Action.STORE_WEIGHTING.ordinal(), moduleWeight, 0).sendToTarget();                //сохраняем стабильный вес
            }

            weightViewIsSwipe = false;

            while (running && moduleWeight >= Main.default_min_auto_capture) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }                                   // ждем разгрузки весов
            }
            vibrator.vibrate(100);
            handler.obtainMessage(Action.UPDATE_PROGRESS.ordinal(), 0, 0).sendToTarget();
            if (!running) {
                if (isStable && weightType == WeightType.SECOND) {                                                      //Если тара зафоксирована и выход через кнопку назад
                    weightType = WeightType.NETTO;
                }
                break;
            }
            handler.obtainMessage(Action.DIALOG_SAVE.ordinal(), false).sendToTarget();                                  // Экран делаем не видимым
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ignored) {
            }                            //задержка
            handler.obtainMessage(Action.DIALOG_SAVE.ordinal(), true).sendToTarget();                                   // Экран делаем видимым
            if (weightType == WeightType.SECOND) {
                stopThread(); //running = true;
            }

            handler.obtainMessage(Action.STOP_WEIGHTING.ordinal()).sendToTarget();
        }
        //start = false;
    }

    protected interface OnCheckEventListener {
        void someEvent();
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

    private CheckTable checkTable;
    private Vibrator vibrator; //вибратор
    private ProgressBar progressBarWeight;
    private WeightTextView weightTextView;
    private TabHost mTabHost;
    private TabsAdapter mTabsAdapter;
    private ImageView buttonFinish;
    private SimpleGestureFilter detectorWeightView;
    private Drawable dProgressWeight, dWeightDanger;

    /**
     * Энумератор типа веса.
     */
    protected enum WeightType {
        /** Первое взвешивание. */
        FIRST,
        /** Второе взвешивание. */
        SECOND,
        /** Вес нетто. */
        NETTO
    }

    public WeightType weightType;

    /**
     * Количество стабильных показаний веса для авто сохранения
     */
    public static final int COUNT_STABLE = 64;

    ContentValues values = new ContentValues();
    public int entryID;
    public int numStable;
    protected boolean isStable;
    int moduleWeight;
    protected int tempWeight;

    private boolean flagExit = true;
    private boolean touchWeightView;
    private boolean weightViewIsSwipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check);

        main = (Main)getApplication();
        scaleModule = main.getScaleModule();
        scaleModule.setOnEventResultWeight(onEventResultWeight);

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

        progressBarWeight = (ProgressBar) findViewById(R.id.progressBarWeight);
        progressBarWeight.setMax(scaleModule.getMarginTenzo());
        progressBarWeight.setSecondaryProgress(scaleModule.getLimitTenzo());

        buttonFinish = (ImageView) findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(this);

        findViewById(R.id.imageViewPage).setOnClickListener(this);

        if (values.getAsInteger(CheckTable.KEY_WEIGHT_FIRST) > 0) {
            weightType = values.getAsInteger(CheckTable.KEY_WEIGHT_SECOND) == 0 ? WeightType.SECOND : WeightType.NETTO;
        } else {
            weightType = WeightType.FIRST;
        }

        if (values.getAsInteger(CheckTable.KEY_WEIGHT_FIRST) == 0 || values.getAsInteger(CheckTable.KEY_WEIGHT_SECOND) == 0) {
            scaleModule.startMeasuringWeight();
        }
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
        scaleModule.startMeasuringWeight();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scaleModule.stopMeasuringWeight(false);
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

    protected void exit() {
        stopThread();
        if (weightType == WeightType.NETTO) {
            values.put(CheckTable.KEY_IS_READY, 1);
            new TaskTable(this).setCheckReady(entryID);
            taskToContact();
            startActivity(new Intent(getBaseContext(), ActivityViewCheck.class).putExtra("id", entryID));
        }
        checkTable.updateEntry(entryID, values);
        finish();
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
                        new TaskTable(this).insertNewTaskPhone(TaskCommand.TaskType.TYPE_CHECK_SEND_SMS_CONTACT, entryID, contactId, address);
                    else if(CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(type))
                        new TaskTable(this).insertNewTaskEmail(TaskCommand.TaskType.TYPE_CHECK_SEND_MAIL, entryID, contactId, address);
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
        while (moduleWeight > main.getAutoCapture()) {
            if (capture) {
                return true;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(main.timeDelayDetectCapture);
                } catch (InterruptedException ignored) {}
                capture = true;
            }
        }
        return false;
    }

    public boolean processStable(int weight) {
        if (tempWeight - main.getStepMeasuring() <= weight && tempWeight + main.getStepMeasuring() >= weight) {
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
        return weight / main.getStepMeasuring() * main.getStepMeasuring();
    }

    /**
     * Устоновка индикатора веса.
     */
    private void setupWeightView() {

        weightTextView = new WeightTextView(this);
        weightTextView = (WeightTextView) findViewById(R.id.weightTextView);
        weightTextView.setMax(COUNT_STABLE);
        weightTextView.setSecondaryProgress(numStable = 0);
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
                        if (weightType == WeightType.SECOND) {
                            weightTypeUpdate();
                        }
                        break;
                    default:
                }
            }

            @Override
            public void onDoubleTap() {
                weightTextView.setSecondaryProgress(0);
                vibrator.vibrate(100);
                new ZeroThread(ActivityCheck.this).start();
            }
        };

        detectorWeightView = new SimpleGestureFilter(this, weightViewGestureListener);
        detectorWeightView.setSwipeMinVelocity(50);
        weightTextView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detectorWeightView.setSwipeMaxDistance(v.getMeasuredWidth());
                detectorWeightView.setSwipeMinDistance(detectorWeightView.getSwipeMaxDistance() / 3);
                detectorWeightView.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        touchWeightView = true;
                        vibrator.vibrate(5);
                        int progress = (int) (event.getX() / (detectorWeightView.getSwipeMaxDistance() / weightTextView.getMax()));
                        weightTextView.setSecondaryProgress(progress);
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

    private boolean saveWeight(int weight/*, WeightType type*/) {
        boolean flag = false;
        switch (weightType) {
            case FIRST:
                if (weight > 0) {
                    values.put(CheckTable.KEY_WEIGHT_FIRST, weight);
                    vibrator.vibrate(100); //вибрация
                    flag = true;
                }
                break;
            case SECOND:
                values.put(CheckTable.KEY_WEIGHT_SECOND, weight);
                int total = sumNetto();
                values.put(CheckTable.KEY_PRICE_SUM, total);
                vibrator.vibrate(100); //вибрация
                flag = true;
                break;
            case NETTO:
                scaleModule.stopMeasuringWeight(false);
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
        switch (weightType) {
            case FIRST:
                weightType = WeightType.SECOND;
                break;
            case SECOND:
                weightType = WeightType.NETTO;
                saveWeight(0);
                break;
            default:
                weightType = WeightType.FIRST;
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

    /** Обработчик показаний веса
     * Возвращяем время обновления показаний веса в милисекундах.
     */
    final ScaleModule.OnEventResultWeight onEventResultWeight = new ScaleModule.OnEventResultWeight() {
        @Override
        public int weight(final ScaleModule.ResultWeight what, final int weight, final int sensor) {
            runOnUiThread(new Runnable() {
                Rect bounds;

                @Override
                public void run() {
                    switch (what) {
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
                    }
                }
            });
            return 20; // Обновляем через милисикунды
        }
    };

    /**
     * Обработчик сообщений.
     */
    final Handler handler = new Handler() {
        private ProgressDialog dialogSave;
        /** Сообщение от обработчика авто сохранения.
         * @param msg Данные сообщения.
         */
        @Override
        public void handleMessage(Message msg) {

            switch (Action.values()[msg.what] ) {
                case STORE_WEIGHTING:
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
                    break;
                case UPDATE_PROGRESS:
                    weightTextView.setSecondaryProgress(msg.arg1);
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
        threadAutoWeight = new Thread(this);
        threadAutoWeight.setPriority(Thread.MIN_PRIORITY);
        threadAutoWeight.setDaemon(true);
        threadAutoWeight.start();
    }

    public void stopThread(){
        running = false;
        boolean retry = true;
        while(retry){
            try {
                threadAutoWeight.join(3000);
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

