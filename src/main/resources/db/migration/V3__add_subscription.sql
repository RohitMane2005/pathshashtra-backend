-- V3: Add subscription support
-- Adds plan column to users table and creates subscriptions table.
-- Idempotent: safe to run on fresh or existing databases.

ALTER TABLE users ADD COLUMN IF NOT EXISTS plan VARCHAR(50) DEFAULT 'FREE';

CREATE TABLE IF NOT EXISTS subscriptions (
    id                        BIGSERIAL PRIMARY KEY,
    user_id                   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    razorpay_order_id         VARCHAR(255),
    razorpay_payment_id       VARCHAR(255),
    razorpay_subscription_id  VARCHAR(255),
    plan                      VARCHAR(50)  DEFAULT 'PRO',
    status                    VARCHAR(50)  DEFAULT 'ACTIVE',
    current_period_end        TIMESTAMP,
    created_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sub_user_id         ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_sub_razorpay_sub_id ON subscriptions(razorpay_subscription_id);
