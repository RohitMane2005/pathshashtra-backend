package com.pathshashtra.backend.user;

import com.pathshashtra.backend.auth.PasswordResetRepository;
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

    public UserService(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       CodingProblemRepository codingProblemRepository,
                       QuizRepository quizRepository,
                       RoadmapRepository roadmapRepository,
                       StudyPlanRepository studyPlanRepository,
                       StudyTopicRepository studyTopicRepository,
                       PasswordResetRepository passwordResetRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.codingProblemRepository = codingProblemRepository;
        this.quizRepository = quizRepository;
        this.roadmapRepository = roadmapRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.studyTopicRepository = studyTopicRepository;
        this.passwordResetRepository = passwordResetRepository;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Hard-deletes all user data in dependency order to avoid FK constraint violations.
     * Wrapped in @Transactional — if anything fails, all deletes are rolled back.
     */
    @Transactional
    public void deleteAccount(String email) {
        User user = findByEmail(email);
        Long userId = user.getId();

        // Delete child records first (FK order)
        studyTopicRepository.deleteByStudyPlanUserId(userId);
        studyPlanRepository.deleteByUserId(userId);
        codingProblemRepository.deleteByUserId(userId);
        quizRepository.deleteByUserId(userId);
        roadmapRepository.deleteByUserId(userId);
        profileRepository.deleteByUserId(userId);
        passwordResetRepository.deleteByUserId(userId);
        userRepository.delete(user);

        log.info("Account deleted for user {}", userId);
    }
}
