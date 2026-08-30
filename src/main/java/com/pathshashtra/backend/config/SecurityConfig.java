package com.pathshashtra.backend.config;

import com.pathshashtra.backend.config.RedisAvailabilityTracker;
import com.pathshashtra.backend.ratelimit.GlobalRateLimitFilter;
import com.pathshashtra.backend.security.JwtAuthenticationFilter;
import com.pathshashtra.backend.security.JwtUtil;
import com.pathshashtra.backend.security.OAuth2LoginFailureHandler;
import com.pathshashtra.backend.security.OAuth2LoginSuccessHandler;
import com.pathshashtra.backend.security.TokenBlacklist;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

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
             * CRIT-01 FIX: CSRF protection is conditional on the SameSite cookie attribute.
             * - SameSite=Lax: browser refuses to send cookies on cross-origin POST/DELETE,
             *   so CSRF tokens are redundant → disable for simpler API integration.
             * - SameSite=None (required when frontend/backend are on different domains):
             *   browser WILL send cookies cross-origin, so CSRF tokens are essential.
             * See: https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
             */
            .csrf(csrf -> {
                if ("None".equalsIgnoreCase(cookieSameSite)) {
                    // SameSite=None provides zero CSRF protection — enable CSRF tokens.
                    // CookieCsrfTokenRepository puts X-XSRF-TOKEN in a readable cookie
                    // that the frontend reads and sends back in a header on state-changing requests.
                    csrf.csrfTokenRepository(
                        org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse()
                    ).csrfTokenRequestHandler(
                        new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler()
                    ).ignoringRequestMatchers(
                        // Public auth endpoints don't have a CSRF cookie yet
                        "/api/auth/login", "/api/auth/register", "/api/auth/logout",
                        "/api/auth/exchange-code", "/api/auth/forgot-password",
                        "/api/auth/reset-password", "/api/auth/reset-password/validate"
                    );
                } else {
                    // SameSite=Lax or Strict — browser handles CSRF natively
                    csrf.disable();
                }
            })
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
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
                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/exchange-code").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/auth/reset-password/validate").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/quiz/share/**").permitAll()
                    .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
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
