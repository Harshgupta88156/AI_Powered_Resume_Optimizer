package com.ai_resume.history_service.service;

import com.ai_resume.history_service.dto.AnalysisComparisonResponse;
import com.ai_resume.history_service.dto.HistoryQuery;
import com.ai_resume.history_service.dto.HistoryTimelineItemResponse;
import com.ai_resume.history_service.entity.ResumeAnalysis;
import com.ai_resume.history_service.exception.ResourceNotFoundException;
import com.ai_resume.history_service.repository.ResumeAnalysisRepository;
import com.ai_resume.history_service.spec.AnalysisSpecifications;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;

    @Transactional(readOnly = true)
    public Page<HistoryTimelineItemResponse> getHistory(Long userId, HistoryQuery query, Pageable pageable) {
        validateQuery(query);

        // Newer Spring Data JPA versions throw IllegalArgumentException ("Other specification
        // must not be null") if Specification.and(null) is called. Most of these filters are
        // optional and legitimately return null when unset, so combine them with a null-safe
        // helper instead of chaining .and(...) directly.
        Specification<ResumeAnalysis> spec = AnalysisSpecifications.ownerIs(userId);
        spec = and(spec, AnalysisSpecifications.resumeIdEquals(query.resumeId()));
        spec = and(spec, AnalysisSpecifications.resumeVersionIdEquals(query.resumeVersionId()));
        spec = and(spec, AnalysisSpecifications.jobDescriptionIdEquals(query.jobDescriptionId()));
        spec = and(spec, AnalysisSpecifications.companyContains(query.company()));
        spec = and(spec, AnalysisSpecifications.jobTitleContains(query.jobTitle()));
        spec = and(spec, AnalysisSpecifications.atsScoreBetween(query.atsMin(), query.atsMax()));
        spec = and(spec, AnalysisSpecifications.matchScoreBetween(query.matchMin(), query.matchMax()));
        spec = and(spec, AnalysisSpecifications.createdBetween(query.from(), query.to()));
        spec = and(spec, AnalysisSpecifications.statusEquals(query.status()));

        return resumeAnalysisRepository.findAll(spec, pageable).map(this::toTimelineItem);
    }

    private static Specification<ResumeAnalysis> and(
            Specification<ResumeAnalysis> base, Specification<ResumeAnalysis> addition) {
        if (addition == null) {
            return base;
        }
        return base == null ? addition : base.and(addition);
    }

    @Transactional(readOnly = true)
    public AnalysisComparisonResponse compare(Long userId, Long leftAnalysisId, Long rightAnalysisId) {
        if (leftAnalysisId == null || rightAnalysisId == null) {
            throw new IllegalArgumentException("Both leftAnalysisId and rightAnalysisId are required");
        }

        ResumeAnalysis left = findOwnedAnalysis(userId, leftAnalysisId);
        ResumeAnalysis right = findOwnedAnalysis(userId, rightAnalysisId);

        return AnalysisComparisonResponse.builder()
                .left(toTimelineItem(left))
                .right(toTimelineItem(right))
                .delta(AnalysisComparisonResponse.ScoreDelta.builder()
                        .atsDelta(delta(left.getAtsScore(), right.getAtsScore()))
                        .matchDelta(delta(left.getMatchScore(), right.getMatchScore()))
                        .build())
                .missingSkills(diff(left.getMissingSkills(), right.getMissingSkills()))
                .suggestedSkills(diff(left.getSuggestedSkills(), right.getSuggestedSkills()))
                .build();
    }

    private ResumeAnalysis findOwnedAnalysis(Long userId, Long analysisId) {
        return resumeAnalysisRepository.findByAnalysisIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with id: " + analysisId));
    }

    private void validateQuery(HistoryQuery query) {
        validateRange("ATS score", query.atsMin(), query.atsMax());
        validateRange("Match score", query.matchMin(), query.matchMax());

        LocalDate from = query.from();
        LocalDate to = query.to();
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
    }

    private void validateRange(String label, Integer min, Integer max) {
        if (min != null && (min < 0 || min > 100)) {
            throw new IllegalArgumentException(label + " min must be between 0 and 100");
        }
        if (max != null && (max < 0 || max > 100)) {
            throw new IllegalArgumentException(label + " max must be between 0 and 100");
        }
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(label + " min must be <= max");
        }
    }

    private Integer delta(Integer left, Integer right) {
        return left == null || right == null ? null : right - left;
    }

    private AnalysisComparisonResponse.SkillDelta diff(List<String> left, List<String> right) {
        Set<String> leftSet = new LinkedHashSet<>(safeList(left));
        Set<String> rightSet = new LinkedHashSet<>(safeList(right));

        List<String> added = rightSet.stream().filter(s -> !leftSet.contains(s)).toList();
        List<String> removed = leftSet.stream().filter(s -> !rightSet.contains(s)).toList();

        return AnalysisComparisonResponse.SkillDelta.builder()
                .added(added)
                .removed(removed)
                .build();
    }

    private HistoryTimelineItemResponse toTimelineItem(ResumeAnalysis analysis) {
        return HistoryTimelineItemResponse.builder()
                .analysisId(analysis.getAnalysisId())
                .resume(HistoryTimelineItemResponse.ResumeRef.builder()
                        .resumeId(analysis.getResume().getResumeId())
                        .displayName(analysis.getResume().getDisplayName())
                        .build())
                .resumeVersion(HistoryTimelineItemResponse.ResumeVersionRef.builder()
                        .resumeVersionId(analysis.getResumeVersion().getResumeVersionId())
                        .versionNumber(analysis.getResumeVersion().getVersionNumber())
                        .build())
                .jobDescription(HistoryTimelineItemResponse.JobDescriptionRef.builder()
                        .jobDescriptionId(analysis.getJobDescription().getJobDescriptionId())
                        .company(analysis.getJobDescription().getCompany())
                        .jobTitle(analysis.getJobDescription().getJobTitle())
                        .build())
                .status(analysis.getStatus())
                .atsScore(analysis.getAtsScore())
                .matchScore(analysis.getMatchScore())
                .overallSummary(analysis.getOverallSummary())
                .missingSkills(List.copyOf(safeList(analysis.getMissingSkills())))
                .suggestedSkills(List.copyOf(safeList(analysis.getSuggestedSkills())))
                .suggestions(List.copyOf(safeList(analysis.getSuggestions())))
                .errorMessage(analysis.getErrorMessage())
                .engineVersion(analysis.getEngineVersion())
                .analysisDate(analysis.getCreatedAt())
                .completedAt(analysis.getCompletedAt())
                .build();
    }

    private List<String> safeList(List<String> items) {
        return items == null ? List.of() : items;
    }
}

