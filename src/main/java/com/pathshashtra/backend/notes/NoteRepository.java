package com.pathshashtra.backend.notes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByUserIdOrderByIsPinnedDescUpdatedAtDesc(Long userId);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND n.category = :category ORDER BY n.isPinned DESC, n.updatedAt DESC")
    List<Note> findByUserIdAndCategory(Long userId, String category);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND LOWER(n.title) LIKE LOWER(CONCAT('%',:q,'%')) ORDER BY n.updatedAt DESC")
    List<Note> search(Long userId, String q);

    void deleteByUserId(Long userId);

    /** M-03 FIX: Efficient COUNT instead of loading all notes to count them. */
    long countByUserId(Long userId);
}
