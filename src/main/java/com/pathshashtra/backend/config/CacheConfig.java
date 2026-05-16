package com.pathshashtra.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * PERF-02 FIX: Each cache now has its own TTL and size limit instead of all
 * caches sharing the same config (which resulted in wrong TTLs for some caches).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);

        // quiz-questions: 10 min — questions change every session but stable within one
        manager.registerCustomCache("quiz-questions",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());

        // career-questions: 15 min — per-profile, stable within assessment
        manager.registerCustomCache("career-questions",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .build());

        // dsa-roadmap: 2 hours — roadmap per user+goal rarely changes
        manager.registerCustomCache("dsa-roadmap",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(2, TimeUnit.HOURS)
                        .build());

        // leaderboard: 5 minutes — aggregate query result, expensive to recompute
        manager.registerCustomCache("leaderboard",
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build());

        return manager;
    }
}
