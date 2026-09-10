package com.ai_resume.notification_service.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget wrapper around {@link EmailService}, run on the dedicated
 * {@code emailTaskExecutor} pool (see {@code AsyncConfig}).
 *
 * <p>This is what lets {@code NotificationServiceImpl} return immediately
 * instead of making a caller (e.g. auth-service during registration) wait for
 * an SMTP round-trip before its own request can complete.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDispatcher {

    private final EmailService emailService;

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> dispatch(
            String to, String subject, String templateName, Map<String, Object> variables) {
        boolean sent = emailService.sendHtmlEmail(to, subject, templateName, variables);
        if (!sent) {
            log.warn("Async dispatch of '{}' email to {} did not succeed", templateName, to);
        }
        return CompletableFuture.completedFuture(sent);
    }
}

