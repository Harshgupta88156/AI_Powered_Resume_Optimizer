package com.ai_resume.resume_service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request contract for {@code POST /api/ai/generate-markdown} on ai-service.
 * Mirrors {@link AiAnalysisRequest} plus the skill gaps identified by a prior
 * analysis, so the AI can weave them into the regenerated resume.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkdownGenerationRequest {

    private Long analysisId;
    private String resumeText;
    private String jobDescriptionText;
    private String company;
    private String jobTitle;
    private List<String> missingSkills;
    private List<String> suggestedSkills;
}
