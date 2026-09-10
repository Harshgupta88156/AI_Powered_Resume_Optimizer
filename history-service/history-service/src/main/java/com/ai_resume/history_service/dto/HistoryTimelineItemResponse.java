package com.ai_resume.history_service.dto;

import com.ai_resume.history_service.entity.AnalysisStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * Timeline-focused analysis row. Keeps relation ids and display fields without
 * duplicating full resume/JD payloads.
 */
@Builder
public record HistoryTimelineItemResponse(
        Long analysisId,
        ResumeRef resume,
        ResumeVersionRef resumeVersion,
        JobDescriptionRef jobDescription,
        AnalysisStatus status,
        Integer atsScore,
        Integer matchScore,
        String overallSummary,
        List<String> missingSkills,
        List<String> suggestedSkills,
        List<String> suggestions,
        String errorMessage,
        String engineVersion,
        LocalDateTime analysisDate,
        LocalDateTime completedAt) {

    @Builder
    public record ResumeRef(Long resumeId, String displayName) {
    }

    @Builder
    public record ResumeVersionRef(Long resumeVersionId, Integer versionNumber) {
    }

    @Builder
    public record JobDescriptionRef(Long jobDescriptionId, String company, String jobTitle) {
    }
}

