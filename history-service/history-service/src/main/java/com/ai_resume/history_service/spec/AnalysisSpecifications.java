package com.ai_resume.history_service.spec;

import com.ai_resume.history_service.entity.AnalysisStatus;
import com.ai_resume.history_service.entity.ResumeAnalysis;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class AnalysisSpecifications {

    private AnalysisSpecifications() {
    }

    public static Specification<ResumeAnalysis> ownerIs(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<ResumeAnalysis> resumeIdEquals(Long resumeId) {
        return resumeId == null ? null
                : (root, query, cb) -> cb.equal(root.get("resume").get("resumeId"), resumeId);
    }

    public static Specification<ResumeAnalysis> resumeVersionIdEquals(Long resumeVersionId) {
        return resumeVersionId == null ? null
                : (root, query, cb) -> cb.equal(root.get("resumeVersion").get("resumeVersionId"), resumeVersionId);
    }

    public static Specification<ResumeAnalysis> jobDescriptionIdEquals(Long jobDescriptionId) {
        return jobDescriptionId == null ? null
                : (root, query, cb) -> cb.equal(root.get("jobDescription").get("jobDescriptionId"), jobDescriptionId);
    }

    public static Specification<ResumeAnalysis> companyContains(String company) {
        if (company == null || company.isBlank()) {
            return null;
        }
        String value = "%" + company.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("jobDescription").get("company")), value);
    }

    public static Specification<ResumeAnalysis> jobTitleContains(String jobTitle) {
        if (jobTitle == null || jobTitle.isBlank()) {
            return null;
        }
        String value = "%" + jobTitle.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("jobDescription").get("jobTitle")), value);
    }

    public static Specification<ResumeAnalysis> atsScoreBetween(Integer min, Integer max) {
        if (min == null && max == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("atsScore"), min, max);
            }
            return min != null ? cb.greaterThanOrEqualTo(root.get("atsScore"), min)
                    : cb.lessThanOrEqualTo(root.get("atsScore"), max);
        };
    }

    public static Specification<ResumeAnalysis> matchScoreBetween(Integer min, Integer max) {
        if (min == null && max == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("matchScore"), min, max);
            }
            return min != null ? cb.greaterThanOrEqualTo(root.get("matchScore"), min)
                    : cb.lessThanOrEqualTo(root.get("matchScore"), max);
        };
    }

    public static Specification<ResumeAnalysis> createdBetween(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }

        LocalDateTime start = from == null ? null : from.atStartOfDay();
        // End is exclusive start-of-next-day so callers can pass whole calendar dates.
        LocalDateTime endExclusive = to == null ? null : to.plusDays(1).atStartOfDay();

        return (root, query, cb) -> {
            if (start != null && endExclusive != null) {
                return cb.and(
                        cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                        cb.lessThan(root.get("createdAt"), endExclusive));
            }
            return start != null
                    ? cb.greaterThanOrEqualTo(root.get("createdAt"), start)
                    : cb.lessThan(root.get("createdAt"), endExclusive);
        };
    }

    public static Specification<ResumeAnalysis> statusEquals(AnalysisStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}

