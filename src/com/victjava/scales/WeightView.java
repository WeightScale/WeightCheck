/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.victjava.scales;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.victjava.scales.R;

/*
 * Created by Kostya on 18.11.2014.
 */
public class WeightView extends ProgressBar {
    private CharSequence text = "";
    //private int textColor = Color.BLACK;
    //private float textSize = getResources().getDimension( R.dimen.text_big);
    private final Paint textPaint;
    private final Rect bounds;

    public WeightView(Context context) {
        super(context);
        textPaint = new Paint();
        bounds = new Rect();
        textPaint.setAntiAlias(true);
        //textPaint.setTextSize(textSize);
        setWillNotDraw(false);
    }

    public WeightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint = new Paint();
        bounds = new Rect();
        textPaint.setAntiAlias(true);
        //textPaint.setTextSize(textSize);
        setWillNotDraw(false);
    }

    public synchronized void updateProgress(CharSequence text) {
        setText(text);
        drawableStateChanged();
    }

    @Override
    protected synchronized void onDraw( Canvas canvas) {
        super.onDraw(canvas);
        //textPaint.setColor(textColor);
        //textPaint.setTextSize(textSize);
        //Rect bounds = new Rect();
        textPaint.getTextBounds(text.toString(), 0, text.toString().length(), bounds);
        int x = getWidth() - bounds.right - (int) getResources().getDimension(R.dimen.padding);
        int y = getHeight() / 2 - bounds.centerY();
        canvas.drawText(text, 0, text.length(), x, y, textPaint);
    }

    private void setText(CharSequence text) {
        this.text = text;
        postInvalidate();
    }

    /*private void setTextColor(int color) {
        textColor = color;
        postInvalidate();
    }*/

}
