package com.ai_resume.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards the three service-to-service endpoints that must stay reachable before
 * a JWT exists (registration, credential verification, OAuth find-or-create).
 *
 * <p>Previously these were {@code permitAll}, which meant anybody who could reach
 * port 8081 directly could create accounts and brute-force passwords. They now
 * require a shared {@code X-Internal-Api-Key} header that only auth-service sends.
 *
 * <p>This is a stop-gap for mTLS / a proper service mesh; it is still a large
 * improvement over an open endpoint.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";

    private static final Set<String> INTERNAL_PATHS = Set.of(
            "/api/users",
            "/api/users/verify-credentials",
            "/api/users/oauth");

    private final byte[] expectedKey;

    public InternalApiKeyFilter(@Value("${internal.api-key}") String internalApiKey) {
        this.expectedKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only POSTs to the internal paths are protected here; every other route
        // is covered by the normal JWT filter chain.
        return !HttpMethod.POST.matches(request.getMethod())
                || !INTERNAL_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String provided = request.getHeader(HEADER);
        if (provided == null
                || !MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\","
                            + "\"message\":\"Missing or invalid internal API key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

