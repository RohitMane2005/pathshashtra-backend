package com.pathshashtra.backend.report;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<WeeklyReport>> listWeekly(Authentication auth) {
        return ResponseEntity.ok(service.getReports(auth.getName()));
    }

    @GetMapping("/weekly/current")
    public ResponseEntity<Map<String, Object>> currentWeek(Authentication auth) {
        return ResponseEntity.ok(service.getCurrentWeekStats(auth.getName()));
    }

    @GetMapping("/weekly/{id}")
    public ResponseEntity<WeeklyReport> getReport(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getReport(auth.getName(), id));
    }
}
