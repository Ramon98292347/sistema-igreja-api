-- Align cartas table closer to IPDA schema (non-destructive)

-- Turno enum optional; we keep column as text for now to avoid breaking existing.

ALTER TABLE cartas
  ADD COLUMN IF NOT EXISTS igreja_origem_id uuid;

ALTER TABLE cartas
  ADD COLUMN IF NOT EXISTS igreja_destino_id uuid;

ALTER TABLE cartas
  ADD COLUMN IF NOT EXISTS horario_pregacao time;

ALTER TABLE cartas
  ADD COLUMN IF NOT EXISTS data_emissao timestamptz NOT NULL DEFAULT now();

ALTER TABLE cartas
  ADD COLUMN IF NOT EXISTS criado_por uuid;

ALTER TABLE cartas
  ADD COLUMN IF NOT EXISTS observacao text;

-- FKs (best-effort)
DO $$ BEGIN
  ALTER TABLE cartas
    ADD CONSTRAINT fk_cartas_igreja_origem_id
    FOREIGN KEY (igreja_origem_id) REFERENCES igrejas(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE cartas
    ADD CONSTRAINT fk_cartas_igreja_destino_id
    FOREIGN KEY (igreja_destino_id) REFERENCES igrejas(id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Backfill origem from membros.church_id when possible
UPDATE cartas c
SET igreja_origem_id = m.church_id
FROM membros m
WHERE c.igreja_origem_id IS NULL
  AND c.membro_id IS NOT NULL
  AND m.id = c.membro_id;

-- Unique index for member/date/turno (allows 2 in same day with different turno)
DO $$ BEGIN
  CREATE UNIQUE INDEX IF NOT EXISTS uq_cartas_membro_data_turno
  ON cartas(membro_id, data_pregacao, turno)
  WHERE membro_id IS NOT NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
