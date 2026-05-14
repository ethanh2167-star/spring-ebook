package com.ebook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String token, String frontendUrl) {
    	String resetLink = frontendUrl + "/index.html?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("【電子書平台】密碼重設通知");
        message.setText(
            "您好，\n\n" +
            "我們收到您的密碼重設請求，請點擊以下連結完成重設（連結 30 分鐘內有效）：\n\n" +
            resetLink + "\n\n" +
            "若您未申請此服務，請忽略本信件。\n\n" +
            "電子書平台 敬上"
        );

        mailSender.send(message);
    }
}