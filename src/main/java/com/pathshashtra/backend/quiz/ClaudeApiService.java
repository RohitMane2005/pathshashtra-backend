package com.pathshashtra.backend.quiz;

import com.pathshashtra.backend.common.GroqClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ClaudeApiService {

    private final GroqClient groqClient;

    public ClaudeApiService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    @Cacheable(value = "quiz-questions", key = "#userProfile.hashCode()")
    public String generateQuizQuestions(String userProfile) {
        String prompt = """
            You are a career guidance expert for Indian college students.

            Based on this student profile:
            %s

            Generate exactly 10 psychometric career assessment questions.
            Each question should reveal personality traits, work preferences,
            strengths, and interests relevant to Indian tech/business careers.

            Respond ONLY with valid JSON in this exact format, no extra text:
            {
              "questions": [
                {
                  "number": 1,
                  "question": "Question text here?",
                  "options": ["A) Option one", "B) Option two", "C) Option three", "D) Option four"]
                }
              ]
            }
            """.formatted(userProfile);

        return groqClient.call(prompt, 2000);
    }

    public String analyzeQuizAnswers(String userProfile, String questionsJson, String answersJson) {
        String prompt = """
            You are an expert AI career counselor for Indian college students.

            Student Profile:
            %s

            Quiz Questions:
            %s

            Student's Answers:
            %s

            Analyze the answers and provide a comprehensive career guidance report.

            Respond ONLY with valid JSON in this exact format, no extra text:
            {
              "summary": "2-3 sentence personalized summary of the student",
              "careerMatches": [
                {
                  "title": "Career title",
                  "matchPercent": 87,
                  "reason": "Why this career suits them",
                  "requiredSkills": ["Skill 1", "Skill 2", "Skill 3"]
                }
              ],
              "skillGaps": ["Gap 1", "Gap 2", "Gap 3", "Gap 4", "Gap 5"],
              "roadmap": [
                {
                  "phase": 1,
                  "title": "Phase title",
                  "duration": "3 months",
                  "actions": ["Action 1", "Action 2", "Action 3"],
                  "resources": ["Resource 1", "Resource 2"]
                }
              ],
              "salaryInfo": {
                "role": "Top matched career title",
                "entryLevel": "₹4-6 LPA",
                "midLevel": "₹10-18 LPA",
                "seniorLevel": "₹25-50 LPA",
                "growthOutlook": "High demand description"
              }
            }

            Rules:
            - Return exactly 3 career matches ranked by match percentage
            - Return exactly 4 roadmap phases
            - All salary figures in Indian LPA format
            - Be specific and actionable for Indian students
            - Do not include markdown, only pure JSON
            """.formatted(userProfile, questionsJson, answersJson);

        return groqClient.call(prompt, 2000);
    }
}
