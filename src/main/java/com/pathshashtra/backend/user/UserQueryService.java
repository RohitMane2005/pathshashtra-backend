package com.pathshashtra.backend.user;

import com.pathshashtra.backend.coding.CodingProblemRepository;
import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.quiz.QuizRepository;
import com.pathshashtra.backend.study.StudyTopicRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Handles read-only user queries: find by email, streak calculation, leaderboard.
 * Extracted from UserService to eliminate the god-class anti-pattern (was 22 dependencies).
 */
@Service
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserActivityRepository activityRepository;
    private final CodingProblemRepository codingProblemRepository;
    private final StudyTopicRepository studyTopicRepository;
    private final QuizRepository quizRepository;

    public UserQueryService(UserRepository userRepository,
                            UserActivityRepository activityRepository,
                            CodingProblemRepository codingProblemRepository,
                            StudyTopicRepository studyTopicRepository,
                            QuizRepository quizRepository) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.codingProblemRepository = codingProblemRepository;
        this.studyTopicRepository = studyTopicRepository;
        this.quizRepository = quizRepository;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public int getStreak(String email) {
        User user = findByEmail(email);
        List<LocalDate> dates = activityRepository.findDatesByUserIdDesc(user.getId());
        if (dates.isEmpty())
            return 0;

        LocalDate today = LocalDate.now();
        LocalDate expected = dates.get(0).equals(today) ? today : today.minusDays(1);
        if (!dates.get(0).equals(expected))
            return 0;

        int streak = 0;
        for (LocalDate d : dates) {
            if (d.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * FIX: was loading ALL users then doing per-user queries (N+1 problem).
     * Now uses a single aggregate JPQL query per metric, then merges in-memory.
     * Limits to top 20 by XP. Excludes soft-deleted users.
     */
    @Cacheable("leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        // Single query per metric — aggregate counts grouped by userId
        List<Object[]> problemCounts = codingProblemRepository.countSolvedGroupedByUser();
        List<Object[]> topicCounts = studyTopicRepository.countCompletedGroupedByUser();
        List<Object[]> quizCounts = quizRepository.countCompletedGroupedByUser();

        Map<Long, Long> problemMap = toMap(problemCounts);
        Map<Long, Long> topicMap = toMap(topicCounts);
        Map<Long, Long> quizMap = toMap(quizCounts);

        /**
         * HIGH-05 FIX: Instead of loading ALL active users into memory,
         * only load users who have at least one activity (appear in any aggregate map).
         * At 100K users, this reduces memory from 100K entities to ~500 active ones.
         */
        Set<Long> activeUserIds = new HashSet<>();
        activeUserIds.addAll(problemMap.keySet());
        activeUserIds.addAll(topicMap.keySet());
        activeUserIds.addAll(quizMap.keySet());

        if (activeUserIds.isEmpty()) return List.of();

        // Only fetch the users who have XP — not ALL users
        List<User> activeUsers = userRepository.findAllById(activeUserIds).stream()
                .filter(u -> u.getDeletedAt() == null)
                .toList();

        List<Map<String, Object>> board = new ArrayList<>();
        for (User user : activeUsers) {
            long problems = problemMap.getOrDefault(user.getId(), 0L);
            long topics = topicMap.getOrDefault(user.getId(), 0L);
            long quizzes = quizMap.getOrDefault(user.getId(), 0L);
            long xp = problems * 50 + topics * 30 + quizzes * 100;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", user.getName());
            // FIX B2: Do NOT expose email in leaderboard — PII leak
            entry.put("userId", user.getId());
            entry.put("xp", xp);
            entry.put("problems", problems);
            entry.put("topics", topics);
            entry.put("quizzes", quizzes);
            board.add(entry);
        }

        board.sort(Comparator.<Map<String, Object>, Long>comparing(m -> (Long) m.get("xp")).reversed());
        return board.stream().limit(20).toList();
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
