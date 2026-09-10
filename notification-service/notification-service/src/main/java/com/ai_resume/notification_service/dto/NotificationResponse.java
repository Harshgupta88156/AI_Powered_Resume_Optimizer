package com.ai_resume.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Always returned with HTTP 200/202, even when the email failed to send —
 * a failed notification must never surface as an error to the caller's
 * business flow. Callers can inspect {@code success} for logging/metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private boolean success;
    private String message;
}

