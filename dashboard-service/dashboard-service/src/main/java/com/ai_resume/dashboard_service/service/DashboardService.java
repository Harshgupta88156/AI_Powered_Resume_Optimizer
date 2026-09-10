package com.ai_resume.dashboard_service.service;

import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.client.dto.ResumeView;
import com.ai_resume.dashboard_service.dto.ChartGranularity;
import com.ai_resume.dashboard_service.dto.DashboardCharts;
import com.ai_resume.dashboard_service.dto.DashboardInsights;
import com.ai_resume.dashboard_service.dto.DashboardResponse;
import com.ai_resume.dashboard_service.dto.RecentActivity;
import com.ai_resume.dashboard_service.dto.SummaryStatistics;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Assembles the dashboard.
 *
 * <p>Owns no data and no business rules — scoring, ownership and validation all
 * stay in the services that own them. Its single job is to fetch one snapshot and
 * project it into the shape the frontend renders.
 *
 * <p>Each public method loads the snapshot exactly once and derives its section
 * from it, so a call for just the charts never pays for insights it will not use.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int DEFAULT_RANGE_DAYS = 180;
    private static final int MAX_RANGE_DAYS = 1825;   // 5 years
    private static final int MAX_RECENT_LIMIT = 25;
    private static final int MAX_INSIGHT_LIMIT = 50;

    private final DashboardDataLoader dataLoader;
    private final ChartBuilder chartBuilder;
    private final InsightBuilder insightBuilder;

    // =====================================================================
    // Whole page
    // =====================================================================

    public DashboardResponse getDashboard(
            LocalDate from, LocalDate to, ChartGranularity granularity,
            int recentLimit, int insightLimit) {

        DateRange range = resolveRange(from, to);
        ChartGranularity resolvedGranularity =
                granularity == null ? ChartGranularity.WEEK : granularity;
        int recent = clamp(recentLimit, MAX_RECENT_LIMIT, 5);
        int insights = clamp(insightLimit, MAX_INSIGHT_LIMIT, 10);

        DashboardData data = dataLoader.load(recent);

        return DashboardResponse.builder()
                .summary(buildSummary(data))
                .recentActivity(buildRecentActivity(data, recent))
                .charts(chartBuilder.build(data, range.from(), range.to(), resolvedGranularity))
                .insights(insightBuilder.build(data, insights))
                .generatedAt(LocalDateTime.now())
                .degradedSources(data.degradedSources())
                .build();
    }

    // =====================================================================
    // Individual panels
    // =====================================================================

    public SummaryStatistics getSummary() {
        return buildSummary(dataLoader.load(0));
    }

    public RecentActivity getRecentActivity(int limit) {
        int recent = clamp(limit, MAX_RECENT_LIMIT, 5);
        return buildRecentActivity(dataLoader.load(recent), recent);
    }

    public DashboardCharts getCharts(
            LocalDate from, LocalDate to, ChartGranularity granularity) {
        DateRange range = resolveRange(from, to);
        ChartGranularity resolved = granularity == null ? ChartGranularity.WEEK : granularity;
        return chartBuilder.build(dataLoader.load(0), range.from(), range.to(), resolved);
    }

    public DashboardInsights getInsights(int limit) {
        return insightBuilder.build(dataLoader.load(0), clamp(limit, MAX_INSIGHT_LIMIT, 10));
    }

    // =====================================================================
    // Projections
    // =====================================================================

    private SummaryStatistics buildSummary(DashboardData data) {
        List<AnalysisView> completed = data.analyses().stream()
                .filter(AnalysisView::isCompleted)
                .toList();

        return SummaryStatistics.builder()
                .totalResumes(data.totalResumes())
                .totalResumeVersions(data.resumes().stream()
                        .mapToLong(ResumeView::getTotalVersions)
                        .sum())
                .totalAnalyses(data.totalAnalyses())
                .totalJobDescriptions(data.totalJobDescriptions())
                // -1 means notification-service could not be reached; report 0 to the
                // UI but flag the service in degradedSources so it isn't mistaken for
                // "genuinely nothing unread".
                .totalUnreadNotifications(Math.max(data.unreadNotifications(), 0))
                .completedAnalyses(completed.size())
                .averageAtsScore(average(completed, AnalysisView::getAtsScore))
                .averageMatchScore(average(completed, AnalysisView::getMatchScore))
                .build();
    }

    private RecentActivity buildRecentActivity(DashboardData data, int limit) {
        return RecentActivity.builder()
                .recentResumes(takeMostRecent(
                        data.resumes(), ResumeView::getCreatedAt, limit))
                .recentAnalyses(takeMostRecent(
                        data.analyses(), AnalysisView::getCreatedAt, limit))
                .recentNotifications(data.recentNotifications())
                .build();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private <T> List<T> takeMostRecent(
            List<T> items, java.util.function.Function<T, LocalDateTime> timestamp, int limit) {
        return items.stream()
                .sorted(Comparator.comparing(
                        timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private Double average(List<AnalysisView> analyses,
                           java.util.function.Function<AnalysisView, Integer> score) {
        var stats = analyses.stream()
                .map(score)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .summaryStatistics();
        // Null rather than 0 when there is nothing to average — see SummaryStatistics.
        return stats.getCount() == 0 ? null : Math.round(stats.getAverage() * 10.0) / 10.0;
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(DEFAULT_RANGE_DAYS) : from;

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        // Cap the span: DAY granularity over an unbounded range would generate a
        // point per day forever and blow up both this response and the chart.
        if (resolvedFrom.plusDays(MAX_RANGE_DAYS).isBefore(resolvedTo)) {
            throw new IllegalArgumentException(
                    "Date range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private int clamp(int value, int max, int fallback) {
        if (value <= 0) {
            return fallback;
        }
        return Math.min(value, max);
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}

