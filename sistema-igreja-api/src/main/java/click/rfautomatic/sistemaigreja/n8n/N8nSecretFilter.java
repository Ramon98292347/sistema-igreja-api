package click.rfautomatic.sistemaigreja.n8n;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class N8nSecretFilter extends OncePerRequestFilter {

  @Value("${n8n.webhook.secret:}")
  private String secret;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();

    // Protect endpoints that n8n will call without JWT.
    if (path.startsWith("/api/cartas/") && path.endsWith("/payload")) return false;
    if (path.startsWith("/api/cartas/documentos_emitidos/") && path.endsWith("/callback")) return false;

    return true;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String effective = secret;
    if (effective == null || effective.isBlank()) {
      // fallback to env var
      effective = System.getenv("N8N_WEBHOOK_SECRET");
    }

    if (effective == null || effective.isBlank()) {
      response.setStatus(500);
      response.getWriter().write("N8N webhook secret not configured");
      return;
    }

    String got = request.getHeader("X-Webhook-Secret");
    if (got == null || !got.equals(effective)) {
      response.setStatus(403);
      response.getWriter().write("Forbidden");
      return;
    }

    filterChain.doFilter(request, response);
  }
}
