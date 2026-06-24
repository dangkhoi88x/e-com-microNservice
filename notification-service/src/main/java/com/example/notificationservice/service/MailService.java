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

}
