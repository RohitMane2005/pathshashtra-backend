package com.pathshashtra.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // FIX L1+L2: Restrict origins based on environment.
        // Production: only the configured frontend URL.
        // Development: allow localhost ports for dev convenience.
        List<String> origins = new ArrayList<>();
        origins.add(frontendUrl);

        boolean isProd = "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
        if (!isProd) {
            // Dev-only origins — NEVER in production
            origins.add("http://localhost:3000");
            origins.add("http://localhost:5173");
        }

        config.setAllowedOriginPatterns(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Explicit header allowlist — no wildcard
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "X-Requested-With"
        ));
        config.setExposedHeaders(List.of(
                "Authorization", "X-RateLimit-Remaining", "X-RateLimit-Limit"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
