package com.victjava.scales.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import com.victjava.scales.Main;
import com.victjava.scales.NumberPicker;
import com.victjava.scales.R;

import java.util.Arrays;

/**
 * Created by Kostya on 30.09.2015.
 */
public class DialogSpeedPort extends DialogPreference {
    Main main;
    private int mNumber;
    private final String[] speedPortArray;
    private NumberPicker numberPicker;
    final int minValue;
    final int maxValue;

    public DialogSpeedPort(Context context, AttributeSet attrs) {
        super(context, attrs);
        speedPortArray = context.getResources().getStringArray(R.array.array_speed_port);
        minValue = 1;
        maxValue = speedPortArray.length > 0 ? speedPortArray.length : 1;
        main = (Main)context.getApplicationContext();
        int speed = main.getScaleModule().isAttach()?((Main)context.getApplicationContext()).getScaleModule().getSpeedPort():0;
        if(speed >= minValue && speed <= maxValue)
            mNumber = speed;
        setPersistent(true);
        setDialogLayoutResource(R.layout.number_picker);
    }

    @Override
    protected void onBindDialogView(View view) {
        numberPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setMinValue(minValue);
        numberPicker.setDisplayedValues(speedPortArray);
        numberPicker.setValue(mNumber);
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            // needed when user edits the text field and clicks OK
            numberPicker.clearFocus();
            setValue(numberPicker.getValue());
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mNumber) : (Integer) defaultValue);
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
        }

        if (value != mNumber) {
            mNumber = value;
            notifyChanged();
            callChangeListener(value);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }



}
