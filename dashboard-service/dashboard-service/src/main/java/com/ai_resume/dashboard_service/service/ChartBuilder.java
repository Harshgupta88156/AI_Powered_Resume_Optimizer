package com.ai_resume.dashboard_service.service;

import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.client.dto.ResumeView;
import com.ai_resume.dashboard_service.dto.ChartGranularity;
import com.ai_resume.dashboard_service.dto.DashboardCharts;
import com.ai_resume.dashboard_service.dto.TimeSeriesPoint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Turns the snapshot into chart series. Pure functions — no I/O, no state.
 *
 * <p>Buckets are pre-seeded across the whole requested range so that periods with
 * no activity appear as explicit zeros. Skipping empty buckets makes a line chart
 * lie: two points a month apart get drawn adjacent, implying continuous activity.
 */
@Component
public class ChartBuilder {

    public DashboardCharts build(
            DashboardData data, LocalDate from, LocalDate to, ChartGranularity granularity) {

        List<AnalysisView> inRange = data.analyses().stream()
                .filter(a -> withinRange(a.getCreatedAt(), from, to))
                .toList();

        List<ResumeView> resumesInRange = data.resumes().stream()
                .filter(r -> withinRange(r.getCreatedAt(), from, to))
                .toList();

        return DashboardCharts.builder()
                .from(from)
                .to(to)
                .granularity(granularity)
                .weeklyAnalysisCount(
                        countSeries(inRange, AnalysisView::getCreatedAt, from, to, ChartGranularity.WEEK))
                .monthlyAnalysisCount(
                        countSeries(inRange, AnalysisView::getCreatedAt, from, to, ChartGranularity.MONTH))
                .atsImprovement(
                        averageSeries(inRange, AnalysisView::getAtsScore, from, to, granularity))
                .matchImprovement(
                        averageSeries(inRange, AnalysisView::getMatchScore, from, to, granularity))
                .resumeUploadTrend(
                        countSeries(resumesInRange, ResumeView::getCreatedAt, from, to, granularity))
                .build();
    }

    // =====================================================================
    // Series builders
    // =====================================================================

    private <T> List<TimeSeriesPoint> countSeries(
            List<T> items, Function<T, LocalDateTime> timestamp,
            LocalDate from, LocalDate to, ChartGranularity granularity) {

        Map<LocalDate, Double> buckets = emptyBuckets(from, to, granularity);
        for (T item : items) {
            LocalDateTime createdAt = timestamp.apply(item);
            if (createdAt == null) {
                continue;
            }
            LocalDate bucket = bucketOf(createdAt.toLocalDate(), granularity);
            buckets.computeIfPresent(bucket, (k, v) -> v + 1);
        }
        return toPoints(buckets, granularity);
    }

    /**
     * Average of a score per bucket, over COMPLETED analyses only.
     *
     * <p>Buckets with no completed analysis get a null value rather than 0, so the
     * chart shows a gap instead of a phantom crash to zero.
     */
    private List<TimeSeriesPoint> averageSeries(
            List<AnalysisView> analyses, Function<AnalysisView, Integer> score,
            LocalDate from, LocalDate to, ChartGranularity granularity) {

        Map<LocalDate, double[]> sums = new LinkedHashMap<>();
        for (LocalDate bucket : emptyBuckets(from, to, granularity).keySet()) {
            sums.put(bucket, new double[]{0, 0});
        }

        for (AnalysisView analysis : analyses) {
            Integer value = score.apply(analysis);
            if (!analysis.isCompleted() || value == null || analysis.getCreatedAt() == null) {
                continue;
            }
            LocalDate bucket = bucketOf(analysis.getCreatedAt().toLocalDate(), granularity);
            double[] acc = sums.get(bucket);
            if (acc != null) {
                acc[0] += value;
                acc[1] += 1;
            }
        }

        List<TimeSeriesPoint> points = new ArrayList<>(sums.size());
        sums.forEach((bucket, acc) -> points.add(TimeSeriesPoint.builder()
                .date(bucket)
                .label(labelOf(bucket, granularity))
                .value(acc[1] == 0 ? null : round(acc[0] / acc[1]))
                .build()));
        return points;
    }

    // =====================================================================
    // Bucketing
    // =====================================================================

    private Map<LocalDate, Double> emptyBuckets(
            LocalDate from, LocalDate to, ChartGranularity granularity) {
        Map<LocalDate, Double> buckets = new LinkedHashMap<>();
        LocalDate cursor = bucketOf(from, granularity);
        LocalDate last = bucketOf(to, granularity);
        while (!cursor.isAfter(last)) {
            buckets.put(cursor, 0.0);
            cursor = next(cursor, granularity);
        }
        return buckets;
    }

    private LocalDate bucketOf(LocalDate date, ChartGranularity granularity) {
        return switch (granularity) {
            case DAY -> date;
            // Monday of the ISO week, so week buckets line up with the ISO labels.
            case WEEK -> date.minusDays(date.getDayOfWeek().getValue() - 1L);
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate next(LocalDate bucket, ChartGranularity granularity) {
        return switch (granularity) {
            case DAY -> bucket.plusDays(1);
            case WEEK -> bucket.plusWeeks(1);
            case MONTH -> bucket.plusMonths(1);
        };
    }

    private String labelOf(LocalDate bucket, ChartGranularity granularity) {
        return switch (granularity) {
            case DAY -> bucket.toString();
            case WEEK -> "%d-W%02d".formatted(
                    bucket.get(IsoFields.WEEK_BASED_YEAR),
                    bucket.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MONTH -> "%d-%02d".formatted(bucket.getYear(), bucket.getMonthValue());
        };
    }

    private List<TimeSeriesPoint> toPoints(
            Map<LocalDate, Double> buckets, ChartGranularity granularity) {
        List<TimeSeriesPoint> points = new ArrayList<>(buckets.size());
        buckets.forEach((bucket, value) -> points.add(TimeSeriesPoint.builder()
                .date(bucket)
                .label(labelOf(bucket, granularity))
                .value(value)
                .build()));
        return points;
    }

    private boolean withinRange(LocalDateTime timestamp, LocalDate from, LocalDate to) {
        if (timestamp == null) {
            return false;
        }
        LocalDate date = timestamp.toLocalDate();
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}


