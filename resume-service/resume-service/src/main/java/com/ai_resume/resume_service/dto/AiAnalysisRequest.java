package com.ai_resume.resume_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request contract for {@code POST /api/ai/analyze} on ai-service.
 *
 * <p>ai-service is not implemented yet; this class is the agreed contract it
 * must accept. Company / job title are passed through so the AI can tailor its
 * wording without resume-service having to re-parse the JD.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisRequest {

    private Long analysisId;
    private String resumeText;
    private String jobDescriptionText;
    private String company;
    private String jobTitle;
}
