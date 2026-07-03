-- V2: Align database schema with current JPA entities.
--
-- CRIT-03 FIX: V1 migration was a baseline from an older version.
-- The actual JPA entities have evolved significantly — Hibernate's
-- validate mode will FAIL on startup without these column additions.
--
-- This migration is idempotent (IF NOT EXISTS / ADD COLUMN IF NOT EXISTS)
-- so it is safe to run on both fresh and existing databases.

-- =====================================================================
-- CODING PROBLEMS — entity has many more columns than V1
-- =====================================================================
ALTER TABLE coding_problems ADD COLUMN IF NOT EXISTS topic VARCHAR(255);
ALTER TABLE coding_problems ADD COLUMN IF NOT EXISTS problem_title VARCHAR(500);
ALTER TABLE coding_problems ADD COLUMN IF NOT EXISTS problem_json TEXT;
ALTER TABLE coding_problems ADD COLUMN IF NOT EXISTS submitted_code TEXT;
ALTER TABLE coding_problems ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'GENERATED';
ALTER TABLE coding_problems ADD COLUMN IF NOT EXISTS hints_used INTEGER DEFAULT 0;
ALTER TABLE coding_problems ADD COLUMN IF NOT EXISTS solved_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_coding_user_status ON coding_problems(user_id, status);

-- =====================================================================
-- QUIZ SESSIONS — entity has status enum, share_token, completed_at
-- =====================================================================
ALTER TABLE quiz_sessions ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'STARTED';
ALTER TABLE quiz_sessions ADD COLUMN IF NOT EXISTS share_token VARCHAR(255);
ALTER TABLE quiz_sessions ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_quiz_share_token ON quiz_sessions(share_token);

-- =====================================================================
-- STUDY PLANS — entity has exam_date, daily_hours, start_date, status
-- =====================================================================
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS plan_title VARCHAR(500);
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS exam_date DATE;
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS daily_hours INTEGER DEFAULT 4;
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE study_plans ADD COLUMN IF NOT EXISTS plan_json TEXT;

-- =====================================================================
-- STUDY TOPICS — entity has subject, topic_name, week/day, confidence, weak flag
-- =====================================================================
ALTER TABLE study_topics ADD COLUMN IF NOT EXISTS subject VARCHAR(255);
ALTER TABLE study_topics ADD COLUMN IF NOT EXISTS topic_name VARCHAR(500);
ALTER TABLE study_topics ADD COLUMN IF NOT EXISTS week_number INTEGER DEFAULT 1;
ALTER TABLE study_topics ADD COLUMN IF NOT EXISTS day_number INTEGER DEFAULT 1;
ALTER TABLE study_topics ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'PENDING';
ALTER TABLE study_topics ADD COLUMN IF NOT EXISTS confidence_score INTEGER DEFAULT 5;
ALTER TABLE study_topics ADD COLUMN IF NOT EXISTS is_weak BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_study_topic_plan_week_day ON study_topics(study_plan_id, week_number, day_number);

-- =====================================================================
-- ROADMAPS — entity has current_level, timeframe, focus_area
-- =====================================================================
ALTER TABLE roadmaps ADD COLUMN IF NOT EXISTS current_level VARCHAR(100);
ALTER TABLE roadmaps ADD COLUMN IF NOT EXISTS timeframe VARCHAR(100);
ALTER TABLE roadmaps ADD COLUMN IF NOT EXISTS focus_area VARCHAR(255);

-- =====================================================================
-- NOTES — entity has category, tags, is_pinned
-- =====================================================================
ALTER TABLE notes ADD COLUMN IF NOT EXISTS category VARCHAR(100) DEFAULT 'GENERAL';
ALTER TABLE notes ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE notes ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE;

-- =====================================================================
-- ACHIEVEMENTS — entity uses badge_key (not key), unlocked_at (not earned_at)
-- =====================================================================
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS badge_key VARCHAR(100);
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS unlocked_at TIMESTAMP;
-- Migrate data from old columns if they exist (safe on fresh DBs where old columns never existed)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='achievements' AND column_name='key') THEN
    UPDATE achievements SET badge_key = key WHERE badge_key IS NULL AND key IS NOT NULL;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='achievements' AND column_name='earned_at') THEN
    UPDATE achievements SET unlocked_at = earned_at WHERE unlocked_at IS NULL AND earned_at IS NOT NULL;
  END IF;
END $$;

-- Drop old unique constraint and add new one
ALTER TABLE achievements DROP CONSTRAINT IF EXISTS achievements_user_id_key_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_achievement_user_badge ON achievements(user_id, badge_key);

-- =====================================================================
-- USER PROFILES — entity has education_level, experience_level (not just college/branch)
-- =====================================================================
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS education_level VARCHAR(255);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS experience_level VARCHAR(100);

-- =====================================================================
-- CONTESTS — table doesn't exist in V1 at all
-- =====================================================================
CREATE TABLE IF NOT EXISTS contests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'UPCOMING',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contest_problems (
    id BIGSERIAL PRIMARY KEY,
    contest_id BIGINT NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    difficulty VARCHAR(50),
    points INTEGER DEFAULT 100,
    order_index INTEGER DEFAULT 0,
    test_cases_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_contest_problems_contest ON contest_problems(contest_id, order_index);

-- Fix contest_submissions to use BIGINT foreign keys instead of VARCHAR
ALTER TABLE contest_submissions ALTER COLUMN contest_id TYPE BIGINT USING contest_id::BIGINT;
ALTER TABLE contest_submissions ALTER COLUMN problem_id TYPE BIGINT USING problem_id::BIGINT;
ALTER TABLE contest_submissions ADD COLUMN IF NOT EXISTS score INTEGER DEFAULT 0;
ALTER TABLE contest_submissions ADD COLUMN IF NOT EXISTS user_name VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_contest_subs_contest ON contest_submissions(contest_id, score DESC);

-- =====================================================================
-- WEEKLY REPORTS — entity has structured columns (not just report_json)
-- =====================================================================
ALTER TABLE weekly_reports ADD COLUMN IF NOT EXISTS week_end DATE;
ALTER TABLE weekly_reports ADD COLUMN IF NOT EXISTS problems_solved INTEGER DEFAULT 0;
ALTER TABLE weekly_reports ADD COLUMN IF NOT EXISTS topics_completed INTEGER DEFAULT 0;
ALTER TABLE weekly_reports ADD COLUMN IF NOT EXISTS quizzes_completed INTEGER DEFAULT 0;
ALTER TABLE weekly_reports ADD COLUMN IF NOT EXISTS xp_gained BIGINT DEFAULT 0;

-- =====================================================================
-- DISCUSSION REPLIES — add atomic vote update support
-- =====================================================================
ALTER TABLE discussion_replies ADD COLUMN IF NOT EXISTS upvotes INTEGER DEFAULT 0;

-- =====================================================================
-- Add missing FK constraints on discussion_posts
-- =====================================================================
-- Note: discussion_posts.user_id had no FK in V1. We do NOT add one now
-- because users can be soft-deleted and we still want to show their posts.

-- =====================================================================
-- CAREER ASSESSMENTS — entity has more structured fields
-- =====================================================================
ALTER TABLE career_assessments ADD COLUMN IF NOT EXISTS answers_json TEXT;
ALTER TABLE career_assessments ADD COLUMN IF NOT EXISTS result_json TEXT;
ALTER TABLE career_assessments ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'STARTED';
ALTER TABLE career_assessments ADD COLUMN IF NOT EXISTS top_career VARCHAR(255);

-- =====================================================================
-- ADDITIONAL INDEXES for query performance
-- =====================================================================
CREATE INDEX IF NOT EXISTS idx_coding_problems_solved ON coding_problems(user_id) WHERE status = 'SOLVED';
CREATE INDEX IF NOT EXISTS idx_follows_composite ON follows(follower_id, following_id);
