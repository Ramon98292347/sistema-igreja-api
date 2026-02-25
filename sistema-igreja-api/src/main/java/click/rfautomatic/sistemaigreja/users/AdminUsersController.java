package click.rfautomatic.sistemaigreja.users;

import click.rfautomatic.sistemaigreja.auth.Authz;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/usuarios")
public class AdminUsersController {

  private final UserRepository users;
  private final PasswordEncoder encoder;

  public AdminUsersController(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  public record ResetPasswordRequest(String nova_senha, Boolean first_access_pending) {}

  public record ResetPasswordResponse(String senha_temporaria) {}

  /**
   * Admin-only reset. If nova_senha is omitted/blank, generates a random temporary password.
   * Returns the temporary password once (caller must show/copy to the user).
   */
  @PostMapping("/{id}/reset-password")
  @ResponseStatus(HttpStatus.OK)
  public ResetPasswordResponse resetPassword(
      @PathVariable UUID id, @RequestBody(required = false) ResetPasswordRequest body, Authentication authentication) {
    Authz.requireAdmin(authentication);

    UserEntity u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    String next = body == null ? null : body.nova_senha();
    if (next == null || next.isBlank()) {
      next = generateTempPassword(12);
    }

    u.setSenhaHash(encoder.encode(next));

    // Optionally mark as first access pending (so user can set their own password on first login)
    boolean pending = body != null && Boolean.TRUE.equals(body.first_access_pending());
    u.setFirstAccessPending(pending);

    users.save(u);

    return new ResetPasswordResponse(next);
  }

  private static String generateTempPassword(int len) {
    // No special chars to avoid WhatsApp/Telegram copy issues.
    final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    SecureRandom rnd = new SecureRandom();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
    }
    return sb.toString();
  }
}
