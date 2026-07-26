package com.example.notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    @Value("${spring.mail.username}")
    private String from;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine springTemplateEngine;

    @Async
    public void sendEmailWithTemplate(String to, String fullName, String subject, String templateName) {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message);
        try {
            Context context = new Context();
            context.setVariable("name", fullName);
            context.setVariable("accountEmail", to);
            context.setVariable("loginUrl", "http://localhost:5173/auth");
            String htmlContent = springTemplateEngine.process(templateName, context);

            helper.setFrom(from, "Backend Service");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully");
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Error setting from address: {}", e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetOtp(String to, String otp, long expiresInMinutes) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            Context context = new Context();
            context.setVariable("otp", otp);
            context.setVariable("expiresInMinutes", expiresInMinutes);
            String htmlContent = springTemplateEngine.process("password-reset-otp", context);

            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from, "NovaShop");
            helper.setTo(to);
            helper.setSubject("Mã xác nhận đặt lại mật khẩu NovaShop");
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset email sent successfully");
        } catch (MessagingException | UnsupportedEncodingException exception) {
            log.error("Could not send password reset email", exception);
        }
    }

}
