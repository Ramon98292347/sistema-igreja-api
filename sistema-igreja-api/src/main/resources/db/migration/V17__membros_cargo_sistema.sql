-- Cargo do sistema (permissões) separado da função ministerial

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS cargo_sistema text;

CREATE INDEX IF NOT EXISTS idx_membros_cargo_sistema ON membros(cargo_sistema);
