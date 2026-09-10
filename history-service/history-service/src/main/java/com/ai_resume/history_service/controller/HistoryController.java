package com.ai_resume.history_service.controller;

import com.ai_resume.history_service.dto.AnalysisComparisonResponse;
import com.ai_resume.history_service.dto.HistoryQuery;
import com.ai_resume.history_service.dto.HistoryTimelineItemResponse;
import com.ai_resume.history_service.entity.AnalysisStatus;
import com.ai_resume.history_service.service.HistoryService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final HistoryService historyService;

    @GetMapping
    public ResponseEntity<Page<HistoryTimelineItemResponse>> getHistory(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) Long resumeVersionId,
            @RequestParam(required = false) Long jobDescriptionId,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) Integer atsMin,
            @RequestParam(required = false) Integer atsMax,
            @RequestParam(required = false) Integer matchMin,
            @RequestParam(required = false) Integer matchMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AnalysisStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        HistoryQuery query = HistoryQuery.builder()
                .resumeId(resumeId)
                .resumeVersionId(resumeVersionId)
                .jobDescriptionId(jobDescriptionId)
                .company(company)
                .jobTitle(jobTitle)
                .atsMin(atsMin)
                .atsMax(atsMax)
                .matchMin(matchMin)
                .matchMax(matchMax)
                .from(from)
                .to(to)
                .status(status)
                .build();

        return ResponseEntity.ok(historyService.getHistory(userId, query, pageable));
    }

    @GetMapping("/resumes/{resumeId}")
    public ResponseEntity<Page<HistoryTimelineItemResponse>> getByResume(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long resumeId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        HistoryQuery query = HistoryQuery.builder().resumeId(resumeId).build();
        return ResponseEntity.ok(historyService.getHistory(userId, query, pageable));
    }

    @GetMapping("/resume-versions/{resumeVersionId}")
    public ResponseEntity<Page<HistoryTimelineItemResponse>> getByResumeVersion(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long resumeVersionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        HistoryQuery query = HistoryQuery.builder().resumeVersionId(resumeVersionId).build();
        return ResponseEntity.ok(historyService.getHistory(userId, query, pageable));
    }

    @GetMapping("/job-descriptions/{jobDescriptionId}")
    public ResponseEntity<Page<HistoryTimelineItemResponse>> getByJobDescription(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long jobDescriptionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        HistoryQuery query = HistoryQuery.builder().jobDescriptionId(jobDescriptionId).build();
        return ResponseEntity.ok(historyService.getHistory(userId, query, pageable));
    }

    @GetMapping("/compare")
    public ResponseEntity<AnalysisComparisonResponse> compareAnalyses(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam Long leftAnalysisId,
            @RequestParam Long rightAnalysisId) {
        return ResponseEntity.ok(historyService.compare(userId, leftAnalysisId, rightAnalysisId));
    }
}

