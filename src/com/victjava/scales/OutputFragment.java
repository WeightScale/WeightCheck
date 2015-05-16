package com.victjava.scales;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.victjava.scales.provider.CheckDBAdapter;
import com.victjava.scales.provider.TypeDBAdapter;

/*
 * Created by Kostya on 09.03.2015.
 */
public class OutputFragment extends Fragment implements ActivityCheck.OnCheckEventListener {
    Context mContext;
    ActivityCheck activityCheck;
    TypeDBAdapter typeTable;
    private Spinner spinnerType;
    private TextView viewFirst, viewSecond;
    private LinearLayout layoutSecond, layoutFirst;
    private boolean isCreateView;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activityCheck = (ActivityCheck)activity;
        mContext = activityCheck.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        typeTable = new TypeDBAdapter(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_output_check, container, false);
        layoutFirst = (LinearLayout) v.findViewById(R.id.layoutFirst);
        layoutSecond = (LinearLayout) v.findViewById(R.id.layoutSecond);
        viewFirst = (TextView) v.findViewById(R.id.viewFirst);
        viewSecond = (TextView) v.findViewById(R.id.viewSecond);
        spinnerType = (Spinner) v.findViewById(R.id.spinnerType);

        loadTypeSpinnerData();
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //type_item_id = i;
                activityCheck.values.put(CheckDBAdapter.KEY_TYPE_ID, (int) l);
                activityCheck.values.put(CheckDBAdapter.KEY_TYPE, ((TextView) view.findViewById(R.id.text1)).getText().toString());
                activityCheck.values.put(CheckDBAdapter.KEY_PRICE, typeTable.getPriceColumn((int) l));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

        });

        someEvent();
        isCreateView = true;
        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (isCreateView) {
                update();
            }
        }
    }

    private void loadTypeSpinnerData() {
        Cursor cursor = typeTable.getAllEntries();
        if (cursor == null) {
            return;
        }
        if (cursor.getCount() > 0) {
            String[] columns = {TypeDBAdapter.KEY_TYPE};
            int[] to = {R.id.text1};
            SimpleCursorAdapter typeAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(), R.layout.type_spinner, cursor, columns, to);
            typeAdapter.setDropDownViewResource(R.layout.type_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);
        }
    }

    public void update() {
        int first = activityCheck.values.getAsInteger(CheckDBAdapter.KEY_WEIGHT_FIRST);
        int second = activityCheck.values.getAsInteger(CheckDBAdapter.KEY_WEIGHT_SECOND);
        viewFirst.setText(String.valueOf(first));
        viewSecond.setText(String.valueOf(second));
        switch (activityCheck.weightType) {
            case FIRST:
                if (first == 0) {
                    viewFirst.setText(getString(R.string.weighed));
                }
                layoutFirst.setVisibility(View.VISIBLE);
                break;
            case SECOND:
                if (second == 0) {
                    viewSecond.setText(getString(R.string.weighed));
                }
                layoutFirst.setVisibility(View.VISIBLE);
                layoutSecond.setVisibility(View.VISIBLE);
                break;
            default:
        }

        for (int i = 0; i < spinnerType.getCount(); i++) {
            long object = spinnerType.getItemIdAtPosition(i);
            if ((int) object == activityCheck.values.getAsInteger(CheckDBAdapter.KEY_TYPE_ID)) {
                spinnerType.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void someEvent() {
        update();
    }
}