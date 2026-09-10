package com.resumeoptimizer.resume_optimizer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors notification-service's WelcomeEmailRequest. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeEmailRequest {

    @NotBlank
    @Email
    private String recipientEmail;

    @NotBlank
    private String userName;
}

