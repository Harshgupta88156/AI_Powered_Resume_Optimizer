package com.ai_resume.dashboard_service.controller;

import com.ai_resume.dashboard_service.dto.ChartGranularity;
import com.ai_resume.dashboard_service.dto.DashboardCharts;
import com.ai_resume.dashboard_service.dto.DashboardInsights;
import com.ai_resume.dashboard_service.dto.DashboardResponse;
import com.ai_resume.dashboard_service.dto.RecentActivity;
import com.ai_resume.dashboard_service.dto.SummaryStatistics;
import com.ai_resume.dashboard_service.service.DashboardService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontend-facing dashboard API.
 *
 * <p>{@code X-User-Id} is injected by the API gateway after it validates the JWT
 * and is declared required, so a request that bypassed the gateway is rejected
 * with 401 rather than served with someone else's data. The header is not used
 * directly here — it is propagated to downstream services, which each apply their
 * own ownership scoping.
 *
 * <p>{@code GET /api/dashboard} returns the whole page in one request; the
 * per-panel endpoints exist for partial refreshes (e.g. changing the chart range
 * shouldn't refetch insights).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final DashboardService dashboardService;

    /** Everything: summary + recent activity + charts + insights. */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChartGranularity granularity,
            @RequestParam(defaultValue = "5") int recentLimit,
            @RequestParam(defaultValue = "10") int insightLimit) {
        return ResponseEntity.ok(dashboardService.getDashboard(
                from, to, granularity, recentLimit, insightLimit));
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryStatistics> getSummary(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<RecentActivity> getRecentActivity(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentActivity(limit));
    }

    @GetMapping("/charts")
    public ResponseEntity<DashboardCharts> getCharts(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ChartGranularity granularity) {
        return ResponseEntity.ok(dashboardService.getCharts(from, to, granularity));
    }

    @GetMapping("/insights")
    public ResponseEntity<DashboardInsights> getInsights(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getInsights(limit));
    }
}

