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
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        // FIX BUG 9: Validate required fields and use safe type conversion
        String title = (String) body.get("title");
        String startTimeStr = (String) body.get("startTime");
        String endTimeStr = (String) body.get("endTime");
        if (title == null || startTimeStr == null || endTimeStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "title, startTime, and endTime are required"));
        }

        Contest contest = new Contest();
        contest.setTitle(title);
        contest.setDescription((String) body.get("description"));
        contest.setStartTime(java.time.LocalDateTime.parse(startTimeStr));
        contest.setEndTime(java.time.LocalDateTime.parse(endTimeStr));
        // Safe Number cast — handles both Integer and Long from JSON
        Number durationNum = (Number) body.getOrDefault("duration", 60);
        contest.setDuration(durationNum.intValue());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> problemMaps = (List<Map<String, Object>>) body.getOrDefault("problems", List.of());
        List<ContestProblem> problems = problemMaps.stream().map(m -> {
            ContestProblem p = new ContestProblem();
            p.setTitle((String) m.get("title"));
            p.setProblemStatement((String) m.get("problemStatement"));
            p.setDifficulty((String) m.getOrDefault("difficulty", "MEDIUM"));
            Number pointsNum = (Number) m.getOrDefault("points", 100);
            p.setPoints(pointsNum.intValue());
            return p;
        }).toList();

        return ResponseEntity.ok(service.createContest(auth.getName(), contest, problems));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(
            @PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        // FIX BUG 9: Null checks for required submit fields
        Object problemIdObj = body.get("problemId");
        String code = (String) body.get("code");
        String language = (String) body.get("language");
        if (problemIdObj == null || code == null || language == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "problemId, code, and language are required"));
        }
        return ResponseEntity.ok(service.submitSolution(
                auth.getName(), id,
                Long.valueOf(problemIdObj.toString()),
                code, language));
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> leaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(service.getLeaderboard(id));
    }
}
