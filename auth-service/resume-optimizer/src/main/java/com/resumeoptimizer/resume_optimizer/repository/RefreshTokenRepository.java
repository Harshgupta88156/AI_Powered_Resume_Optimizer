package com.resumeoptimizer.resume_optimizer.repository;

import com.resumeoptimizer.resume_optimizer.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId and r.revoked = false")
    int revokeAllForUser(@Param("userId") Long userId);

    /**
     * Housekeeping: without this the table grows forever, because every login and
     * every refresh rotation inserts a row that is never removed.
     */
    @Modifying
    @Query("delete from RefreshToken r where r.expiryDate < :cutoff or r.revoked = true")
    int deleteExpiredOrRevoked(@Param("cutoff") Instant cutoff);
}
