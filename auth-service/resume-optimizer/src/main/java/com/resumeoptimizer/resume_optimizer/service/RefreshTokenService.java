package com.resumeoptimizer.resume_optimizer.service;

import com.resumeoptimizer.resume_optimizer.entity.RefreshToken;
import com.resumeoptimizer.resume_optimizer.exception.InvalidRefreshTokenException;
import com.resumeoptimizer.resume_optimizer.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, validates, rotates, and revokes opaque refresh tokens stored in the
 * auth database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /** How long revoked/expired rows are kept before the cleanup job removes them. */
    @Value("${jwt.refresh-retention-days:30}")
    private long refreshRetentionDays;

    @Transactional
    public RefreshToken create(Long userId, String email, String name, String role) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .name(name)
                .role(role)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    /** Returns the token if it is known, not revoked, and not expired; else throws. */
    @Transactional(readOnly = true)
    public RefreshToken verifyUsable(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));
        if (stored.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }
        if (stored.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }
        return stored;
    }

    /** Rotates a valid refresh token: revokes the old one and issues a new one. */
    @Transactional
    public RefreshToken rotate(RefreshToken current) {
        current.setRevoked(true);
        refreshTokenRepository.save(current);
        return create(current.getUserId(), current.getEmail(), current.getName(), current.getRole());
    }

    /** Revokes a single refresh token (logout from this device/session). */
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /** Revokes every active refresh token for a user (logout everywhere). */
    @Transactional
    public int revokeAllForUser(Long userId) {
        return refreshTokenRepository.revokeAllForUser(userId);
    }

    /**
     * Nightly housekeeping. Every login and every refresh rotation writes a row;
     * without this the table would grow without bound.
     */
    @Scheduled(cron = "${jwt.refresh-cleanup-cron:0 15 3 * * *}")
    @Transactional
    public void purgeStaleTokens() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(refreshRetentionDays));
        int removed = refreshTokenRepository.deleteExpiredOrRevoked(cutoff);
        if (removed > 0) {
            log.info("Purged {} expired/revoked refresh tokens", removed);
        }
    }
}
