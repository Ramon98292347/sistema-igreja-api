package click.rfautomatic.sistemaigreja.cartas;

import click.rfautomatic.sistemaigreja.docs.DocumentoEmitidoEntity;
import click.rfautomatic.sistemaigreja.docs.DocumentoEmitidoRepository;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CartasService {

  private final DocumentoEmitidoRepository docs;
  private final WebClient webClient;

  @Value("${n8n.webhook.cartas-url:}")
  private String n8nWebhookUrl;

  @Value("${n8n.webhook.secret:}")
  private String n8nWebhookSecret;

  public CartasService(DocumentoEmitidoRepository docs, WebClient.Builder webClient) {
    this.docs = docs;
    this.webClient = webClient.build();
  }

  public record EmitirCartaResult(UUID cartaId, UUID documentoEmitidoId) {}

  public EmitirCartaResult criarDocumentoEmitidoEChamarN8n(UUID cartaId) {
    DocumentoEmitidoEntity d = new DocumentoEmitidoEntity();
    d.setTipo("carta");
    d.setStatus("PENDENTE");
    d.setReferenciaTipo("CARTA");
    d.setReferenciaId(cartaId);
    d = docs.save(d);

    dispararN8n(cartaId, d.getId());

    return new EmitirCartaResult(cartaId, d.getId());
  }

  private void dispararN8n(UUID cartaId, UUID documentoEmitidoId) {
    if (n8nWebhookUrl == null || n8nWebhookUrl.isBlank()) {
      return;
    }

    try {
      WebClient.RequestBodySpec req =
          webClient.post().uri(URI.create(n8nWebhookUrl)).contentType(MediaType.APPLICATION_JSON);

      if (n8nWebhookSecret != null && !n8nWebhookSecret.isBlank()) {
        req = req.header("X-Webhook-Secret", n8nWebhookSecret);
      }

      req.bodyValue(
              Map.of("carta_id", cartaId.toString(), "documento_emitido_id", documentoEmitidoId.toString()))
          .retrieve()
          .toBodilessEntity()
          .subscribe();

    } catch (Exception ignored) {
      // best effort
    }
  }
}
