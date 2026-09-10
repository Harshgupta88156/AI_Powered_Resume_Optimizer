package com.resumeoptimizer.resume_optimizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload sent to user-service to find-or-create an OAuth (e.g. GitHub) user. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserRequest {

    private String email;
    private String name;
    private String provider;

    /** The provider's own immutable id for this account (GitHub's numeric id). */
    private String providerId;

    private String avatarUrl;
}
