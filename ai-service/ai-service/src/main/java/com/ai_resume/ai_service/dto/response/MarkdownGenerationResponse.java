package com.ai_resume.ai_service.dto.response;

public record MarkdownGenerationResponse(

        Long analysisId,

        String markdownCode,

        String engineVersion

) {
}
