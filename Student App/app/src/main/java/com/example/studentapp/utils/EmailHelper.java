package com.example.studentapp.utils;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailHelper {

    private static final String GMAIL_USER = "suyashwagare863@gmail.com";
    private static final String GMAIL_PASS = "mkle flqk qxpn ovkq";

    public static void sendOtpEmail(final String toEmail, final String otp, final EmailCallback callback) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(GMAIL_USER, GMAIL_PASS);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(GMAIL_USER, "AMPV Bus Tracking"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("Password Reset OTP");
                message.setText("Your OTP for password reset is: " + otp + "\n\nThis OTP is valid for 10 minutes.");

                Transport.send(message);
                
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) callback.onFailure(e.getMessage());
            }
        }).start();
    }

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
