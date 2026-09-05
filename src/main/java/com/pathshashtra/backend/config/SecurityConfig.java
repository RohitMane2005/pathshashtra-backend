package com.pathshashtra.backend.config;

// FIX-10: Removed redundant same-package import (RedisAvailabilityTracker is in the same package)
import com.pathshashtra.backend.ratelimit.GlobalRateLimitFilter;
import com.pathshashtra.backend.security.JwtAuthenticationFilter;
import com.pathshashtra.backend.security.JwtUtil;
import com.pathshashtra.backend.security.OAuth2LoginFailureHandler;
import com.pathshashtra.backend.security.OAuth2LoginSuccessHandler;
import com.pathshashtra.backend.security.TokenBlacklist;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final TokenBlacklist tokenBlacklist;
    private final ObjectMapper objectMapper;
    private final GlobalRateLimitFilter globalRateLimitFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final StringRedisTemplate redisTemplate;
    private final RedisAvailabilityTracker redisTracker;

    public SecurityConfig(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, ObjectMapper objectMapper,
                          GlobalRateLimitFilter globalRateLimitFilter,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
                          StringRedisTemplate redisTemplate,
                          RedisAvailabilityTracker redisTracker) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.objectMapper = objectMapper;
        this.globalRateLimitFilter = globalRateLimitFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        this.redisTemplate = redisTemplate;
        this.redisTracker = redisTracker;
    }

    @Bean
    public HttpSessionOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, tokenBlacklist, objectMapper, redisTemplate, redisTracker);
        http
            .cors(cors -> {})
            /**
             * CSRF: Disabled — CORS provides equivalent protection for this cross-domain SPA.
             *
             * Why this is safe:
             *  1. CorsConfig only allows the explicit frontend origin (never wildcard *).
             *  2. All state-changing requests (POST/PUT/DELETE with Content-Type: application/json)
             *     trigger a CORS preflight. The browser checks the preflight against the CORS
             *     allowlist before sending the credentialed request.
             *  3. A cross-origin attacker cannot forge a credentialed request that passes the
             *     CORS preflight because their origin is not in the allowlist.
             *
             * The XSRF-TOKEN cookie approach is NOT used here because:
             *  - The cookie is set on the backend domain
             *  - JavaScript on the frontend domain (different origin) cannot read it
             *  - This would cause 403 Forbidden on every POST/PUT/DELETE from the SPA
             *
             * Reference: https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
             *            §"Use of Custom Request Headers" and §"Defense In Depth Techniques"
             */
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            /**
             * FIX-7: Session policy is deliberately IF_REQUIRED (not STATELESS).
             *
             * OAuth2 authorization code flow requires the server to store the `state` and
             * PKCE verifier between the authorization redirect and the callback. Spring's
             * HttpSessionOAuth2AuthorizationRequestRepository (configured below) uses the
             * HTTP session for this — making STATELESS impossible for OAuth2 without a
             * custom session-free repository (e.g., cookie-based or Redis-backed).
             *
             * Mitigation: sessions are created ONLY for OAuth2 requests (IF_REQUIRED).
             * Normal JWT-cookie API calls never create a session. The session is destroyed
             * after OAuth2LoginSuccessHandler runs (exchange code → set JWT cookie → done).
             */
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // Security headers — HSTS, X-Frame-Options, X-Content-Type, Referrer-Policy, Permissions-Policy, CSP
            .headers(headers -> {
                headers
                    .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000))
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(cto -> {})
                    .referrerPolicy(rp -> rp.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicy(pp -> pp.policy(
                        "camera=(), microphone=(), geolocation=(), payment=()"));
                // FE-02 FIX: Content-Security-Policy (separate statement to avoid chaining issues)
                headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "connect-src 'self'; " +
                    "img-src 'self' data: https:; " +
                    "frame-ancestors 'none'"));
            })

            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                    .requestMatchers(HttpMethod.HEAD, "/api/health").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/exchange-code").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/auth/reset-password/validate").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/quiz/share/**").permitAll()
                    .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                    // Razorpay webhook — no user auth, signature verified inside service
                    .requestMatchers(HttpMethod.POST, "/api/subscription/webhook").permitAll()
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
