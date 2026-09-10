package com.ai_resume.resume_service.controller;

import com.ai_resume.resume_service.dto.AnalysisResponse;
import com.ai_resume.resume_service.dto.AnalysisTriggerRequest;
import com.ai_resume.resume_service.dto.JobDescriptionResponse;
import com.ai_resume.resume_service.dto.JobDescriptionUpdateRequest;
import com.ai_resume.resume_service.dto.LearningResourceResponse;
import com.ai_resume.resume_service.dto.MarkdownResumeResponse;
import com.ai_resume.resume_service.dto.ResumeResponse;
import com.ai_resume.resume_service.dto.ResumeUpdateRequest;
import com.ai_resume.resume_service.dto.ResumeVersionResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ai_resume.resume_service.service.ResumeService;

/**
 * All endpoints are owner-scoped.
 *
 * <p>{@code X-User-Id} is injected by the API gateway after it validates the JWT.
 * It is declared as a required header, so a request that bypassed the gateway is
 * rejected (401) instead of silently operating on everybody's data.
 */
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Slf4j
public class ResumeController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    private final ResumeService resumeService;

    // ------------------------------ resumes ------------------------------
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> uploadResume(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String displayName) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.uploadResume(userId, file, displayName));
    }

    @GetMapping
    public ResponseEntity<Page<ResumeResponse>> getResumes(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(resumeService.getResumes(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(
            @RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(resumeService.getResumeById(userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateResumeMetadata(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ResumeUpdateRequest request) {
        return ResponseEntity.ok(resumeService.updateResumeMetadata(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        resumeService.deleteResume(userId, id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------- resume versions --------------------------
    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeVersionResponse> addResumeVersion(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.addResumeVersion(userId, id, file));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ResumeVersionResponse>> getResumeVersions(
            @RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(resumeService.getResumeVersions(userId, id));
    }

    /**
     * Curated learning resources for an analysis's missing/suggested skills.
     *
     * Served from the database rather than a hardcoded frontend file, so links
     * can be corrected and the catalog extended without a frontend release.
     */
    @GetMapping("/analyses/{id}/learning-resources")
    public ResponseEntity<List<LearningResourceResponse>> getLearningResources(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "6") int limit) {
        return ResponseEntity.ok(resumeService.getLearningResourcesForAnalysis(userId, id, limit));
    }

    // -------------------------- job descriptions -------------------------
    @PostMapping(value = "/job-descriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobDescriptionResponse> uploadJobDescription(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String jobTitle) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.uploadJobDescription(userId, file, text, company, jobTitle));
    }

    @GetMapping("/job-descriptions")
    public ResponseEntity<Page<JobDescriptionResponse>> getJobDescriptions(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(resumeService.getJobDescriptions(userId, pageable));
    }

    @GetMapping("/job-descriptions/{id}")
    public ResponseEntity<JobDescriptionResponse> getJobDescriptionById(
            @RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(resumeService.getJobDescriptionById(userId, id));
    }

    /**
     * Renames or re-edits a job description. See
     * {@link JobDescriptionUpdateRequest} for the null-vs-blank semantics.
     */
    @PutMapping("/job-descriptions/{id}")
    public ResponseEntity<JobDescriptionResponse> updateJobDescription(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody JobDescriptionUpdateRequest request) {
        return ResponseEntity.ok(resumeService.updateJobDescription(userId, id, request));
    }

    @DeleteMapping("/job-descriptions/{id}")
    public ResponseEntity<Void> deleteJobDescription(
            @RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        resumeService.deleteJobDescription(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------ analyses -----------------------------
    /**
     * Returns 202 Accepted: the analysis record is always created, but the result
     * may still be PENDING/FAILED depending on ai-service. Check the {@code status}
     * field rather than assuming success.
     */
    @PostMapping("/analyses")
    public ResponseEntity<AnalysisResponse> triggerAnalysis(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestHeader(value = USER_EMAIL_HEADER, required = false) String userEmail,
            @Valid @RequestBody AnalysisTriggerRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(resumeService.triggerAnalysis(userId, request, userEmail));
    }

    @GetMapping("/analyses")
    public ResponseEntity<Page<AnalysisResponse>> getAnalyses(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) Long resumeVersionId,
            @RequestParam(required = false) Long jobDescriptionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(resumeService.getAnalyses(
                userId, resumeId, resumeVersionId, jobDescriptionId, pageable));
    }

    @GetMapping("/analyses/{id}")
    public ResponseEntity<AnalysisResponse> getAnalysisById(
            @RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(resumeService.getAnalysisById(userId, id));
    }

    /**
     * Generates (or returns the cached) JD-tailored Markdown resume for a
     * COMPLETED analysis, weaving in the missing/suggested skills that
     * analysis identified. Pass {@code ?regenerate=true} to force a fresh
     * call to ai-service instead of the cached result.
     */
    @PostMapping("/analyses/{id}/markdown")
    public ResponseEntity<MarkdownResumeResponse> generateMarkdownResume(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean regenerate) {
        return ResponseEntity.ok(resumeService.generateMarkdownResume(userId, id, regenerate));
    }

    /**
     * Returns a previously generated tailored resume without calling ai-service.
     *
     * The UI hits this on page load so the download buttons appear for a resume
     * that was generated in an earlier session. Returns 404 when nothing has
     * been generated yet, which the client treats as "no cached copy" rather
     * than an error.
     */
    @GetMapping("/analyses/{id}/markdown")
    public ResponseEntity<MarkdownResumeResponse> getMarkdownResume(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(resumeService.getGeneratedMarkdownResume(userId, id));
    }
}
