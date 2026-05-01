package com.pathshashtra.backend.contest;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contest_problems")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ContestProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String problemStatement;

    private String title;
    private String difficulty;
    private int points = 100;
    private int orderIndex;
}
