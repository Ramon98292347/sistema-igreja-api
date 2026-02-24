-- V9: core operational tables + docs upgrade

-- comunicados (create if missing; required for logs_envio_comunicados FK)
CREATE TABLE IF NOT EXISTS comunicados (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  church_id uuid NOT NULL REFERENCES igrejas(id) ON DELETE CASCADE,
  criado_por uuid NULL,
  criado_por_usuario_id uuid NULL REFERENCES usuarios(id) ON DELETE SET NULL,
  titulo text NOT NULL,
  mensagem text NOT NULL,
  tipo text NULL,
  enviar_whatsapp boolean NOT NULL DEFAULT false,
  enviar_email boolean NOT NULL DEFAULT false,
  criado_em timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_comunicados_church_id ON comunicados(church_id);
CREATE INDEX IF NOT EXISTS idx_comunicados_criado_em ON comunicados(criado_em);

-- notificacoes
CREATE TABLE IF NOT EXISTS notificacoes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  church_id uuid NULL REFERENCES igrejas(id) ON DELETE CASCADE,
  tipo text NOT NULL,
  titulo text NULL,
  mensagem text NOT NULL,
  referencia_tipo text NULL,
  referencia_id uuid NULL,
  lida boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_user_id ON notificacoes(user_id);
CREATE INDEX IF NOT EXISTS idx_notificacoes_church_id ON notificacoes(church_id);
CREATE INDEX IF NOT EXISTS idx_notificacoes_lida ON notificacoes(lida);
CREATE INDEX IF NOT EXISTS idx_notificacoes_created_at ON notificacoes(created_at);

-- logs_aniversarios
CREATE TABLE IF NOT EXISTS logs_aniversarios (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  membro_id uuid NOT NULL REFERENCES membros(id) ON DELETE CASCADE,
  church_id uuid NOT NULL REFERENCES igrejas(id) ON DELETE CASCADE,
  ano integer NOT NULL,
  canal text NOT NULL,
  status text NOT NULL DEFAULT 'ENVIADO',
  erro_msg text NULL,
  enviado_em timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_logs_aniversarios_membro_ano ON logs_aniversarios(membro_id, ano);
CREATE INDEX IF NOT EXISTS idx_logs_aniversarios_church_id ON logs_aniversarios(church_id);

-- logs_envio_comunicados
CREATE TABLE IF NOT EXISTS logs_envio_comunicados (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  comunicado_id uuid NOT NULL REFERENCES comunicados(id) ON DELETE CASCADE,
  church_id uuid NOT NULL REFERENCES igrejas(id) ON DELETE CASCADE,
  destinatario text NOT NULL,
  canal text NOT NULL,
  status text NOT NULL DEFAULT 'ENVIADO',
  erro_msg text NULL,
  enviado_em timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_logs_envio_comunicados_comunicado_id ON logs_envio_comunicados(comunicado_id);
CREATE INDEX IF NOT EXISTS idx_logs_envio_comunicados_church_id ON logs_envio_comunicados(church_id);
CREATE INDEX IF NOT EXISTS idx_logs_envio_comunicados_enviado_em ON logs_envio_comunicados(enviado_em);

-- remanejamentos (completo)
CREATE TABLE IF NOT EXISTS remanejamentos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo text NOT NULL,
  status text NOT NULL DEFAULT 'EXECUTADO',

  membro_id uuid NULL REFERENCES membros(id) ON DELETE SET NULL,

  igreja_origem_id uuid NOT NULL REFERENCES igrejas(id) ON DELETE CASCADE,
  igreja_destino_id uuid NOT NULL REFERENCES igrejas(id) ON DELETE CASCADE,

  motivo text NULL,

  realizado_por uuid NULL,
  realizado_por_usuario_id uuid NULL REFERENCES usuarios(id) ON DELETE SET NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  executed_at timestamptz NULL,
  erro_msg text NULL
);

CREATE INDEX IF NOT EXISTS idx_remanejamentos_tipo ON remanejamentos(tipo);
CREATE INDEX IF NOT EXISTS idx_remanejamentos_status ON remanejamentos(status);
CREATE INDEX IF NOT EXISTS idx_remanejamentos_igreja_origem ON remanejamentos(igreja_origem_id);
CREATE INDEX IF NOT EXISTS idx_remanejamentos_igreja_destino ON remanejamentos(igreja_destino_id);

-- documentos_emitidos upgrades (keep current columns)
ALTER TABLE documentos_emitidos
  ADD COLUMN IF NOT EXISTS status text NOT NULL DEFAULT 'PENDENTE';

ALTER TABLE documentos_emitidos
  ADD COLUMN IF NOT EXISTS r2_bucket text;

ALTER TABLE documentos_emitidos
  ADD COLUMN IF NOT EXISTS r2_key text;

ALTER TABLE documentos_emitidos
  ADD COLUMN IF NOT EXISTS erro_msg text;

CREATE INDEX IF NOT EXISTS idx_documentos_emitidos_status ON documentos_emitidos(status);

-- cartas blocking fields (2h lock)
ALTER TABLE cartas
  ADD COLUMN IF NOT EXISTS bloqueada_ate timestamptz;

CREATE INDEX IF NOT EXISTS idx_cartas_bloqueada_ate ON cartas(bloqueada_ate);
