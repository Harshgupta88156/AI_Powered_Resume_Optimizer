package com.resumeoptimizer.resume_optimizer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** User data returned by user-service (mirrors its UserResponse; no password). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAccountResponse {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private String provider;
    private boolean active;
    private LocalDateTime createdAt;
}
