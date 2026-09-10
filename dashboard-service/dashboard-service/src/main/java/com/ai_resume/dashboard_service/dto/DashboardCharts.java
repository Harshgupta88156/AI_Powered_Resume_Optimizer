package com.ai_resume.dashboard_service.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Every chart series, for one date range. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCharts {

    private LocalDate from;
    private LocalDate to;
    private ChartGranularity granularity;

    /** Analyses per ISO week. */
    private List<TimeSeriesPoint> weeklyAnalysisCount;

    /** Analyses per calendar month. */
    private List<TimeSeriesPoint> monthlyAnalysisCount;

    /** Average ATS score per bucket — the "am I improving?" line. */
    private List<TimeSeriesPoint> atsImprovement;

    /** Average match score per bucket, plotted alongside ATS. */
    private List<TimeSeriesPoint> matchImprovement;

    /** Resumes created per bucket. */
    private List<TimeSeriesPoint> resumeUploadTrend;
}

