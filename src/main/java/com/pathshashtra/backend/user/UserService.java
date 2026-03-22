package com.pathshashtra.backend.user;

import com.pathshashtra.backend.auth.PasswordResetRepository;
import com.pathshashtra.backend.bookmark.SavedItemRepository;
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

    public UserService(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       CodingProblemRepository codingProblemRepository,
                       QuizRepository quizRepository,
                       RoadmapRepository roadmapRepository,
                       StudyPlanRepository studyPlanRepository,
                       StudyTopicRepository studyTopicRepository,
                       PasswordResetRepository passwordResetRepository,
                       UserActivityRepository activityRepository,
                       SavedItemRepository savedItemRepository) {
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
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Calculates consecutive-day login streak from activity records.
     * Streak = number of consecutive calendar days ending today (or yesterday if not active today).
     */
    public int getStreak(String email) {
        User user = findByEmail(email);
        List<LocalDate> dates = activityRepository.findDatesByUserIdDesc(user.getId());
        if (dates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        LocalDate expected = dates.get(0).equals(today) ? today : today.minusDays(1);
        if (!dates.get(0).equals(expected)) return 0;

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
     * Leaderboard: top 20 users by XP.
     * XP = solved problems * 50 + completed topics * 30 + completed quizzes * 100
     * Computed in-app from existing tables — no new columns needed.
     */
    public List<Map<String, Object>> getLeaderboard() {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getDeletedAt() == null)
                .toList();

        List<Map<String, Object>> board = new ArrayList<>();
        for (User user : users) {
            long problems = codingProblemRepository.countSolvedByUserId(user.getId());
            long topics   = studyTopicRepository.countCompletedByUserId(user.getId());
            long quizzes  = quizRepository.countCompletedByUserId(user.getId());
            long xp = problems * 50 + topics * 30 + quizzes * 100;

            Map<String, Object> entry = new HashMap<>();
            entry.put("name", user.getName());
            entry.put("xp", xp);
            entry.put("problems", problems);
            entry.put("topics", topics);
            entry.put("quizzes", quizzes);
            board.add(entry);
        }

        board.sort(Comparator.<Map<String, Object>, Long>comparing(m -> (Long) m.get("xp")).reversed());
        return board.stream().limit(20).toList();
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = findByEmail(email);
        Long userId = user.getId();

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
