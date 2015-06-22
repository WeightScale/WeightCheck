package com.victjava.scales.service;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import com.victjava.scales.ActivityScales;
import com.victjava.scales.Scales;

/*
 * Created by Kostya on 02.03.2015.
 */
public class ServiceConnectScale extends Service {
    private class ConnectThread extends Thread {
        final PendingIntent pendingIntent;

        ConnectThread(PendingIntent pi) {
            pendingIntent = pi;
        }

        @Override
        public void run() {
            try {
                if (Scales.connect(device)) {
                    if (Scales.isScales()) {
                        try {
                            if (Scales.vScale.load()) {
                                pendingIntent.send(ServiceConnectScale.this, ActivityScales.STATUS_OK, new Intent());
                            } else {
                                pendingIntent.send(ServiceConnectScale.this, ActivityScales.STATUS_SCALE_ERROR, new Intent());
                            }
                        } catch (Exception e) {
                            Intent intent = new Intent();
                            intent.putExtra("error", e.getMessage());
                            pendingIntent.send(ServiceConnectScale.this, ActivityScales.STATUS_TERMINAL_ERROR, intent);
                        }
                    } else {
                        Intent intent = new Intent();
                        intent.putExtra("device", device.getName());
                        pendingIntent.send(ServiceConnectScale.this, ActivityScales.STATUS_SCALE_UNKNOWN, intent);
                        Scales.disconnect();
                    }
                } else {
                    pendingIntent.send(ServiceConnectScale.this, ActivityScales.STATUS_CONNECT_ERROR, new Intent());
                }
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
            stopSelf();
        }
    }

    BluetoothDevice device;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        device = intent.getParcelableExtra("device");
        PendingIntent pi = intent.getParcelableExtra(ActivityScales.PARAM_PENDING_INTENT);
        new ConnectThread(pi).start();
        return super.onStartCommand(intent, flags, startId);
    }

}
