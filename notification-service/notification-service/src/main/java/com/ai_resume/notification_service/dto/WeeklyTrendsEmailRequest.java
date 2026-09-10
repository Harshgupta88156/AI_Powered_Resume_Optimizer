package com.ai_resume.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent by trends-service's weekly scheduler. This DTO carries data trends-service
 * has already computed - notification-service never calculates trends itself,
 * it only renders and sends the email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTrendsEmailRequest {

    @NotEmpty
    private List<@Email String> recipientEmails;

    /** e.g. "Aug 4 - Aug 10, 2026" */
    @NotBlank
    private String weekRangeLabel;

    private List<String> trendingSkills;
    private List<String> mostAnalyzedJobRoles;
    private List<String> atsImprovements;
    private List<String> weeklyInsights;
}

