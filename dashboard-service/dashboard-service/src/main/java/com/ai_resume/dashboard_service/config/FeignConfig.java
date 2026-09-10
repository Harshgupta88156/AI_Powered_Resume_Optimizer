package com.ai_resume.dashboard_service.config;

import feign.RequestInterceptor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign wiring for all downstream calls.
 *
 * <p>Identity propagation is the important part: dashboard-service never decides
 * who the caller is. It forwards the {@code X-User-Id} header the API gateway
 * injected, so every downstream service applies its own ownership rules. If this
 * interceptor did not forward it, resume-service would reject the call with 401 —
 * which is the correct failure mode, and far better than the dashboard being able
 * to read data it has no claim to.
 */
@Configuration
public class FeignConfig {

    public static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Bean
    public RequestInterceptor userContextPropagationInterceptor() {
        return template -> {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
                return;
            }
            var request = servletAttributes.getRequest();
            copyHeader(template, request.getHeader(USER_ID_HEADER), USER_ID_HEADER);
            copyHeader(template, request.getHeader(USER_EMAIL_HEADER), USER_EMAIL_HEADER);
            copyHeader(template, request.getHeader(USER_ROLE_HEADER), USER_ROLE_HEADER);
        };
    }

    private void copyHeader(feign.RequestTemplate template, String value, String name) {
        if (value != null && !value.isBlank()) {
            template.header(name, value);
        }
    }

    /**
     * A dashboard fans out to several services; without explicit timeouts one slow
     * dependency would hold the request (and a servlet thread) open indefinitely.
     */
    @Bean
    public feign.Request.Options feignRequestOptions(
            @Value("${feign.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${feign.read-timeout-ms:10000}") long readTimeoutMs) {
        return new feign.Request.Options(
                connectTimeoutMs, TimeUnit.MILLISECONDS,
                readTimeoutMs, TimeUnit.MILLISECONDS,
                true);
    }
}

