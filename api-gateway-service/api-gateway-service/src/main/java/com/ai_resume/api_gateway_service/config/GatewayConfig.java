package com.ai_resume.api_gateway_service.config;

import com.ai_resume.api_gateway_service.filter.JwtAuthFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route definitions for the API Gateway.
 * Replaces property-based spring.cloud.gateway.routes[*] config.
 *
 * PUBLIC routes  → no JWT filter (auth endpoints, GitHub OAuth dance)
 * PROTECTED routes → JwtAuthFilter validates Bearer token before forwarding
 */
@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public GatewayConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        JwtAuthFilter.Config jwtConfig = new JwtAuthFilter.Config();

        return builder.routes()

                // ── PUBLIC: email+password auth ──────────────────────────────
                .route("auth-service-public", r -> r
                        .path("/api/auth/**")
                        .uri("lb://auth-service"))

                // ── PUBLIC: GitHub OAuth2 redirect dance ─────────────────────
                .route("auth-service-oauth", r -> r
                        .path("/oauth2/**", "/login/**")
                        .uri("lb://auth-service"))

                // ── PROTECTED: user-service ──────────────────────────────────
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                        .uri("lb://user-service"))

                // ── PROTECTED: resume-service ────────────────────────────────
                .route("resume-service", r -> r
                        .path("/api/resumes/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                        .uri("lb://resume-service"))

                // ── PROTECTED: ai-service ──────────────────────────────────────────
                .route("ai-service", r -> r
                        .path("/api/ai/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                        .uri("lb://ai-service"))

                // ── PROTECTED: dashboard-service ───────────────────────────────────
                // Stateless aggregator. It calls resume-service (and later
                // notification-service) with the X-User-Id this filter injects.
                .route("dashboard-service", r -> r
                        .path("/api/dashboard/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                        .uri("lb://dashboard-service"))

                // ── PROTECTED: trends-service ──────────────────────────────────────
                .route("trends-service", r -> r
                        .path("/api/trends/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                        .uri("lb://trends-service"))

                // ── PROTECTED: history-service ─────────────────────────────────────
                .route("history-service", r -> r
                        .path("/api/history/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                        .uri("lb://history-service"))

                // ── PROTECTED: notification-service ─────────────────────────────────
                // EMAIL ONLY. Exposed mainly for ops/testing; day-to-day usage is
                // service-to-service (auth-service, resume-service, trends-service).
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(jwtConfig)))
                        .uri("lb://notification-service"))

                .build();
    }
}
