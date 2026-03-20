package com.pathshashtra.backend.study;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findByUserIdAndStatus(Long userId, StudyPlan.PlanStatus status);
    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<StudyPlan> findByIdAndUserId(Long id, Long userId);
}