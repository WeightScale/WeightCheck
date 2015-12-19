/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.victjava.scales.camera;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.GestureDetector.OnGestureListener;
import com.victjava.scales.Main;

public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback{
    private volatile Camera camera;
    private SurfaceHolder holder;
    private CameraCallback callback;
    boolean taking;
    //private GestureDetector gesturedetector;

    public CameraSurface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initialize(context);
    }

    public CameraSurface(Context context) {
        super(context);

        initialize(context);
    }

    public CameraSurface(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize(context);
    }

    public void setCallback(CameraCallback callback) {
        this.callback = callback;
    }

    public void startPreview() {
        camera.startPreview();
    }

    public synchronized void startTakePicture(int photoId) {
        while (taking);
        taking = true;
        camera.autoFocus(new AutoFocusCallback() {
            @Override
            public synchronized void onAutoFocus(boolean success, Camera camera) {
                if (success)
                    takePicture(photoId);
            }
        });
    }

    public synchronized void takePicture(int photoId) {
        camera.takePicture( new ShutterCallback() {
            @Override
            public void onShutter(){
                if(callback != null)
                    callback.onShutter();
            }
        }, null, new PictureCallback() {
            @Override
            public synchronized void onPictureTaken(byte[] data, Camera camera) {
                if (callback != null)
                    callback.onJpegPictureTaken(data, camera, photoId);
                taking=false;
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            camera.startPreview();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        camera.setParameters(Main.parameters);
        camera.setDisplayOrientation(90);
        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (callback != null)
                        callback.onPreviewFrame(data, camera);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    /*@Override
    public boolean onTouchEvent(MotionEvent event) {
        return gesturedetector.onTouchEvent(event);
    }*/

    private void initialize(Context context) {
        holder = getHolder();

        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //gesturedetector = new GestureDetector(this);
    }

    public Camera getCamera() { return camera; }
}
