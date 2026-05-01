package com.pathshashtra.backend.achievement;

import com.pathshashtra.backend.coding.CodingProblemRepository;
import com.pathshashtra.backend.discussion.DiscussionPostRepository;
import com.pathshashtra.backend.notification.NotificationService;
import com.pathshashtra.backend.quiz.QuizRepository;
import com.pathshashtra.backend.social.FollowRepository;
import com.pathshashtra.backend.study.StudyTopicRepository;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import com.pathshashtra.backend.user.UserService;
import com.pathshashtra.backend.notes.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AchievementService {

    /** Static badge definitions: key → {emoji, name, description, category} */
    public static final List<Map<String, String>> BADGE_DEFS = List.of(
        Map.of("key","FIRST_PROBLEM",    "emoji","🎯", "name","First Blood",         "desc","Solve your first problem",            "category","coding"),
        Map.of("key","PROBLEM_10",       "emoji","⚡", "name","Problem Solver",       "desc","Solve 10 problems",                   "category","coding"),
        Map.of("key","PROBLEM_50",       "emoji","🔥", "name","Code Warrior",         "desc","Solve 50 problems",                   "category","coding"),
        Map.of("key","PROBLEM_100",      "emoji","💎", "name","Algorithm Master",     "desc","Solve 100 problems",                  "category","coding"),
        Map.of("key","STREAK_7",         "emoji","📅", "name","Week Warrior",         "desc","Maintain a 7-day streak",             "category","streak"),
        Map.of("key","STREAK_30",        "emoji","🏆", "name","Monthly Champion",     "desc","Maintain a 30-day streak",            "category","streak"),
        Map.of("key","QUIZ_MASTER",      "emoji","🧠", "name","Quiz Master",          "desc","Complete 5 career quizzes",           "category","quiz"),
        Map.of("key","STUDY_COMPLETE",   "emoji","📚", "name","Scholar",              "desc","Complete 100% of a study plan",       "category","study"),
        Map.of("key","FIRST_DISCUSSION", "emoji","💬", "name","Community Starter",    "desc","Create your first discussion post",   "category","social"),
        Map.of("key","HELPFUL_10",       "emoji","⭐", "name","Helpful Hand",         "desc","Receive 10 upvotes on discussions",   "category","social"),
        Map.of("key","NOTES_10",         "emoji","📝", "name","Note Taker",           "desc","Create 10 study notes",               "category","study"),
        Map.of("key","SOCIAL_5",         "emoji","👥", "name","Social Butterfly",     "desc","Follow 5 other users",                "category","social")
    );

    private final AchievementRepository achievementRepo;
    private final CodingProblemRepository codingRepo;
    private final QuizRepository quizRepo;
    private final StudyTopicRepository topicRepo;
    private final DiscussionPostRepository discussionRepo;
    private final NoteRepository noteRepo;
    private final FollowRepository followRepo;
    private final NotificationService notifService;
    private final UserRepository userRepo;

    public AchievementService(AchievementRepository achievementRepo,
                              CodingProblemRepository codingRepo,
                              QuizRepository quizRepo,
                              StudyTopicRepository topicRepo,
                              DiscussionPostRepository discussionRepo,
                              NoteRepository noteRepo,
                              FollowRepository followRepo,
                              NotificationService notifService,
                              UserRepository userRepo) {
        this.achievementRepo = achievementRepo;
        this.codingRepo = codingRepo;
        this.quizRepo = quizRepo;
        this.topicRepo = topicRepo;
        this.discussionRepo = discussionRepo;
        this.noteRepo = noteRepo;
        this.followRepo = followRepo;
        this.notifService = notifService;
        this.userRepo = userRepo;
    }

    public List<Map<String, Object>> getAllBadges(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Achievement> unlocked = achievementRepo.findByUserId(user.getId());
        Set<String> unlockedKeys = new HashSet<>();
        Map<String, LocalDateTime> unlockedDates = new HashMap<>();
        for (Achievement a : unlocked) {
            unlockedKeys.add(a.getBadgeKey());
            unlockedDates.put(a.getBadgeKey(), a.getUnlockedAt());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, String> def : BADGE_DEFS) {
            Map<String, Object> badge = new LinkedHashMap<>(def);
            boolean isUnlocked = unlockedKeys.contains(def.get("key"));
            badge.put("unlocked", isUnlocked);
            badge.put("unlockedAt", isUnlocked ? unlockedDates.get(def.get("key")) : null);
            result.add(badge);
        }
        return result;
    }

    /** Check milestones and award new badges. Called after key actions. */
    @Transactional
    public void checkAndAward(Long userId) {
        // This is a simplified check — in production you'd use event-driven patterns
        try {
            long solvedProblems = codingRepo.countSolvedByUserId(userId);
            long quizzes = quizRepo.countByUserId(userId);
            long notes = noteRepo.findByUserIdOrderByIsPinnedDescUpdatedAtDesc(userId).size();
            long following = followRepo.countByFollowerId(userId);

            checkBadge(userId, "FIRST_PROBLEM", solvedProblems >= 1);
            checkBadge(userId, "PROBLEM_10", solvedProblems >= 10);
            checkBadge(userId, "PROBLEM_50", solvedProblems >= 50);
            checkBadge(userId, "PROBLEM_100", solvedProblems >= 100);
            checkBadge(userId, "QUIZ_MASTER", quizzes >= 5);
            checkBadge(userId, "NOTES_10", notes >= 10);
            checkBadge(userId, "SOCIAL_5", following >= 5);
        } catch (Exception e) {
            // Silently fail — achievement checks should never break main flows
        }
    }

    private void checkBadge(Long userId, String badgeKey, boolean condition) {
        if (condition && !achievementRepo.existsByUserIdAndBadgeKey(userId, badgeKey)) {
            Achievement a = new Achievement();
            a.setUserId(userId);
            a.setBadgeKey(badgeKey);
            a.setUnlockedAt(LocalDateTime.now());
            achievementRepo.save(a);

            // Find badge name for notification
            String badgeName = BADGE_DEFS.stream()
                    .filter(d -> d.get("key").equals(badgeKey))
                    .map(d -> d.get("emoji") + " " + d.get("name"))
                    .findFirst().orElse(badgeKey);

            notifService.send(userId, "ACHIEVEMENT",
                    "Badge Unlocked!", "You earned: " + badgeName, "/achievements");
        }
    }
}
