package com.ai_resume.resume_service.service;

import com.ai_resume.resume_service.dto.NotificationAnalysisCompletedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for notification-service, resolved through Eureka - same pattern as
 * {@link AiClientService}.
 *
 * <p>notification-service owns all SMTP logic; resume-service only asks it to
 * send an "analysis completed" email once an analysis reaches COMPLETED.
 * Failures here are always caught by the caller and never break the analysis
 * flow itself (see {@code ResumeService#triggerAnalysis}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClientService {

    private final RestTemplate loadBalancedRestTemplate;

    @Value("${notification.service.base-url}")
    private String notificationServiceBaseUrl;

    @Value("${notification.service.analysis-completed-endpoint}")
    private String analysisCompletedEndpoint;

    public void sendAnalysisCompletedEmail(NotificationAnalysisCompletedRequest request) {
        String url = notificationServiceBaseUrl + analysisCompletedEndpoint;
        try {
            loadBalancedRestTemplate.postForEntity(url, request, Void.class);
        } catch (RestClientException ex) {
            log.warn("Failed to notify notification-service about analysis {}: {}",
                    request.getAnalysisId(), ex.getMessage());
        }
    }
}

