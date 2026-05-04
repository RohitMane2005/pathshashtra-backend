package com.pathshashtra.backend.social;

import com.pathshashtra.backend.coding.CodingProblemRepository;
import com.pathshashtra.backend.notification.NotificationService;
import com.pathshashtra.backend.quiz.QuizRepository;
import com.pathshashtra.backend.study.StudyTopicRepository;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SocialService {

    private final FollowRepository followRepo;
    private final UserRepository userRepo;
    private final CodingProblemRepository codingRepo;
    private final QuizRepository quizRepo;
    private final StudyTopicRepository topicRepo;
    private final NotificationService notifService;

    public SocialService(FollowRepository followRepo, UserRepository userRepo,
                         CodingProblemRepository codingRepo, QuizRepository quizRepo,
                         StudyTopicRepository topicRepo, NotificationService notifService) {
        this.followRepo = followRepo;
        this.userRepo = userRepo;
        this.codingRepo = codingRepo;
        this.quizRepo = quizRepo;
        this.topicRepo = topicRepo;
        this.notifService = notifService;
    }

    @Transactional
    public Map<String, Object> toggleFollow(String email, Long targetUserId) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (me.getId().equals(targetUserId)) {
            throw new RuntimeException("Cannot follow yourself");
        }
        User target = userRepo.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        Optional<Follow> existing = followRepo.findByFollowerIdAndFollowingId(me.getId(), targetUserId);
        boolean following;
        if (existing.isPresent()) {
            followRepo.delete(existing.get());
            following = false;
        } else {
            Follow f = new Follow();
            f.setFollowerId(me.getId());
            f.setFollowingId(targetUserId);
            f.setCreatedAt(LocalDateTime.now());
            followRepo.save(f);
            following = true;
            notifService.send(targetUserId, "SOCIAL",
                    "New Follower", me.getName() + " started following you", "/social");
        }
        return Map.of("following", following);
    }

    public List<Map<String, Object>> getFollowing(String email) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return followRepo.findByFollowerId(me.getId()).stream()
                .map(f -> userToMap(f.getFollowingId(), me.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFollowers(String email) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return followRepo.findByFollowingId(me.getId()).stream()
                .map(f -> userToMap(f.getFollowerId(), me.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getPublicProfile(String email, Long userId) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User target = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long problems = codingRepo.countSolvedByUserId(userId);
        long topics = topicRepo.countCompletedByUserId(userId);
        long quizzes = quizRepo.countByUserId(userId);
        long xp = problems * 50 + topics * 30 + quizzes * 100;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", target.getId());
        result.put("name", target.getName());
        result.put("problems", problems);
        result.put("topics", topics);
        result.put("quizzes", quizzes);
        result.put("xp", xp);
        result.put("level", xp / 500 + 1);
        result.put("followers", followRepo.countByFollowingId(userId));
        result.put("following", followRepo.countByFollowerId(userId));
        result.put("isFollowing", followRepo.existsByFollowerIdAndFollowingId(me.getId(), userId));
        return result;
    }

    /**
     * FIX BUG 4: DB-level search replaces findAll() full table scan.
     * Follow status is batch-checked with a single query instead of N+1.
     */
    public List<Map<String, Object>> searchUsers(String email, String query) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // FIX M5: Truncate search input to prevent oversized LIKE queries
        if (query == null || query.isBlank()) return List.of();
        String safeQuery = query.trim();
        if (safeQuery.length() > 100) safeQuery = safeQuery.substring(0, 100);
        // FIX M6: Escape SQL wildcards to prevent matching every user with '%' or '_'
        safeQuery = safeQuery.replace("%", "\\%").replace("_", "\\_");

        List<User> matched = userRepo.searchByName(
                safeQuery, me.getId(), org.springframework.data.domain.PageRequest.of(0, 20));

        // Batch-check follow status for all matched users in one query
        List<Long> matchedIds = matched.stream().map(User::getId).toList();
        Set<Long> followingIds = matchedIds.isEmpty() ? Set.of()
                : followRepo.findByFollowerId(me.getId()).stream()
                    .map(Follow::getFollowingId)
                    .filter(matchedIds::contains)
                    .collect(Collectors.toSet());

        return matched.stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", u.getId());
                    m.put("name", u.getName());
                    m.put("isFollowing", followingIds.contains(u.getId()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> compareWith(String email, Long userId) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> myStats = getPublicProfile(email, me.getId());
        Map<String, Object> theirStats = getPublicProfile(email, userId);
        return Map.of("me", myStats, "them", theirStats);
    }

    private Map<String, Object> userToMap(Long userId, Long myId) {
        return userRepo.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", u.getId());
                    m.put("name", u.getName());
                    m.put("isFollowing", followRepo.existsByFollowerIdAndFollowingId(myId, u.getId()));
                    return m;
                }).orElse(null);
    }
}
