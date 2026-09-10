package com.ai_resume.history_service.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record AnalysisComparisonResponse(
        HistoryTimelineItemResponse left,
        HistoryTimelineItemResponse right,
        ScoreDelta delta,
        SkillDelta missingSkills,
        SkillDelta suggestedSkills) {

    @Builder
    public record ScoreDelta(Integer atsDelta, Integer matchDelta) {
    }

    @Builder
    public record SkillDelta(List<String> added, List<String> removed) {
    }
}

