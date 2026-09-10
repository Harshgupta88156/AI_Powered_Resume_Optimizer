package com.resumeoptimizer.resume_optimizer.service;

import com.resumeoptimizer.resume_optimizer.client.NotificationClient;
import com.resumeoptimizer.resume_optimizer.client.UserClient;
import com.resumeoptimizer.resume_optimizer.dto.AuthResponse;
import com.resumeoptimizer.resume_optimizer.dto.CreateUserRequest;
import com.resumeoptimizer.resume_optimizer.dto.LoginRequest;
import com.resumeoptimizer.resume_optimizer.dto.OAuthUserRequest;
import com.resumeoptimizer.resume_optimizer.dto.RegisterRequest;
import com.resumeoptimizer.resume_optimizer.dto.UserAccountResponse;
import com.resumeoptimizer.resume_optimizer.dto.VerifyCredentialsRequest;
import com.resumeoptimizer.resume_optimizer.dto.WelcomeEmailRequest;
import com.resumeoptimizer.resume_optimizer.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates authentication: user data lives in user-service (reached via
 * Feign), while this service is responsible for issuing JWT access tokens and
 * managing refresh tokens.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserClient userClient;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final NotificationClient notificationClient;

    public AuthResponse register(RegisterRequest request) {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        UserAccountResponse user = userClient.createUser(createRequest);
        sendWelcomeEmail(user);
        return issueTokens(user);
    }

    /** Never allowed to throw - a notification failure must not affect registration. */
    private void sendWelcomeEmail(UserAccountResponse user) {
        try {
            notificationClient.sendWelcomeEmail(WelcomeEmailRequest.builder()
                    .recipientEmail(user.getEmail())
                    .userName(user.getName())
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to send welcome email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        VerifyCredentialsRequest verifyRequest = VerifyCredentialsRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        UserAccountResponse user = userClient.verifyCredentials(verifyRequest);
        return issueTokens(user);
    }

    /**
     * Called after a successful external (e.g. GitHub) login.
     *
     * {@code providerId} is the provider's own immutable id for the account.
     * user-service matches on it first, which is what lets two GitHub accounts
     * with private (and therefore synthesised) emails resolve to two distinct
     * local users.
     */
    public AuthResponse oauthLogin(
            String email, String name, String provider, String providerId, String avatarUrl) {

        OAuthUserRequest oauthRequest = OAuthUserRequest.builder()
                .email(email)
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .avatarUrl(avatarUrl)
                .build();

        UserAccountResponse user = userClient.findOrCreateOAuthUser(oauthRequest);
        return issueTokens(user);
    }

    /** Exchanges a valid refresh token for a fresh access token, rotating the refresh token. */
    public AuthResponse refresh(String refreshToken) {
        RefreshToken current = refreshTokenService.verifyUsable(refreshToken);
        RefreshToken rotated = refreshTokenService.rotate(current);

        String accessToken = jwtService.generateToken(
                rotated.getUserId(), rotated.getEmail(), rotated.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rotated.getToken())
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .userId(rotated.getUserId())
                .name(rotated.getName())
                .email(rotated.getEmail())
                .role(rotated.getRole())
                .build();
    }

    /** Revokes the given refresh token so it can no longer be used. */
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    /**
     * Revokes every refresh token belonging to the user behind this refresh token
     * ("log out from all devices"). Access tokens already issued stay valid until
     * they expire, which is why the access-token TTL is kept short.
     */
    public int logoutAll(String refreshToken) {
        RefreshToken current = refreshTokenService.verifyUsable(refreshToken);
        return refreshTokenService.revokeAllForUser(current.getUserId());
    }

    private AuthResponse issueTokens(UserAccountResponse user) {
        String accessToken = jwtService.generateToken(
                user.getUserId(), user.getEmail(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.create(
                user.getUserId(), user.getEmail(), user.getName(), user.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
