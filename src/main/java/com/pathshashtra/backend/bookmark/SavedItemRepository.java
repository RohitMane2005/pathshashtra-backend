package com.pathshashtra.backend.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SavedItemRepository extends JpaRepository<SavedItem, Long> {
    List<SavedItem> findByUserIdOrderBySavedAtDesc(Long userId);
    List<SavedItem> findByUserIdAndType(Long userId, String type);
    Optional<SavedItem> findByUserIdAndTypeAndRefId(Long userId, String type, Long refId);
    boolean existsByUserIdAndTypeAndRefId(Long userId, String type, Long refId);
    void deleteByUserIdAndTypeAndRefId(Long userId, String type, Long refId);
    void deleteByUserId(Long userId);
}
