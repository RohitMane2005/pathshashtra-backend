package com.pathshashtra.backend.common;

import org.springframework.stereotype.Component;

/**
 * FIX M7: Basic HTML sanitizer for user-generated content.
 * Strips HTML tags to prevent stored XSS in notes, discussions, and chat.
 * For production-grade HTML support (rich text), consider OWASP Java HTML Sanitizer.
 */
@Component
public class HtmlSanitizer {

    /**
     * Strip all HTML tags from user input.
     * Preserves plain text content while removing any embedded HTML/script tags.
     */
    public String sanitize(String input) {
        if (input == null) return null;
        return input
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<[^>]+>", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
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
