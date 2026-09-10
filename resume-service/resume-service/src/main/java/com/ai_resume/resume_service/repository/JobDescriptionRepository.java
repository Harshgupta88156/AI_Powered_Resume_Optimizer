package com.ai_resume.resume_service.repository;

import com.ai_resume.resume_service.entity.JobDescription;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {

    Page<JobDescription> findByUserId(Long userId, Pageable pageable);

    Optional<JobDescription> findByJobDescriptionIdAndUserId(Long jobDescriptionId, Long userId);

    /** Used by the future dashboard: "total job descriptions". */
    long countByUserId(Long userId);

    /** Used by the future history search: "search by company / job title". */
    Page<JobDescription> findByUserIdAndCompanyContainingIgnoreCase(
            Long userId, String company, Pageable pageable);

    Page<JobDescription> findByUserIdAndJobTitleContainingIgnoreCase(
            Long userId, String jobTitle, Pageable pageable);
}
