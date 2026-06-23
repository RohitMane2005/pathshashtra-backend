package com.pathshashtra.backend.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Only returns active (non-deleted) users. */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmail(String email);

    /** FIX BUG 4: DB-level user search — replaces in-memory findAll() + filter. */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.id <> :excludeId " +
           "AND LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchByName(String query, Long excludeId, Pageable pageable);
}
