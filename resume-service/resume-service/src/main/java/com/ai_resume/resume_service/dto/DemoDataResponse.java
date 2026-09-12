package com.ai_resume.resume_service.dto;

public record DemoDataResponse(
        int resumesCreated,
        int jobDescriptionsCreated,
        int analysesCreated,
        boolean alreadyPresent
) {
}
