package com.victjava.scales;


import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import com.victjava.scales.provider.CheckTable;
import com.victjava.scales.TaskCommand.ObjectParcel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 20.10.13
 * Time: 8:37
 * To change this template use File | Settings | File Templates.
 */
public class ActivityListChecks extends ListActivity implements View.OnClickListener {
    CheckTable checkTable;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_checks);

        setTitle(getString(R.string.app_name) + ' ' + getString(R.string.get_checks)); //установить заголовок*/

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        //ArrayList<Integer> checks = getIntent().getIntegerArrayListExtra("listChecks");
        checkTable = new CheckTable(this);
        listView = getListView();


        findViewById(R.id.buttonBack).setOnClickListener(this);

        if ("notifyChecks".equals(getIntent().getAction())) {
            Bundle b = getIntent().getExtras();
            ArrayList<ObjectParcel> items = b.getParcelableArrayList("listCheckNotify");
            if (items != null) {
                listNotifySetup(items);
                listView.setOnItemClickListener(onItemClickListenerNotify);
            }

        } else {
            listSetup();
            listView.setOnItemClickListener(onItemClickListenerScale);
        }

    }

    private final AdapterView.OnItemClickListener onItemClickListenerScale = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startActivity(new Intent(getApplicationContext(), ActivityPageChecks.class).putExtra("position", position));
        }
    };

    private final AdapterView.OnItemClickListener onItemClickListenerNotify = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startActivity(new Intent().setClass(getApplicationContext(), ActivityViewCheck.class).putExtra("id", (int) id));
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonBack:
                onBackPressed();
                break;
            default:
        }
    }

    private void listSetup() {
        /*
            Устанавливаем флаг не показывать старые чеки
        */
        checkTable.invisibleCheckIsReady(Preferences.read(getString(R.string.KEY_DAY_CHECK_DELETE), Main.default_day_delete_check));
        /*
            Удаляем чеки отправленые на сервер через n дней
        */
        checkTable.deleteCheckIsServer();

        Cursor cursor = checkTable.getAllEntries(CheckTable.VISIBLE);
        if (cursor == null) {
            return;
        }
        String[] columns = {
                CheckTable.KEY_ID,
                CheckTable.KEY_DATE_CREATE,
                CheckTable.KEY_TIME_CREATE,
                CheckTable.KEY_VENDOR,
                CheckTable.KEY_WEIGHT_FIRST,
                CheckTable.KEY_WEIGHT_SECOND,
                CheckTable.KEY_WEIGHT_NETTO,
                CheckTable.KEY_PRICE_SUM, CheckTable.KEY_DIRECT, CheckTable.KEY_DIRECT, CheckTable.KEY_DIRECT};

        int[] to = {
                R.id.check_id,
                R.id.date,
                R.id.time,
                R.id.vendor,
                R.id.gross_row,
                R.id.tare_row,
                R.id.netto_row,
                R.id.sum_row, R.id.imageDirect, R.id.gross, R.id.tare};
        SimpleCursorAdapter namesAdapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.item_check, cursor, columns, to);
        namesAdapter.setViewBinder(new ListCheckViewBinder());
        setListAdapter(namesAdapter);
        //MyCursorAdapter namesAdapter = new MyCursorAdapter(getApplicationContext(), R.layout.item_check, cursor, columns, to);
        //setListAdapter(namesAdapter);
        setTitle(getString(R.string.Checks_closed) + getString(R.string.qty) + listView.getCount()); //установить заголовок

    }

    private void listNotifySetup(ArrayList<ObjectParcel> items) {
        ListAdapter itemsAdapter = new ListNotifyAdapter(this, R.layout.list_item_bluetooth, items);
        setListAdapter(itemsAdapter);
        setTitle(getString(R.string.Checks_closed) + getString(R.string.qty) + listView.getCount()); //установить заголовок
    }

    private class ListCheckViewBinder implements SimpleCursorAdapter.ViewBinder {
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
        }

        public void setViewText(TextView v, CharSequence text) {
            v.setText(text);
        }
    }

    public class ListNotifyAdapter extends ArrayAdapter<ObjectParcel> {
        final int mLayout;

        public ListNotifyAdapter(Context ctx, int layout, List<ObjectParcel> items) {
            super(ctx, layout, items);
            mLayout = layout;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int item = getItem(position).getIntValue();
            String str = getItem(position).getStrValue();


            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(mLayout, parent, false);
            }
            // Lookup view for data population
            TextView textTop = (TextView) convertView.findViewById(R.id.topText);
            TextView textBottom = (TextView) convertView.findViewById(R.id.bottomText);
            // Populate the data into the template view using the data object

            textTop.setText(getString(R.string.Check_N) + item);
            textBottom.setText(str);

            // Return the completed view to render on screen
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getIntValue();
        }
    }
}
