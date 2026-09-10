package com.ai_resume.dashboard_service.service;

import com.ai_resume.dashboard_service.client.NotificationServiceClient;
import com.ai_resume.dashboard_service.client.dto.NotificationView;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fault-isolating wrapper around notification-service.
 *
 * <p>notification-service is not built yet, and even once it is, notifications are
 * the least important thing on the dashboard. Neither its absence nor an outage
 * should stop a user seeing their resumes, scores and charts — so every failure
 * here degrades to "no notifications" and is reported through
 * {@code degradedSources} rather than propagated.
 *
 * <p>Enable with {@code dashboard.notifications.enabled=true} once the service is
 * registered in Eureka.
 */
@Component
@Slf4j
public class NotificationGateway {

    public static final String SOURCE_NAME = "notification-service";

    private final ObjectProvider<NotificationServiceClient> clientProvider;
    private final boolean enabled;

    public NotificationGateway(
            ObjectProvider<NotificationServiceClient> clientProvider,
            @Value("${dashboard.notifications.enabled:false}") boolean enabled) {
        this.clientProvider = clientProvider;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Unread count, or -1 if it could not be determined. */
    public long fetchUnreadCount() {
        if (!enabled) {
            return -1;
        }
        try {
            NotificationServiceClient client = clientProvider.getIfAvailable();
            if (client == null) {
                return -1;
            }
            var response = client.getUnreadCount();
            return response == null ? -1 : response.unreadCount();
        } catch (Exception ex) {
            log.warn("notification-service unavailable while fetching unread count: {}",
                    ex.getMessage());
            return -1;
        }
    }

    /** Most recent notifications, or an empty list if they could not be fetched. */
    public List<NotificationView> fetchRecent(int limit) {
        if (!enabled) {
            return List.of();
        }
        try {
            NotificationServiceClient client = clientProvider.getIfAvailable();
            if (client == null) {
                return List.of();
            }
            var page = client.getNotifications(0, limit, "createdAt,desc");
            return page == null ? List.of() : page.getContent();
        } catch (Exception ex) {
            log.warn("notification-service unavailable while fetching recent notifications: {}",
                    ex.getMessage());
            return List.of();
        }
    }
}

