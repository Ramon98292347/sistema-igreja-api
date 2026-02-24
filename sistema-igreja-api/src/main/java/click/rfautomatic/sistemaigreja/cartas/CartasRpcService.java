package click.rfautomatic.sistemaigreja.cartas;

import click.rfautomatic.sistemaigreja.auth.JwtPrincipal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartasRpcService {

  private final JdbcTemplate jdbc;

  public CartasRpcService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public UUID emitirCartaValidada(
      JwtPrincipal principal,
      UUID membroId,
      UUID igrejaDestinoId,
      LocalDate dataPregacao,
      String turno,
      LocalTime horarioPregacao,
      String observacao) {

    if (principal == null || principal.userId() == null || principal.userId().isBlank()) {
      throw new IllegalArgumentException("Usuário não autenticado");
    }

    // Bind current user id to the DB session (replacement for auth.uid())
    jdbc.update("SET LOCAL app.user_id = ?", principal.userId());

    String sql =
        "SELECT emitir_carta_validada(?, ?, ?, ?, ?, ?)";

    return jdbc.queryForObject(
        sql,
        (rs, rowNum) -> (UUID) rs.getObject(1),
        membroId,
        igrejaDestinoId,
        java.sql.Date.valueOf(dataPregacao),
        turno,
        Time.valueOf(horarioPregacao),
        observacao);
  }
}
