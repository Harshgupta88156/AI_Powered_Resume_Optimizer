package com.ai_resume.ai_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalysisResponse(

        Integer atsScore,

        Integer matchScore,

        String summary,

        List<String> matchingSkills,

        List<String> missingSkills,

        List<String> strengths,

        List<String> weaknesses,

        List<String> suggestedSkills,

        List<String> suggestions,

        String engineVersion

) {
}
