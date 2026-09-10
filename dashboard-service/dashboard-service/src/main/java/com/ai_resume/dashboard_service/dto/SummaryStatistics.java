package com.ai_resume.dashboard_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The counter cards at the top of the dashboard. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryStatistics {

    private long totalResumes;
    private long totalResumeVersions;
    private long totalAnalyses;
    private long totalJobDescriptions;
    private long totalUnreadNotifications;

    /** Analyses with status COMPLETED — the denominator behind the averages. */
    private long completedAnalyses;

    /**
     * Null, not 0, when there is nothing to average yet. Showing "0" for a user
     * who has never completed an analysis would read as a terrible score rather
     * than as "no data".
     */
    private Double averageAtsScore;
    private Double averageMatchScore;
}

