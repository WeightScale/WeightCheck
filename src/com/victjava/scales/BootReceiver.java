package com.victjava.scales;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.victjava.scales.service.ServiceSmsCommand;

/**
 * @author Kostya
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //context.startService(new Intent(context, ServiceGetDateServer.class));// Запускаем сервис для передачи данных на google disk
        //context.startService(new Intent(context, ServiceSentSheetServer.class));// Запускаем сервис для передачи данных на google disk//todo временно отключен
        context.startService(new Intent(context, ServiceSmsCommand.class));// Запускаем сервис для приемеа смс команд
    }
}
