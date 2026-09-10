package com.ai_resume.notification_service.service.impl;

import com.ai_resume.notification_service.dto.AnalysisCompletedEmailRequest;
import com.ai_resume.notification_service.dto.NotificationResponse;
import com.ai_resume.notification_service.dto.WeeklyTrendsEmailRequest;
import com.ai_resume.notification_service.dto.WelcomeEmailRequest;
import com.ai_resume.notification_service.service.EmailDispatcher;
import com.ai_resume.notification_service.service.NotificationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds the template variables for each email type and hands the actual send
 * off to {@link EmailDispatcher} (async), so the caller's request always
 * returns immediately. Contains no SMTP/provider-specific logic.
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final String WELCOME_TEMPLATE = "welcome";
    private static final String ANALYSIS_COMPLETED_TEMPLATE = "analysis-completed";
    private static final String WEEKLY_TRENDS_TEMPLATE = "weekly-trends";

    private final EmailDispatcher emailDispatcher;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public NotificationServiceImpl(EmailDispatcher emailDispatcher) {
        this.emailDispatcher = emailDispatcher;
    }

    @Override
    public NotificationResponse sendWelcomeEmail(WelcomeEmailRequest request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", request.getUserName());
        variables.put("startOptimizingUrl", frontendBaseUrl + "/dashboard");

        emailDispatcher.dispatch(
                request.getRecipientEmail(),
                "Welcome to Resume Optimizer!",
                WELCOME_TEMPLATE,
                variables);

        return queuedResponse("welcome");
    }

    @Override
    public NotificationResponse sendAnalysisCompletedEmail(AnalysisCompletedEmailRequest request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", request.getUserName());
        variables.put("resumeName", request.getResumeName());
        variables.put("jobDescriptionName", request.getJobDescriptionName());
        variables.put("atsScore", request.getAtsScore());
        variables.put("matchScore", request.getMatchScore());
        variables.put("overallSummary", request.getOverallSummary());
        variables.put("matchingSkills", nullSafe(request.getMatchingSkills()));
        variables.put("missingSkills", nullSafe(request.getMissingSkills()));
        variables.put("suggestedSkills", nullSafe(request.getSuggestedSkills()));
        variables.put("strengths", nullSafe(request.getStrengths()));
        variables.put("weaknesses", nullSafe(request.getWeaknesses()));
        variables.put("suggestions", nullSafe(request.getSuggestions()));
        variables.put("reportUrl", frontendBaseUrl + "/analyses/" + request.getAnalysisId());

        emailDispatcher.dispatch(
                request.getRecipientEmail(),
                "Your resume analysis is ready",
                ANALYSIS_COMPLETED_TEMPLATE,
                variables);

        return queuedResponse("analysis-completed");
    }

    @Override
    public NotificationResponse sendWeeklyTrendsEmail(WeeklyTrendsEmailRequest request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("weekRangeLabel", request.getWeekRangeLabel());
        variables.put("trendingSkills", nullSafe(request.getTrendingSkills()));
        variables.put("mostAnalyzedJobRoles", nullSafe(request.getMostAnalyzedJobRoles()));
        variables.put("atsImprovements", nullSafe(request.getAtsImprovements()));
        variables.put("weeklyInsights", nullSafe(request.getWeeklyInsights()));
        variables.put("dashboardUrl", frontendBaseUrl + "/dashboard");

        for (String recipient : request.getRecipientEmails()) {
            emailDispatcher.dispatch(
                    recipient,
                    "Your weekly resume trends - " + request.getWeekRangeLabel(),
                    WEEKLY_TRENDS_TEMPLATE,
                    variables);
        }

        return queuedResponse("weekly-trends");
    }

    private List<String> nullSafe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private NotificationResponse queuedResponse(String type) {
        return NotificationResponse.builder()
                .success(true)
                .message("'" + type + "' email accepted and queued for delivery")
                .build();
    }
}


