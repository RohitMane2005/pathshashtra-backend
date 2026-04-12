package com.pathshashtra.backend.user;

import com.pathshashtra.backend.auth.PasswordResetRepository;
import com.pathshashtra.backend.bookmark.SavedItemRepository;
import com.pathshashtra.backend.career.CareerAssessmentRepository;
import com.pathshashtra.backend.coding.CodingProblemRepository;
import com.pathshashtra.backend.profile.UserProfileRepository;
import com.pathshashtra.backend.quiz.QuizRepository;
import com.pathshashtra.backend.roadmap.RoadmapRepository;
import com.pathshashtra.backend.study.StudyPlanRepository;
import com.pathshashtra.backend.study.StudyTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final CodingProblemRepository codingProblemRepository;
    private final QuizRepository quizRepository;
    private final RoadmapRepository roadmapRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudyTopicRepository studyTopicRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final UserActivityRepository activityRepository;
    private final SavedItemRepository savedItemRepository;
    private final CareerAssessmentRepository careerAssessmentRepository;

    public UserService(UserRepository userRepository,
            UserProfileRepository profileRepository,
            CodingProblemRepository codingProblemRepository,
            QuizRepository quizRepository,
            RoadmapRepository roadmapRepository,
            StudyPlanRepository studyPlanRepository,
            StudyTopicRepository studyTopicRepository,
            PasswordResetRepository passwordResetRepository,
            UserActivityRepository activityRepository,
            SavedItemRepository savedItemRepository,
            CareerAssessmentRepository careerAssessmentRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.codingProblemRepository = codingProblemRepository;
        this.quizRepository = quizRepository;
        this.roadmapRepository = roadmapRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.studyTopicRepository = studyTopicRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.activityRepository = activityRepository;
        this.savedItemRepository = savedItemRepository;
        this.careerAssessmentRepository = careerAssessmentRepository;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
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
    public List<Map<String, Object>> getLeaderboard() {
        // Single query per metric — aggregate counts grouped by userId
        List<Object[]> problemCounts = codingProblemRepository.countSolvedGroupedByUser();
        List<Object[]> topicCounts = studyTopicRepository.countCompletedGroupedByUser();
        List<Object[]> quizCounts = quizRepository.countCompletedGroupedByUser();

        Map<Long, Long> problemMap = toMap(problemCounts);
        Map<Long, Long> topicMap = toMap(topicCounts);
        Map<Long, Long> quizMap = toMap(quizCounts);

        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getDeletedAt() == null)
                .toList();

        List<Map<String, Object>> board = new ArrayList<>();
        for (User user : users) {
            long problems = problemMap.getOrDefault(user.getId(), 0L);
            long topics = topicMap.getOrDefault(user.getId(), 0L);
            long quizzes = quizMap.getOrDefault(user.getId(), 0L);
            long xp = problems * 50 + topics * 30 + quizzes * 100;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", user.getName());
            entry.put("email", user.getEmail());
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

    @Transactional
    public void deleteAccount(String email) {
        User user = findByEmail(email);
        Long userId = user.getId();

        careerAssessmentRepository.deleteByUserId(userId);
        studyTopicRepository.deleteByStudyPlanUserId(userId);
        studyPlanRepository.deleteByUserId(userId);
        codingProblemRepository.deleteByUserId(userId);
        quizRepository.deleteByUserId(userId);
        roadmapRepository.deleteByUserId(userId);
        profileRepository.deleteByUserId(userId);
        passwordResetRepository.deleteByUserId(userId);
        activityRepository.deleteByUserId(userId);
        savedItemRepository.deleteByUserId(userId);
        userRepository.delete(user);

        log.info("Account deleted for user {}", userId);
    }
}
