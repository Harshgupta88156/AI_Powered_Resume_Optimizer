package com.ai_resume.notification_service.service.impl;

import com.ai_resume.notification_service.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * EmailService implementation backed by Spring Boot Mail (SMTP).
 *
 * <p>To switch providers (Amazon SES, SendGrid, Brevo, Mailgun...) later, add a
 * new {@link EmailService} implementation and swap the bean - nothing in
 * {@code NotificationServiceImpl} or the controller needs to change.
 */
@Service
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${notification.mail.from}")
    private String fromAddress;

    @Value("${notification.mail.from-name}")
    private String fromName;

    public SmtpEmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom(fromAddress, fromName);

            mailSender.send(message);
            log.info("Sent '{}' email to {}", templateName, to);
            return true;
        } catch (Exception ex) {
            // Any failure here (bad SMTP creds, network issue, template error) is
            // logged and swallowed - the caller's business flow must keep succeeding.
            log.error("Failed to send '{}' email to {}: {}", templateName, to, ex.getMessage(), ex);
            return false;
        }
    }
}

