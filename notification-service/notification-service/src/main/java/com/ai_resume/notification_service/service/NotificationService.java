package com.ai_resume.notification_service.service;

import com.ai_resume.notification_service.dto.AnalysisCompletedEmailRequest;
import com.ai_resume.notification_service.dto.NotificationResponse;
import com.ai_resume.notification_service.dto.WeeklyTrendsEmailRequest;
import com.ai_resume.notification_service.dto.WelcomeEmailRequest;

/** Business-level email notifications. EMAIL ONLY - no in-app/push/SMS. */
public interface NotificationService {

    NotificationResponse sendWelcomeEmail(WelcomeEmailRequest request);

    NotificationResponse sendAnalysisCompletedEmail(AnalysisCompletedEmailRequest request);

    NotificationResponse sendWeeklyTrendsEmail(WeeklyTrendsEmailRequest request);
}

