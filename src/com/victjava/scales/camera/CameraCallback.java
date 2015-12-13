/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.victjava.scales.camera;

import android.hardware.Camera;

public interface CameraCallback {
    void onPreviewFrame(byte[] data, Camera camera);
    void onShutter();
    //void onRawPictureTaken(byte[] data, Camera camera);
    void onJpegPictureTaken(byte[] data, Camera camera);
    //String onGetVideoFilename();
}
