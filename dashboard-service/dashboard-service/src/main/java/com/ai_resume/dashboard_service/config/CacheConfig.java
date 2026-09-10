package com.ai_resume.dashboard_service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Short-lived cache for the assembled dashboard snapshot.
 *
 * <p>Rendering a dashboard fans out to several services and pulls a page of
 * analyses. Users reload dashboards constantly, so without a cache a single
 * impatient user can multiply load across the whole system.
 *
 * <p>The TTL is deliberately short (30s by default). A dashboard that is half a
 * minute stale is fine; one that still shows old numbers a minute after an upload
 * feels broken. Set {@code dashboard.cache.ttl-seconds=0} to disable.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DASHBOARD_CACHE = "dashboardSnapshot";

    @Bean
    public CacheManager cacheManager(
            @Value("${dashboard.cache.ttl-seconds:30}") long ttlSeconds,
            @Value("${dashboard.cache.max-entries:1000}") long maxEntries) {

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DASHBOARD_CACHE);
        if (ttlSeconds <= 0) {
            // Zero-size cache = effectively disabled, without branching elsewhere.
            cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(0));
        } else {
            cacheManager.setCaffeine(Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                    .maximumSize(maxEntries));
        }
        return cacheManager;
    }
}

