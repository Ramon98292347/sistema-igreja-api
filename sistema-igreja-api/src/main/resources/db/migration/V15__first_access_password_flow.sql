-- First access flow: allow users to set a password on first access (admin-created accounts)

ALTER TABLE usuarios
  ADD COLUMN IF NOT EXISTS first_access_pending boolean NOT NULL DEFAULT false;

-- Backfill: existing users are considered already provisioned
UPDATE usuarios SET first_access_pending = false WHERE first_access_pending IS DISTINCT FROM false;
