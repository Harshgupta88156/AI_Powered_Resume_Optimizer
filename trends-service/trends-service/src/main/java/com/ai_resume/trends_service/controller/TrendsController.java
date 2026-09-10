package com.ai_resume.trends_service.controller;

import com.ai_resume.trends_service.dto.LabelCount;
import com.ai_resume.trends_service.dto.TimeSeriesPoint;
import com.ai_resume.trends_service.dto.TrendGranularity;
import com.ai_resume.trends_service.service.TrendsService;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
@Validated
public class TrendsController {

    private final TrendsService trendsService;

    @GetMapping("/missing-skills")
    public ResponseEntity<List<LabelCount>> getMissingSkills(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Integer limit) {
        return ResponseEntity.ok(trendsService.getTopMissingSkills(from, to, limit));
    }

    @GetMapping("/suggested-skills")
    public ResponseEntity<List<LabelCount>> getSuggestedSkills(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Integer limit) {
        return ResponseEntity.ok(trendsService.getTopSuggestedSkills(from, to, limit));
    }

    @GetMapping("/technologies")
    public ResponseEntity<List<LabelCount>> getTechnologies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Integer limit) {
        return ResponseEntity.ok(trendsService.getTopTechnologies(from, to, limit));
    }

    @GetMapping("/companies")
    public ResponseEntity<List<LabelCount>> getCompanies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Integer limit) {
        return ResponseEntity.ok(trendsService.getTopCompanies(from, to, limit));
    }

    @GetMapping("/job-titles")
    public ResponseEntity<List<LabelCount>> getJobTitles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Integer limit) {
        return ResponseEntity.ok(trendsService.getTopJobTitles(from, to, limit));
    }

    @GetMapping("/upload-activity")
    public ResponseEntity<List<TimeSeriesPoint>> getUploadActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TrendGranularity granularity) {
        return ResponseEntity.ok(trendsService.getUploadActivity(from, to, granularity));
    }
}

