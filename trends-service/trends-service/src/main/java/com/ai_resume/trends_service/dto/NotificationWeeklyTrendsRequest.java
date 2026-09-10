package com.ai_resume.trends_service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors notification-service's WeeklyTrendsEmailRequest. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationWeeklyTrendsRequest {

    private List<String> recipientEmails;
    private String weekRangeLabel;
    private List<String> trendingSkills;
    private List<String> mostAnalyzedJobRoles;
    private List<String> atsImprovements;
    private List<String> weeklyInsights;
}

