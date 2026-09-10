package com.ai_resume.resume_service.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * The result of analysing one {@link ResumeVersion} against one
 * {@link JobDescription}. This is the central fact table of the product: the
 * future history, dashboard, and trends features are all built on it.
 *
 * <p>Records here are append-only and must never be deleted or mutated, so that
 * "ATS improvement over time" stays truthful.
 *
 * <p>Skill lists are stored as element-collection child tables rather than a JSON
 * blob so trends queries can simply {@code GROUP BY skill} in SQL.
 */
@Entity
@Table(name = "resume_analyses", indexes = {
        @Index(name = "idx_analysis_user", columnList = "user_id"),
        @Index(name = "idx_analysis_resume", columnList = "resume_id"),
        @Index(name = "idx_analysis_version", columnList = "resume_version_id"),
        @Index(name = "idx_analysis_jd", columnList = "job_description_id"),
        @Index(name = "idx_analysis_created", columnList = "created_at")
})
@Getter
@Setter
@ToString(exclude = {"resume", "resumeVersion", "jobDescription"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    /** Denormalised owner id so history/dashboard queries never need a join. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_version_id", nullable = false)
    private ResumeVersion resumeVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AnalysisStatus status;

    /** 0-100. Null until the analysis completes. */
    @Column(name = "ats_score")
    private Integer atsScore;

    /** 0-100. Null until the analysis completes. */
    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "overall_summary", columnDefinition = "TEXT")
    private String overallSummary;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_matching_skills",
            joinColumns = @JoinColumn(name = "analysis_id"),
            indexes = @Index(name = "idx_matching_skill", columnList = "skill"))
    @Column(name = "skill", nullable = false)
    private List<String> matchingSkills = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_missing_skills",
            joinColumns = @JoinColumn(name = "analysis_id"),
            indexes = @Index(name = "idx_missing_skill", columnList = "skill"))
    @Column(name = "skill", nullable = false)
    private List<String> missingSkills = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_strengths",
            joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "strength", nullable = false, columnDefinition = "TEXT")
    private List<String> strengths = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_weaknesses",
            joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "weakness", nullable = false, columnDefinition = "TEXT")
    private List<String> weaknesses = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_suggested_skills",
            joinColumns = @JoinColumn(name = "analysis_id"),
            indexes = @Index(name = "idx_suggested_skill", columnList = "skill"))
    @Column(name = "skill", nullable = false)
    private List<String> suggestedSkills = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_suggestions",
            joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "suggestion", nullable = false, columnDefinition = "TEXT")
    private List<String> suggestions = new ArrayList<>();

    /**
     * Untouched payload returned by ai-service, kept for debugging and for
     * re-parsing if the response schema changes. TEXT, not varchar(255) — the
     * previous column silently guaranteed an insert failure for any real result.
     */
    @Column(name = "raw_result", columnDefinition = "TEXT")
    private String rawResult;

    /** Populated when {@link #status} is FAILED. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Markdown source for a regenerated, JD-tailored version of this resume,
     * produced on demand via {@code POST /api/resumes/analyses/{id}/markdown}.
     * Null until the client requests it at least once.
     */
    @Column(name = "generated_markdown", columnDefinition = "TEXT")
    private String generatedMarkdown;

    /** Lets us re-run old analyses when the prompt or model changes. */
    @Column(name = "engine_version", length = 64)
    private String engineVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
