package click.rfautomatic.sistemaigreja.auth;

import click.rfautomatic.sistemaigreja.users.UserEntity;
import click.rfautomatic.sistemaigreja.users.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  public record LoginRequest(@NotBlank String login, @NotBlank String senha) {}

  public record LoginResponse(String access_token, String token_type) {}

  public record FirstAccessStartRequest(@NotBlank String cpf) {}
  public record FirstAccessStartResponse(String user_id, String cpf_masked) {}

  @PostMapping("/first-access")
  public FirstAccessStartResponse firstAccessStart(@Valid @RequestBody FirstAccessStartRequest body) {
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
  public void firstAccessSetPassword(@Valid @RequestBody FirstAccessSetPasswordRequest body) {
    String cpf = onlyDigits(body.cpf());
    if (cpf.length() != 11) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido");

    UserEntity u = users.findById(java.util.UUID.fromString(body.user_id()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    if (!u.isAtivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo");
    if (u.getCpf() == null || !u.getCpf().equals(cpf)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF não confere");
    }
    if (!u.isFirstAccessPending()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Senha já definida");
    }

    String next = body.nova_senha() == null ? "" : body.nova_senha().trim();
    if (next.length() < 8) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha deve ter ao menos 8 caracteres");
    }

    u.setSenhaHash(encoder.encode(next));
    u.setFirstAccessPending(false);
    users.save(u);
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest body) {
    String login = body.login().trim();

    UserEntity user;
    if (isCpf(login)) {
      user =
          users
              .findByCpf(onlyDigits(login))
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
    } else {
      user =
          users
              .findByEmailIgnoreCase(login)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
    }

    if (!user.isAtivo()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo");
    }

    if (!encoder.matches(body.senha(), user.getSenhaHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    }

    String token =
        jwt.createAccessToken(
            user.getId().toString(),
            Map.of("role", user.getRole().name(), "cpf", user.getCpf() == null ? "" : user.getCpf()));

    return new LoginResponse(token, "Bearer");
  }

  private static boolean isCpf(String s) {
    String d = onlyDigits(s);
    return d.length() == 11;
  }

  private static String onlyDigits(String s) {
    return s.replaceAll("\\D", "");
  }

  private static String maskCpf(String cpf) {
    if (cpf == null || cpf.length() != 11) return "***";
    return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
  }
}
