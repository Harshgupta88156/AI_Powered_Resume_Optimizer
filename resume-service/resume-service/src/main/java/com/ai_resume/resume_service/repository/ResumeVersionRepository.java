package com.ai_resume.resume_service.repository;

import com.ai_resume.resume_service.entity.ResumeVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    List<ResumeVersion> findByResume_ResumeIdAndResume_UserIdOrderByVersionNumberAsc(
            Long resumeId, Long userId);

    Optional<ResumeVersion> findByResumeVersionIdAndResume_UserId(Long resumeVersionId, Long userId);

    Optional<ResumeVersion> findTopByResume_ResumeIdOrderByVersionNumberDesc(Long resumeId);

    /** Used by the future dashboard: "total resume versions". */
    long countByResume_UserId(Long userId);
}

