package com.pathshashtra.backend.contest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContestRepository extends JpaRepository<Contest, Long> {
    List<Contest> findAllByOrderByStartTimeDesc();
}
