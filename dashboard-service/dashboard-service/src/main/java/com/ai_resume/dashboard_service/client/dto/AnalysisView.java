package com.ai_resume.dashboard_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Mirror of resume-service's AnalysisResponse.
 *
 * <p>This single record feeds the summary averages, every chart, and every
 * insight — which is why the dashboard fetches analyses exactly once per request
 * and derives all four sections from that one snapshot.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisView {

    private Long analysisId;

    private Long resumeId;
    private String resumeDisplayName;
    private Long resumeVersionId;
    private Integer resumeVersionNumber;

    private Long jobDescriptionId;
    private String company;
    private String jobTitle;

    /** PENDING / PROCESSING / COMPLETED / FAILED — kept as a String so a new
     *  status added upstream can never break deserialization here. */
    private String status;

    private Integer atsScore;
    private Integer matchScore;
    private String overallSummary;

    private List<String> missingSkills = new ArrayList<>();
    private List<String> suggestedSkills = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    private String errorMessage;
    private String engineVersion;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public List<String> getMissingSkills() {
        return missingSkills == null ? List.of() : missingSkills;
    }

    public List<String> getSuggestedSkills() {
        return suggestedSkills == null ? List.of() : suggestedSkills;
    }
}

