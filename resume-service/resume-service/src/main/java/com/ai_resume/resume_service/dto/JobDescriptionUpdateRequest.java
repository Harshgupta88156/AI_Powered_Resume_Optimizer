package com.ai_resume.resume_service.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Edits to an existing job description.
 *
 * A null field means "leave as is"; a blank string means "clear it". That lets
 * the rename dialog send only the title without wiping the company.
 *
 * The uploaded file itself is never replaced here — swapping the file would
 * invalidate every analysis already run against this JD. Delete and re-upload
 * instead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDescriptionUpdateRequest {

    @Size(max = 200, message = "Company must be at most 200 characters")
    private String company;

    @Size(max = 200, message = "Job title must be at most 200 characters")
    private String jobTitle;

    /**
     * Replacement text. Only honoured for TEXT-sourced job descriptions —
     * for an uploaded file the text is what was parsed out of the document,
     * and letting it drift from the file would be misleading.
     */
    private String text;
}
