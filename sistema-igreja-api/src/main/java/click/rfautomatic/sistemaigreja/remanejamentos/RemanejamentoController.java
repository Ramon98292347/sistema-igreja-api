package click.rfautomatic.sistemaigreja.remanejamentos;

import click.rfautomatic.sistemaigreja.auth.Authz;
import click.rfautomatic.sistemaigreja.auth.JwtPrincipal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/remanejamentos")
public class RemanejamentoController {

  private final RemanejamentoRpcService rpc;

  public RemanejamentoController(RemanejamentoRpcService rpc) {
    this.rpc = rpc;
  }

  public record TransferirMembroRequest(UUID membro_id, UUID nova_igreja_id, String motivo) {}

  @PostMapping("/transferir-membro")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void transferirMembro(@RequestBody TransferirMembroRequest body, Authentication authentication) {
    JwtPrincipal p = Authz.requirePrincipal(authentication);
    rpc.transferirMembro(p, body.membro_id(), body.nova_igreja_id(), body.motivo());
  }

  public record RemanejarPastorRequest(UUID pastor_id, UUID nova_igreja_id, String motivo) {}

  @PostMapping("/remanejar-pastor")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remanejarPastor(@RequestBody RemanejarPastorRequest body, Authentication authentication) {
    JwtPrincipal p = Authz.requirePrincipal(authentication);
    rpc.remanejarPastor(p, body.pastor_id(), body.nova_igreja_id(), body.motivo());
  }
}
