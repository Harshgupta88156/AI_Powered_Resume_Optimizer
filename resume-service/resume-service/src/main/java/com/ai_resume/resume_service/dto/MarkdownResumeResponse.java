package com.ai_resume.resume_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkdownResumeResponse {

    private Long analysisId;

    private String markdownCode;

    private String engineVersion;
}
