package com.ai_resume.dashboard_service.service;

import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.dto.DashboardInsights;
import com.ai_resume.dashboard_service.dto.LabelCount;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Turns the snapshot into "top N" insight lists. Pure functions — no I/O.
 *
 * <p>Labels are counted case-insensitively but reported using the first spelling
 * seen. Without this, "React", "react" and "REACT" would occupy three separate
 * slots in a top-10 and crowd out genuinely different skills.
 */
@Component
public class InsightBuilder {

    public DashboardInsights build(DashboardData data, int limit) {
        List<AnalysisView> analyses = data.analyses();

        return DashboardInsights.builder()
                .topMissingSkills(topLabels(
                        analyses.stream().flatMap(a -> a.getMissingSkills().stream()), limit))
                .topRecommendedSkills(topLabels(
                        analyses.stream().flatMap(a -> a.getSuggestedSkills().stream()), limit))
                .topTechnologies(topLabels(
                        analyses.stream().flatMap(a -> Stream.concat(
                                a.getMissingSkills().stream(),
                                a.getSuggestedSkills().stream())), limit))
                .mostAnalyzedCompanies(topLabels(
                        analyses.stream().map(AnalysisView::getCompany), limit))
                .mostAnalyzedJobTitles(topLabels(
                        analyses.stream().map(AnalysisView::getJobTitle), limit))
                .build();
    }

    private List<LabelCount> topLabels(Stream<String> values, int limit) {
        Map<String, long[]> counts = new HashMap<>();
        Map<String, String> displayNames = new HashMap<>();

        values.filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .forEach(value -> {
                    String key = value.toLowerCase();
                    displayNames.putIfAbsent(key, value);
                    counts.computeIfAbsent(key, k -> new long[1])[0]++;
                });

        List<LabelCount> result = new ArrayList<>(counts.size());
        counts.forEach((key, count) -> result.add(LabelCount.builder()
                .label(displayNames.get(key))
                .count(count[0])
                .build()));

        result.sort(Comparator
                .comparingLong(LabelCount::getCount).reversed()
                // Alphabetical tie-break keeps the order stable between reloads;
                // otherwise equal-count entries would shuffle on every request.
                .thenComparing(LabelCount::getLabel, Comparator.nullsLast(String::compareToIgnoreCase)));

        return result.size() > limit ? List.copyOf(result.subList(0, limit)) : List.copyOf(result);
    }

    /** Exposed for reuse if a future panel needs a top-N over some other field. */
    public <T> List<LabelCount> topBy(List<T> items, Function<T, String> extractor, int limit) {
        return topLabels(items.stream().map(extractor), limit);
    }
}

