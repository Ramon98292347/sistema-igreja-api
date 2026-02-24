-- V10: emitir_carta_validada RPC (PostgreSQL)

CREATE OR REPLACE FUNCTION emitir_carta_validada(
  p_membro_id uuid,
  p_igreja_origem_id uuid,
  p_igreja_destino_id uuid,
  p_data_pregacao date,
  p_horario_pregacao time,
  p_turno text,
  p_criado_por_usuario_id uuid,
  p_observacao text DEFAULT NULL
) RETURNS uuid
LANGUAGE plpgsql
AS $$
DECLARE
  v_carta_id uuid;
  v_ativo boolean;
  v_start_week date;
  v_end_week date;
  v_count_week integer;
BEGIN
  SELECT m.ativo INTO v_ativo
  FROM membros m
  WHERE m.id = p_membro_id;

  IF v_ativo IS DISTINCT FROM true THEN
    RAISE EXCEPTION 'Membro inativo';
  END IF;

  IF p_data_pregacao < CURRENT_DATE THEN
    RAISE EXCEPTION 'Data de pregação não pode ser passada';
  END IF;

  IF p_data_pregacao > (CURRENT_DATE + INTERVAL '5 days')::date THEN
    RAISE EXCEPTION 'Data de pregação não pode ser maior que 5 dias à frente';
  END IF;

  -- segunda..domingo (date_trunc('week') no Postgres inicia na segunda)
  v_start_week := date_trunc('week', p_data_pregacao)::date;
  v_end_week := (v_start_week + INTERVAL '6 days')::date;

  SELECT count(*) INTO v_count_week
  FROM cartas c
  WHERE c.membro_id = p_membro_id
    AND c.data_pregacao BETWEEN v_start_week AND v_end_week;

  IF v_count_week >= 5 THEN
    RAISE EXCEPTION 'Limite de 5 cartas por semana atingido';
  END IF;

  -- unique (membro_id, data_pregacao, turno) garante não duplicar no mesmo turno
  INSERT INTO cartas (
    membro_id,
    igreja_origem_id,
    igreja_destino_id,
    data_pregacao,
    horario_pregacao,
    turno,
    status,
    criado_por_usuario_id,
    observacao,
    created_at
  ) VALUES (
    p_membro_id,
    p_igreja_origem_id,
    p_igreja_destino_id,
    p_data_pregacao,
    p_horario_pregacao,
    p_turno,
    'emitida',
    p_criado_por_usuario_id,
    p_observacao,
    now()
  )
  RETURNING id INTO v_carta_id;

  RETURN v_carta_id;
END;
$$;
