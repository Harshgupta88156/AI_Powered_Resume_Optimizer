package com.ai_resume.dashboard_service.client;

import com.ai_resume.dashboard_service.client.dto.AnalysisView;
import com.ai_resume.dashboard_service.client.dto.PagedResponse;
import com.ai_resume.dashboard_service.client.dto.ResumeView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Reads from resume-service.
 *
 * <p>Only endpoints that ALREADY exist are used — no analytics endpoints were
 * added to resume-service for the dashboard's benefit. Everything the dashboard
 * shows is derived here from these plain, owner-scoped listings.
 *
 * <p>The caller's identity travels via the {@code X-User-Id} header, which
 * {@code FeignConfig} propagates from the inbound request, so resume-service
 * applies exactly the same ownership scoping it applies to a direct call.
 */
@FeignClient(name = "resume-service")
public interface ResumeServiceClient {

    @GetMapping("/api/resumes")
    PagedResponse<ResumeView> getResumes(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sort") String sort);

    @GetMapping("/api/resumes/analyses")
    PagedResponse<AnalysisView> getAnalyses(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sort") String sort);

    /**
     * Only ever called with {@code size=1}: the dashboard needs the job-description
     * total, not the rows, and {@code totalElements} gives that for the price of a
     * single-row page.
     */
    @GetMapping("/api/resumes/job-descriptions")
    PagedResponse<Object> getJobDescriptions(
            @RequestParam("page") int page,
            @RequestParam("size") int size);
}

