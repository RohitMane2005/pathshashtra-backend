package com.pathshashtra.backend.study;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class StudyPlanRequest {
    private String planTitle;          // e.g. "GATE 2026 Prep"
    private LocalDate examDate;        // Target exam date
    private int dailyHours;            // e.g. 4
    private List<String> subjects;     // e.g. ["Mathematics", "Physics", "Chemistry"]
    private String examType;           // e.g. "JEE", "GATE", "University Exam"
    private String currentLevel;       // "Beginner", "Intermediate", "Advanced"
}