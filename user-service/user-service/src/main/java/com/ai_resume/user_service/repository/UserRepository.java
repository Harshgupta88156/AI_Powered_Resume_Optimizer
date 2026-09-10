package com.ai_resume.user_service.repository;

import com.ai_resume.user_service.entity.AuthProvider;
import com.ai_resume.user_service.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Case-insensitive lookup — emails are identifiers, not display strings. */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Primary lookup for external logins - stable even when the email changes. */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
