package com.pathshashtra.backend.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.springframework.stereotype.Component;

/**
 * Strips markdown code fences that LLMs sometimes wrap around JSON responses.
 */
@Component
public class JsonCleaner {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Clean then parse — null-safe single entry point for all LLM JSON fields.
     * Returns NullNode (never Java null) so callers can safely call .path(), .asText() etc.
     * FIX: Previously each service called objectMapper.readTree(jsonCleaner.clean(x)) directly,
     * which threw NullPointerException when x was null (e.g. feedbackJson on unsolved problems).
     */
    public JsonNode cleanAndParse(String raw) {
        String cleaned = clean(raw);
        if (cleaned == null) return NullNode.getInstance();
        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            return NullNode.getInstance();
        }
    }

    public String clean(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        s = s.trim();
        // Find first { or [ in case of any leading text (handle both objects and arrays)
        int objStart = s.indexOf('{');
        int arrStart = s.indexOf('[');
        int start = -1;
        if (objStart >= 0 && arrStart >= 0) start = Math.min(objStart, arrStart);
        else if (objStart >= 0) start = objStart;
        else if (arrStart >= 0) start = arrStart;
        if (start > 0) s = s.substring(start);
        return s;
    }
}
