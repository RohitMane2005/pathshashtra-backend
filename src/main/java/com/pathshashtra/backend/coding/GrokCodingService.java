package com.pathshashtra.backend.coding;

import com.pathshashtra.backend.common.GroqClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GrokCodingService {

    private final GroqClient groqClient;

    public GrokCodingService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\"'`<>]", "").strip();
    }

    public String generateProblem(String topic, String difficulty, String language) {
        topic = sanitize(topic); difficulty = sanitize(difficulty); language = sanitize(language);
        String prompt = """
                You are an expert DSA coding tutor for Indian college students preparing for placements and GATE.

                Generate a %s difficulty DSA problem on topic: %s
                Target language: %s

                Respond ONLY with valid JSON, no extra text:
                {
                  "title": "Problem title",
                  "topic": "%s",
                  "difficulty": "%s",
                  "language": "%s",
                  "problemStatement": "Clear problem description",
                  "inputFormat": "Input format description",
                  "outputFormat": "Output format description",
                  "constraints": ["constraint 1", "constraint 2"],
                  "examples": [
                    {
                      "input": "example input",
                      "output": "example output",
                      "explanation": "why this is the answer"
                    }
                  ],
                  "hints": ["hint 1", "hint 2", "hint 3"],
                  "expectedTimeComplexity": "O(n)",
                  "expectedSpaceComplexity": "O(1)",
                  "tags": ["tag1", "tag2"]
                }
                """.formatted(difficulty, topic, language, topic, difficulty, language);

        return groqClient.call(prompt, 2000);
    }

    public String generateHint(String problemJson, String currentCode, int hintsUsed) {
        String prompt = """
                You are a coding tutor helping a student solve a DSA problem.

                Problem:
                %s

                Student's current code (may be empty if just started):
                %s

                Hints already given: %d

                Give hint number %d. Be progressive:
                - Hint 1: High-level approach/algorithm to use
                - Hint 2: Key data structure or technique needed
                - Hint 3: Specific implementation detail

                Respond ONLY with valid JSON:
                {
                  "hintNumber": %d,
                  "hint": "The hint text here",
                  "encouragement": "A short motivating message"
                }
                """.formatted(problemJson, currentCode.isEmpty() ? "Not started yet" : currentCode,
                hintsUsed, hintsUsed + 1, hintsUsed + 1);

        return groqClient.call(prompt, 500);
    }

    public String reviewCode(String problemJson, String submittedCode, String language) {
        String prompt = """
                You are an expert code reviewer and DSA tutor for Indian college students.

                Problem:
                %s

                Student's %s Solution:
                %s

                Review this solution thoroughly.

                Respond ONLY with valid JSON, no extra text:
                {
                  "isCorrect": true,
                  "score": 85,
                  "overallFeedback": "Overall assessment in 2-3 sentences",
                  "strengths": ["strength 1", "strength 2"],
                  "improvements": ["improvement 1", "improvement 2"],
                  "bugs": ["bug description if any"],
                  "optimizedApproach": "Better approach if exists, otherwise say solution is optimal",
                  "timeComplexity": "O(n) - student solution complexity",
                  "spaceComplexity": "O(1) - student solution complexity",
                  "suggestedTimeComplexity": "O(n log n) - optimal complexity",
                  "suggestedSpaceComplexity": "O(1) - optimal complexity"
                }
                """.formatted(problemJson, language, submittedCode);

        return groqClient.call(prompt, 2000);
    }

    @Cacheable(value = "dsa-roadmap", key = "#currentLevel + ':' + #targetGoal")
    public String generateRoadmap(String studentName, String currentLevel, String targetGoal) {
        studentName = sanitize(studentName); currentLevel = sanitize(currentLevel); targetGoal = sanitize(targetGoal);
        String prompt = """
                You are an expert DSA mentor for Indian college students.

                Student: %s
                Current Level: %s
                Target Goal: %s

                Create a concise personalized DSA learning roadmap.

                Respond ONLY with valid JSON, no extra text:
                {
                  "roadmapTitle": "Title",
                  "estimatedDuration": "3 months",
                  "phases": [
                    {
                      "phase": 1,
                      "title": "Phase title",
                      "duration": "2 weeks",
                      "topics": ["topic1", "topic2", "topic3"],
                      "practiceProblems": 20,
                      "milestone": "What student can do after this phase"
                    }
                  ],
                  "dailyPracticeGoal": "2 problems per day",
                  "recommendedResources": ["Resource 1", "Resource 2", "Resource 3"],
                  "tips": ["Tip 1", "Tip 2"]
                }
                """.formatted(studentName, currentLevel, targetGoal);

        return groqClient.call(prompt, 2000);
    }
}
