package com.pathshashtra.backend.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sliding-window rate limiter — no extra dependencies.
 *
 * FIX 1 (off-by-one): the old code wrote the timestamp THEN checked the count.
 *   That means the (max+1)-th request was written to the store before being
 *   rejected, so every window allowed max+1 requests, not max.
 *   Fixed: check count BEFORE writing, only write if allowed.
 *
 * FIX 2 (register flood): added allowRegister() — 5 accounts/hour per IP.
 *   Without this, an attacker can create unlimited accounts, exhausting DB
 *   and filling user tables.
 *
 * FIX 3 (memory): cleanup window extended to 24h to cover daily AI limits.
 */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, long[]> store = new ConcurrentHashMap<>();
    private static final long DAY = 86_400L;

    /**
     * Sliding window check + record.
     * Returns true (allowed) only if current window count < max BEFORE recording.
     * The request timestamp is only written when allowed — no off-by-one.
     */
    public boolean isAllowed(String key, int max, long windowSeconds) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;

        // Use AtomicReference to capture the allow decision inside compute()
        AtomicReference<Boolean> allowed = new AtomicReference<>(false);

        store.compute(key, (k, existing) -> {
            if (existing == null) existing = new long[0];

            // Count how many timestamps are still within the window
            int count = 0;
            for (long ts : existing) if (ts > windowStart) count++;

            if (count >= max) {
                // Limit reached — compact stale but do NOT add current timestamp
                allowed.set(false);
                long[] compacted = new long[count];
                int i = 0;
                for (long ts : existing) if (ts > windowStart) compacted[i++] = ts;
                return compacted;
            }

            // Allowed — record this request
            allowed.set(true);
            long[] compacted = new long[count + 1];
            int i = 0;
            for (long ts : existing) if (ts > windowStart) compacted[i++] = ts;
            compacted[i] = now;
            return compacted;
        });

        return allowed.get();
    }

    // ── Auth limits ───────────────────────────────────────────────────────

    /**
     * Login: both IP and email must pass.
     * IP:    10/min  — blocks password spraying from one machine
     * Email: 5/5min  — blocks credential stuffing against one account
     */
    public boolean allowLogin(String ip, String email) {
        boolean ipOk    = isAllowed("login_ip:"    + ip,    10, 60);
        boolean emailOk = isAllowed("login_email:" + email,  5, 300);
        return ipOk && emailOk;
    }

    /**
     * Register: 5 accounts/hour per IP.
     * Prevents account creation floods and DB exhaustion.
     * Note: this uses IP only since email changes with every request.
     */
    public boolean allowRegister(String ip) {
        return isAllowed("register_ip:" + ip, 5, 3600);
    }

    // ── Per-user AI daily limits ──────────────────────────────────────────

    public boolean allowRoadmapGenerate(String email)  { return isAllowed("ai_roadmap:"        + email, 5,  DAY); }
    public boolean allowQuizStart(String email)         { return isAllowed("ai_quiz:"           + email, 3,  DAY); }
    public boolean allowCodingGenerate(String email)    { return isAllowed("ai_coding_gen:"     + email, 20, DAY); }
    public boolean allowCodingHint(String email)        { return isAllowed("ai_coding_hint:"    + email, 30, DAY); }
    public boolean allowCodingSubmit(String email)      { return isAllowed("ai_coding_submit:"  + email, 30, DAY); }
    public boolean allowStudyPlanGenerate(String email) { return isAllowed("ai_study:"          + email, 3,  DAY); }

    public int remaining(String prefix, String email, int max) {
        long windowStart = Instant.now().getEpochSecond() - DAY;
        long[] ts = store.get(prefix + email);
        if (ts == null) return max;
        int used = 0;
        for (long t : ts) if (t > windowStart) used++;
        return Math.max(0, max - used);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    /** Evict entries whose newest timestamp is older than 24 hours. Runs every 10 min. */
    @Scheduled(fixedDelay = 600_000)
    public void evictStaleEntries() {
        long cutoff = Instant.now().getEpochSecond() - DAY;
        store.entrySet().removeIf(e -> {
            long[] ts = e.getValue();
            return ts == null || ts.length == 0 || ts[ts.length - 1] < cutoff;
        });
    }
}
