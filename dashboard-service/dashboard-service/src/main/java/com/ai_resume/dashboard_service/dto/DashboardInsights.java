package com.ai_resume.dashboard_service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Top-N insights derived from the user's analyses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardInsights {

    /** Skills most often reported as missing from this user's resumes. */
    private List<LabelCount> topMissingSkills;

    /** Skills most often recommended to this user. */
    private List<LabelCount> topRecommendedSkills;

    /**
     * Skills mentioned most overall (missing + recommended combined) — the
     * technologies that dominate the roles this user targets.
     *
     * <p>Derived from the two skill lists rather than from a hand-maintained
     * technology dictionary, which would be guesswork that rots. When ai-service
     * starts returning a dedicated technologies list, feed it in here; the API
     * shape does not change.
     */
    private List<LabelCount> topTechnologies;

    /** Companies this user has run the most analyses against. */
    private List<LabelCount> mostAnalyzedCompanies;

    /** Job titles this user has run the most analyses against. */
    private List<LabelCount> mostAnalyzedJobTitles;
}

