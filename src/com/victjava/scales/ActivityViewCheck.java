package com.victjava.scales;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import com.victjava.scales.provider.CheckTable;

import java.util.*;

/*
 * Created by Kostya on 27.12.2014.
 */
public class ActivityViewCheck extends Activity implements View.OnClickListener {

    private int entryID;
    //private LinearLayout layoutImageView;
    private final String[] mColumns = {
            CheckTable.KEY_ID,
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
            CheckTable.KEY_DIRECT, CheckTable.KEY_DIRECT, CheckTable.KEY_DIRECT};
    private final int[] mTo = {
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
    private int[] mFrom;

    private int contactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        entryID = getIntent().getIntExtra("id", 1);
        setTitle(getString(R.string.Check_N) + ' ' + entryID); //установить заголовок

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        Cursor cursor = new CheckTable(getApplicationContext()).getEntryItem(entryID);
        if (cursor == null) {
            return;
        }
        if (cursor.getCount() == 0 || !cursor.moveToFirst()) {
            return;
        }
        findColumns(mColumns, cursor);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.page_checks, null);
        ((TextView) view.findViewById(R.id.textNumTerminal)).setText(BluetoothAdapter.getDefaultAdapter().getAddress());
        view.findViewById(R.id.imageViewBack).setOnClickListener(this);
        view.findViewById(R.id.imageViewMail).setOnClickListener(this);
        view.findViewById(R.id.imageViewMessage).setOnClickListener(this);
        //layoutImageView = (LinearLayout)view.findViewById(R.id.layoutImageView);
        //layoutImageView.setVisibility(View.VISIBLE);

        bindView(view, cursor);
        setContentView(view);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
        Map<String, ContentValues> map = mQueryMap.getRows();
        ContentValues values = map.get(String.valueOf(entryID));

        contactId = values.getAsInteger(CheckTable.KEY_VENDOR_ID);
    }

    /*@Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }*/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageViewBack:
                onBackPressed();
                break;
            case R.id.imageViewMail:
                new TaskMessageDialog(this, contactId, entryID).openListEmailDialog();
                break;
            case R.id.imageViewMessage:
                new TaskMessageDialog(this, contactId, entryID).openListPhoneDialog();
                break;
            default:
        }
    }

    public void bindView(View view, Cursor cursor) {

        final int count = mTo.length;
        final int[] from = mFrom;
        final int[] to = mTo;
        int direct = cursor.getInt(cursor.getColumnIndex(CheckTable.KEY_DIRECT));

        for (int i = 0; i < count; i++) {
            final View v = view.findViewById(to[i]);
            if (v != null) {

                switch (v.getId()) {
                    case R.id.gross:
                        if (direct == CheckTable.DIRECT_UP) {
                            setViewText((TextView) v, getString(R.string.Tape));
                        } else {
                            setViewText((TextView) v, getString(R.string.Gross));
                        }
                        break;
                    case R.id.tare:
                        if (direct == CheckTable.DIRECT_DOWN) {
                            setViewText((TextView) v, getString(R.string.Tape));
                        } else {
                            setViewText((TextView) v, getString(R.string.Gross));
                        }
                        break;
                    default:
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
                                    " view that can be bounds by this CustomAdapter");
                        }
                }
            }
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

    private void findColumns(String[] from, Cursor cursor) {
        if (cursor != null) {
            int count = from.length;
            if (mFrom == null || mFrom.length != count) {
                mFrom = new int[count];
            }
            for (int i = 0; i < count; i++) {
                mFrom[i] = cursor.getColumnIndexOrThrow(from[i]);
            }
        } else {
            mFrom = null;
        }
    }

}
