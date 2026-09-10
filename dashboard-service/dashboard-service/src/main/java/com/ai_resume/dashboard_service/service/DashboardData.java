package com.ai_resume.dashboard_service.service;

import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.client.dto.NotificationView;
import com.ai_resume.dashboard_service.client.dto.ResumeView;
import java.util.List;

/**
 * Immutable snapshot of everything the dashboard needs, fetched ONCE per request.
 *
 * <p>This type exists to enforce the "no duplicate queries" rule structurally:
 * summary, charts and insights are all pure functions of this object, so they
 * physically cannot issue their own calls. Adding a fifth panel later means
 * reading from this snapshot, not adding a fifth fan-out.
 *
 * @param resumes             the user's resumes (bounded page)
 * @param analyses            the user's analyses (bounded page) — feeds averages,
 *                            all charts and all insights
 * @param totalResumes        authoritative count from resume-service
 * @param totalAnalyses       authoritative count from resume-service
 * @param totalJobDescriptions authoritative count from resume-service
 * @param unreadNotifications -1 when notification-service could not be reached
 * @param recentNotifications empty when notification-service could not be reached
 * @param degradedSources     services that failed or were unavailable
 * @param truncated           true when the user has more analyses than were
 *                            fetched, so derived figures cover a recent window
 *                            rather than all time
 */
public record DashboardData(
        List<ResumeView> resumes,
        List<AnalysisView> analyses,
        long totalResumes,
        long totalAnalyses,
        long totalJobDescriptions,
        long unreadNotifications,
        List<NotificationView> recentNotifications,
        List<String> degradedSources,
        boolean truncated) {

    public boolean hasNotifications() {
        return unreadNotifications >= 0;
    }
}

