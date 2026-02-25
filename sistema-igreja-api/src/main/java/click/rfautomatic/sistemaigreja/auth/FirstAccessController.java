package click.rfautomatic.sistemaigreja.auth;

import click.rfautomatic.sistemaigreja.users.UserEntity;
import click.rfautomatic.sistemaigreja.users.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class FirstAccessController {

  private final UserRepository users;
  private final PasswordEncoder encoder;

  public FirstAccessController(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  public record FirstAccessStartRequest(@NotBlank String cpf) {}
  public record FirstAccessStartResponse(String user_id, String cpf_masked) {}

  /**
   * MVP: identify by CPF only. For production, add an out-of-band verification (email/SMS/whatsapp code).
   */
  @PostMapping("/first-access")
  public FirstAccessStartResponse start(@Valid @RequestBody FirstAccessStartRequest body) {
    String cpf = onlyDigits(body.cpf());
    if (cpf.length() != 11) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido");

    UserEntity u = users.findByCpf(cpf).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CPF não encontrado"));
    if (!u.isAtivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo");

    if (!u.isFirstAccessPending()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já possui senha definida");
    }

    return new FirstAccessStartResponse(u.getId().toString(), maskCpf(cpf));
  }

  public record FirstAccessSetPasswordRequest(@NotBlank String user_id, @NotBlank String cpf, @NotBlank String nova_senha) {}

  @PostMapping("/first-access/set-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setPassword(@Valid @RequestBody FirstAccessSetPasswordRequest body) {
    UUID userId = UUID.fromString(body.user_id());
    String cpf = onlyDigits(body.cpf());

    UserEntity u = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    if (!u.isAtivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo");

    if (u.getCpf() == null || !u.getCpf().equals(cpf)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF não confere");
    }

    if (!u.isFirstAccessPending()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Senha já definida");
    }

    if (body.nova_senha().trim().length() < 8) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha deve ter ao menos 8 caracteres");
    }

    u.setSenhaHash(encoder.encode(body.nova_senha()));
    u.setFirstAccessPending(false);
    users.save(u);
  }

  private static String onlyDigits(String s) {
    return s == null ? "" : s.replaceAll("\\D", "");
  }

  private static String maskCpf(String cpf) {
    if (cpf == null || cpf.length() != 11) return "***";
    return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
  }
}
