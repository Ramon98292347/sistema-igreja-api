package click.rfautomatic.sistemaigreja.me;

import click.rfautomatic.sistemaigreja.auth.JwtAuthenticationToken;
import click.rfautomatic.sistemaigreja.auth.JwtPrincipal;
import click.rfautomatic.sistemaigreja.members.MembroEntity;
import click.rfautomatic.sistemaigreja.members.MembroRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

  private final MembroRepository membros;

  public MeController(MembroRepository membros) {
    this.membros = membros;
  }

  public record MeResponse(
      String user_id,
      String cpf,
      String role,
      String membro_id,
      String church_id,
      String nome) {}

  @GetMapping("/me")
  public MeResponse me(Authentication authentication) {
    if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
      return new MeResponse("", "", "", null, null, null);
    }

    JwtPrincipal p = (JwtPrincipal) authentication.getPrincipal();

    UUID userId = UUID.fromString(p.userId());
    MembroEntity m = membros.findFirstByUsuarioId(userId).orElse(null);

    String membroId = m != null ? m.getId().toString() : null;
    String churchId = (m != null && m.getIgreja() != null) ? m.getIgreja().getId().toString() : null;
    String nome = m != null ? m.getNome() : null;

    return new MeResponse(p.userId(), p.cpf(), p.role(), membroId, churchId, nome);
  }
}

