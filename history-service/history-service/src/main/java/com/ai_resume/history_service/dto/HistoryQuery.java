package com.ai_resume.history_service.dto;

import com.ai_resume.history_service.entity.AnalysisStatus;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record HistoryQuery(
        Long resumeId,
        Long resumeVersionId,
        Long jobDescriptionId,
        String company,
        String jobTitle,
        Integer atsMin,
        Integer atsMax,
        Integer matchMin,
        Integer matchMax,
        LocalDate from,
        LocalDate to,
        AnalysisStatus status) {
}

