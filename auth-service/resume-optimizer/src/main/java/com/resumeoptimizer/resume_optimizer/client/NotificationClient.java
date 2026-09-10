package com.resumeoptimizer.resume_optimizer.client;

import com.resumeoptimizer.resume_optimizer.dto.WelcomeEmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative HTTP client to notification-service, resolved through Eureka -
 * same pattern as {@link UserClient}. notification-service owns all SMTP
 * logic; auth-service only ever asks it to send an email.
 */
@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications/welcome")
    void sendWelcomeEmail(@RequestBody WelcomeEmailRequest request);
}

