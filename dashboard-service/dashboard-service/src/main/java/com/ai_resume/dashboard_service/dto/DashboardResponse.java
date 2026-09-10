package com.ai_resume.dashboard_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The whole dashboard in one response — the payload {@code GET /api/dashboard}
 * returns, so the frontend renders the entire page with a single request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private SummaryStatistics summary;
    private RecentActivity recentActivity;
    private DashboardCharts charts;
    private DashboardInsights insights;

    /** When this snapshot was assembled (it may have been served from cache). */
    private LocalDateTime generatedAt;

    /**
     * Services that could not be reached while building this response, e.g.
     * "notification-service". The page still renders; the frontend can show a
     * subtle "notifications unavailable" hint instead of the whole dashboard
     * failing because one dependency is down.
     */
    private List<String> degradedSources;
}

