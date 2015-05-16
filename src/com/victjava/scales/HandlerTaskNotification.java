package com.victjava.scales;


import android.os.Handler;
import android.support.v4.app.NotificationCompat;

/*
 * Created by Kostya on 04.05.2015.
 */
public abstract class HandlerTaskNotification extends Handler {

    public abstract void handleRemoveEntry(int what, int arg1);

    //public abstract void handleRemoveEntry(int what, TaskCommand.MsgNotify msg);
    //public abstract void handleNotification(int what, int arg1, TaskCommand.MsgNotify msg);
    //public abstract void handleNotification(int what, int arg1, int arg2, ArrayList<TaskCommand.ObjParcel> objParcels);
    public abstract void handleNotificationError(int what, int arg1, TaskCommand.MsgNotify msg);

    public abstract void handleError(int what, String msg);
}
