package com.ai_resume.user_service.dto;

import com.ai_resume.user_service.entity.AuthProvider;
import com.ai_resume.user_service.entity.Role;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Public projection of a user. Never contains the password hash. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String name;
    private String email;
    private Role role;
    private AuthProvider provider;
    private String providerId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
