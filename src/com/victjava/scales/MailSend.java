package com.victjava.scales;

import android.content.Context;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

/**
 * @author Kostya
 */
public class MailSend {
    protected final Context mContext;
    protected final MailObject mailObject;


    public MailSend(Context cxt, MailObject object){
        mContext = cxt;
        mailObject = object;
    }

    /*public MailSend(Context cxt, String email, String subject, String messageBody) {
        mContext = cxt;
        mailObject = new MailObject(email, subject, messageBody);
    }*/

    /** Отправляем письмо.
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public void sendMail() throws MessagingException, UnsupportedEncodingException {
        Session session = createSessionObject();
        Message message = createMessage(mailObject.getSubject(), mailObject.getBody(), session);
        Transport.send(message);
    }

    /** Отправляем письмо с приклепленным файлом.
     * Добавляем путь файла при создании обьекта письма. {@link com.victjava.scales.MailSend.MailObject#addFile(String)}.
     * @throws MessagingException Ошибка.
     * @throws UnsupportedEncodingException Ошибка.
     */
    public void sendMailAttach() throws MessagingException, UnsupportedEncodingException{
        Session session = createSessionObject();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(mailObject.getUser(), mailObject.getPersonal()));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailObject.getEmail(), mailObject.getEmail()));
        message.setSubject(mailObject.getSubject());

        // Create the message part
        BodyPart messageBodyPart = new MimeBodyPart();
        // Now set the actual message
        messageBodyPart.setText(mailObject.getBody());
        // Create a multipar message
        Multipart multipart = new MimeMultipart();
        // Set text message part
        multipart.addBodyPart(messageBodyPart);
        // Part two is attachment

        String[] files = (String[]) mailObject.getFiles().toArray();
        for (String file : files){
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(file);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(file);
            multipart.addBodyPart(messageBodyPart);
        }

        // Send the complete message parts
        message.setContent(multipart);

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
        message.setFrom(new InternetAddress(mailObject.getUser(), mailObject.getPersonal()));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailObject.getEmail(), mailObject.getEmail()));
        message.setSubject(subject);
        message.setText(messageBody);
        return message;
    }

    static class MailObject {
        List<String> files;
        protected String mEmail;
        protected String mSubject;
        protected String mBody;
        protected String mUser;
        protected String mPassword;
        protected String personal = "";

        public MailObject(String email){
            mEmail = email;
        }

        MailObject(String email, String subject, String message){
            mEmail = email;
            mSubject = subject;
            mBody = message;
        }

        public String getEmail() { return mEmail; }

        public void setEmail(String mEmail) { this.mEmail = mEmail; }

        public String getSubject() { return mSubject; }

        public void setSubject(String mSubject) { this.mSubject = mSubject; }

        public String getBody() { return mBody; }

        public void setBody(String mBody) { this.mBody = mBody; }

        public String getUser() { return mUser; }

        public void setUser(String mUser) { this.mUser = mUser; }

        public String getPassword() { return mPassword; }

        public void setPassword(String mPassword) { this.mPassword = mPassword; }

        public String getPersonal() { return personal; }

        public void setPersonal(String personal) { this.personal = personal; }

        public void addFile(String path){
            files.add(path);
        }

        public List<String> getFiles() {
            return files;
        }
    }

}
