package com.pathshashtra.backend.config;

import com.pathshashtra.backend.ratelimit.GlobalRateLimitFilter;
import com.pathshashtra.backend.security.JwtAuthenticationFilter;
import com.pathshashtra.backend.security.JwtUtil;
import com.pathshashtra.backend.security.OAuth2LoginFailureHandler;
import com.pathshashtra.backend.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // FIX B1: Enable @PreAuthorize for RBAC
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final GlobalRateLimitFilter globalRateLimitFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    public SecurityConfig(JwtUtil jwtUtil, GlobalRateLimitFilter globalRateLimitFilter,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          OAuth2LoginFailureHandler oAuth2LoginFailureHandler) {
        this.jwtUtil = jwtUtil;
        this.globalRateLimitFilter = globalRateLimitFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
    }

    @Bean
    public HttpSessionOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil);
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // FIX E1: Security headers — HSTS, X-Frame-Options, X-Content-Type, Referrer-Policy, Permissions-Policy
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))            // 1 year
                .frameOptions(frame -> frame.deny())       // Prevent clickjacking
                .contentTypeOptions(cto -> {})             // X-Content-Type-Options: nosniff
                .referrerPolicy(rp -> rp.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(pp -> pp.policy(
                    "camera=(), microphone=(), geolocation=(), payment=()"))
            )

            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/auth/reset-password/validate").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/quiz/share/**").permitAll()
                    .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll() // Allow OAuth2 endpoints
                    // FIX D3: removed /actuator/health from public access — use /api/health instead
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(a -> a
                        .authorizationRequestRepository(authorizationRequestRepository())
                    )
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(oAuth2LoginFailureHandler)
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(globalRateLimitFilter, JwtAuthenticationFilter.class);  // FIX D1
        return http.build();
    }
}
