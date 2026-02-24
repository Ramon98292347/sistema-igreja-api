CREATE TABLE IF NOT EXISTS usuarios (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  cpf varchar(11) UNIQUE,
  email citext UNIQUE,

  senha_hash varchar(100) NOT NULL,
  ativo boolean NOT NULL DEFAULT true,

  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),

  CONSTRAINT usuarios_login_check CHECK (cpf IS NOT NULL OR email IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_ativo ON usuarios(ativo);
