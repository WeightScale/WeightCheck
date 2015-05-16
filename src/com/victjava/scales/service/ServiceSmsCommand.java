package com.victjava.scales.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsMessage;
import com.victjava.scales.BootReceiver;
import com.victjava.scales.SMS;
import com.victjava.scales.SmsCommand;

import java.text.SimpleDateFormat;
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
        String str = encodeMessage(msg);
        decodeMessage(str);
        byte[] pdu = fromHexString("079183503082456201000C9183503082456200004A33DCCC56DBE16EB5DCC82C4FA7C98059AC86CBED7423B33C9D2E8FD47235DE5E07B8EB68B91A1D8FBDD543359CCC7EC7CC72F8482D57CFED7AC0FA6E46AFCD351C");

        Intent intent = new Intent(ServiceSmsCommand.IncomingSMSReceiver.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", new Object[]{pdu});
        sendBroadcast(intent);*/

        //processingSmsThread = new ProcessingSmsThread(this);
    }

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
                        SmsMessage[] messages = new SmsMessage[pdus.length];
                        StringBuilder bodyText = new StringBuilder();
                        String address = "";
                        int length = pdus.length;
                        for (int i = 0; i < length; i++) {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            address = messages[i].getDisplayOriginatingAddress();
                            bodyText.append(messages[i].getMessageBody());
                        }
                        try {
                            String textSent = SMS.decrypt(codeword, bodyText.toString());
                            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
                            new Thread(new ParsingSmsCommand(address, textSent, date)).start();
                            abortBroadcast();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

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

            for (SMS.SmsObject object : smsInboxList) {
                try {
                    StringBuilder textSent = new StringBuilder(SMS.decrypt(codeword, object.getMsg()));
                    extractSmsCommand(object.getAddress(), textSent);
                    sms.delete(Integer.valueOf(object.getId()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            start = false;
        }
    }

    public class ParsingSmsCommand implements Runnable {
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

    }

    void extractSmsCommand(String address, StringBuilder msg) {

        if (address == null)
            return;
        if (msg.indexOf(" ") != -1) {
            String body_address = msg.substring(0, msg.indexOf(" "));
            if (!body_address.isEmpty()) {
                if (body_address.length() > address.length()) {
                    body_address = body_address.substring(body_address.length() - address.length(), body_address.length());
                } else if (body_address.length() < address.length()) {
                    address = address.substring(address.length() - body_address.length(), address.length());
                }
                if (body_address.equals(address)) {
                    msg.delete(0, msg.indexOf(" ") + 1);
                    StringBuilder textSent = new StringBuilder();
                    try {
                        SmsCommand command = new SmsCommand(getApplicationContext(), msg.toString());
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
    }

}
