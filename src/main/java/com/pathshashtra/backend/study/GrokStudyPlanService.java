package com.pathshashtra.backend.study;

import com.pathshashtra.backend.common.GroqClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class GrokStudyPlanService {

    private final GroqClient groqClient;

    public GrokStudyPlanService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    public String generateStudyPlan(StudyPlanRequest request, String studentName) {
        long daysUntilExam = ChronoUnit.DAYS.between(LocalDate.now(), request.getExamDate());

        String prompt = """
            You are an expert academic planner for Indian students.

            Student: %s
            Exam: %s
            Exam Type: %s
            Current Level: %s
            Subjects: %s
            Days Until Exam: %d
            Daily Study Hours: %d hours/day

            Create a week-by-week study plan. Keep it concise.

            STRICT RULES:
            - Maximum 4 weeks total
            - Maximum 2 topics per subject per week
            - Maximum 3 days per week per subject
            - Response must be complete valid JSON, never cut off

            Respond ONLY with valid JSON in this exact format:
            {
              "planSummary": "Brief overview",
              "totalWeeks": 4,
              "subjects": [
                {
                  "name": "Subject Name",
                  "totalTopics": 8,
                  "priority": "High",
                  "weeklyTopics": [
                    {
                      "week": 1,
                      "topics": [
                        {
                          "day": 1,
                          "topicName": "Topic name",
                          "estimatedHours": 2,
                          "description": "Brief description"
                        }
                      ]
                    }
                  ]
                }
              ],
              "studyTips": ["Tip 1", "Tip 2"],
              "weeklyGoals": ["Week 1 goal", "Week 2 goal"]
            }

            Do not include markdown, only pure JSON.
            """.formatted(
                studentName,
                request.getPlanTitle(),
                request.getExamType(),
                request.getCurrentLevel(),
                String.join(", ", request.getSubjects()),
                daysUntilExam,
                request.getDailyHours()
        );

        return groqClient.call(prompt, 4000);
    }
}
