package com.pathshashtra.backend.study;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class StudyPlanRequest {

    @NotBlank(message = "Plan title is required")
    @Size(max = 200)
    private String planTitle;

    @NotNull(message = "Exam date is required")
    private LocalDate examDate;

    @Min(value = 1, message = "Daily hours must be at least 1")
    @Max(value = 16, message = "Daily hours cannot exceed 16")
    private int dailyHours;

    @Size(max = 20, message = "Maximum 20 subjects allowed")
    private List<@Size(max = 100) String> subjects;

    @Size(max = 50)
    private String examType;

    @Size(max = 50)
    private String currentLevel;
}
