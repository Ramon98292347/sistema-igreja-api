package click.rfautomatic.sistemaigreja.members;

import click.rfautomatic.sistemaigreja.auth.Authz;
import click.rfautomatic.sistemaigreja.users.UserEntity;
import click.rfautomatic.sistemaigreja.users.UserRepository;
import click.rfautomatic.sistemaigreja.users.UserRole;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/membros")
public class MemberUserProvisionController {

  private final MembroRepository membros;
  private final UserRepository users;
  private final PasswordEncoder encoder;

  public MemberUserProvisionController(MembroRepository membros, UserRepository users, PasswordEncoder encoder) {
    this.membros = membros;
    this.users = users;
    this.encoder = encoder;
  }

  public record ProvisionUserRequest(String nova_senha) {}

  public record ProvisionUserResponse(String usuario_id, String senha_temporaria) {}

  /**
   * Creates (or links) a login user for a member so they can access the system.
   * ADMIN/PASTOR only.
   */
  @PostMapping("/{id}/provision-user")
  @ResponseStatus(HttpStatus.OK)
  public ProvisionUserResponse provision(
      @PathVariable UUID id,
      @RequestBody(required = false) ProvisionUserRequest body,
      Authentication authentication) {
    Authz.requireAdminOrPastor(authentication);

    MembroEntity m = membros.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));

    if (m.getUsuarioId() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Este membro já tem usuário vinculado");
    }

    String cpf = m.getCpf();
    String email = m.getEmail();
    if ((cpf == null || cpf.isBlank()) && (email == null || email.isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe CPF ou e-mail no cadastro do membro antes de criar usuário");
    }

    // Try to reuse an existing user by CPF, otherwise create a new one
    UserEntity u = null;
    if (cpf != null && !cpf.isBlank()) {
      u = users.findByCpf(cpf).orElse(null);
    }
    if (u == null && email != null && !email.isBlank()) {
      u = users.findByEmailIgnoreCase(email).orElse(null);
    }

    // If user already linked to another member, block
    if (u != null) {
      MembroEntity existing = membros.findFirstByUsuarioId(u.getId()).orElse(null);
      if (existing != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já está vinculado a outro membro");
      }
    }

    String temp = body == null ? null : body.nova_senha();
    if (temp == null || temp.isBlank()) {
      temp = generateTempPassword(12);
    }

    if (u == null) {
      u = new UserEntity();
      u.setCpf(cpf);
      u.setEmail(email);
      u.setRole(roleFromCargo(m.getCargoMinisterial()));
      u.setAtivo(true);
    }

    u.setSenhaHash(encoder.encode(temp));
    u.setFirstAccessPending(false);
    u = users.save(u);

    m.setUsuarioId(u.getId());
    membros.save(m);

    return new ProvisionUserResponse(u.getId().toString(), temp);
  }

  private static UserRole roleFromCargo(String cargo) {
    if (cargo == null) return UserRole.OPERADOR;
    String c = cargo.trim().toLowerCase();
    if (c.equals("pastor")) return UserRole.PASTOR;
    if (c.equals("financeiro")) return UserRole.FINANCEIRO;
    return UserRole.OPERADOR;
  }

  private static String generateTempPassword(int len) {
    final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    SecureRandom rnd = new SecureRandom();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
    }
    return sb.toString();
  }
}
