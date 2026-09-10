package com.ai_resume.user_service.repository;

import com.ai_resume.user_service.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUser_UserId(Long userId);

    /**
     * Loads the profile with its owning user.
     *
     * The four child collections are deliberately NOT fetched here. Hibernate
     * cannot fetch-join more than one {@code List} in a single query - it throws
     * {@code MultipleBagFetchException: cannot simultaneously fetch multiple
     * bags} - so an entity graph naming experiences, educations, certifications
     * and skills together fails every time it runs. That is what made
     * GET /api/users/me/profile return "An unexpected error occurred".
     *
     * The collections are initialised instead by
     * {@code UserProfileService.initialiseCollections}, which runs inside the
     * same transaction. That costs four extra selects rather than one join, and
     * unlike the join it does not multiply rows across four collections.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("select p from UserProfile p where p.user.userId = :userId")
    Optional<UserProfile> findFullByUserId(@Param("userId") Long userId);
}
