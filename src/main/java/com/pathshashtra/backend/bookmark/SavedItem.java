package com.pathshashtra.backend.bookmark;

import com.pathshashtra.backend.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_items", indexes = {
    @Index(name = "idx_saved_user_id", columnList = "user_id"),
    @Index(name = "idx_saved_unique", columnList = "user_id, type, ref_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SavedItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private User user;

    @Column(nullable = false) private String type;
    @Column(name = "ref_id", nullable = false) private Long refId;
    private String label;
    private LocalDateTime savedAt = LocalDateTime.now();
}
