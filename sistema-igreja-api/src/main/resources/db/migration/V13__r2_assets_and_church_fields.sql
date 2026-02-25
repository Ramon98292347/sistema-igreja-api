-- Add core fields for igreja hierarchy + R2 asset storage

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS nivel text;

-- already added earlier, but keep idempotent
ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS parent_id uuid;

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS pastor_responsavel_id uuid;

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS foto_r2_bucket text;

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS foto_r2_key text;

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS carimbo_r2_bucket text;

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS carimbo_r2_key text;

CREATE INDEX IF NOT EXISTS idx_igrejas_nivel ON igrejas(nivel);

DO $$ BEGIN
  ALTER TABLE igrejas
    ADD CONSTRAINT fk_igrejas_parent_id
    FOREIGN KEY (parent_id) REFERENCES igrejas(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE igrejas
    ADD CONSTRAINT fk_igrejas_pastor_responsavel_id
    FOREIGN KEY (pastor_responsavel_id) REFERENCES membros(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Add R2 fields for membros
ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS foto_r2_bucket text;

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS foto_r2_key text;

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS assinatura_r2_bucket text;

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS assinatura_r2_key text;

-- Ensure usuario_id exists (already in v8)
ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS usuario_id uuid;

CREATE INDEX IF NOT EXISTS idx_membros_usuario_id ON membros(usuario_id);

DO $$ BEGIN
  ALTER TABLE membros
    ADD CONSTRAINT fk_membros_usuario_id
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
