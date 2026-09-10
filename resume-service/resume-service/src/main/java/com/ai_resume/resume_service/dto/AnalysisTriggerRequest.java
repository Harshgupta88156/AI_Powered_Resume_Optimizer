package com.ai_resume.resume_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTriggerRequest {

    @NotNull(message = "Resume id is required")
    private Long resumeId;

    /**
     * Optional. When omitted the latest version of the resume is analysed —
     * which is what a client that doesn't care about versioning wants.
     */
    private Long resumeVersionId;

    @NotNull(message = "Job description id is required")
    private Long jobDescriptionId;
}
