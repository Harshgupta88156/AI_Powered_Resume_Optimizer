package com.ai_resume.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent by resume-service once ai-service has finished analysing a resume
 * against a job description. Never sent before the analysis is COMPLETED.
 *
 * Now carries the full analysis report so the email can show all details
 * inline, rather than just linking to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisCompletedEmailRequest {

    @NotBlank
    @Email
    private String recipientEmail;

    @NotBlank
    private String userName;

    @NotBlank
    private String resumeName;

    @NotBlank
    private String jobDescriptionName;

    /** May be null if ai-service did not return a score. */
    private Integer atsScore;

    /** Match score against the job description. */
    private Integer matchScore;

    @NotNull
    private Long analysisId;

    /** Overall summary from the analysis. */
    private String overallSummary;

    /** Skills the resume already has that match the JD. */
    private List<String> matchingSkills;

    /** Skills the JD requires but the resume lacks. */
    private List<String> missingSkills;

    /** Additional skills suggested to add. */
    private List<String> suggestedSkills;

    /** Strengths identified in the resume. */
    private List<String> strengths;

    /** Weaknesses identified in the resume. */
    private List<String> weaknesses;

    /** Actionable suggestions for improvement. */
    private List<String> suggestions;
}
