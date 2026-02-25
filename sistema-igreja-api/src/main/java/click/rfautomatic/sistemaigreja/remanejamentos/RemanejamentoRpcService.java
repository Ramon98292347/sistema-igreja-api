package click.rfautomatic.sistemaigreja.remanejamentos;

import click.rfautomatic.sistemaigreja.auth.JwtPrincipal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemanejamentoRpcService {

  private final JdbcTemplate jdbc;

  public RemanejamentoRpcService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public void transferirMembro(JwtPrincipal principal, UUID membroId, UUID novaIgrejaId, String motivo) {
    if (principal == null || principal.userId() == null || principal.userId().isBlank()) {
      throw new IllegalArgumentException("Usuário não autenticado");
    }
    jdbc.update("SET LOCAL app.user_id = ?", principal.userId());
    jdbc.update("SELECT transferir_membro(?, ?, ?)", membroId, novaIgrejaId, motivo);
  }

  @Transactional
  public void remanejarPastor(JwtPrincipal principal, UUID pastorId, UUID novaIgrejaId, String motivo) {
    if (principal == null || principal.userId() == null || principal.userId().isBlank()) {
      throw new IllegalArgumentException("Usuário não autenticado");
    }
    jdbc.update("SET LOCAL app.user_id = ?", principal.userId());
    jdbc.update("SELECT remanejar_pastor(?, ?, ?)", pastorId, novaIgrejaId, motivo);
  }
}
