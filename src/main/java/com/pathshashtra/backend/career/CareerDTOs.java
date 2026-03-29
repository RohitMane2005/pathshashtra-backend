package com.pathshashtra.backend.career;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/** Full career analysis result returned after assessment submission */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CareerResult {

    private String personalitySummary;
    private String strengthsOverview;
    private List<CareerPath> topCareers;
    private List<String> skillGaps;
    private List<String> personalityTraits;
    private SalaryInsight salaryInsight;
    private List<ActionStep> nextSteps;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CareerPath {
        private String title;
        private int matchPercent;
        private String whyItFits;
        private List<String> keySkills;
        private String indianMarketOutlook;
        private String topCompanies;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SalaryInsight {
        private String role;
        private String entryLevel;
        private String midLevel;
        private String seniorLevel;
        private String growthOutlook;
        private String topHiringCities;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ActionStep {
        private int step;
        private String title;
        private String description;
        private String timeframe;
    }
}
