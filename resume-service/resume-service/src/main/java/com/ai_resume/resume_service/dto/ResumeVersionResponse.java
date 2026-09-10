package com.ai_resume.resume_service.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeVersionResponse {

    private Long resumeVersionId;
    private Long resumeId;
    private Integer versionNumber;
    private String fileName;
    private String contentType;
    private Long fileSizeBytes;
    private String cloudinaryUrl;
    private LocalDateTime createdAt;
}

