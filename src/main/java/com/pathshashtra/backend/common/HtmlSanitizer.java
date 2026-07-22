package com.pathshashtra.backend.common;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * CRIT-08 FIX: Replaced homebrew regex sanitizer with OWASP Java HTML Sanitizer.
 *
 * The old regex approach was bypassable because:
 *   - <script> regex didn't match newlines (DOTALL mode not set)
 *   - <img onerror="...">, <svg onload="..."> passed through unstripped
 *
 * OWASP Java HTML Sanitizer is battle-tested against all XSS vectors.
 * The FORMATTING policy allows only safe formatting tags (bold, italic, etc.)
 * with no event handlers or script tags.
 */
@Component
public class HtmlSanitizer {

    /**
     * HIGH-10 FIX: Strict plain-text policy — strips ALL HTML tags.
     *
     * Previously used FORMATTING.and(BLOCKS).and(STYLES) which allowed <b>, <i>,
     * <p>, <h1>-<h6>, <blockquote>, and inline CSS styles. This contradicted the
     * "plain text only" intent and allowed users to inject styled content that
     * could disrupt layout in discussions, notes, and profile fields.
     *
     * An empty HtmlPolicyBuilder strips everything — the safest possible policy.
     */
    private static final PolicyFactory PLAIN_TEXT_POLICY =
            new org.owasp.html.HtmlPolicyBuilder().toFactory();

    /**
     * Sanitize input — strips all unsafe HTML including event handlers, scripts, and iframes.
     * Returns null for null input.
     */
    public String sanitize(String input) {
        if (input == null) return null;
        // OWASP sanitize: removes all script/event-handler/iframe tags
        return PLAIN_TEXT_POLICY.sanitize(input);
    }

    /**
     * Sanitize and enforce a maximum length.
     */
    public String sanitize(String input, int maxLen) {
        String s = sanitize(input);
        if (s != null && s.length() > maxLen) {
            return s.substring(0, maxLen);
        }
        return s;
    }
}
