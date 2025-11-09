package com.app.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Email service for sending notifications
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${notification.email.from}")
    private String fromAddress;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Send HTML email using Thymeleaf template
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!emailEnabled) {
            logger.info("📧 Email disabled - would have sent: {} to {}", subject, to);
            return;
        }

        try {
            // Process template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            // Create message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Send
            mailSender.send(message);
            logger.info("✅ Email sent successfully to {} - Subject: {}", to, subject);

        } catch (MessagingException e) {
            logger.error("❌ Failed to send email to {}: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error sending email: {}", e.getMessage(), e);
        }
    }

    /**
     * Send plain text email (fallback)
     */
    @Async
    public void sendPlainTextEmail(String to, String subject, String content) {
        if (!emailEnabled) {
            logger.info("📧 Email disabled - would have sent: {} to {}", subject, to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(message);
            logger.info("✅ Plain text email sent to {} - Subject: {}", to, subject);

        } catch (Exception e) {
            logger.error("❌ Failed to send plain text email to {}: {}", to, e.getMessage(), e);
        }
    }
}
