CREATE TABLE IF NOT EXISTS documentos_emitidos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo text NOT NULL,
  status text NOT NULL DEFAULT 'PENDENTE',
  url_documento text,
  erro_msg text,
  referencia_tipo text,
  referencia_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_documentos_emitidos_tipo ON documentos_emitidos(tipo);
CREATE INDEX IF NOT EXISTS idx_documentos_emitidos_status ON documentos_emitidos(status);
CREATE INDEX IF NOT EXISTS idx_documentos_emitidos_referencia ON documentos_emitidos(referencia_tipo, referencia_id);
