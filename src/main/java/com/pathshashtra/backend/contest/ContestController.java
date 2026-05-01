package com.pathshashtra.backend.contest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService service;

    public ContestController(ContestService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Contest>> list() {
        return ResponseEntity.ok(service.listContests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getContest(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Contest> create(@RequestBody Map<String, Object> body, Authentication auth) {
        Contest contest = new Contest();
        contest.setTitle((String) body.get("title"));
        contest.setDescription((String) body.get("description"));
        contest.setStartTime(java.time.LocalDateTime.parse((String) body.get("startTime")));
        contest.setEndTime(java.time.LocalDateTime.parse((String) body.get("endTime")));
        contest.setDuration((Integer) body.getOrDefault("duration", 60));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> problemMaps = (List<Map<String, Object>>) body.getOrDefault("problems", List.of());
        List<ContestProblem> problems = problemMaps.stream().map(m -> {
            ContestProblem p = new ContestProblem();
            p.setTitle((String) m.get("title"));
            p.setProblemStatement((String) m.get("problemStatement"));
            p.setDifficulty((String) m.getOrDefault("difficulty", "MEDIUM"));
            p.setPoints((Integer) m.getOrDefault("points", 100));
            return p;
        }).toList();

        return ResponseEntity.ok(service.createContest(auth.getName(), contest, problems));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ContestSubmission> submit(
            @PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        return ResponseEntity.ok(service.submitSolution(
                auth.getName(), id,
                Long.valueOf(body.get("problemId").toString()),
                (String) body.get("code"),
                (String) body.get("language")));
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> leaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(service.getLeaderboard(id));
    }
}
