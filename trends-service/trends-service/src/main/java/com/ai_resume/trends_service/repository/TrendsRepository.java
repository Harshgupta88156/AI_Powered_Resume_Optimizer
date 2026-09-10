package com.ai_resume.trends_service.repository;

import com.ai_resume.trends_service.dto.LabelCount;
import com.ai_resume.trends_service.dto.TrendGranularity;
import java.time.LocalDateTime;
import java.util.List;

public interface TrendsRepository {

    List<LabelCount> topMissingSkills(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit);

    List<LabelCount> topSuggestedSkills(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit);

    List<LabelCount> topTechnologies(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit);

    List<LabelCount> topCompanies(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit);

    List<LabelCount> topJobTitles(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit);

    List<UploadTrendRow> uploadActivity(LocalDateTime fromTs, LocalDateTime toExclusiveTs, TrendGranularity granularity);
}

