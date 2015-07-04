package com.victjava.scales.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import com.konst.sms_commander.OnSmsCommandListener;
import com.konst.sms_commander.SMS;
import com.konst.sms_commander.SmsCommander;
import com.victjava.scales.BootReceiver;
import com.victjava.scales.SmsCommand;

import java.util.*;

/**
 * @author Kostya
 */
public class ServiceSmsCommand extends Service {

    /**
     * Экземпляр приемника смс сообщений.
     */
    final IncomingSMSReceiver incomingSMSReceiver = new IncomingSMSReceiver();
    /**
     * Кодовое слово для дешифрации сообщения
     */
    final String codeword = "weightcheck";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter(IncomingSMSReceiver.SMS_DELIVER_ACTION);
        intentFilter.addAction(IncomingSMSReceiver.SMS_RECEIVED_ACTION);
        intentFilter.addAction(IncomingSMSReceiver.SMS_COMPLETED_ACTION);
        intentFilter.setPriority(999);
        registerReceiver(incomingSMSReceiver, intentFilter);

        /*String msg = "0503285426 coffa=0.25687 coffb gogusr=kreogen.lg@gmail.com gogpsw=htcehc25";
        String str = null;
        try {
            str = SMS.encrypt(codeword, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //decodeMessage(str);
        byte[] pdu = SMS.fromHexString("07914400000000F001000B811000000000F000006D51E7FCC8CC96EDED2C19199D078D6A375D1BAEE3CCF397F2CE44CAD736E1BA6D9EC770D8A0B4166697ADECE079655EAAF341EC1D7E54B76FF86C1EC93CB6CDF4B2F9AE383ADF6EB83A2C5FE1CA3228121B7CE663D6B052796EAE84526515D603");

        Intent intent = new Intent(ServiceSmsCommand.IncomingSMSReceiver.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", new Object[]{pdu});
        sendBroadcast(intent);*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /** Обрабатываем смс команды */
        new ProcessingSmsThread(this).start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //processingSmsThread.cancel();
        //while(processingSmsThread.isStart());
        unregisterReceiver(incomingSMSReceiver);
    }

    /**
     * Приемник смс сообщений.
     */
    public class IncomingSMSReceiver extends BootReceiver {

        /**
         * Входящее сообщение.
         */
        public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
        /**
         * Принятые непрочитаные сообщения.
         */
        public static final String SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER";
        /**
         * Транзакция завершена.
         */
        public static final String SMS_COMPLETED_ACTION = "android.intent.action.TRANSACTION_COMPLETED_ACTION";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {

                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
                        try {
                            new SmsCommander(codeword, pdus, onSmsCommandListener);
                            abortBroadcast();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    /**
     * Слушатель обработчика смс команд.
     * Возвращяем событие если смс это команда.
     */
    final OnSmsCommandListener onSmsCommandListener = new OnSmsCommandListener() {
        StringBuilder result = new StringBuilder();

        /** Событие есть смс команда.
         *  @param address Адресс отправителя.
         *  @param list Лист смс команд.
         */
        @Override
        public void onEvent(String address, List<SmsCommander.Command> list) {
            try {
                /** Обрабатываем лист команд и возвращяем результат */
                result = new SmsCommand(getApplicationContext(), list).process();
            } catch (Exception e) {
                result.append(e.getMessage());
            }

            try {
                /** Отправляем результат выполнения команд адресату */
                SMS.sendSMS(address, result.toString());
            } catch (Exception e) {
            }
        }
    };

    /**
     * Процесс обработки смс команд.
     * Обрабатывам команды которые приняты и не обработаные.
     */
    public class ProcessingSmsThread extends Thread {
        private boolean start;
        private boolean cancelled;
        private final SMS sms;
        private final List<SMS.SmsObject> smsInboxList;
        private final Context mContext;

        ProcessingSmsThread(Context context) {
            mContext = context;
            sms = new SMS(mContext);
            smsInboxList = sms.getInboxSms();
        }

        @Override
        public synchronized void start() {
            super.start();
            start = true;
        }

        private void cancel() {
            cancelled = true;
        }

        public boolean isStart() {
            return start;
        }

        @Override
        public void run() {

            for (final SMS.SmsObject smsObject : smsInboxList) {
                try {
                    new SmsCommander(codeword, smsObject.getAddress(), smsObject.getMsg(), onSmsCommandListener);
                } catch (Exception e) {
                }
            }
            start = false;
        }
    }

}
