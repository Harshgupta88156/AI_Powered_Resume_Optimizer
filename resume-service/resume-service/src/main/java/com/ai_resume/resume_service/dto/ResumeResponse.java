package com.ai_resume.resume_service.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A resume plus a flattened view of its latest version, so existing clients that
 * expect fileName / cloudinaryUrl on the resume keep working.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private Long resumeId;
    private String displayName;
    private String notes;
    private String fileName;
    private String contentType;
    private String cloudinaryUrl;
    private Integer latestVersionNumber;
    private Long latestVersionId;
    private int totalVersions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
