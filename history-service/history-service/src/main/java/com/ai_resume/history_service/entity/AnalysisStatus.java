package com.ai_resume.history_service.entity;

/** Lifecycle state of one resume-vs-job-description analysis record. */
public enum AnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

