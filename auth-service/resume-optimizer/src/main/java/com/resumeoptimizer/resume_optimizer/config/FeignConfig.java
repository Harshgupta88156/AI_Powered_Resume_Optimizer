package com.resumeoptimizer.resume_optimizer.config;

import feign.RequestInterceptor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign wiring for the user-service client.
 *
 * <ul>
 *   <li>Attaches the shared internal API key so user-service can distinguish a
 *       genuine auth-service call from an attacker hitting port 8081 directly.</li>
 *   <li>Sets explicit connect/read timeouts — the default is effectively
 *       "wait forever", which would let a stalled user-service hang every login.</li>
 * </ul>
 */
@Configuration
public class FeignConfig {

    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return template -> template.header(INTERNAL_API_KEY_HEADER, internalApiKey);
    }

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

