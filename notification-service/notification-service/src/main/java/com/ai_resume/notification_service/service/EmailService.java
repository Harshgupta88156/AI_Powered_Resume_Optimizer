package com.ai_resume.notification_service.service;

import java.util.Map;

/**
 * Abstraction over the email transport. Today {@code SmtpEmailService} implements
 * this with Spring Boot Mail (SMTP). Switching to Amazon SES, SendGrid, Brevo or
 * Mailgun later only means adding a new implementation of this interface -
 * {@link NotificationService} and the controller never change.
 */
public interface EmailService {

    /**
     * Renders the given Thymeleaf template with {@code variables} and sends it as
     * an HTML email to {@code to}.
     *
     * @return true if the email was sent successfully, false otherwise. Never
     *         throws - failures are logged and reflected in the return value so
     *         callers can never let email delivery break a business operation.
     */
    boolean sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);
}

