package com.ai_resume.resume_service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors notification-service's AnalysisCompletedEmailRequest. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAnalysisCompletedRequest {

    private String recipientEmail;
    private String userName;
    private String resumeName;
    private String jobDescriptionName;
    private Integer atsScore;
    private Integer matchScore;
    private Long analysisId;
    private String overallSummary;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private List<String> suggestedSkills;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
}
