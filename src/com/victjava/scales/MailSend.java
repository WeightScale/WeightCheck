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
    protected final String mEmail;
    protected final String mSubject;
    protected final String mBody;
    //StringBuilder stringBuilderBody;

    public MailSend(Context cxt, String email, String subject, String messageBody) {
        mContext = cxt;
        mEmail = email;
        mSubject = subject;
        mBody = messageBody;
    }

    public void sendMail() throws MessagingException, UnsupportedEncodingException {
        Session session = createSessionObject();
        Message message = createMessage(mSubject, mBody, session);
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
                return new PasswordAuthentication(ScaleModule.getUserName(), ScaleModule.getPassword());
            }
        });
    }

    private Message createMessage(String subject, String messageBody, Session session) throws MessagingException, UnsupportedEncodingException {
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress("scale", mContext.getString(R.string.app_name) + " \"" + ScaleModule.getName()));
        } catch (Exception e) {
            message.setFrom(new InternetAddress("scale", mContext.getString(R.string.app_name) + " \""));
        }
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(mEmail, mEmail));
        //message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mEmail));
        //message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(builderMail.toString(),false));
        message.setSubject(subject);
        message.setText(messageBody);
        return message;
    }

}
