package com.ai_resume.resume_service.dto;

import com.ai_resume.resume_service.entity.AnalysisStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full analysis record. Shaped to be exactly what the future History and
 * Dashboard features render, so those services can reuse it verbatim.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {

    private Long analysisId;

    private Long resumeId;
    private String resumeDisplayName;
    private Long resumeVersionId;
    private Integer resumeVersionNumber;

    private Long jobDescriptionId;
    private String company;
    private String jobTitle;

    private AnalysisStatus status;
    private Integer atsScore;
    private Integer matchScore;
    private String overallSummary;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestedSkills;
    private List<String> suggestions;

    private String errorMessage;
    private String engineVersion;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
