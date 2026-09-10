package com.ai_resume.resume_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUpdateRequest {

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String notes;
}

