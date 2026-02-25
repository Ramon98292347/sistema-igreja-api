-- V13: igrejas nivel + photo R2 key columns; membros photo key.

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS nivel text;

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS foto_r2_bucket text;

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS foto_r2_key text;

CREATE INDEX IF NOT EXISTS idx_igrejas_nivel ON igrejas(nivel);

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS foto_r2_bucket text;

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS foto_r2_key text;

CREATE INDEX IF NOT EXISTS idx_membros_foto_r2_key ON membros(foto_r2_key);
