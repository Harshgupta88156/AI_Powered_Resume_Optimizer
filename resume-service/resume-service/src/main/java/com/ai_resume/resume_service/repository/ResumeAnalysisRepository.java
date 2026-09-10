package com.ai_resume.resume_service.repository;

import com.ai_resume.resume_service.entity.ResumeAnalysis;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    Page<ResumeAnalysis> findByUserId(Long userId, Pageable pageable);

    Page<ResumeAnalysis> findByUserIdAndResume_ResumeId(Long userId, Long resumeId, Pageable pageable);

    Page<ResumeAnalysis> findByUserIdAndResumeVersion_ResumeVersionId(
            Long userId, Long resumeVersionId, Pageable pageable);

    Page<ResumeAnalysis> findByUserIdAndJobDescription_JobDescriptionId(
            Long userId, Long jobDescriptionId, Pageable pageable);

    Optional<ResumeAnalysis> findByAnalysisIdAndUserId(Long analysisId, Long userId);

    /** Used by the future dashboard: "total analyses". */
    long countByUserId(Long userId);

    boolean existsByResume_ResumeId(Long resumeId);

    boolean existsByJobDescription_JobDescriptionId(Long jobDescriptionId);
}
