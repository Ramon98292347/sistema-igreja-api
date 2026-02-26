-- Add cargo ministerial + ficha completa (json) + status

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS cargo_ministerial text;

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS ficha_json jsonb;

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS ativo boolean NOT NULL DEFAULT true;

-- Default cargo for existing rows
UPDATE membros SET cargo_ministerial = COALESCE(cargo_ministerial, 'membro') WHERE cargo_ministerial IS NULL;

CREATE INDEX IF NOT EXISTS idx_membros_cargo_ministerial ON membros(cargo_ministerial);
