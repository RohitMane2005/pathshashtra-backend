package com.pathshashtra.backend.career;

import com.pathshashtra.backend.common.GroqClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CareerAIService {

    private final GroqClient groqClient;

    public CareerAIService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    /**
     * Generate 12 psychometric questions tailored to the student's profile.
     * Covers personality, work style, interests, values, and strengths.
     */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\\\"'`<>{}]", "").strip();
    }

    @Cacheable(value = "career-questions", key = "#userProfile.hashCode()")
    public String generateAssessmentQuestions(String userProfile) {
        String prompt = """
            You are an expert psychometric career counselor for Indian college students.

            Student Profile:
            %s

            Generate exactly 12 career psychometric assessment questions.
            These questions should deeply assess:
            - Work style preferences (collaborative vs solo, structured vs creative)
            - Core strengths and natural talents
            - Values (money, impact, stability, innovation, leadership)
            - Problem-solving approach
            - Communication style
            - Long-term ambitions relevant to Indian tech/business landscape

            Respond ONLY with valid JSON, no extra text or markdown:
            {
              "questions": [
                {
                  "number": 1,
                  "category": "Work Style",
                  "question": "Question text here?",
                  "options": [
                    "A) First option",
                    "B) Second option",
                    "C) Third option",
                    "D) Fourth option"
                  ]
                }
              ]
            }

            Rules:
            - Each question must have exactly 4 options labeled A) B) C) D)
            - Questions must be relevant to Indian students (IIT, NIT, private colleges, tier-2 cities)
            - Cover all categories: Work Style, Strengths, Values, Problem Solving, Communication, Ambition
            - Make questions insightful and scenario-based, not trivial
            """.formatted(sanitize(userProfile));

        return groqClient.call(prompt, 2500);
    }

    /**
     * Analyse psychometric answers and produce a full career guidance report.
     */
    public String analyzeAssessment(String userProfile, String questionsJson, String answersJson) {
        String prompt = """
            You are a world-class career counselor specializing in Indian tech and business careers.

            Student Profile:
            %s

            Assessment Questions (JSON):
            %s

            Student's Answers:
            %s

            Analyze the answers thoroughly and produce a comprehensive, personalized career guidance report.

            Respond ONLY with valid JSON in this exact format, no markdown or extra text:
            {
              "personalitySummary": "2-3 sentence personalized personality overview of this specific student",
              "strengthsOverview": "2 sentences about their unique strengths",
              "personalityTraits": ["Trait 1", "Trait 2", "Trait 3", "Trait 4", "Trait 5"],
              "topCareers": [
                {
                  "title": "Career title",
                  "matchPercent": 91,
                  "whyItFits": "Specific reason this career matches their personality and strengths",
                  "keySkills": ["Skill 1", "Skill 2", "Skill 3", "Skill 4"],
                  "indianMarketOutlook": "Strong demand in India, especially in Bengaluru/Hyderabad/Pune",
                  "topCompanies": "Google, Microsoft, Flipkart, Swiggy, startups"
                }
              ],
              "skillGaps": ["Gap 1", "Gap 2", "Gap 3", "Gap 4", "Gap 5"],
              "salaryInsight": {
                "role": "Top matched career title",
                "entryLevel": "₹5-8 LPA",
                "midLevel": "₹15-25 LPA",
                "seniorLevel": "₹35-70 LPA",
                "growthOutlook": "One sentence on market demand and future growth",
                "topHiringCities": "Bengaluru, Hyderabad, Pune, Mumbai, Delhi NCR"
              },
              "nextSteps": [
                {
                  "step": 1,
                  "title": "Action step title",
                  "description": "Specific actionable advice",
                  "timeframe": "This week / Next month / In 3 months"
                }
              ]
            }

            Rules:
            - Return exactly 3 career paths ranked by match percentage (highest first)
            - Return exactly 5 next steps, ordered from immediate to long-term
            - All salaries in Indian LPA format (₹X-Y LPA)
            - Be highly specific and personalized — reference their answers and profile
            - Do NOT return generic advice; tailor everything to this student
            - Only pure JSON, no markdown, no extra explanation
            """.formatted(userProfile, questionsJson, answersJson);

        return groqClient.call(prompt, 3000);
    }
}
