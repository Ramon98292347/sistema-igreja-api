package click.rfautomatic.sistemaigreja.members;

import click.rfautomatic.sistemaigreja.auth.Authz;
import click.rfautomatic.sistemaigreja.churches.IgrejaRepository;
import click.rfautomatic.sistemaigreja.users.UserEntity;
import click.rfautomatic.sistemaigreja.users.UserRepository;
import click.rfautomatic.sistemaigreja.users.UserRole;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/membros")
public class AdminMemberOpsController {

  private final MembroRepository membros;
  private final IgrejaRepository igrejas;
  private final UserRepository users;

  public AdminMemberOpsController(MembroRepository membros, IgrejaRepository igrejas, UserRepository users) {
    this.membros = membros;
    this.igrejas = igrejas;
    this.users = users;
  }

  public record SetCargoRequest(String cargo) {}
  public record SetChurchRequest(UUID church_id) {}

  @PostMapping("/{id}/set-cargo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setCargo(@PathVariable UUID id, @RequestBody SetCargoRequest body, Authentication authentication) {
    Authz.requireAdminOrPastor(authentication);
    if (body == null || body.cargo() == null || body.cargo().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cargo é obrigatório");
    }

    UserRole next;
    try {
      next = UserRole.valueOf(body.cargo().trim().toUpperCase());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cargo inválido");
    }

    // Pastor não pode promover para ADMIN/FINANCEIRO
    var p = Authz.requirePrincipal(authentication);
    boolean isAdmin = p.role() != null && p.role().equalsIgnoreCase("ADMIN");
    if (!isAdmin && (next == UserRole.ADMIN || next == UserRole.FINANCEIRO)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão");
    }

    MembroEntity m = membros.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
    m.setCargoSistema(next.name());
    membros.save(m);

    if (m.getUsuarioId() != null) {
      UserEntity u = users.findById(m.getUsuarioId()).orElse(null);
      if (u != null) {
        u.setRole(next);
        users.save(u);
      }
    }
  }

  @PostMapping("/{id}/set-church")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setChurch(@PathVariable UUID id, @RequestBody SetChurchRequest body, Authentication authentication) {
    Authz.requireAdminOrPastor(authentication);
    if (body == null || body.church_id() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "church_id é obrigatório");
    }

    MembroEntity m = membros.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));
    var igreja = igrejas.findById(body.church_id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "church_id inválido"));
    m.setIgreja(igreja);
    membros.save(m);
  }
}
