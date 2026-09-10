package com.ai_resume.dashboard_service.service;

import com.ai_resume.dashboard_service.client.ResumeServiceClient;
import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.client.dto.NotificationView;
import com.ai_resume.dashboard_service.client.dto.PagedResponse;
import com.ai_resume.dashboard_service.client.dto.ResumeView;
import com.ai_resume.dashboard_service.config.CacheConfig;
import com.ai_resume.dashboard_service.config.FeignConfig;
import com.ai_resume.dashboard_service.exception.UpstreamUnavailableException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Builds the {@link DashboardData} snapshot by calling the OTHER services.
 *
 * <p>This is the only class in dashboard-service that performs I/O. Everything
 * downstream of it is pure computation, which is what keeps the "aggregate, don't
 * duplicate" boundary honest.
 *
 * <h2>Why analyses are fetched in bounded pages</h2>
 * Averages, charts and insights are all functions of the user's analyses. Since
 * resume-service exposes analyses as a paginated list (and dashboard-service owns
 * no tables of its own), they are pulled in pages up to a configurable cap.
 * The cap matters: without it a heavy user would drag their entire history over
 * HTTP on every dashboard load. When the cap is hit, {@code truncated} is set and
 * surfaced to the client, so the numbers are never quietly wrong.
 *
 * <p>If this ever becomes a bottleneck, the fix is to have resume-service expose
 * pre-aggregated projections and to have this loader consume those instead —
 * the response DTOs would not change.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardDataLoader {

    private static final String CREATED_AT_DESC = "createdAt,desc";
    private static final String RESUME_SOURCE = "resume-service";

    private final ResumeServiceClient resumeServiceClient;
    private final NotificationGateway notificationGateway;

    /** Page size used when walking the analysis list. */
    @Value("${dashboard.fetch.page-size:100}")
    private int pageSize;

    /** Hard ceiling on analyses pulled into memory for one dashboard render. */
    @Value("${dashboard.fetch.max-analyses:1000}")
    private int maxAnalyses;

    /** Hard ceiling on resumes pulled into memory. */
    @Value("${dashboard.fetch.max-resumes:500}")
    private int maxResumes;

    /**
     * Loads the snapshot for the current user.
     *
     * <p>Cached per user for a few seconds: the four dashboard endpoints and any
     * impatient reload would otherwise repeat the same fan-out. The key is the
     * caller's id from {@code X-User-Id} — never a shared constant, or one user
     * would be served another user's data out of the cache.
     */
    @Cacheable(cacheNames = CacheConfig.DASHBOARD_CACHE, key = "#root.target.currentUserKey()")
    public DashboardData load(int recentNotificationLimit) {
        List<String> degraded = new ArrayList<>();


        PagedResponse<ResumeView> firstResumePage = fetchResumes(0);
        List<ResumeView> resumes = collectResumes(firstResumePage);

        PagedResponse<AnalysisView> firstAnalysisPage = fetchAnalyses(0);
        List<AnalysisView> analyses = collectAnalyses(firstAnalysisPage);

        long totalJobDescriptions = fetchJobDescriptionCount(degraded);

        long unread = notificationGateway.fetchUnreadCount();
        List<NotificationView> recentNotifications =
                notificationGateway.fetchRecent(recentNotificationLimit);
        if (notificationGateway.isEnabled() && unread < 0) {
            degraded.add(NotificationGateway.SOURCE_NAME);
        }

        boolean truncated = firstAnalysisPage.total() > analyses.size();
        if (truncated) {
            log.debug("Analysis history truncated for dashboard: {} of {} loaded",
                    analyses.size(), firstAnalysisPage.total());
        }

        return new DashboardData(
                resumes,
                analyses,
                firstResumePage.total(),
                firstAnalysisPage.total(),
                totalJobDescriptions,
                unread,
                recentNotifications,
                List.copyOf(degraded),
                truncated);
    }

    // =====================================================================
    // resume-service
    // =====================================================================

    /**
     * Cache key: the authenticated user's id.
     *
     * <p>Public because the SpEL in {@link Cacheable} evaluates it against the
     * target bean. If no request context is bound (e.g. a future scheduled call)
     * it returns a unique key so nothing is ever cached under a shared bucket.
     */
    public String currentUserKey() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            String userId = servletAttributes.getRequest().getHeader(FeignConfig.USER_ID_HEADER);
            if (userId != null && !userId.isBlank()) {
                return userId;
            }
        }
        return "anonymous-" + java.util.UUID.randomUUID();
    }

    private PagedResponse<ResumeView> fetchResumes(int page) {
        try {
            return resumeServiceClient.getResumes(page, pageSize, CREATED_AT_DESC);
        } catch (Exception ex) {
            // resume-service is the dashboard's core dependency. Unlike
            // notifications, there is no meaningful dashboard without it, so this
            // failure is surfaced as 503 rather than silently rendering zeros.
            log.error("resume-service unavailable while loading resumes: {}", ex.getMessage());
            throw new UpstreamUnavailableException(RESUME_SOURCE, ex);
        }
    }

    private PagedResponse<AnalysisView> fetchAnalyses(int page) {
        try {
            return resumeServiceClient.getAnalyses(page, pageSize, CREATED_AT_DESC);
        } catch (Exception ex) {
            log.error("resume-service unavailable while loading analyses: {}", ex.getMessage());
            throw new UpstreamUnavailableException(RESUME_SOURCE, ex);
        }
    }

    private long fetchJobDescriptionCount(List<String> degraded) {
        try {
            // size=1: only totalElements is needed, not the rows.
            return resumeServiceClient.getJobDescriptions(0, 1).total();
        } catch (Exception ex) {
            log.warn("Could not read job description count: {}", ex.getMessage());
            degraded.add(RESUME_SOURCE + ":job-descriptions");
            return 0;
        }
    }

    private List<ResumeView> collectResumes(PagedResponse<ResumeView> firstPage) {
        List<ResumeView> all = new ArrayList<>(firstPage.getContent());
        int totalPages = firstPage.pages();
        for (int page = 1; page < totalPages && all.size() < maxResumes; page++) {
            all.addAll(fetchResumes(page).getContent());
        }
        return all.size() > maxResumes ? all.subList(0, maxResumes) : all;
    }

    private List<AnalysisView> collectAnalyses(PagedResponse<AnalysisView> firstPage) {
        List<AnalysisView> all = new ArrayList<>(firstPage.getContent());
        int totalPages = firstPage.pages();
        for (int page = 1; page < totalPages && all.size() < maxAnalyses; page++) {
            all.addAll(fetchAnalyses(page).getContent());
        }
        return all.size() > maxAnalyses ? all.subList(0, maxAnalyses) : all;
    }
}




