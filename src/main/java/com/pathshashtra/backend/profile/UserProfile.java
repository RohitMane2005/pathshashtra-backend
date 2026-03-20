package com.pathshashtra.backend.profile;

import com.pathshashtra.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String educationLevel;

    private String careerGoal;

    private String experienceLevel;

    private String skills;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}