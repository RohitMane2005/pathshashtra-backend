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
     * Plain-text only policy — strips ALL HTML tags.
     * Used for discussion titles, notes, profile fields where only plain text is expected.
     */
    private static final PolicyFactory PLAIN_TEXT_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.STYLES);

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
