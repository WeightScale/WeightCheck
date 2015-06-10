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

/*
 * Created by Kostya on 29.03.2015.
 */
public class ServiceSmsCommand extends Service {

    final IncomingSMSReceiver incomingSMSReceiver = new IncomingSMSReceiver();
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
        byte[] pdu = fromHexString("07914400000000F001000B811000000000F000006D51E7FCC8CC96EDED2C19199D078D6A375D1BAEE3CCF397F2CE44CAD736E1BA6D9EC770D8A0B4166697ADECE079655EAAF341EC1D7E54B76FF86C1EC93CB6CDF4B2F9AE383ADF6EB83A2C5FE1CA3228121B7CE663D6B052796EAE84526515D603");

        Intent intent = new Intent(ServiceSmsCommand.IncomingSMSReceiver.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", new Object[]{pdu});
        sendBroadcast(intent);*/

        //processingSmsThread = new ProcessingSmsThread(this);
    }

    /*public static byte[] fromHexString(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*if(!processingSmsThread.isStart()) {
            processingSmsThread.start();
        }*/
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

    //==================================================================================================================
    public class IncomingSMSReceiver extends BootReceiver {

        public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
        public static final String SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER";
        public static final String SMS_COMPLETED_ACTION = "android.intent.action.TRANSACTION_COMPLETED_ACTION";

        @Override
        public void onReceive(Context context, Intent intent) {
            //this.context = context;
            if (intent.getAction() != null) {
                if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {

                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
                        try {
                            new SmsCommander(codeword, pdus, onSmsCommandListener);
                            abortBroadcast();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    final OnSmsCommandListener onSmsCommandListener = new OnSmsCommandListener() {
        StringBuilder textSent = new StringBuilder();
        @Override
        public void onEvent(String address, List<SmsCommander.Command> list) {
            try {
                SmsCommand command = new SmsCommand(getApplicationContext(), list);
                textSent = command.commandsExt();
            } catch (Exception e) {
                textSent.append(e.getMessage());
            }

            try {
                SMS.sendSMS(address, textSent.toString());
            } catch (Exception e) {}
        }
    };

    //==================================================================================================================
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

            for (final SMS.SmsObject object : smsInboxList) {
                try {
                    new SmsCommander(codeword, object.getAddress(), object.getMsg(), new OnSmsCommandListener() {
                        StringBuilder textSent = new StringBuilder();
                        @Override
                        public void onEvent(String address, List<SmsCommander.Command> list) {
                            try {
                                SmsCommand command = new SmsCommand(getApplicationContext(), list);
                                textSent = command.commandsExt();
                            } catch (Exception e) {
                                textSent.append(e.getMessage());
                            }

                            try { SMS.sendSMS(address, textSent.toString()); } catch (Exception e) {}
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            start = false;
        }
    }
    /*public class ParsingSmsCommand implements Runnable {
        final String mAddress;
        final StringBuilder mText;
        final String date;

        ParsingSmsCommand(String address, String msg, String d) {
            mAddress = address;
            mText = new StringBuilder(msg);
            date = d;
        }

        @Override
        public void run() {
            extractSmsCommand(mAddress, mText);
        }

    }*/

    /** Фармат пакета комманд.
     * Формат пакета [ [address] space [ [комманда 1] space [комманда 2] space [комманда n] ] ]
     * Формат комманды [ [имя комманды]=[параметр] ]
     * Формат параметра [ [[значение 1]-[параметр 2]]_[[значение 2]-[параметр 2]]_[[значение n]-[параметр n]] ]
     * @param address Отправитель.
     * @param _package Пакет комманд.  */
    /*void extractSmsCommand(String address, StringBuilder _package) {

        if (address == null)
            return;
        if (_package.indexOf(" ") != -1) {
            String body_address = _package.substring(0, _package.indexOf(" "));
            if (!body_address.isEmpty()) {
                if (body_address.length() > address.length()) {
                    body_address = body_address.substring(body_address.length() - address.length(), body_address.length());
                } else if (body_address.length() < address.length()) {
                    address = address.substring(address.length() - body_address.length(), address.length());
                }
                if (body_address.equals(address)) {
                    _package.delete(0, _package.indexOf(" ") + 1);
                    StringBuilder textSent = new StringBuilder();
                    try {
                        SmsCommand command = new SmsCommand(getApplicationContext(), _package.toString());
                        textSent = command.commandsExt();
                    } catch (Exception e) {
                        textSent.append(e.getMessage());
                    }
                    try {
                        SMS.sendSMS(address, textSent.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }*/

}
