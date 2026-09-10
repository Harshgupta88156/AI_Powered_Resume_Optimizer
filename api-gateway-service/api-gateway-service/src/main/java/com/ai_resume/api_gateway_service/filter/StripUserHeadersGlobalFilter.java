package com.ai_resume.api_gateway_service.filter;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * CRITICAL security filter.
 *
 * <p>Downstream services (resume-service in particular) trust {@code X-User-Id}
 * to decide which records the caller owns. Those headers are only trustworthy if
 * the gateway is the *only* thing that can set them.
 *
 * <p>This filter runs before every route — including the public {@code /api/auth/**}
 * routes, which have no JWT filter — and strips any client-supplied
 * {@code X-User-*} header. Without it, a caller could simply send
 * {@code X-User-Id: 1} and impersonate another user.
 */
@Component
public class StripUserHeadersGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> SPOOFABLE_HEADERS =
            List.of("X-User-Id", "X-User-Email", "X-User-Role");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                .headers(headers -> SPOOFABLE_HEADERS.forEach(headers::remove))
                .build();
        return chain.filter(exchange.mutate().request(sanitized).build());
    }

    @Override
    public int getOrder() {
        // Must run before JwtAuthFilter re-adds the trusted values.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

