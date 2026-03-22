package com.pathshashtra.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * In-memory Caffeine cache for AI responses.
 *
 * Cache names and TTLs:
 *   quiz-questions:  10 min  — quiz questions don't change between sessions
 *   dsa-roadmap:     1 hour  — DSA roadmap per user+goal is stable
 *
 * Study plan and career roadmap are NOT cached because they depend on
 * user-specific inputs that change on every request.
 *
 * Cache keys are built from method params — if inputs change, cache misses.
 * Maximum 500 entries per cache to bound memory usage.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "quiz-questions", "dsa-roadmap"
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(60, TimeUnit.MINUTES)
        );
        return manager;
    }
}
