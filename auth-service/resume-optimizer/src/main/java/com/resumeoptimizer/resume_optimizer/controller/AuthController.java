package com.resumeoptimizer.resume_optimizer.controller;

import com.resumeoptimizer.resume_optimizer.dto.AuthResponse;
import com.resumeoptimizer.resume_optimizer.dto.LoginRequest;
import com.resumeoptimizer.resume_optimizer.dto.RefreshTokenRequest;
import com.resumeoptimizer.resume_optimizer.dto.RegisterRequest;
import com.resumeoptimizer.resume_optimizer.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /** Logs the user out of every device by revoking all of their refresh tokens. */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutAll(
            @Valid @RequestBody RefreshTokenRequest request) {
        int revoked = authService.logoutAll(request.getRefreshToken());
        return ResponseEntity.ok(Map.of(
                "message", "Logged out from all devices",
                "revokedSessions", revoked));
    }
}
