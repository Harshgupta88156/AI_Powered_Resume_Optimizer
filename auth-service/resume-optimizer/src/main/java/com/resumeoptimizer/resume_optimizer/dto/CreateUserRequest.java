package com.resumeoptimizer.resume_optimizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload sent to user-service to create a new user during registration.
 * Mirrors user-service's UserRequest — no role field: user-service always
 * creates accounts with role USER.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    private String name;
    private String email;
    private String password;
}
