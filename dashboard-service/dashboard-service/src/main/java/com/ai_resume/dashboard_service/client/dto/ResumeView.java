package com.ai_resume.dashboard_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.Data;

/** Mirror of resume-service's ResumeResponse. Read-only view; no behaviour. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeView {

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

