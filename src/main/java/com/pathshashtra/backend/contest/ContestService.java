package com.pathshashtra.backend.contest;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContestService {

    private final ContestRepository contestRepo;
    private final ContestProblemRepository problemRepo;
    private final ContestSubmissionRepository submissionRepo;
    private final UserRepository userRepo;

    public ContestService(ContestRepository contestRepo,
                          ContestProblemRepository problemRepo,
                          ContestSubmissionRepository submissionRepo,
                          UserRepository userRepo) {
        this.contestRepo = contestRepo;
        this.problemRepo = problemRepo;
        this.submissionRepo = submissionRepo;
        this.userRepo = userRepo;
    }

    /**
     * FIX BUG 5: Status is now computed on-the-fly from startTime/endTime
     * instead of writing to the DB in a GET handler. This is side-effect-free,
     * follows REST semantics, and avoids transactional issues.
     */
    public List<Contest> listContests() {
        List<Contest> contests = contestRepo.findAllByOrderByStartTimeDesc();
        LocalDateTime now = LocalDateTime.now();
        for (Contest c : contests) {
            c.setStatus(deriveStatus(c, now));
        }
        return contests;
    }

    /** Derive contest status from time bounds — pure function, no DB writes. */
    private String deriveStatus(Contest c, LocalDateTime now) {
        if (now.isBefore(c.getStartTime())) return "UPCOMING";
        if (now.isAfter(c.getEndTime())) return "ENDED";
        return "ACTIVE";
    }

    public Map<String, Object> getContest(Long id) {
        Contest contest = contestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Contest not found"));
        List<ContestProblem> problems = problemRepo.findByContestIdOrderByOrderIndexAsc(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contest", contest);
        result.put("problems", problems);
        return result;
    }

    @Transactional
    public Contest createContest(String email, Contest contest, List<ContestProblem> problems) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        contest.setCreatedBy(user.getId());
        contest.setCreatedAt(LocalDateTime.now());
        contest.setStatus("UPCOMING");
        Contest saved = contestRepo.save(contest);

        for (int i = 0; i < problems.size(); i++) {
            ContestProblem p = problems.get(i);
            p.setContestId(saved.getId());
            p.setOrderIndex(i);
            problemRepo.save(p);
        }
        return saved;
    }

    @Transactional
    public ContestSubmission submitSolution(String email, Long contestId, Long problemId, String code, String language) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Contest contest = contestRepo.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest not found"));

        // FIX C2: Use deriveStatus() instead of stale DB field.
        // contest.getStatus() holds the value from creation time ("UPCOMING") and is never
        // updated by listContests(). deriveStatus() computes the real-time status.
        String currentStatus = deriveStatus(contest, LocalDateTime.now());
        if (!"ACTIVE".equals(currentStatus)) {
            throw new RuntimeException("Contest is not active");
        }

        ContestSubmission sub = new ContestSubmission();
        sub.setContestId(contestId);
        sub.setUserId(user.getId());
        sub.setProblemId(problemId);
        sub.setCode(code);
        sub.setLanguage(language);
        sub.setUserName(user.getName());
        // Score is calculated based on submission — simplified to points based on problem difficulty
        ContestProblem problem = problemRepo.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
        sub.setScore(problem.getPoints());
        sub.setSubmittedAt(LocalDateTime.now());
        return submissionRepo.save(sub);
    }

    public List<Map<String, Object>> getLeaderboard(Long contestId) {
        List<ContestSubmission> subs = submissionRepo.findLeaderboard(contestId);
        // Group by user, sum scores
        Map<Long, Map<String, Object>> userScores = new LinkedHashMap<>();
        for (ContestSubmission s : subs) {
            userScores.computeIfAbsent(s.getUserId(), uid -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("userId", uid);
                entry.put("name", s.getUserName());
                entry.put("totalScore", 0);
                entry.put("submissions", 0);
                return entry;
            });
            Map<String, Object> entry = userScores.get(s.getUserId());
            entry.put("totalScore", (int) entry.get("totalScore") + s.getScore());
            entry.put("submissions", (int) entry.get("submissions") + 1);
        }
        return userScores.values().stream()
                .sorted(Comparator.<Map<String, Object>, Integer>comparing(m -> (Integer) m.get("totalScore")).reversed())
                .collect(Collectors.toList());
    }
}
