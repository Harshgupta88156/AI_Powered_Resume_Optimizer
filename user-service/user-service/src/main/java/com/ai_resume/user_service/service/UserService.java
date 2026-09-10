package com.ai_resume.user_service.service;

import com.ai_resume.user_service.dto.OAuthUserRequest;
import com.ai_resume.user_service.dto.RoleUpdateRequest;
import com.ai_resume.user_service.dto.UserRequest;
import com.ai_resume.user_service.dto.UserResponse;
import com.ai_resume.user_service.dto.UserUpdateRequest;
import com.ai_resume.user_service.dto.VerifyCredentialsRequest;
import com.ai_resume.user_service.entity.AuthProvider;
import com.ai_resume.user_service.entity.Role;
import com.ai_resume.user_service.entity.User;
import com.ai_resume.user_service.exception.EmailAlreadyExistsException;
import com.ai_resume.user_service.exception.InvalidCredentialsException;
import com.ai_resume.user_service.exception.ResourceNotFoundException;
import com.ai_resume.user_service.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException("A user already exists with email: " + email);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                // Role is NEVER taken from the request: new accounts are always USER.
                .role(Role.USER)
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    /**
     * Find-or-create for external identity providers (e.g. GitHub).
     *
     * Resolution order matters:
     *
     *  1. <b>provider + providerId</b> — the only truly stable identifier. GitHub
     *     hands out an immutable numeric id; emails are optional (private
     *     profiles expose none) and changeable.
     *  2. <b>email</b> — links an external login to an account the user already
     *     created with a password, and covers rows written before providerId
     *     existed. When that happens the providerId is backfilled so subsequent
     *     logins take path 1.
     *  3. otherwise create a new passwordless account.
     *
     * Previously only step 2 existed. Because auth-service synthesises
     * "{login}@users.noreply.github.com" when GitHub withholds the email, a
     * second GitHub account whose email was also private would resolve to
     * whichever row happened to match first — which is exactly the "it logged
     * me into the wrong account" symptom.
     */
    @Transactional
    public UserResponse findOrCreateOAuthUser(OAuthUserRequest request) {
        AuthProvider provider = AuthProvider.from(request.getProvider());
        String email = normalizeEmail(request.getEmail());
        String providerId = trimToNull(request.getProviderId());

        // 1. Stable provider identity.
        if (providerId != null) {
            Optional<User> byProviderId =
                    userRepository.findByProviderAndProviderId(provider, providerId);

            if (byProviderId.isPresent()) {
                User existing = byProviderId.get();
                assertActive(existing);

                // Keep the local copy in step with the provider: people rename
                // themselves and make a previously private email public.
                boolean dirty = false;

                if (email != null && !email.equalsIgnoreCase(existing.getEmail())
                        && !userRepository.existsByEmailIgnoreCase(email)) {
                    existing.setEmail(email);
                    dirty = true;
                }
                if (hasText(request.getName())
                        && !request.getName().trim().equals(existing.getName())) {
                    existing.setName(request.getName().trim());
                    dirty = true;
                }

                return toResponse(dirty ? userRepository.save(existing) : existing);
            }
        }

        // 2. Existing account with this email — link the provider to it.
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            assertActive(existing);

            if (providerId != null && existing.getProviderId() == null) {
                existing.setProviderId(providerId);
                // A LOCAL account signing in through GitHub for the first time
                // keeps its password; the provider is only promoted when the
                // account had no credentials of its own.
                if (existing.getPassword() == null) {
                    existing.setProvider(provider);
                }
                existing = userRepository.save(existing);
            }
            return toResponse(existing);
        }

        // 3. Brand new external account.
        User user = User.builder()
                .name(hasText(request.getName()) ? request.getName().trim() : "New user")
                .email(email)
                .password(null)
                .role(Role.USER)
                .provider(provider)
                .providerId(providerId)
                .active(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse verifyCredentials(VerifyCredentialsRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Same message for "unknown email" and "wrong password" so the endpoint
            // cannot be used to enumerate registered accounts.
            throw new InvalidCredentialsException("Invalid email or password");
        }
        assertActive(user);

        return toResponse(user);
    }

    /** Paginated so the endpoint stays usable once the user table grows. */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUserOrThrow(id);
        user.setName(request.getName().trim());
        return toResponse(userRepository.save(user));
    }

    /** ADMIN-only role change, kept separate from the self-service profile update. */
    @Transactional
    public UserResponse updateRole(Long id, RoleUpdateRequest request) {
        User user = findUserOrThrow(id);
        user.setRole(request.getRole());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private void assertActive(User user) {
        if (!user.isActive()) {
            throw new InvalidCredentialsException("This account has been deactivated");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
