package com.pathshashtra.backend.common;

/**
 * CRIT-01 FIX: Single source of truth for XP calculation.
 *
 * Previously the XP formula was defined in three different places with
 * TWO different formulas:
 *   - UserQueryService + SocialService: problems*50 + topics*30 + quizzes*100
 *   - ReportService:                    problems*10 + topics*5  + quizzes*15
 *
 * Now every service delegates to this utility class. Changing the formula
 * only requires updating this one file.
 */
public final class XpCalculator {

    public static final int XP_PER_PROBLEM = 50;
    public static final int XP_PER_TOPIC = 30;
    public static final int XP_PER_QUIZ = 100;
    public static final int XP_PER_LEVEL = 500;

    private XpCalculator() {} // utility class — not instantiable

    public static long calculate(long problems, long topics, long quizzes) {
        return problems * XP_PER_PROBLEM + topics * XP_PER_TOPIC + quizzes * XP_PER_QUIZ;
    }

    public static long levelFromXp(long xp) {
        return xp / XP_PER_LEVEL + 1;
    }
}
