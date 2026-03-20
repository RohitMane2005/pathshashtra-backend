package com.pathshashtra.backend.roadmap;

import com.pathshashtra.backend.user.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "roadmaps")
@Data
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String goal;
    private String currentLevel;
    private String timeframe;
    private String focusArea;

    @Column(columnDefinition = "TEXT")
    private String roadmapJson;

    private LocalDateTime createdAt = LocalDateTime.now();
}
