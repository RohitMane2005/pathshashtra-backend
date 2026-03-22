package com.pathshashtra.backend.roadmap;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoadmapRequest {

    @NotBlank(message = "Goal is required")
    @Size(max = 200, message = "Goal must be under 200 characters")
    private String goal;

    @Size(max = 50)
    private String currentLevel;

    @Size(max = 50)
    private String timeframe;

    @Size(max = 100)
    private String focusArea;

    @Size(max = 500, message = "Current skills must be under 500 characters")
    private String currentSkills;
}
