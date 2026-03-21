package com.pathshashtra.backend.roadmap;

import com.pathshashtra.backend.common.GroqClient;
import org.springframework.stereotype.Service;

@Service
public class GroqRoadmapService {

    private final GroqClient groqClient;

    public GroqRoadmapService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    public String generateRoadmap(RoadmapRequest request, String studentName) {
        String prompt = """
            You are an expert career and learning roadmap generator for Indian college students.

            Student: %s
            Goal: %s
            Current Level: %s
            Timeframe: %s
            Focus Area: %s
            Current Skills: %s

            Generate a detailed, actionable learning roadmap. Respond ONLY with valid JSON, no extra text, no markdown:
            {
              "roadmapTitle": "Roadmap title",
              "goal": "%s",
              "timeframe": "%s",
              "overview": "2-3 sentence overview of this roadmap",
              "totalPhases": 4,
              "phases": [
                {
                  "phase": 1,
                  "title": "Phase title",
                  "duration": "Month 1-2",
                  "objective": "What they will achieve",
                  "topics": ["topic1", "topic2", "topic3", "topic4", "topic5"],
                  "projects": ["Build X project", "Create Y app"],
                  "resources": [
                    {"type": "video", "name": "Resource name", "url": "url or platform"},
                    {"type": "book", "name": "Book name", "author": "Author"},
                    {"type": "practice", "name": "Platform", "url": "url"}
                  ],
                  "milestone": "Milestone checkpoint",
                  "skills": ["skill1", "skill2"],
                  "estimatedHours": 60,
                  "difficulty": "Beginner"
                }
              ],
              "keySkills": ["skill1", "skill2", "skill3", "skill4", "skill5", "skill6"],
              "careerOutcome": "What jobs/roles this roadmap leads to",
              "salaryRange": "Expected salary range after completion",
              "tips": ["tip1", "tip2", "tip3"],
              "dailySchedule": {
                "weekday": "2-3 hours on theory and practice",
                "weekend": "4-5 hours on projects and revision"
              }
            }

            Make it realistic for Indian college students. Include free resources like YouTube channels, free courses.
            Generate exactly 4 phases covering the full %s timeframe.
            """.formatted(
                studentName, request.getGoal(), request.getCurrentLevel(),
                request.getTimeframe(), request.getFocusArea(),
                request.getCurrentSkills() != null ? request.getCurrentSkills() : "None specified",
                request.getGoal(), request.getTimeframe(), request.getTimeframe()
            );

        return groqClient.call(prompt, 4000);
    }
}
