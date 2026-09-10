package com.ai_resume.user_service.controller;

import com.ai_resume.user_service.dto.OAuthUserRequest;
import com.ai_resume.user_service.dto.RoleUpdateRequest;
import com.ai_resume.user_service.dto.UserRequest;
import com.ai_resume.user_service.dto.UserResponse;
import com.ai_resume.user_service.dto.UserProfileResponse;
import com.ai_resume.user_service.dto.UserProfileUpdateRequest;
import com.ai_resume.user_service.dto.UserUpdateRequest;
import com.ai_resume.user_service.dto.VerifyCredentialsRequest;
import com.ai_resume.user_service.service.UserProfileService;
import com.ai_resume.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    // ---------------------------------------------------------------------
    // INTERNAL endpoints — called by auth-service only, guarded by
    // InternalApiKeyFilter (X-Internal-Api-Key), never exposed via the gateway.
    // ---------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PostMapping("/verify-credentials")
    public ResponseEntity<UserResponse> verifyCredentials(
            @Valid @RequestBody VerifyCredentialsRequest request) {
        return ResponseEntity.ok(userService.verifyCredentials(request));
    }

    @PostMapping("/oauth")
    public ResponseEntity<UserResponse> findOrCreateOAuthUser(
            @Valid @RequestBody OAuthUserRequest request) {
        return ResponseEntity.ok(userService.findOrCreateOAuthUser(request));
    }

    // ---------------------------------------------------------------------
    // PUBLIC (authenticated) endpoints
    // ---------------------------------------------------------------------

    /** Current user's own profile — resolved from the JWT subject, not from a path id. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserById(Long.valueOf(authentication.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            Authentication authentication, @Valid @RequestBody UserUpdateRequest request) {
        Long userId = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    /**
     * Extended profile: location, experience, education, certificates, links.
     *
     * Read-on-first-touch creates an empty profile row, so this never 404s for
     * a user who has simply never opened the profile page.
     */
    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        return ResponseEntity.ok(
                userProfileService.getProfile(Long.valueOf(authentication.getName())));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(
                userProfileService.updateProfile(Long.valueOf(authentication.getName()), request));
    }

    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(userService.updateRole(id, request));
    }

    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
