package com.ai_resume.resume_service.repository;

import com.ai_resume.resume_service.entity.Resume;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    /**
     * Ownership-scoped lookups. Every query in this repository takes a userId —
     * the previous findAll()/findById() allowed any authenticated user to read
     * and delete any other user's resumes.
     */
    Page<Resume> findByUserId(Long userId, Pageable pageable);

    Optional<Resume> findByResumeIdAndUserId(Long resumeId, Long userId);

    /** Used by the future dashboard: "total resumes". */
    long countByUserId(Long userId);
}
