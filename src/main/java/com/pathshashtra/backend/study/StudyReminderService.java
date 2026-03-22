package com.pathshashtra.backend.study;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Sends a daily study reminder email at 9 AM (IST = UTC+5:30 → 03:30 UTC).
 * Only sends to users with an active study plan and exam within the next 60 days.
 * Silently skips if mail is not configured (dev mode).
 */
@Service
public class StudyReminderService {

    private static final Logger log = LoggerFactory.getLogger(StudyReminderService.class);

    private final StudyPlanRepository planRepository;
    private final StudyTopicRepository topicRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public StudyReminderService(StudyPlanRepository planRepository,
                                StudyTopicRepository topicRepository,
                                UserRepository userRepository,
                                JavaMailSender mailSender) {
        this.planRepository = planRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    /** Runs daily at 03:30 UTC = 09:00 IST */
    @Scheduled(cron = "0 30 3 * * *")
    public void sendDailyReminders() {
        boolean devMode = mailUsername == null || mailUsername.isBlank()
                || mailUsername.equals("noreply@pathshashtra.com");
        if (devMode) {
            log.info("[DEV] Skipping daily reminder emails — mail not configured");
            return;
        }

        List<StudyPlan> activePlans = planRepository.findAllByStatus(StudyPlan.PlanStatus.ACTIVE);
        int sent = 0;

        for (StudyPlan plan : activePlans) {
            try {
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), plan.getExamDate());
                if (daysLeft < 0 || daysLeft > 60) continue; // only remind if exam is within 60 days

                long daysSinceStart = ChronoUnit.DAYS.between(plan.getStartDate(), LocalDate.now()) + 1;
                int week = (int) Math.ceil(daysSinceStart / 7.0);
                int day = (int) (daysSinceStart % 7 == 0 ? 7 : daysSinceStart % 7);

                List<StudyTopic> todayTopics = topicRepository
                        .findByStudyPlanIdAndWeekNumberAndDayNumber(plan.getId(), week, day);

                User user = plan.getUser();
                sendReminder(user.getEmail(), user.getName(), plan.getPlanTitle(),
                        todayTopics, daysLeft);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send reminder for plan {}: {}", plan.getId(), e.getMessage());
            }
        }

        log.info("Daily study reminders sent: {}", sent);
    }

    private void sendReminder(String email, String name, String planTitle,
                               List<StudyTopic> topics, long daysLeft) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hey ").append(name).append(",\n\n");
        sb.append("📚 You have ").append(daysLeft).append(" day").append(daysLeft == 1 ? "" : "s")
          .append(" until your exam: ").append(planTitle).append("\n\n");

        if (!topics.isEmpty()) {
            sb.append("Today's topics:\n");
            for (StudyTopic t : topics) {
                sb.append("  • ").append(t.getTopicName())
                  .append(" (").append(t.getSubject()).append(")\n");
            }
        } else {
            sb.append("No scheduled topics today — great time to review weak areas!\n");
        }

        sb.append("\nKeep going 💪 You're doing great.\n\n");
        sb.append("— Team PathShashtra\nhttps://pathshashtra.vercel.app");

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("📚 Today's Study Plan — " + daysLeft + " days to go!");
        msg.setText(sb.toString());
        mailSender.send(msg);
    }
}
