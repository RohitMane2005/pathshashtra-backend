package com.pathshashtra.backend.common;

import org.springframework.stereotype.Component;

/**
 * Strips markdown code fences that LLMs sometimes wrap around JSON responses.
 */
@Component
public class JsonCleaner {

    public String clean(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        s = s.trim();
        // Find first { in case of any leading text
        int start = s.indexOf('{');
        if (start > 0) s = s.substring(start);
        return s;
    }
}
