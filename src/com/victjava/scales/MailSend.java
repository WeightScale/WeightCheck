package com.victjava.scales;

import android.content.Context;
import com.konst.module.ScaleModule;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/*
 * Created by Kostya on 12.02.2015.
 */
public class MailSend {
    protected final Context mContext;
    final ScaleModule scaleModule;
    final MailObject mailObject;

    //StringBuilder stringBuilderBody;
    public MailSend(Context cxt, MailObject object){
        mContext = cxt;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
        mailObject = object;
    }

    public MailSend(Context cxt, String email, String subject, String messageBody) {
        mContext = cxt;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
        mailObject = new MailObject(email, subject, messageBody);
    }

    public void sendMail() throws MessagingException, UnsupportedEncodingException {
        Session session = createSessionObject();
        Message message = createMessage(mailObject.getSubject(), mailObject.getBody(), session);
        Transport.send(message);
    }

    private Session createSessionObject() {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", "smtp.gmail.com");
        properties.setProperty("mail.smtp.socketFactory.port", "465");
        properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.port", "465");
        properties.put("mail.smtp.timeout", 10000);
        properties.put("mail.smtp.connectiontimeout", 10000);

        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailObject.getUser(), mailObject.getPassword());
            }
        });
    }

    private Message createMessage(String subject, String messageBody, Session session) throws MessagingException, UnsupportedEncodingException {
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress("scale", mContext.getString(R.string.app_name) + " \"" + scaleModule.getNameBluetoothDevice()));
        } catch (Exception e) {
            message.setFrom(new InternetAddress("scale", mContext.getString(R.string.app_name) + " \""));
        }
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailObject.getEmail(), mailObject.getEmail()));
        //message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mEmail));
        //message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(builderMail.toString(),false));
        message.setSubject(subject);
        message.setText(messageBody);
        return message;
    }

    static class MailObject {
        protected String mEmail;
        protected String mSubject;
        protected String mBody;
        protected String mUser;
        protected String mPassword;

        public MailObject(String email){
            mEmail = email;
        }

        MailObject(String email, String subject, String message){
            mEmail = email;
            mSubject = subject;
            mBody = message;

        }

        public String getEmail() {
            return mEmail;
        }

        public void setEmail(String mEmail) {
            this.mEmail = mEmail;
        }

        public String getSubject() {
            return mSubject;
        }

        public void setSubject(String mSubject) {
            this.mSubject = mSubject;
        }

        public String getBody() {
            return mBody;
        }

        public void setBody(String mBody) {
            this.mBody = mBody;
        }

        public String getUser() {
            return mUser;
        }

        public void setUser(String mUser) {
            this.mUser = mUser;
        }

        public String getPassword() {
            return mPassword;
        }

        public void setPassword(String mPassword) {
            this.mPassword = mPassword;
        }
    }

}
