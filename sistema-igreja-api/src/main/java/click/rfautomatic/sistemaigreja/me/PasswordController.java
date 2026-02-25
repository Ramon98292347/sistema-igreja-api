package click.rfautomatic.sistemaigreja.me;

import click.rfautomatic.sistemaigreja.auth.Authz;
import click.rfautomatic.sistemaigreja.auth.JwtPrincipal;
import click.rfautomatic.sistemaigreja.users.UserEntity;
import click.rfautomatic.sistemaigreja.users.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/me")
public class PasswordController {

  private final UserRepository users;
  private final PasswordEncoder encoder;

  public PasswordController(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  public record ChangePasswordRequest(@NotBlank String senha_atual, @NotBlank String nova_senha) {}

  @PostMapping("/change-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@Valid @RequestBody ChangePasswordRequest body, Authentication authentication) {
    JwtPrincipal p = Authz.requirePrincipal(authentication);
    UUID userId = UUID.fromString(p.userId());

    UserEntity u = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (!u.isAtivo()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo");

    if (!encoder.matches(body.senha_atual(), u.getSenhaHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual inválida");
    }

    if (body.nova_senha() == null || body.nova_senha().isBlank() || body.nova_senha().length() < 8) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova senha deve ter ao menos 8 caracteres");
    }

    u.setSenhaHash(encoder.encode(body.nova_senha()));
    users.save(u);
  }
}
