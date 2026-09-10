package com.ai_resume.dashboard_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Mirror of the notification-service response contract.
 *
 * <p>notification-service does not exist yet. This is the shape dashboard-service
 * expects; until it ships, {@code NotificationGateway} returns empty results and
 * the dashboard degrades gracefully instead of failing.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationView {

    private Long notificationId;
    private String category;
    private String priority;
    private String title;
    private String message;
    private String actionUrl;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}

