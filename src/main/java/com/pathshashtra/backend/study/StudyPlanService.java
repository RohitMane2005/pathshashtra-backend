package com.pathshashtra.backend.study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.common.JsonCleaner;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class StudyPlanService {

    private static final Logger log = LoggerFactory.getLogger(StudyPlanService.class);

    private final StudyPlanRepository planRepository;
    private final StudyTopicRepository topicRepository;
    private final UserRepository userRepository;
    private final GrokStudyPlanService grokService;
    private final JsonCleaner jsonCleaner;
    private final ObjectMapper objectMapper;

    public StudyPlanService(StudyPlanRepository planRepository,
            StudyTopicRepository topicRepository,
            UserRepository userRepository,
            GrokStudyPlanService grokService,
            JsonCleaner jsonCleaner,
            ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.grokService = grokService;
        this.jsonCleaner = jsonCleaner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StudyPlan generatePlan(String email, StudyPlanRequest request) {
        User user = getUser(email);

        // Archive existing active plan
        planRepository.findByUserIdAndStatus(user.getId(), StudyPlan.PlanStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(StudyPlan.PlanStatus.ARCHIVED);
                    planRepository.save(existing);
                });

        String planJson = grokService.generateStudyPlan(request, user.getName());

        StudyPlan plan = new StudyPlan();
        plan.setUser(user);
        plan.setPlanTitle(request.getPlanTitle());
        plan.setExamDate(request.getExamDate());
        plan.setDailyHours(request.getDailyHours());
        plan.setStartDate(LocalDate.now());
        plan.setPlanJson(planJson);
        plan.setStatus(StudyPlan.PlanStatus.ACTIVE);
        planRepository.save(plan);

        saveTopicsFromPlan(plan, planJson);
        return plan;
    }

    public Map<String, Object> getActivePlan(String email) {
        User user = getUser(email);
        Optional<StudyPlan> planOpt = planRepository
                .findByUserIdAndStatus(user.getId(), StudyPlan.PlanStatus.ACTIVE);

        if (planOpt.isEmpty()) {
            return Map.of("exists", false);
        }

        StudyPlan plan = planOpt.get();

        Map<String, Object> response = new HashMap<>();
        response.put("exists", true);
        response.put("planId", plan.getId());
        response.put("planTitle", plan.getPlanTitle());
        response.put("examDate", plan.getExamDate());
        response.put("startDate", plan.getStartDate());
        response.put("daysUntilExam", ChronoUnit.DAYS.between(LocalDate.now(), plan.getExamDate()));

        try {
            response.put("plan", jsonCleaner.cleanAndParse(plan.getPlanJson()));
        } catch (Exception e) {
            response.put("plan", plan.getPlanJson());
        }

        return response;
    }

    public List<StudyTopic> getTodaysTopics(String email) {
        User user = getUser(email);
        Optional<StudyPlan> planOpt = planRepository
                .findByUserIdAndStatus(user.getId(), StudyPlan.PlanStatus.ACTIVE);

        if (planOpt.isEmpty())
            return Collections.emptyList();
        StudyPlan plan = planOpt.get();

        long daysSinceStart = ChronoUnit.DAYS.between(plan.getStartDate(), LocalDate.now()) + 1;
        int weekNumber = (int) Math.ceil(daysSinceStart / 7.0);
        int dayNumber = (int) (daysSinceStart % 7 == 0 ? 7 : daysSinceStart % 7);

        log.debug("Today = Day {} → Week {}, Day {}", daysSinceStart, weekNumber, dayNumber);
        return topicRepository.findByStudyPlanIdAndWeekNumberAndDayNumber(plan.getId(), weekNumber, dayNumber);
    }

    @Transactional
    public StudyTopic updateTopicProgress(String email, TopicProgressRequest request) {
        User user = getUser(email);

        StudyTopic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        if (!topic.getStudyPlan().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        topic.setStatus(request.getStatus());
        topic.setConfidenceScore(request.getConfidenceScore());
        boolean isWeak = request.getConfidenceScore() < 4
                || request.getStatus() == StudyTopic.TopicStatus.STRUGGLING;
        topic.setWeak(isWeak);

        return topicRepository.save(topic);
    }

    public List<StudyTopic> getWeakTopics(String email) {
        User user = getUser(email);
        Optional<StudyPlan> planOpt = planRepository
                .findByUserIdAndStatus(user.getId(), StudyPlan.PlanStatus.ACTIVE);
        if (planOpt.isEmpty())
            return Collections.emptyList();
        return topicRepository.findByStudyPlanIdAndIsWeak(planOpt.get().getId(), true);
    }

    public StudyProgressResponse getProgress(String email) {
        User user = getUser(email);
        Optional<StudyPlan> planOpt = planRepository
                .findByUserIdAndStatus(user.getId(), StudyPlan.PlanStatus.ACTIVE);

        if (planOpt.isEmpty()) {
            return new StudyProgressResponse(0, 0, 0, Collections.emptyList(), 0, 0);
        }

        StudyPlan plan = planOpt.get();

        List<StudyTopic> allTopics = topicRepository.findByStudyPlanId(plan.getId());
        int total = allTopics.size();
        int completed = (int) allTopics.stream()
                .filter(t -> t.getStatus() == StudyTopic.TopicStatus.COMPLETED).count();
        int overallPercent = total == 0 ? 0 : (completed * 100) / total;

        List<Object[]> rawProgress = topicRepository.getProgressBySubject(plan.getId());
        List<StudyProgressResponse.SubjectProgress> subjectProgress = new ArrayList<>();
        for (Object[] row : rawProgress) {
            String subject = (String) row[0];
            int subTotal = ((Long) row[1]).intValue();
            int subCompleted = ((Long) row[2]).intValue();
            int subPercent = subTotal == 0 ? 0 : (subCompleted * 100) / subTotal;
            subjectProgress.add(new StudyProgressResponse.SubjectProgress(subject, subTotal, subCompleted, subPercent));
        }

        int weakCount = (int) allTopics.stream().filter(StudyTopic::isWeak).count();
        long daysUntilExam = ChronoUnit.DAYS.between(LocalDate.now(), plan.getExamDate());

        return new StudyProgressResponse(total, completed, overallPercent, subjectProgress, weakCount, daysUntilExam);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void saveTopicsFromPlan(StudyPlan plan, String planJson) {
        try {
            JsonNode root = jsonCleaner.cleanAndParse(planJson);
            JsonNode subjectsNode = root.path("subjects");

            if (subjectsNode.isMissingNode() || !subjectsNode.isArray()) {
                log.warn("No subjects array found in plan JSON for plan {}", plan.getId());
                return;
            }

            List<StudyTopic> topicsToSave = new ArrayList<>();

            for (JsonNode subject : subjectsNode) {
                String subjectName = subject.path("name").asText();
                JsonNode weeklyTopics = subject.path("weeklyTopics");
                if (weeklyTopics.isMissingNode() || !weeklyTopics.isArray())
                    continue;

                for (JsonNode weekNode : weeklyTopics) {
                    int week = weekNode.path("week").asInt();
                    JsonNode topics = weekNode.path("topics");
                    if (topics.isMissingNode() || !topics.isArray())
                        continue;

                    for (JsonNode topicNode : topics) {
                        StudyTopic topic = new StudyTopic();
                        topic.setStudyPlan(plan);
                        topic.setSubject(subjectName);
                        topic.setTopicName(topicNode.path("topicName").asText("Unknown Topic"));
                        topic.setWeekNumber(week);
                        topic.setDayNumber(topicNode.path("day").asInt(1));
                        topic.setStatus(StudyTopic.TopicStatus.PENDING);
                        topic.setConfidenceScore(5);
                        topic.setWeak(false);
                        topicsToSave.add(topic);
                    }
                }
            }

            topicRepository.saveAll(topicsToSave);
            log.info("Saved {} topics for plan {}", topicsToSave.size(), plan.getId());

        } catch (Exception e) {
            log.error("Error saving topics for plan {}: {}", plan.getId(), e.getMessage());
        }
    }
}
