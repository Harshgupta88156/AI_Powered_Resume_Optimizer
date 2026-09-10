package com.ai_resume.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal request used by auth-service to find-or-create a user that logged in
 * through an external identity provider (e.g. GitHub). No password is involved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    /**
     * The provider's own id for this account. Optional so an older auth-service
     * that does not send it still works, but strongly preferred: it is what
     * makes switching between two GitHub accounts resolve correctly.
     */
    private String providerId;

    /** Avatar URL from the provider, when it supplies one. */
    private String avatarUrl;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Provider is required")
    private String provider;
}
