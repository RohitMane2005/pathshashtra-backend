package com.pathshashtra.backend.roadmap;

import lombok.Data;

@Data
public class RoadmapRequest {
    private String goal;              // e.g. "Become a Full Stack Developer"
    private String currentLevel;      // Beginner / Intermediate / Advanced
    private String timeframe;         // "3 months", "6 months", "1 year"
    private String focusArea;         // "Frontend", "Backend", "DSA", "AI/ML", "DevOps", etc.
    private String currentSkills;     // comma-separated skills they already have
}
