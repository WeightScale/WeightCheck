package com.victjava.scales;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import com.victjava.scales.provider.CheckTable;
import com.victjava.scales.provider.TypeTable;

//import static com.victjava.scales.ActivityCheck.WeightType.*;

//import static com.victjava.scales.ActivityCheck.*;

/*
 * Created by Kostya on 09.03.2015.
 */
public class InputFragment extends Fragment implements ActivityCheck.OnCheckEventListener {

    TypeTable typeTable;
    Context mContext;
    ActivityCheck activityCheck;
    private EditText editTextPrice;
    private TextView viewFirst;
    private TextView viewSecond/*, viewSum*/;
    private Spinner spinnerType;
    private LinearLayout layoutSecond, layoutFirst;
    private boolean isCreateView;

    @Override
    public void someEvent() {
        update();

        /*switch (event){
            case FIRST:
                layoutFirst.setVisibility(View.VISIBLE);
            break;
            case SECOND:
                layoutFirst.setVisibility(View.VISIBLE);
                layoutSecond.setVisibility(View.VISIBLE);
            break;
            default:
        }*/
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activityCheck = (ActivityCheck)activity;
        mContext = activityCheck.getApplicationContext();
    }

   /* @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        typeTable = new TypeTable(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_input_check, container, false);
        layoutFirst = (LinearLayout) v.findViewById(R.id.layoutFirst);
        layoutSecond = (LinearLayout) v.findViewById(R.id.layoutSecond);
        viewFirst = (TextView) v.findViewById(R.id.viewFirst);
        viewSecond = (TextView) v.findViewById(R.id.viewSecond);
        editTextPrice = (EditText) v.findViewById(R.id.editTextPrice);
        spinnerType = (Spinner) v.findViewById(R.id.spinnerType);

        editTextPrice.setText(activityCheck.values.getAsString(CheckTable.KEY_PRICE));
        editTextPrice.clearFocus();
        editTextPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().isEmpty()) {
                    activityCheck.values.put(CheckTable.KEY_PRICE, Integer.valueOf(editable.toString()));
                    activityCheck.values.put(CheckTable.KEY_PRICE_SUM, activityCheck.sumTotal(activityCheck.sumNetto()));
                    typeTable.updateEntry(activityCheck.values.getAsInteger(CheckTable.KEY_TYPE_ID), TypeTable.KEY_PRICE, activityCheck.values.getAsInteger(CheckTable.KEY_PRICE));
                }
            }
        });


        loadTypeSpinnerData();
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //type_item_id = i;
                activityCheck.values.put(CheckTable.KEY_TYPE_ID, (int)l);
                activityCheck.values.put(CheckTable.KEY_TYPE, ((TextView) view.findViewById(R.id.text1)).getText().toString());
                activityCheck.values.put(CheckTable.KEY_PRICE, typeTable.getPriceColumn((int) l));
                editTextPrice.setText(activityCheck.values.getAsString(CheckTable.KEY_PRICE));
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

    public void loadTypeSpinnerData() {
        Cursor cursor = typeTable.getAllEntries();
        if (cursor == null) {
            return;
        }
        if (cursor.getCount() > 0) {
            String[] columns = {TypeTable.KEY_TYPE};
            int[] to = {R.id.text1};
            SimpleCursorAdapter typeAdapter = new SimpleCursorAdapter(mContext, R.layout.type_spinner, cursor, columns, to);
            typeAdapter.setDropDownViewResource(R.layout.type_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);
        }
    }

    public void update() {

        int first = activityCheck.values.getAsInteger(CheckTable.KEY_WEIGHT_FIRST);
        int second = activityCheck.values.getAsInteger(CheckTable.KEY_WEIGHT_SECOND);
        viewFirst.setText(String.valueOf(first));
        viewSecond.setText(String.valueOf(second));

        switch (((ActivityCheck) getActivity()).weightType) {
            case FIRST:
                layoutFirst.setVisibility(View.VISIBLE);
                if (first == 0) {
                    viewFirst.setText(getString(R.string.weighed));
                }
                break;
            case SECOND:
                layoutFirst.setVisibility(View.VISIBLE);
                layoutSecond.setVisibility(View.VISIBLE);
                if (second == 0) {
                    viewSecond.setText(getString(R.string.weighed));
                }
                break;
            default:
        }


        for (int i = 0; i < spinnerType.getCount(); i++) {
            long object = spinnerType.getItemIdAtPosition(i);
            if ((int) object == activityCheck.values.getAsInteger(CheckTable.KEY_TYPE_ID)) {
                spinnerType.setSelection(i);
                break;
            }
        }
    }
}
