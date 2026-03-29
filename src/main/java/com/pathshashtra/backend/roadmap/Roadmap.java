package com.pathshashtra.backend.roadmap;

import com.pathshashtra.backend.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "roadmaps", indexes = {
    @Index(name = "idx_roadmap_user_id", columnList = "user_id")
})
@Data
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private User user;

    private String goal;
    private String currentLevel;
    private String timeframe;
    private String focusArea;

    @Column(columnDefinition = "TEXT")
    private String roadmapJson;

    private LocalDateTime createdAt = LocalDateTime.now();
}
