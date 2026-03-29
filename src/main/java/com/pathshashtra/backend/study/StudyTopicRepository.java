package com.pathshashtra.backend.study;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface StudyTopicRepository extends JpaRepository<StudyTopic, Long> {
    List<StudyTopic> findByStudyPlanId(Long planId);
    List<StudyTopic> findByStudyPlanIdAndIsWeak(Long planId, boolean isWeak);
    List<StudyTopic> findByStudyPlanIdAndSubject(Long planId, String subject);
    List<StudyTopic> findByStudyPlanIdAndWeekNumberAndDayNumber(Long planId, int week, int day);

    @Query("SELECT t.subject, COUNT(t), SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) " +
           "FROM StudyTopic t WHERE t.studyPlan.id = :planId GROUP BY t.subject")
    List<Object[]> getProgressBySubject(Long planId);

    void deleteByStudyPlanUserId(Long userId);

    @Query("SELECT COUNT(t) FROM StudyTopic t WHERE t.studyPlan.user.id = :userId AND t.status = 'COMPLETED'")
    long countCompletedByUserId(Long userId);

    /** FIX: grouped aggregate for leaderboard — avoids N+1 */
    @Query("SELECT t.studyPlan.user.id, COUNT(t) FROM StudyTopic t WHERE t.status = 'COMPLETED' GROUP BY t.studyPlan.user.id")
    List<Object[]> countCompletedGroupedByUser();
}
