-- Core RPCs for IPDA (Postgres-only). Uses app_uid() set by backend.

-- Ensure expected columns exist (best-effort, non-destructive)
ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS parent_id uuid;

ALTER TABLE igrejas
  ADD COLUMN IF NOT EXISTS pastor_responsavel_id uuid;

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

CREATE INDEX IF NOT EXISTS idx_igrejas_parent_id ON igrejas(parent_id);
CREATE INDEX IF NOT EXISTS idx_igrejas_pastor_resp ON igrejas(pastor_responsavel_id);

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS cargo_lideranca text;

ALTER TABLE membros
  ADD COLUMN IF NOT EXISTS ativo boolean NOT NULL DEFAULT true;

CREATE INDEX IF NOT EXISTS idx_membros_ativo ON membros(ativo);

CREATE OR REPLACE FUNCTION app_uid()
RETURNS uuid
LANGUAGE sql
AS $$
  SELECT nullif(current_setting('app.user_id', true), '')::uuid;
$$;

-- A) get_sub_churches
CREATE OR REPLACE FUNCTION get_sub_churches(p_church_id uuid)
RETURNS TABLE(id uuid)
LANGUAGE sql
AS $$
  WITH RECURSIVE tree AS (
    SELECT i.id
    FROM igrejas i
    WHERE i.id = p_church_id
    UNION ALL
    SELECT c.id
    FROM igrejas c
    JOIN tree t ON c.parent_id = t.id
  )
  SELECT id FROM tree;
$$;

-- carta_tentativas (if not exists)
CREATE TABLE IF NOT EXISTS carta_tentativas (
  membro_id uuid NOT NULL REFERENCES membros(id) ON DELETE CASCADE,
  semana_ref date NOT NULL,
  tentativas integer NOT NULL DEFAULT 0,
  bloqueado_ate timestamptz NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (membro_id, semana_ref)
);

-- helper: registrar tentativa (bloqueia 7 dias ao chegar em 3)
CREATE OR REPLACE FUNCTION registrar_tentativa_carta(
  p_membro_id uuid,
  p_data_pregacao date,
  p_motivo text
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_semana_ref date := date_trunc('week', p_data_pregacao)::date;
  v_tentativas integer;
  v_bloqueado_ate timestamptz;

  v_igreja_origem_id uuid;
  v_pastor_resp_id uuid;
  v_pastor_user_id uuid;
BEGIN
  INSERT INTO carta_tentativas (membro_id, semana_ref, tentativas, bloqueado_ate, updated_at)
  VALUES (p_membro_id, v_semana_ref, 1, NULL, now())
  ON CONFLICT (membro_id, semana_ref)
  DO UPDATE SET tentativas = carta_tentativas.tentativas + 1,
                updated_at = now()
  RETURNING tentativas, bloqueado_ate INTO v_tentativas, v_bloqueado_ate;

  IF v_tentativas >= 3 THEN
    v_bloqueado_ate := now() + interval '7 days';
    UPDATE carta_tentativas
    SET bloqueado_ate = v_bloqueado_ate,
        updated_at = now()
    WHERE membro_id = p_membro_id AND semana_ref = v_semana_ref;

    -- notify pastor responsible of origin church
    SELECT m.church_id INTO v_igreja_origem_id FROM membros m WHERE m.id = p_membro_id;
    IF v_igreja_origem_id IS NOT NULL THEN
      SELECT pastor_responsavel_id INTO v_pastor_resp_id FROM igrejas WHERE id = v_igreja_origem_id;
      IF v_pastor_resp_id IS NOT NULL THEN
        SELECT usuario_id INTO v_pastor_user_id FROM membros WHERE id = v_pastor_resp_id;
        IF v_pastor_user_id IS NOT NULL THEN
          INSERT INTO notificacoes (user_id, church_id, tipo, titulo, mensagem, lida, created_at)
          VALUES (
            v_pastor_user_id,
            v_igreja_origem_id,
            'tentativas_invalidas',
            'Bloqueio por tentativas inválidas',
            'Membro bloqueado por 7 dias após 3 tentativas inválidas. Motivo: ' || coalesce(p_motivo,'-'),
            false,
            now()
          );
        END IF;
      END IF;
    END IF;
  END IF;
END;
$$;

-- D) emitir_carta_validada (final signature)
CREATE OR REPLACE FUNCTION emitir_carta_validada(
  p_membro_id uuid,
  p_igreja_destino_id uuid,
  p_data_pregacao date,
  p_turno text,
  p_horario time,
  p_observacao text DEFAULT NULL
) RETURNS uuid
LANGUAGE plpgsql
AS $$
DECLARE
  v_executor uuid := app_uid();
  v_membro_ativo boolean;
  v_igreja_origem uuid;
  v_start_week date;
  v_end_week date;
  v_count_week integer;
  v_carta_id uuid;

  v_semana_ref date := date_trunc('week', p_data_pregacao)::date;
  v_bloqueado_ate timestamptz;

  v_pastor_resp_id uuid;
  v_pastor_user_id uuid;
  v_nome_pregador text;
  v_destino_nome text;
BEGIN
  IF v_executor IS NULL THEN
    RAISE EXCEPTION 'Executor não identificado (SET LOCAL app.user_id ausente)';
  END IF;

  SELECT m.ativo, m.church_id, m.nome
  INTO v_membro_ativo, v_igreja_origem, v_nome_pregador
  FROM membros m
  WHERE m.id = p_membro_id;

  IF v_igreja_origem IS NULL THEN
    PERFORM registrar_tentativa_carta(p_membro_id, p_data_pregacao, 'Membro não encontrado');
    RAISE EXCEPTION 'Membro não encontrado';
  END IF;

  SELECT bloqueado_ate INTO v_bloqueado_ate
  FROM carta_tentativas
  WHERE membro_id = p_membro_id AND semana_ref = v_semana_ref;

  IF v_bloqueado_ate IS NOT NULL AND v_bloqueado_ate > now() THEN
    RAISE EXCEPTION 'Emissão bloqueada até %', v_bloqueado_ate;
  END IF;

  IF v_membro_ativo IS DISTINCT FROM true THEN
    PERFORM registrar_tentativa_carta(p_membro_id, p_data_pregacao, 'Membro inativo');
    RAISE EXCEPTION 'Membro inativo';
  END IF;

  IF p_data_pregacao < CURRENT_DATE THEN
    PERFORM registrar_tentativa_carta(p_membro_id, p_data_pregacao, 'Data passada');
    RAISE EXCEPTION 'Data de pregação não pode ser passada';
  END IF;

  IF p_data_pregacao > (CURRENT_DATE + interval '5 days')::date THEN
    PERFORM registrar_tentativa_carta(p_membro_id, p_data_pregacao, 'Data > 5 dias');
    RAISE EXCEPTION 'Data de pregação não pode ser maior que 5 dias à frente';
  END IF;

  v_start_week := date_trunc('week', p_data_pregacao)::date;
  v_end_week := (v_start_week + interval '6 days')::date;

  SELECT count(*) INTO v_count_week
  FROM cartas c
  WHERE c.membro_id = p_membro_id
    AND c.data_pregacao BETWEEN v_start_week AND v_end_week;

  IF v_count_week >= 5 THEN
    PERFORM registrar_tentativa_carta(p_membro_id, p_data_pregacao, 'Limite semanal');
    RAISE EXCEPTION 'Limite de 5 cartas por semana atingido';
  END IF;

  SELECT nome INTO v_destino_nome FROM igrejas WHERE id = p_igreja_destino_id;

  INSERT INTO cartas (
    membro_id,
    pregador_nome,
    cargo_ministerial,
    igreja_origem_nome,
    igreja_origem_codigo,
    igreja_destino_nome,
    igreja_destino_codigo,
    igreja_origem_id,
    igreja_destino_id,
    data_emissao,
    data_pregacao,
    turno,
    horario_pregacao,
    status,
    criado_por_usuario_id,
    criado_por,
    observacao,
    created_at
  ) VALUES (
    p_membro_id,
    coalesce(v_nome_pregador,'-'),
    NULL,
    (SELECT nome FROM igrejas WHERE id = v_igreja_origem),
    NULL,
    v_destino_nome,
    NULL,
    v_igreja_origem,
    p_igreja_destino_id,
    now(),
    p_data_pregacao,
    p_turno,
    p_horario,
    'emitida',
    v_executor,
    v_executor,
    p_observacao,
    now()
  )
  RETURNING id INTO v_carta_id;

  -- success notification to pastor responsible of origin church
  SELECT pastor_responsavel_id INTO v_pastor_resp_id FROM igrejas WHERE id = v_igreja_origem;

  IF v_pastor_resp_id IS NOT NULL THEN
    SELECT usuario_id INTO v_pastor_user_id FROM membros WHERE id = v_pastor_resp_id;
    IF v_pastor_user_id IS NOT NULL THEN
      INSERT INTO notificacoes (user_id, church_id, tipo, titulo, mensagem, lida, created_at)
      VALUES (
        v_pastor_user_id,
        v_igreja_origem,
        'carta_emitida',
        'Carta emitida',
        'Pregador: ' || coalesce(v_nome_pregador,'-') ||
        ' | Data: ' || p_data_pregacao::text ||
        ' | Turno: ' || coalesce(p_turno,'-') ||
        ' | Destino: ' || coalesce(v_destino_nome,'-'),
        false,
        now()
      );
    END IF;
  END IF;

  RETURN v_carta_id;

EXCEPTION
  WHEN unique_violation THEN
    PERFORM registrar_tentativa_carta(p_membro_id, p_data_pregacao, 'Duplicidade de turno');
    RAISE EXCEPTION 'Já existe carta para este membro nesta data e turno';
  WHEN others THEN
    PERFORM registrar_tentativa_carta(p_membro_id, p_data_pregacao, SQLERRM);
    RAISE;
END;
$$;

-- B) transferir_membro (uses app_uid and members.user_id legacy)
CREATE OR REPLACE FUNCTION transferir_membro(p_membro_id uuid, p_nova_igreja_id uuid, p_motivo text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_executor uuid := app_uid();
  v_executor_membro_id uuid;
  v_igreja_executor uuid;
  v_cargo text;
  v_igreja_antiga uuid;
BEGIN
  IF v_executor IS NULL THEN
    RAISE EXCEPTION 'Executor não identificado (SET LOCAL app.user_id ausente)';
  END IF;

  SELECT m.id, m.church_id, lower(coalesce(m.cargo_lideranca,''))
  INTO v_executor_membro_id, v_igreja_executor, v_cargo
  FROM membros m
  WHERE m.usuario_id = v_executor
  LIMIT 1;

  IF v_executor_membro_id IS NULL THEN
    RAISE EXCEPTION 'Executor não possui membro vinculado';
  END IF;

  IF v_cargo NOT IN ('estadual','setorial','central') THEN
    RAISE EXCEPTION 'Sem permissão para transferir membros';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM get_sub_churches(v_igreja_executor) s WHERE s.id = p_nova_igreja_id) THEN
    RAISE EXCEPTION 'Igreja destino fora da hierarquia do executor';
  END IF;

  SELECT church_id INTO v_igreja_antiga FROM membros WHERE id = p_membro_id;
  IF v_igreja_antiga IS NULL THEN
    RAISE EXCEPTION 'Membro não encontrado';
  END IF;

  INSERT INTO remanejamentos (membro_id, igreja_origem_id, igreja_destino_id, motivo, realizado_por, tipo, created_at)
  VALUES (p_membro_id, v_igreja_antiga, p_nova_igreja_id, p_motivo, v_executor, 'transferencia_membro', now());

  UPDATE membros SET church_id = p_nova_igreja_id WHERE id = p_membro_id;
END;
$$;

-- C) remanejar_pastor
CREATE OR REPLACE FUNCTION remanejar_pastor(p_pastor_id uuid, p_nova_igreja_id uuid, p_motivo text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_executor uuid := app_uid();
  v_igreja_executor uuid;
  v_cargo text;
  v_igreja_antiga uuid;
  v_user_pastor uuid;
BEGIN
  IF v_executor IS NULL THEN
    RAISE EXCEPTION 'Executor não identificado (SET LOCAL app.user_id ausente)';
  END IF;

  SELECT m.church_id, lower(coalesce(m.cargo_lideranca,''))
  INTO v_igreja_executor, v_cargo
  FROM membros m
  WHERE m.usuario_id = v_executor
  LIMIT 1;

  IF v_igreja_executor IS NULL THEN
    RAISE EXCEPTION 'Executor não possui membro vinculado';
  END IF;

  IF v_cargo NOT IN ('estadual','setorial','central') THEN
    RAISE EXCEPTION 'Sem permissão para remanejar pastor';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM get_sub_churches(v_igreja_executor) s WHERE s.id = p_nova_igreja_id) THEN
    RAISE EXCEPTION 'Igreja destino fora da hierarquia do executor';
  END IF;

  SELECT church_id, NULL::uuid INTO v_igreja_antiga, v_user_pastor
  FROM membros
  WHERE id = p_pastor_id;

  IF v_igreja_antiga IS NULL THEN
    RAISE EXCEPTION 'Pastor não encontrado';
  END IF;

  UPDATE igrejas SET pastor_responsavel_id = NULL WHERE pastor_responsavel_id = p_pastor_id;

  INSERT INTO remanejamentos (membro_id, igreja_origem_id, igreja_destino_id, motivo, realizado_por, tipo, created_at)
  VALUES (p_pastor_id, v_igreja_antiga, p_nova_igreja_id, p_motivo, v_executor, 'remanejamento_pastoral', now());

  UPDATE membros SET church_id = p_nova_igreja_id WHERE id = p_pastor_id;

  IF v_user_pastor IS NOT NULL AND to_regclass('public.church_users') IS NOT NULL THEN
    UPDATE church_users
    SET church_id = p_nova_igreja_id
    WHERE user_id = v_user_pastor AND church_id = v_igreja_antiga;
  END IF;

  UPDATE igrejas SET pastor_responsavel_id = p_pastor_id WHERE id = p_nova_igreja_id;
END;
$$;
