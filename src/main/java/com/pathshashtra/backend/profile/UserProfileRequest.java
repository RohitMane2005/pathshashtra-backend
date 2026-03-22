package com.pathshashtra.backend.profile;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserProfileRequest {

    @Size(max = 200, message = "Education level must be under 200 characters")
    private String educationLevel;

    @Size(max = 300, message = "Career goal must be under 300 characters")
    private String careerGoal;

    @Size(max = 50, message = "Experience level must be under 50 characters")
    private String experienceLevel;

    @Size(max = 1000, message = "Skills must be under 1000 characters")
    private String skills;
}
