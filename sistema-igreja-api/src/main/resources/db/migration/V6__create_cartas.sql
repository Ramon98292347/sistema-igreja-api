CREATE TABLE IF NOT EXISTS cartas (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  membro_id uuid,

  pregador_nome text NOT NULL,
  cargo_ministerial text,

  igreja_origem_nome text,
  igreja_origem_codigo text,

  igreja_destino_nome text,
  igreja_destino_codigo text,

  data_emissao date NOT NULL DEFAULT (now()::date),
  data_pregacao date NOT NULL,
  turno text NOT NULL,

  status text NOT NULL DEFAULT 'ATIVA',

  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_cartas_membro_id ON cartas(membro_id);
CREATE INDEX IF NOT EXISTS idx_cartas_data_pregacao ON cartas(data_pregacao);

-- unique for same day/turno per membro (allows multiple days)
CREATE UNIQUE INDEX IF NOT EXISTS uq_cartas_membro_data_turno
  ON cartas(membro_id, data_pregacao, turno)
  WHERE membro_id IS NOT NULL;
