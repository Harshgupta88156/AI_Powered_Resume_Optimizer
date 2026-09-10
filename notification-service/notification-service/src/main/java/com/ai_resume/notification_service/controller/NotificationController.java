package com.ai_resume.notification_service.controller;

import com.ai_resume.notification_service.dto.AnalysisCompletedEmailRequest;
import com.ai_resume.notification_service.dto.NotificationResponse;
import com.ai_resume.notification_service.dto.WeeklyTrendsEmailRequest;
import com.ai_resume.notification_service.dto.WelcomeEmailRequest;
import com.ai_resume.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EMAIL ONLY. Other microservices call these endpoints whenever an email needs
 * to be sent; none of them contain SMTP logic themselves.
 *
 * <p>Every endpoint returns 202 ACCEPTED - the request to send is accepted, but
 * whether the email actually reached the SMTP server is a fire-and-forget
 * concern the caller should never block business logic on.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/welcome")
    public ResponseEntity<NotificationResponse> sendWelcomeEmail(@Valid @RequestBody WelcomeEmailRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(notificationService.sendWelcomeEmail(request));
    }

    @PostMapping("/analysis-completed")
    public ResponseEntity<NotificationResponse> sendAnalysisCompletedEmail(
            @Valid @RequestBody AnalysisCompletedEmailRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(notificationService.sendAnalysisCompletedEmail(request));
    }

    @PostMapping("/weekly-trends")
    public ResponseEntity<NotificationResponse> sendWeeklyTrendsEmail(
            @Valid @RequestBody WeeklyTrendsEmailRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(notificationService.sendWeeklyTrendsEmail(request));
    }
}

