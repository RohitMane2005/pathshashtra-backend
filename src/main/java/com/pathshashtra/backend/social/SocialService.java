package com.pathshashtra.backend.social;

import com.pathshashtra.backend.common.XpCalculator;

import com.pathshashtra.backend.coding.CodingProblemRepository;
import com.pathshashtra.backend.notification.NotificationService;
import com.pathshashtra.backend.quiz.QuizRepository;
import com.pathshashtra.backend.study.StudyTopicRepository;
import com.pathshashtra.backend.exception.BadRequestException;
import com.pathshashtra.backend.exception.NotFoundException;
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
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (me.getId().equals(targetUserId)) {
            throw new BadRequestException("Cannot follow yourself");
        }
        User target = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Target user not found"));

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

    /**
     * HIGH-02 FIX: Batch-fetch users instead of N+1.
     * OLD: each Follow → findById + existsBy = 2 queries per follower.
     * NEW: 1 query for all users + 1 query for follow status = 2 total.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowing(String email) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        List<Long> followingIds = followRepo.findByFollowerId(me.getId()).stream()
                .map(Follow::getFollowingId).toList();
        return batchUserMaps(followingIds, me.getId());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowers(String email) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        List<Long> followerIds = followRepo.findByFollowingId(me.getId()).stream()
                .map(Follow::getFollowerId).toList();
        return batchUserMaps(followerIds, me.getId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPublicProfile(String email, Long userId) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        User target = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        long problems = codingRepo.countSolvedByUserId(userId);
        long topics = topicRepo.countCompletedByUserId(userId);
        long quizzes = quizRepo.countByUserId(userId);
        long xp = XpCalculator.calculate(problems, topics, quizzes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", target.getId());
        result.put("name", target.getName());
        result.put("problems", problems);
        result.put("topics", topics);
        result.put("quizzes", quizzes);
        result.put("xp", xp);
        result.put("level", XpCalculator.levelFromXp(xp));
        result.put("followers", followRepo.countByFollowingId(userId));
        result.put("following", followRepo.countByFollowerId(userId));
        result.put("isFollowing", followRepo.existsByFollowerIdAndFollowingId(me.getId(), userId));
        return result;
    }

    /**
     * FIX BUG 4: DB-level search replaces findAll() full table scan.
     * Follow status is batch-checked with a single query instead of N+1.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchUsers(String email, String query) {
        User me = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

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
                .orElseThrow(() -> new NotFoundException("User not found"));
        Map<String, Object> myStats = getPublicProfile(email, me.getId());
        Map<String, Object> theirStats = getPublicProfile(email, userId);
        return Map.of("me", myStats, "them", theirStats);
    }

    /**
     * HIGH-02 FIX: Batch-fetch all users by IDs in one query, then check follow
     * status for all of them in one query. Replaces the old userToMap() which did
     * 2 DB queries per user (N+1 pattern).
     */
    private List<Map<String, Object>> batchUserMaps(List<Long> userIds, Long myId) {
        if (userIds.isEmpty()) return List.of();
        List<User> users = userRepo.findAllById(userIds);
        Set<Long> myFollowingIds = followRepo.findByFollowerId(myId).stream()
                .map(Follow::getFollowingId)
                .filter(userIds::contains)
                .collect(Collectors.toSet());

        return users.stream()
                .filter(u -> u.getDeletedAt() == null)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", u.getId());
                    m.put("name", u.getName());
                    m.put("isFollowing", myFollowingIds.contains(u.getId()));
                    return m;
                })
                .collect(Collectors.toList());
    }
}
