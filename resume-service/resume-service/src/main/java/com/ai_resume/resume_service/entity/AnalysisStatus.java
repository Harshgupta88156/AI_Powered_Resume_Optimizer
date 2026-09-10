package com.ai_resume.resume_service.entity;

/** Lifecycle of a single resume-vs-job-description analysis. */
public enum AnalysisStatus {
    /** Persisted, waiting for ai-service to produce a result. */
    PENDING,
    /** ai-service is currently processing it (used once analysis becomes async). */
    PROCESSING,
    /** Result stored successfully. */
    COMPLETED,
    /** ai-service was unreachable or returned an unusable payload. */
    FAILED
}

