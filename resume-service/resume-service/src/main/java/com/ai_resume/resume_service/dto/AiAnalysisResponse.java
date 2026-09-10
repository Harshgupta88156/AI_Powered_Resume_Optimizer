package com.ai_resume.resume_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response contract that ai-service's {@code POST /api/ai/analyze} must return.
 *
 * <p>ai-service is not built yet — this is the schema it has to honour. Keeping
 * it here (rather than parsing a free-form blob) means the day ai-service ships,
 * resume-service needs no changes at all.
 *
 * <pre>
 * {
 *   "atsScore": 78,
 *   "matchScore": 64,
 *   "summary": "Strong backend match, missing cloud experience.",
 *   "matchingSkills": ["Java", "Spring Boot"],
 *   "missingSkills": ["Kubernetes", "Terraform"],
 *   "strengths": ["Strong API design", "Clean layered architecture"],
 *   "weaknesses": ["No Kubernetes exposure"],
 *   "suggestedSkills": ["Docker", "AWS"],
 *   "suggestions": ["Quantify impact in the last role", "Add a skills section"],
 *   "engineVersion": "gemini-1.5-pro/prompt-v1"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiAnalysisResponse {

    /** 0-100 ATS friendliness of the resume. */
    private Integer atsScore;

    /** 0-100 semantic match between the resume and the job description. */
    private Integer matchScore;

    private String summary;

    private List<String> matchingSkills;

    private List<String> missingSkills;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> suggestedSkills;

    private List<String> suggestions;

    /** Model + prompt version, so old analyses can be identified and re-run. */
    private String engineVersion;
}
