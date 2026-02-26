package click.rfautomatic.sistemaigreja.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class Authz {
  private Authz() {}

  public static JwtPrincipal requirePrincipal(Authentication authentication) {
    if (authentication == null || !(authentication instanceof JwtAuthenticationToken) || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
    }
    return (JwtPrincipal) authentication.getPrincipal();
  }

  public static void requireAdmin(Authentication authentication) {
    JwtPrincipal p = requirePrincipal(authentication);
    if (p.role() == null || !p.role().equalsIgnoreCase("ADMIN")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão");
    }
  }

  public static void requireAdminOrPastor(Authentication authentication) {
    JwtPrincipal p = requirePrincipal(authentication);
    String role = p.role() == null ? "" : p.role().trim();
    if (!(role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("PASTOR"))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão");
    }
  }
}
