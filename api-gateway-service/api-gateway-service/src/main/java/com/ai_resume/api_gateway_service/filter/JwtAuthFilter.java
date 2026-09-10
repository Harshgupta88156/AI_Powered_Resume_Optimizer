package com.ai_resume.api_gateway_service.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway-level JWT validation filter. Applied only to protected routes via
 * spring.cloud.gateway.routes[x].filters[0]=JwtAuthFilter in properties.
 *
 * On success: the userId, email, and role claims are forwarded as request headers
 * (X-User-Id, X-User-Email, X-User-Role) so downstream services don't re-parse
 * the token.
 *
 * On failure: returns a 401 JSON response immediately; the request never reaches
 * the downstream service.
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private final SecretKey key;
    private final String issuer;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret,
                         @Value("${jwt.issuer:auth-service}") String issuer) {
        super(Config.class);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .requireIssuer(issuer)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                if (userId == null || userId.isBlank()) {
                    return unauthorized(exchange, "Token is missing a subject");
                }

                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);

                // Forward claims as headers to downstream services.
                // StripUserHeadersGlobalFilter has already removed any client-supplied
                // values, so these are the only X-User-* headers that survive.
                ServerHttpRequest mutated = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email == null ? "" : email)
                        .header("X-User-Role", role == null ? "" : role)
                        .build();

                return chain.filter(exchange.mutate().request(mutated).build());

            } catch (JwtException | IllegalArgumentException ex) {
                return unauthorized(exchange, "Invalid or expired token");
            }
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // Escape quotes so the message can never break out of the JSON string.
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + safeMessage + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // No config fields needed for now
    }
}
