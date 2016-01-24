package com.victjava.scales.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import com.victjava.scales.NumberPicker;
import com.victjava.scales.R;

/**
 * @author Kostya
 */
class DialogFilterADC extends DialogPreference /*implements ActivityPreferences.InterfacePreference*/ {
    private int mNumber;
    private NumberPicker numberPicker;
    final int minValue;
    final int maxValue;

    public DialogFilterADC(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributesArray = context.obtainStyledAttributes(attrs, R.styleable.dialogFilterADC, R.attr.dialogFilterADCStyle, 0);
        minValue = attributesArray.getInt(R.styleable.dialogFilterADC_minFilterADC, 0);
        maxValue = attributesArray.getInt(R.styleable.dialogFilterADC_maxFilterADC, 0);
        setPersistent(true);
        setDialogLayoutResource(R.layout.number_picker);
    }

    @Override
    protected void onBindDialogView(View view) {
        numberPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setMinValue(minValue);
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
