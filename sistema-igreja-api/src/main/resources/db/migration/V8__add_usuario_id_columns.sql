-- Add new columns referencing usuarios, without breaking legacy auth.users uuids.

DO $$
BEGIN
  -- membros: new usuario_id (keep legacy user_id if exists)
  IF to_regclass('public.membros') IS NOT NULL THEN
    EXECUTE 'ALTER TABLE membros ADD COLUMN IF NOT EXISTS usuario_id uuid';

    BEGIN
      EXECUTE 'ALTER TABLE membros ADD CONSTRAINT fk_membros_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL';
    EXCEPTION WHEN duplicate_object THEN
      NULL;
    END;

    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_membros_usuario_id ON membros(usuario_id)';
  END IF;

  -- church_users: add usuario_id; keep legacy user_id
  IF to_regclass('public.church_users') IS NOT NULL THEN
    EXECUTE 'ALTER TABLE church_users ADD COLUMN IF NOT EXISTS usuario_id uuid';

    BEGIN
      EXECUTE 'ALTER TABLE church_users ADD CONSTRAINT fk_church_users_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE';
    EXCEPTION WHEN duplicate_object THEN
      NULL;
    END;

    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_church_users_usuario_id ON church_users(usuario_id)';
  END IF;

  -- cartas: add criado_por_usuario_id
  IF to_regclass('public.cartas') IS NOT NULL THEN
    EXECUTE 'ALTER TABLE cartas ADD COLUMN IF NOT EXISTS criado_por_usuario_id uuid';

    BEGIN
      EXECUTE 'ALTER TABLE cartas ADD CONSTRAINT fk_cartas_criado_por_usuario_id FOREIGN KEY (criado_por_usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL';
    EXCEPTION WHEN duplicate_object THEN
      NULL;
    END;

    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_cartas_criado_por_usuario_id ON cartas(criado_por_usuario_id)';
  END IF;

  -- documentos_emitidos: add emitido_por_usuario_id
  IF to_regclass('public.documentos_emitidos') IS NOT NULL THEN
    EXECUTE 'ALTER TABLE documentos_emitidos ADD COLUMN IF NOT EXISTS emitido_por_usuario_id uuid';

    BEGIN
      EXECUTE 'ALTER TABLE documentos_emitidos ADD CONSTRAINT fk_documentos_emitidos_emitido_por_usuario_id FOREIGN KEY (emitido_por_usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL';
    EXCEPTION WHEN duplicate_object THEN
      NULL;
    END;

    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_docs_emitido_por_usuario_id ON documentos_emitidos(emitido_por_usuario_id)';
  END IF;

  -- comunicados: add criado_por_usuario_id
  IF to_regclass('public.comunicados') IS NOT NULL THEN
    EXECUTE 'ALTER TABLE comunicados ADD COLUMN IF NOT EXISTS criado_por_usuario_id uuid';

    BEGIN
      EXECUTE 'ALTER TABLE comunicados ADD CONSTRAINT fk_comunicados_criado_por_usuario_id FOREIGN KEY (criado_por_usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL';
    EXCEPTION WHEN duplicate_object THEN
      NULL;
    END;

    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_comunicados_criado_por_usuario_id ON comunicados(criado_por_usuario_id)';
  END IF;
END $$;
