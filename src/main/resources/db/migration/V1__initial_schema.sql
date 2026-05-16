-- V1 represents the baseline schema for existing deployments.
-- For EXISTING databases: set FLYWAY_BASELINE_ON_MIGRATE=true on first deployment.
--   This tells Flyway to mark this version as already applied without running it.
-- For FRESH databases: this script will create all tables from scratch.
--
-- All future schema changes must be in V2__*.sql, V3__*.sql, etc.

-- =====================================================================
-- USERS
-- =====================================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'STUDENT',
    auth_provider VARCHAR(50) DEFAULT 'LOCAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(id) WHERE deleted_at IS NULL;

-- =====================================================================
-- PASSWORD RESET TOKENS
-- =====================================================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE
);

-- =====================================================================
-- USER ACTIVITY (streak tracking)
-- =====================================================================
CREATE TABLE IF NOT EXISTS user_activity (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL,
    UNIQUE(user_id, activity_date)
);

CREATE INDEX IF NOT EXISTS idx_user_activity_user_date ON user_activity(user_id, activity_date DESC);

-- =====================================================================
-- USER PROFILES
-- =====================================================================
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    college VARCHAR(500),
    branch VARCHAR(500),
    year_of_study VARCHAR(50),
    skills TEXT,
    career_goal VARCHAR(500),
    coding_level VARCHAR(100),
    interests TEXT
);

-- =====================================================================
-- CODING PROBLEMS
-- =====================================================================
CREATE TABLE IF NOT EXISTS coding_problems (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_statement TEXT,
    difficulty VARCHAR(50),
    language VARCHAR(50),
    user_code TEXT,
    feedback_json TEXT,
    solved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_coding_problems_user ON coding_problems(user_id);

-- =====================================================================
-- QUIZ SESSIONS
-- =====================================================================
CREATE TABLE IF NOT EXISTS quiz_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    questions_json TEXT,
    answers_json TEXT,
    result_json TEXT,
    score INTEGER,
    completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quiz_sessions_user ON quiz_sessions(user_id);

-- =====================================================================
-- ROADMAPS
-- =====================================================================
CREATE TABLE IF NOT EXISTS roadmaps (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal VARCHAR(500),
    language VARCHAR(100),
    level VARCHAR(100),
    roadmap_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_roadmaps_user ON roadmaps(user_id);

-- =====================================================================
-- STUDY PLANS
-- =====================================================================
CREATE TABLE IF NOT EXISTS study_plans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(500),
    plan_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS study_topics (
    id BIGSERIAL PRIMARY KEY,
    study_plan_id BIGINT REFERENCES study_plans(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(500),
    completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- CAREER ASSESSMENTS
-- =====================================================================
CREATE TABLE IF NOT EXISTS career_assessments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assessment_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- SAVED ITEMS (bookmarks)
-- =====================================================================
CREATE TABLE IF NOT EXISTS saved_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    ref_id BIGINT NOT NULL,
    label VARCHAR(500),
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, type, ref_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_items_user ON saved_items(user_id, type);

-- =====================================================================
-- DISCUSSION POSTS & REPLIES
-- =====================================================================
CREATE TABLE IF NOT EXISTS discussion_posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    author_name VARCHAR(255),
    title VARCHAR(200) NOT NULL,
    content TEXT,
    tags VARCHAR(500),
    upvotes INTEGER DEFAULT 0,
    reply_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_discussion_posts_created ON discussion_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_discussion_posts_upvotes ON discussion_posts(upvotes DESC);
CREATE INDEX IF NOT EXISTS idx_discussion_posts_tags ON discussion_posts(tags);

CREATE TABLE IF NOT EXISTS discussion_replies (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES discussion_posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    author_name VARCHAR(255),
    content TEXT,
    upvotes INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_discussion_replies_post ON discussion_replies(post_id, created_at ASC);

CREATE TABLE IF NOT EXISTS discussion_votes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(10) NOT NULL,
    target_id BIGINT NOT NULL,
    UNIQUE(user_id, target_type, target_id)
);

-- =====================================================================
-- CHAT SESSIONS & MESSAGES
-- =====================================================================
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id, created_at ASC);

-- =====================================================================
-- NOTES
-- =====================================================================
CREATE TABLE IF NOT EXISTS notes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(500),
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- NOTIFICATIONS
-- =====================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50),
    title VARCHAR(255),
    message TEXT,
    link VARCHAR(500),
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, read, created_at DESC);

-- =====================================================================
-- ACHIEVEMENTS
-- =====================================================================
CREATE TABLE IF NOT EXISTS achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    title VARCHAR(255),
    description TEXT,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, key)
);

-- =====================================================================
-- FOLLOW (social graph)
-- =====================================================================
CREATE TABLE IF NOT EXISTS follows (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(follower_id, following_id)
);

CREATE INDEX IF NOT EXISTS idx_follows_follower ON follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_follows_following ON follows(following_id);

-- =====================================================================
-- CONTEST SUBMISSIONS
-- =====================================================================
CREATE TABLE IF NOT EXISTS contest_submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contest_id VARCHAR(100),
    problem_id VARCHAR(100),
    language VARCHAR(50),
    code TEXT,
    result VARCHAR(50),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- WEEKLY REPORTS
-- =====================================================================
CREATE TABLE IF NOT EXISTS weekly_reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    week_start DATE,
    report_json TEXT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
