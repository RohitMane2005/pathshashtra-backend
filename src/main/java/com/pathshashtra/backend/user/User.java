package com.pathshashtra.backend.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String role = "STUDENT";

    private String authProvider = "LOCAL";

    /** Subscription plan: FREE (default) or PRO. */
    private String plan = "FREE";

    /** Soft-delete timestamp. Null = active account. */
    private LocalDateTime deletedAt;
}