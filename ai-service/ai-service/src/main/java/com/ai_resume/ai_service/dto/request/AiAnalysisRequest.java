package com.ai_resume.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiAnalysisRequest(

        @NotNull(message = "Analysis id is required")
        Long analysisId,

        @NotBlank(message = "Resume text cannot be empty")
        String resumeText,

        @NotBlank(message = "Job description text cannot be empty")
        String jobDescriptionText,

        String company,

        String jobTitle

) {
}
