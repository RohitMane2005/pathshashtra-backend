-- V4: Fix notification column names to match JPA entity.
--
-- The Notification entity expects:
--   @Column(name = "is_read")   →  column is_read
--   actionUrl (no annotation)   →  column action_url
--
-- But V1 created columns named "read" and "link".
-- Hibernate validate mode fails because the expected columns don't exist.
--
-- Idempotent: uses DO $$ blocks to check before renaming.

-- Rename "read" → "is_read" (only if "read" exists and "is_read" doesn't)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='read')
     AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='is_read')
  THEN
    ALTER TABLE notifications RENAME COLUMN "read" TO is_read;
  END IF;
END $$;

-- Rename "link" → "action_url" (only if "link" exists and "action_url" doesn't)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='link')
     AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='action_url')
  THEN
    ALTER TABLE notifications RENAME COLUMN link TO action_url;
  END IF;
END $$;

-- Recreate index on corrected column names
DROP INDEX IF EXISTS idx_notifications_user;
CREATE INDEX IF NOT EXISTS idx_notif_user    ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_read    ON notifications(user_id, is_read);
