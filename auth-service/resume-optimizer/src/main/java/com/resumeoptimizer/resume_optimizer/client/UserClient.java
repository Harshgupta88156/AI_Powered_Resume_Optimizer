package com.resumeoptimizer.resume_optimizer.client;

import com.resumeoptimizer.resume_optimizer.dto.CreateUserRequest;
import com.resumeoptimizer.resume_optimizer.dto.OAuthUserRequest;
import com.resumeoptimizer.resume_optimizer.dto.UserAccountResponse;
import com.resumeoptimizer.resume_optimizer.dto.VerifyCredentialsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative HTTP client to user-service. Spring Cloud resolves "user-service"
 * through Eureka, so no host/port is hardcoded here.
 */
@FeignClient(name = "user-service")
public interface UserClient {

    @PostMapping("/api/users")
    UserAccountResponse createUser(@RequestBody CreateUserRequest request);

    @PostMapping("/api/users/verify-credentials")
    UserAccountResponse verifyCredentials(@RequestBody VerifyCredentialsRequest request);

    @PostMapping("/api/users/oauth")
    UserAccountResponse findOrCreateOAuthUser(@RequestBody OAuthUserRequest request);
}
