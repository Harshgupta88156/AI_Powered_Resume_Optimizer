package com.ai_resume.history_service.repository;

import com.ai_resume.history_service.entity.ResumeAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeAnalysisRepository
        extends JpaRepository<ResumeAnalysis, Long>, JpaSpecificationExecutor<ResumeAnalysis> {

    @EntityGraph(attributePaths = {"resume", "resumeVersion", "jobDescription"})
    Optional<ResumeAnalysis> findByAnalysisIdAndUserId(Long analysisId, Long userId);
}

