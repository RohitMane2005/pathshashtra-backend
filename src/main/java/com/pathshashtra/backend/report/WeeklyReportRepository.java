package com.pathshashtra.backend.report;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    List<WeeklyReport> findByUserIdOrderByWeekStartDesc(Long userId);
    void deleteByUserId(Long userId);
}
