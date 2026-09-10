package com.ai_resume.dashboard_service.client;

import com.ai_resume.dashboard_service.client.dto.NotificationView;
import com.ai_resume.dashboard_service.client.dto.PagedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Reads from notification-service.
 *
 * <p>That service is NOT built yet. This client is the agreed contract; calls are
 * disabled by default via {@code dashboard.notifications.enabled=false} and are
 * always routed through {@code NotificationGateway}, which swallows failures.
 * Flip the flag when notification-service ships — no other change is needed.
 */
@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @GetMapping("/api/notifications/unread-count")
    UnreadCount getUnreadCount();

    @GetMapping("/api/notifications")
    PagedResponse<NotificationView> getNotifications(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sort") String sort);

    /** Response body of {@code GET /api/notifications/unread-count}. */
    record UnreadCount(long unreadCount) {
    }
}

