package com.pathshashtra.backend.quiz;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuizResult {

    private List<CareerMatch> careerMatches;
    private List<String> skillGaps;
    private List<RoadmapStep> roadmap;
    private SalaryInfo salaryInfo;
    private String summary;

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class CareerMatch {
        private String title;           // e.g. "Software Engineer"
        private int matchPercent;       // e.g. 87
        private String reason;          // Why this matches
        private List<String> requiredSkills;
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class RoadmapStep {
        private int phase;              // 1, 2, 3...
        private String title;           // e.g. "Build Foundations"
        private String duration;        // e.g. "3 months"
        private List<String> actions;   // Specific things to do
        private List<String> resources; // Courses, books, links
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class SalaryInfo {
        private String role;
        private String entryLevel;      // e.g. "₹4–6 LPA"
        private String midLevel;        // e.g. "₹8–15 LPA"
        private String seniorLevel;     // e.g. "₹20–40 LPA"
        private String growthOutlook;   // e.g. "High demand, 25% growth by 2027"
    }
}
