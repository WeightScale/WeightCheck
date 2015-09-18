package com.victjava.scales;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author Kostya
 */
class DialogStepWeight extends DialogPreference /*implements ActivityPreferences.InterfacePreference*/ {
    private int mNumber;
    private final String[] intArray;
    private NumberPicker numberPicker;
    final int minValue;
    final int maxValue;

    public DialogStepWeight(Context context, AttributeSet attrs) {
        super(context, attrs);
        intArray = context.getResources().getStringArray(R.array.array_step_kg);
        minValue = 0;
        if(intArray.length > 0)
            maxValue = intArray.length-1;
        else
            maxValue = 0;
        setPersistent(true);
        setDialogLayoutResource(R.layout.number_picker);
    }

    @Override
    protected void onBindDialogView(View view) {
        numberPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setMinValue(minValue);
        numberPicker.setDisplayedValues(intArray);
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
            callChangeListener(Integer.valueOf(intArray[value]));
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }


}
