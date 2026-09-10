package com.ai_resume.resume_service.repository;

import com.ai_resume.resume_service.entity.LearningResource;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    /**
     * All resources for any of the given normalised skill keys, ordered so the
     * caller can group by skill and take the first N of each.
     */
    @Query("""
           select r from LearningResource r
           where r.skillKey in :keys
           order by r.skillKey asc, r.displayOrder asc
           """)
    List<LearningResource> findForSkillKeys(@Param("keys") Collection<String> keys);

    boolean existsBySkillKey(String skillKey);

    boolean existsByUrl(String url);
}
