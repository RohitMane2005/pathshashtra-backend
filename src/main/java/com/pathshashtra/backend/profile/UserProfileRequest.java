package com.pathshashtra.backend.profile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileRequest {
    private String educationLevel;
    private String careerGoal;
    private String experienceLevel;
    private String skills;
}
