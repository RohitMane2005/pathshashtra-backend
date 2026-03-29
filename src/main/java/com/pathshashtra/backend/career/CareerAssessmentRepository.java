package com.pathshashtra.backend.career;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CareerAssessmentRepository extends JpaRepository<CareerAssessment, Long> {
    List<CareerAssessment> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CareerAssessment> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
}
