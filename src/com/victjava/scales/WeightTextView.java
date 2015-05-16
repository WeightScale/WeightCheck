package com.victjava.scales;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/*
 * Created by Kostya on 18.11.2014.
 */
public class WeightTextView extends ProgressBar {
    private String text = "";
    private int textColor = Color.BLACK;
    private float textSize = getResources().getDimension(R.dimen.text_big);
    private final Paint textPaint;
    private final Rect bounds;

    public WeightTextView(Context context) {
        super(context);
        textPaint = new Paint();
        bounds = new Rect();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
    }

    public WeightTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint = new Paint();
        bounds = new Rect();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
    }

    public synchronized void updateProgress(int progress, int color, float size) {
        setText(progress + getResources().getString(R.string.scales_kg));
        textSize = size;
        textColor = color;
        drawableStateChanged();
    }

    public synchronized void updateProgress(String progress, int color, float size) {
        setText(progress);
        textSize = size;
        textColor = color;
        drawableStateChanged();
    }

    @Override
    protected synchronized void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        //Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        int x = getWidth() - bounds.right - (int) getResources().getDimension(R.dimen.padding);
        int y = getHeight() / 2 - bounds.centerY();
        canvas.drawText(text, x, y, textPaint);
    }

    private void setText(String text) {
        this.text = text;
        postInvalidate();
    }

    private void setTextColor(int color) {
        textColor = color;
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }
}
