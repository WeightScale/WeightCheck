package com.victjava.scales;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import com.victjava.scales.provider.CheckTable;

import java.util.Map;

public class ActivityPageChecks extends Activity {

    CheckTable checkTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkTable = new CheckTable(this);
        int pos = getIntent().getIntExtra("position", 0);
        //long checkId = getIntent().getIntExtra("id", 1);
        setTitle(getString(R.string.app_name) + ' ' + getString(R.string.Check)); //установить заголовок*/

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        String[] columns = {CheckTable.KEY_ID,
                CheckTable.KEY_DATE_CREATE,
                CheckTable.KEY_TIME_CREATE,
                CheckTable.KEY_VENDOR,
                CheckTable.KEY_WEIGHT_FIRST,
                CheckTable.KEY_WEIGHT_SECOND,
                CheckTable.KEY_WEIGHT_NETTO,
                CheckTable.KEY_TYPE,
                CheckTable.KEY_PRICE,
                CheckTable.KEY_PRICE_SUM,
                CheckTable.KEY_NUMBER_BT,
                CheckTable.KEY_DIRECT,
                CheckTable.KEY_DIRECT,
                CheckTable.KEY_DIRECT};

        int[] to = {
                R.id.check_id,
                R.id.date,
                R.id.time,
                R.id.vendor,
                R.id.gross_row,
                R.id.tare_row,
                R.id.netto_row,
                R.id.type_row,
                R.id.price_row,
                R.id.sum_row,
                R.id.textNumScale,
                R.id.imageDirect, R.id.gross, R.id.tare};
        Cursor cursor = checkTable.getAllReadyCheck(CheckTable.VISIBLE);
        if (cursor == null) {
            return;
        }
        MyAdapter myAdapter = new MyAdapter(/*getApplicationContext(),*/  cursor, columns, to);
        ViewPager pager = new ViewPager(this);
        myAdapter.setViewBinder(new PageCheckViewBinder());
        pager.setAdapter(myAdapter);
        pager.setCurrentItem(pos);
        setContentView(pager);
    }

    private class MyAdapter extends PagerAdapter implements View.OnClickListener {
        //private final Context context;
        private View mCurrentView;
        private final Cursor mCursor;
        private final int count;
        private final int layout;
        private final String[] mColumns;
        private final int[] mTo;
        private int[] mFrom;
        private SimpleCursorAdapter.ViewBinder mViewBinder;

        public MyAdapter(/*Context context,*/  Cursor cursor, String[] columns, int... to) {
            //this.context = context;
            layout = R.layout.page_checks;
            mColumns = columns;
            mTo = to;
            mCursor = cursor;
            count = cursor.getCount();
            findColumns(mColumns);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            position %= mCursor.getCount();
            mCursor.moveToPosition(position);
            View view = inflater.inflate(layout, null);
            //LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.layoutImageView);
            //linearLayout.setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.textNumTerminal)).setText(BluetoothAdapter.getDefaultAdapter().getAddress());
            view.findViewById(R.id.imageViewBack).setOnClickListener(this);
            view.findViewById(R.id.imageViewMail).setOnClickListener(this);
            view.findViewById(R.id.imageViewMessage).setOnClickListener(this);
            bindView(view, mCursor);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            mCurrentView = (View) object;
        }

        private void findColumns(String... from) {
            int count = from.length;
            if (mFrom == null || mFrom.length != count) {
                mFrom = new int[count];
            }
            for (int i = 0; i < count; i++) {
                mFrom[i] = mCursor.getColumnIndexOrThrow(from[i]);
            }
        }

        public void setViewText(TextView v, CharSequence text) {
            v.setText(text);
        }

        public void setViewImage(ImageView v, String value) {
            try {
                v.setImageResource(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                v.setImageURI(Uri.parse(value));
            }
        }

        public void setViewBinder(SimpleCursorAdapter.ViewBinder viewBinder) {
            mViewBinder = viewBinder;
        }

        public void bindView(View view, Cursor cursor) {
            final SimpleCursorAdapter.ViewBinder binder = mViewBinder;

            final int count = mTo.length;
            final int[] from = mFrom;
            final int[] to = mTo;

            for (int i = 0; i < count; i++) {
                final View v = view.findViewById(to[i]);
                if (v != null) {
                    boolean bound = false;
                    if (binder != null) {
                        bound = binder.setViewValue(v, cursor, from[i]);
                    }

                    if (!bound) {
                        String text = cursor.getString(from[i]);
                        if (text == null) {
                            text = "";
                        }

                        if (v instanceof TextView) {
                            setViewText((TextView) v, text);
                        } else if (v instanceof ImageView) {
                            setViewImage((ImageView) v, text);
                        } else {
                            throw new IllegalStateException(v.getClass().getName() + " is not a " +
                                    " view that can be bounds by this SimpleCursorAdapter");
                        }
                    }
                }
            }
        }

        @Override
        public void onClick(View v) {
            if (mCurrentView == null) {
                return;
            }
            Cursor cursor = mCursor;
            ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
            Map<String, ContentValues> map = mQueryMap.getRows();
            TextView textView = (TextView) mCurrentView.findViewById(R.id.check_id);
            String checkId = textView.getText().toString();
            ContentValues values = map.get(checkId);
            String contactId = values.getAsString(CheckTable.KEY_VENDOR_ID);
            switch (v.getId()) {
                case R.id.imageViewBack:
                    onBackPressed();
                    break;
                case R.id.imageViewMail:
                    if (contactId != null) {
                        new TaskMessageDialog(ActivityPageChecks.this, Integer.valueOf(contactId), Integer.valueOf(checkId)).openListEmailDialog();
                    }
                    break;
                case R.id.imageViewMessage:
                    if (contactId != null) {
                        new TaskMessageDialog(ActivityPageChecks.this, Integer.valueOf(contactId), Integer.valueOf(checkId)).openListPhoneDialog();
                    }
                    break;
                default:
            }
        }
    }

    private class PageCheckViewBinder implements SimpleCursorAdapter.ViewBinder {
        private int direct;

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

            switch (view.getId()) {
                case R.id.gross:
                    direct = cursor.getInt(cursor.getColumnIndex(CheckTable.KEY_DIRECT));
                    if (direct == CheckTable.DIRECT_UP) {
                        setViewText((TextView) view, getString(R.string.Tape));
                    } else {
                        setViewText((TextView) view, getString(R.string.Gross));
                    }
                    break;
                case R.id.tare:
                    direct = cursor.getInt(cursor.getColumnIndex(CheckTable.KEY_DIRECT));
                    if (direct == CheckTable.DIRECT_DOWN) {
                        setViewText((TextView) view, getString(R.string.Tape));
                    } else {
                        setViewText((TextView) view, getString(R.string.Gross));
                    }
                    break;
                default:
                    return false;
            }
            return true;

            /*if(view.getId() == R.id.gross){
                direct = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_DIRECT));
                if(direct == CheckDBAdapter.DIRECT_UP){
                    //((TextView)view).setText(R.string.Tape);
                    setViewText((TextView) view, getString(R.string.Tape));
                    return true;
                }else {
                    //((TextView)view).setText(R.string.Gross);
                    setViewText((TextView) view, getString(R.string.Gross));
                    return true;
                }
            }else  if (view.getId() == R.id.tare){
                direct = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_DIRECT));
                if(direct == CheckDBAdapter.DIRECT_DOWN){
                    //((TextView)view).setText(R.string.Tape);
                    setViewText((TextView) view, getString(R.string.Tape));
                    return true;
                }else {
                    //((TextView)view).setText(R.string.Gross);
                    setViewText((TextView) view, getString(R.string.Gross));
                    return true;
                }
            }
            return false;*/
        }

        public void setViewText(TextView v, CharSequence text) {
            v.setText(text);
        }
    }
}
