package com.ai_resume.user_service.entity;

import java.util.Arrays;

/**
 * Identity provider that owns the credentials for an account.
 * LOCAL accounts have a BCrypt password; every other provider is passwordless.
 */
public enum AuthProvider {
    LOCAL,
    GITHUB,
    GOOGLE;

    /** Lenient parser: unknown / null values fall back to LOCAL rather than blowing up. */
    public static AuthProvider from(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(p -> p.name().equals(normalized))
                .findFirst()
                .orElse(LOCAL);
    }
}

