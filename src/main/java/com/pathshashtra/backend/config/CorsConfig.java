package com.pathshashtra.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // FIX: Use ONLY setAllowedOriginPatterns.
        // Mixing setAllowedOrigins + setAllowedOriginPatterns causes Spring to
        // ignore one of the lists depending on request origin, making CORS policy
        // unpredictable and potentially bypassed for some origins.
        // setAllowedOriginPatterns supports exact strings AND wildcards and
        // is fully compatible with allowCredentials(true).
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:*",
                "https://*.vercel.app",
                frontendUrl
        ));

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
