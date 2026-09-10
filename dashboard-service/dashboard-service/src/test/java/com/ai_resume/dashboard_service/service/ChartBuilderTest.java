package com.ai_resume.dashboard_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.client.dto.ResumeView;
import com.ai_resume.dashboard_service.dto.ChartGranularity;
import com.ai_resume.dashboard_service.dto.DashboardCharts;
import com.ai_resume.dashboard_service.dto.TimeSeriesPoint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChartBuilderTest {

    private final ChartBuilder chartBuilder = new ChartBuilder();

    @Test
    void bucketsAnalysesByMonthAndSeedsEmptyMonthsWithZero() {
        DashboardData data = snapshot(
                List.of(
                        analysis("COMPLETED", 60, 50, LocalDateTime.of(2026, 1, 10, 9, 0)),
                        analysis("COMPLETED", 80, 70, LocalDateTime.of(2026, 1, 20, 9, 0)),
                        analysis("COMPLETED", 90, 90, LocalDateTime.of(2026, 3, 5, 9, 0))),
                List.of());

        DashboardCharts charts = chartBuilder.build(
                data, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), ChartGranularity.MONTH);

        List<TimeSeriesPoint> monthly = charts.getMonthlyAnalysisCount();
        assertThat(monthly).hasSize(3);
        assertThat(monthly.get(0).getLabel()).isEqualTo("2026-01");
        assertThat(monthly.get(0).getValue()).isEqualTo(2.0);
        // February had no activity: it must still appear, as an explicit zero.
        assertThat(monthly.get(1).getLabel()).isEqualTo("2026-02");
        assertThat(monthly.get(1).getValue()).isEqualTo(0.0);
        assertThat(monthly.get(2).getValue()).isEqualTo(1.0);
    }

    @Test
    void averagesOnlyCompletedAnalysesAndLeavesEmptyBucketsNull() {
        DashboardData data = snapshot(
                List.of(
                        analysis("COMPLETED", 60, 40, LocalDateTime.of(2026, 1, 10, 9, 0)),
                        analysis("COMPLETED", 80, 60, LocalDateTime.of(2026, 1, 20, 9, 0)),
                        // FAILED analyses carry no score and must not drag the average down.
                        analysis("FAILED", null, null, LocalDateTime.of(2026, 1, 25, 9, 0))),
                List.of());

        DashboardCharts charts = chartBuilder.build(
                data, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28), ChartGranularity.MONTH);

        List<TimeSeriesPoint> ats = charts.getAtsImprovement();
        assertThat(ats.get(0).getValue()).isEqualTo(70.0);
        assertThat(charts.getMatchImprovement().get(0).getValue()).isEqualTo(50.0);
        // No completed analysis in February -> gap, not a phantom drop to zero.
        assertThat(ats.get(1).getValue()).isNull();
    }

    @Test
    void weekBucketsStartOnMonday() {
        // 2026-07-31 is a Friday; its ISO week starts Monday 2026-07-27.
        DashboardData data = snapshot(
                List.of(analysis("COMPLETED", 70, 70, LocalDateTime.of(2026, 7, 31, 9, 0))),
                List.of());

        DashboardCharts charts = chartBuilder.build(
                data, LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2), ChartGranularity.WEEK);

        List<TimeSeriesPoint> weekly = charts.getWeeklyAnalysisCount();
        assertThat(weekly).hasSize(1);
        assertThat(weekly.get(0).getDate()).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(weekly.get(0).getValue()).isEqualTo(1.0);
    }

    @Test
    void excludesActivityOutsideTheRequestedRange() {
        DashboardData data = snapshot(
                List.of(analysis("COMPLETED", 70, 70, LocalDateTime.of(2025, 12, 31, 9, 0))),
                List.of(resume(LocalDateTime.of(2026, 1, 15, 9, 0))));

        DashboardCharts charts = chartBuilder.build(
                data, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), ChartGranularity.MONTH);

        assertThat(charts.getMonthlyAnalysisCount().get(0).getValue()).isEqualTo(0.0);
        assertThat(charts.getResumeUploadTrend().get(0).getValue()).isEqualTo(1.0);
    }

    // ------------------------------ fixtures ------------------------------

    private DashboardData snapshot(List<AnalysisView> analyses, List<ResumeView> resumes) {
        return new DashboardData(
                resumes, analyses, resumes.size(), analyses.size(), 0, -1,
                List.of(), List.of(), false);
    }

    private AnalysisView analysis(String status, Integer ats, Integer match, LocalDateTime createdAt) {
        AnalysisView view = new AnalysisView();
        view.setStatus(status);
        view.setAtsScore(ats);
        view.setMatchScore(match);
        view.setCreatedAt(createdAt);
        return view;
    }

    private ResumeView resume(LocalDateTime createdAt) {
        ResumeView view = new ResumeView();
        view.setCreatedAt(createdAt);
        return view;
    }
}

