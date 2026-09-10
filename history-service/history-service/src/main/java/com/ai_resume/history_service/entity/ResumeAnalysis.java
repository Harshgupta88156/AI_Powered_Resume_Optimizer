package com.ai_resume.history_service.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "resume_analyses")
@Getter
@Setter
public class ResumeAnalysis {

    @Id
    @Column(name = "analysis_id")
    private Long analysisId;

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
    @Column(name = "status", nullable = false)
    private AnalysisStatus status;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "overall_summary", columnDefinition = "TEXT")
    private String overallSummary;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_missing_skills", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "skill", nullable = false)
    private List<String> missingSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_suggested_skills", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "skill", nullable = false)
    private List<String> suggestedSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_suggestions", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "suggestion", nullable = false, columnDefinition = "TEXT")
    private List<String> suggestions = new ArrayList<>();

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "engine_version")
    private String engineVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}

