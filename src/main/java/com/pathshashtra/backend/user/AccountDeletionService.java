package com.pathshashtra.backend.user;

import com.pathshashtra.backend.achievement.AchievementRepository;
import com.pathshashtra.backend.auth.PasswordResetRepository;
import com.pathshashtra.backend.bookmark.SavedItemRepository;
import com.pathshashtra.backend.career.CareerAssessmentRepository;
import com.pathshashtra.backend.chat.ChatMessageRepository;
import com.pathshashtra.backend.chat.ChatSessionRepository;
import com.pathshashtra.backend.coding.CodingProblemRepository;
import com.pathshashtra.backend.contest.ContestSubmissionRepository;
import com.pathshashtra.backend.discussion.DiscussionPostRepository;
import com.pathshashtra.backend.discussion.DiscussionReplyRepository;
import com.pathshashtra.backend.discussion.DiscussionVoteRepository;
import com.pathshashtra.backend.exception.BadRequestException;
import com.pathshashtra.backend.notes.NoteRepository;
import com.pathshashtra.backend.notification.NotificationRepository;
import com.pathshashtra.backend.profile.UserProfileRepository;
import com.pathshashtra.backend.quiz.QuizRepository;
import com.pathshashtra.backend.report.WeeklyReportRepository;
import com.pathshashtra.backend.roadmap.RoadmapRepository;
import com.pathshashtra.backend.social.FollowRepository;
import com.pathshashtra.backend.study.StudyPlanRepository;
import com.pathshashtra.backend.study.StudyTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles permanent account deletion with cascading cleanup of all user data.
 * Extracted from UserService to isolate the heavy dependency graph
 * (18 repositories) to only the code path that actually needs them.
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository userRepository;
    private final UserQueryService userQueryService;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    // Domain repositories for cascading deletes
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
    private final DiscussionPostRepository discussionPostRepository;
    private final DiscussionReplyRepository discussionReplyRepository;
    private final DiscussionVoteRepository discussionVoteRepository;
    private final ContestSubmissionRepository contestSubmissionRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NoteRepository noteRepository;
    private final NotificationRepository notificationRepository;
    private final AchievementRepository achievementRepository;
    private final FollowRepository followRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    public AccountDeletionService(
            UserRepository userRepository,
            StringRedisTemplate redisTemplate,
            UserQueryService userQueryService,
            PasswordEncoder passwordEncoder,
            UserProfileRepository profileRepository,
            CodingProblemRepository codingProblemRepository,
            QuizRepository quizRepository,
            RoadmapRepository roadmapRepository,
            StudyPlanRepository studyPlanRepository,
            StudyTopicRepository studyTopicRepository,
            PasswordResetRepository passwordResetRepository,
            UserActivityRepository activityRepository,
            SavedItemRepository savedItemRepository,
            CareerAssessmentRepository careerAssessmentRepository,
            DiscussionPostRepository discussionPostRepository,
            DiscussionReplyRepository discussionReplyRepository,
            DiscussionVoteRepository discussionVoteRepository,
            ContestSubmissionRepository contestSubmissionRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            NoteRepository noteRepository,
            NotificationRepository notificationRepository,
            AchievementRepository achievementRepository,
            FollowRepository followRepository,
            WeeklyReportRepository weeklyReportRepository) {
        this.userRepository = userRepository;
        this.userQueryService = userQueryService;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
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
        this.discussionPostRepository = discussionPostRepository;
        this.discussionReplyRepository = discussionReplyRepository;
        this.discussionVoteRepository = discussionVoteRepository;
        this.contestSubmissionRepository = contestSubmissionRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.noteRepository = noteRepository;
        this.notificationRepository = notificationRepository;
        this.achievementRepository = achievementRepository;
        this.followRepository = followRepository;
        this.weeklyReportRepository = weeklyReportRepository;
    }

    /**
     * FIX H3: Verify password before permanent account deletion.
     * OAuth users (authProvider != LOCAL) are allowed to delete without
     * password verification since they never set a password.
     */
    @Transactional
    public void deleteAccount(String email, String password) {
        User user = userQueryService.findByEmail(email);

        // For LOCAL users: verify password before deletion
        if ("LOCAL".equals(user.getAuthProvider())) {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BadRequestException("Incorrect password. Account deletion cancelled.");
            }
        }

        Long userId = user.getId();

        // Original cleanups
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

        // New feature cleanups
        discussionVoteRepository.deleteByUserId(userId);
        discussionReplyRepository.deleteByUserId(userId);
        discussionPostRepository.deleteByUserId(userId);
        contestSubmissionRepository.deleteByUserId(userId);
        chatMessageRepository.deleteByUserId(userId);
        chatSessionRepository.deleteByUserId(userId);
        noteRepository.deleteByUserId(userId);
        notificationRepository.deleteByUserId(userId);
        achievementRepository.deleteByUserId(userId);
        followRepository.deleteByFollowerIdOrFollowingId(userId, userId);
        weeklyReportRepository.deleteByUserId(userId);

        userRepository.delete(user);

        // HIGH-01 FIX: Invalidate all active JWTs for this user.
        // Uses the same pwd_changed: pattern checked by JwtAuthenticationFilter.
        // Any JWT issued before this timestamp will be rejected, preventing
        // deleted users from making API calls with still-valid cookies.
        String pwdChangeKey = "pwd_changed:" + email;
        redisTemplate.opsForValue().set(pwdChangeKey,
                String.valueOf(System.currentTimeMillis()),
                86400, java.util.concurrent.TimeUnit.SECONDS);

        log.info("[AUDIT] Account permanently deleted for userId={}, all tokens invalidated", userId);
    }
}
