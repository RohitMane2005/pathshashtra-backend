package com.pathshashtra.backend.report;

import com.pathshashtra.backend.common.XpCalculator;

import com.pathshashtra.backend.coding.CodingProblemRepository;
import com.pathshashtra.backend.quiz.QuizRepository;
import com.pathshashtra.backend.exception.ForbiddenException;
import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.study.StudyTopicRepository;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReportService {

    private final WeeklyReportRepository reportRepo;
    private final UserRepository userRepo;
    private final CodingProblemRepository codingRepo;
    private final QuizRepository quizRepo;
    private final StudyTopicRepository topicRepo;

    public ReportService(WeeklyReportRepository reportRepo, UserRepository userRepo,
                         CodingProblemRepository codingRepo, QuizRepository quizRepo,
                         StudyTopicRepository topicRepo) {
        this.reportRepo = reportRepo;
        this.userRepo = userRepo;
        this.codingRepo = codingRepo;
        this.quizRepo = quizRepo;
        this.topicRepo = topicRepo;
    }

    @Transactional(readOnly = true)
    public List<WeeklyReport> getReports(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return reportRepo.findByUserIdOrderByWeekStartDesc(user.getId());
    }

    /** Live stats for the current week */
    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentWeekStats(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.atTime(23, 59, 59);

        // MED-04 FIX: Use date-bounded counts for weekly stats instead of all-time.
        // These use createdAt-based queries scoped to the current week.
        long problems = codingRepo.countByUserIdAndStatusAndCreatedAtBetween(
                user.getId(), "SOLVED", weekStartTime, weekEndTime);
        long topics = topicRepo.countByStudyPlanUserIdAndCompletedTrue(
                user.getId(), weekStartTime, weekEndTime);
        long quizzes = quizRepo.countByUserIdAndCreatedAtBetween(
                user.getId(), weekStartTime, weekEndTime);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("weekStart", weekStart);
        stats.put("weekEnd", weekEnd);
        stats.put("problemsSolved", problems);
        stats.put("topicsCompleted", topics);
        stats.put("quizzesCompleted", quizzes);

        // CRIT-01 FIX: Use canonical XP formula from XpCalculator
        long xp = XpCalculator.calculate(problems, topics, quizzes);
        stats.put("xpTotal", xp);
        stats.put("level", XpCalculator.levelFromXp(xp));

        // Get previous week for comparison
        List<WeeklyReport> reports = reportRepo.findByUserIdOrderByWeekStartDesc(user.getId());
        if (!reports.isEmpty()) {
            WeeklyReport prev = reports.get(0);
            stats.put("prevProblems", prev.getProblemsSolved());
            stats.put("prevTopics", prev.getTopicsCompleted());
            stats.put("prevQuizzes", prev.getQuizzesCompleted());
            stats.put("prevXp", prev.getXpGained());
        }

        return stats;
    }

    @Transactional(readOnly = true)
    public WeeklyReport getReport(String email, Long reportId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        WeeklyReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found"));
        if (!report.getUserId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
        return report;
    }
}
