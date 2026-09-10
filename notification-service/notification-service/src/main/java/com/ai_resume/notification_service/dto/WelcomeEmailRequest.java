package com.ai_resume.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sent by auth-service right after a user registers successfully. */
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

