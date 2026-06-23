package com.pathshashtra.backend.user;

import com.pathshashtra.backend.coding.CodingProblemRepository;
import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.quiz.QuizRepository;
import com.pathshashtra.backend.study.StudyTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserQueryService — covers user lookup, streak calculation,
 * and leaderboard aggregation.
 */
@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserActivityRepository activityRepository;
    @Mock private CodingProblemRepository codingProblemRepository;
    @Mock private StudyTopicRepository studyTopicRepository;
    @Mock private QuizRepository quizRepository;

    @InjectMocks private UserQueryService userQueryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("findByEmail() returns user when found")
    void findByEmail_found() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = userQueryService.findByEmail("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("findByEmail() throws NotFoundException for unknown email")
    void findByEmail_notFound_throws() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.findByEmail("missing@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getStreak() returns 0 when no activity dates")
    void getStreak_noActivity_returnsZero() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(activityRepository.findDatesByUserIdDesc(1L)).thenReturn(List.of());

        assertThat(userQueryService.getStreak("test@example.com")).isZero();
    }

    @Test
    @DisplayName("getStreak() counts consecutive days correctly")
    void getStreak_consecutiveDays_countsCorrectly() {
        LocalDate today = LocalDate.now();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(activityRepository.findDatesByUserIdDesc(1L)).thenReturn(
                List.of(today, today.minusDays(1), today.minusDays(2))
        );

        assertThat(userQueryService.getStreak("test@example.com")).isEqualTo(3);
    }

    @Test
    @DisplayName("getStreak() breaks on gap day")
    void getStreak_gapInDays_breaksStreak() {
        LocalDate today = LocalDate.now();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(activityRepository.findDatesByUserIdDesc(1L)).thenReturn(
                List.of(today, today.minusDays(1), today.minusDays(3)) // gap at day 2
        );

        assertThat(userQueryService.getStreak("test@example.com")).isEqualTo(2);
    }

    @Test
    @DisplayName("getLeaderboard() returns empty list when no active users")
    void getLeaderboard_noActiveUsers_returnsEmptyList() {
        when(codingProblemRepository.countSolvedGroupedByUser()).thenReturn(List.of());
        when(studyTopicRepository.countCompletedGroupedByUser()).thenReturn(List.of());
        when(quizRepository.countCompletedGroupedByUser()).thenReturn(List.of());

        List<?> result = userQueryService.getLeaderboard();

        assertThat(result).isEmpty();
    }
}
