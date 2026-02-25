package click.rfautomatic.sistemaigreja.remanejamentos;

import click.rfautomatic.sistemaigreja.auth.JwtAuthenticationToken;
import click.rfautomatic.sistemaigreja.auth.JwtPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/remanejamentos")
public class RemanejamentoController {

  private final RemanejamentoRpcService rpc;
  private final RemanejamentoRepository repo;

  public RemanejamentoController(RemanejamentoRpcService rpc, RemanejamentoRepository repo) {
    this.rpc = rpc;
    this.repo = repo;
  }

  public record RemanejamentoDto(
      UUID id,
      String tipo,
      String status,
      UUID membro_id,
      UUID igreja_origem_id,
      UUID igreja_destino_id,
      String motivo,
      UUID realizado_por,
      UUID realizado_por_usuario_id,
      Instant created_at,
      Instant executed_at,
      String erro_msg) {
    static RemanejamentoDto from(RemanejamentoEntity e) {
      return new RemanejamentoDto(
          e.getId(),
          e.getTipo(),
          e.getStatus(),
          e.getMembroId(),
          e.getIgrejaOrigemId(),
          e.getIgrejaDestinoId(),
          e.getMotivo(),
          e.getRealizadoPor(),
          e.getRealizadoPorUsuarioId(),
          e.getCreatedAt(),
          e.getExecutedAt(),
          e.getErroMsg());
    }
  }

  public record TransferirMembroRequest(String membro_id, String nova_igreja_id, String motivo) {}

  @PostMapping("/transferir-membro")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void transferirMembro(@RequestBody TransferirMembroRequest body, Authentication authentication) {
    JwtPrincipal principal = ((JwtAuthenticationToken) authentication).getPrincipal() instanceof JwtPrincipal p ? p : null;
    rpc.transferirMembro(principal, UUID.fromString(body.membro_id()), UUID.fromString(body.nova_igreja_id()), body.motivo());
  }

  public record RemanejarPastorRequest(String pastor_id, String nova_igreja_id, String motivo) {}

  @PostMapping("/remanejar-pastor")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remanejarPastor(@RequestBody RemanejarPastorRequest body, Authentication authentication) {
    JwtPrincipal principal = ((JwtAuthenticationToken) authentication).getPrincipal() instanceof JwtPrincipal p ? p : null;
    rpc.remanejarPastor(principal, UUID.fromString(body.pastor_id()), UUID.fromString(body.nova_igreja_id()), body.motivo());
  }

  @GetMapping
  public List<RemanejamentoDto> list(@RequestParam(value = "church_id", required = false) UUID churchId) {
    List<RemanejamentoEntity> rows =
        churchId == null
            ? repo.findTop200ByOrderByCreatedAtDesc()
            : repo.findTop200ByIgrejaOrigemIdOrIgrejaDestinoIdOrderByCreatedAtDesc(churchId, churchId);
    return rows.stream().map(RemanejamentoDto::from).toList();
  }
}
