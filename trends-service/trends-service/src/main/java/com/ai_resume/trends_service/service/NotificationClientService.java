package com.ai_resume.trends_service.service;

import com.ai_resume.trends_service.dto.NotificationWeeklyTrendsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for notification-service, resolved through Eureka.
 *
 * <p>trends-service only ever sends notification-service the data it has
 * already computed; notification-service never calculates trends itself.
 * Failures are caught here so a notification-service outage never breaks the
 * weekly trends computation job.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClientService {

    private final RestTemplate loadBalancedRestTemplate;

    @Value("${notification.service.base-url:}")
    private String notificationServiceBaseUrl;

    @Value("${notification.service.weekly-trends-endpoint:/api/notifications/weekly-trends}")
    private String weeklyTrendsEndpoint;

    public void sendWeeklyTrendsEmail(NotificationWeeklyTrendsRequest request) {
        String url = notificationServiceBaseUrl + weeklyTrendsEndpoint;
        try {
            loadBalancedRestTemplate.postForEntity(url, request, Void.class);
        } catch (RestClientException ex) {
            log.warn("Failed to notify notification-service about weekly trends: {}", ex.getMessage());
        }
    }
}

