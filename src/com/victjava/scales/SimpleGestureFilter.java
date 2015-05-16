package com.victjava.scales;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

/*
 * Created by Kostya on 08.03.2015.
 */
public class SimpleGestureFilter extends SimpleOnGestureListener {

    public static final int SWIPE_UP = 1;
    public static final int SWIPE_DOWN = 2;
    public static final int SWIPE_LEFT = 3;
    public static final int SWIPE_RIGHT = 4;

    public static final int MODE_TRANSPARENT = 0;
    public static final int MODE_SOLID = 1;
    public static final int MODE_DYNAMIC = 2;

    private static final int ACTION_FAKE = -13; //just an unlikely number
    private int swipe_Min_Distance = 100;
    private int swipe_Max_Distance = 350;
    private int swipe_Min_Velocity = 100;

    private int mode = MODE_DYNAMIC;
    private boolean running = true;
    private boolean tapIndicator;

    private final Activity context;
    private final GestureDetector detector;
    private final SimpleGestureListener listener;
    private View view;

    public SimpleGestureFilter(Activity context, SimpleGestureListener sgl) {

        this.context = context;
        detector = new GestureDetector(context, this);
        listener = sgl;
    }

    public void onTouchEvent(MotionEvent event) {

        if (!running) {
            return;
        }

        boolean result = detector.onTouchEvent(event);

        if (mode == MODE_SOLID) {
            event.setAction(MotionEvent.ACTION_CANCEL);
        } else if (mode == MODE_DYNAMIC) {

            if (event.getAction() == ACTION_FAKE) {
                event.setAction(MotionEvent.ACTION_UP);
            } else if (result) {
                event.setAction(MotionEvent.ACTION_CANCEL);
            } else if (tapIndicator) {
                event.setAction(MotionEvent.ACTION_DOWN);
                tapIndicator = false;
            }

        }
        //else just do nothing, it's Transparent
    }

    public void setMode(int m) {
        mode = m;
    }

    public int getMode() {
        return mode;
    }

    public void setEnabled(boolean status) {
        running = status;
    }

    public void setSwipeMaxDistance(int distance) {
        swipe_Max_Distance = distance;
    }

    public void setSwipeMinDistance(int distance) {
        swipe_Min_Distance = distance;
    }

    public void setSwipeMinVelocity(int distance) {
        swipe_Min_Velocity = distance;
    }

    public int getSwipeMaxDistance() {
        return swipe_Max_Distance;
    }

    public int getSwipeMinDistance() {
        return swipe_Min_Distance;
    }

    public int getSwipeMinVelocity() {
        return swipe_Min_Velocity;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        final float xDistance = Math.abs(e1.getX() - e2.getX());
        final float yDistance = Math.abs(e1.getY() - e2.getY());

        if (xDistance > swipe_Max_Distance || yDistance > swipe_Max_Distance) {
            return false;
        }

        velocityX = Math.abs(velocityX);
        velocityY = Math.abs(velocityY);
        boolean result = false;

        if (velocityX > swipe_Min_Velocity && xDistance > swipe_Min_Distance) {
            if (e1.getX() > e2.getX()) // right to left
            {
                listener.onSwipe(SWIPE_LEFT);
            } else {
                listener.onSwipe(SWIPE_RIGHT);
            }

            result = true;
        } else if (velocityY > swipe_Min_Velocity && yDistance > swipe_Min_Distance) {
            if (e1.getY() > e2.getY()) // bottom to up
            {
                listener.onSwipe(SWIPE_UP);
            } else {
                listener.onSwipe(SWIPE_DOWN);
            }

            result = true;
        }

        return result;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        tapIndicator = true;
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent arg) {
        listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent arg) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent arg) {

        if (mode == MODE_DYNAMIC) {        // we owe an ACTION_UP, so we fake an
            arg.setAction(ACTION_FAKE);      //action which will be converted to an ACTION_UP later.
            context.dispatchTouchEvent(arg);
        }

        return false;
    }

    interface SimpleGestureListener {
        void onSwipe(int direction);

        void onDoubleTap();
    }

}