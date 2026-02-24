package click.rfautomatic.sistemaigreja.cartas;

import click.rfautomatic.sistemaigreja.docs.DocumentoEmitidoEntity;
import click.rfautomatic.sistemaigreja.docs.DocumentoEmitidoRepository;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CartasService {

  private final CartaRepository cartas;
  private final DocumentoEmitidoRepository docs;
  private final WebClient webClient;

  @Value("${n8n.webhook.cartas-url:}")
  private String n8nWebhookUrl;

  @Value("${n8n.webhook.secret:}")
  private String n8nWebhookSecret;

  public CartasService(CartaRepository cartas, DocumentoEmitidoRepository docs, WebClient.Builder webClient) {
    this.cartas = cartas;
    this.docs = docs;
    this.webClient = webClient.build();
  }

  public record EmitirCartaResult(UUID cartaId, UUID documentoEmitidoId) {}

  public EmitirCartaResult emitir(
      UUID membroId,
      String pregadorNome,
      String cargoMinisterial,
      String igrejaOrigemNome,
      String igrejaOrigemCodigo,
      String igrejaDestinoNome,
      String igrejaDestinoCodigo,
      LocalDate dataPregacao,
      String turno) {

    validarRegras(membroId, dataPregacao, turno);

    CartaEntity c = new CartaEntity();
    c.setMembroId(membroId);
    c.setPregadorNome(pregadorNome);
    c.setCargoMinisterial(cargoMinisterial);
    c.setIgrejaOrigemNome(igrejaOrigemNome);
    c.setIgrejaOrigemCodigo(igrejaOrigemCodigo);
    c.setIgrejaDestinoNome(igrejaDestinoNome);
    c.setIgrejaDestinoCodigo(igrejaDestinoCodigo);
    c.setDataPregacao(dataPregacao);
    c.setTurno(turno);

    c = cartas.save(c);

    DocumentoEmitidoEntity d = new DocumentoEmitidoEntity();
    d.setTipo("CARTA_PREGACAO");
    d.setStatus("PENDENTE");
    d.setReferenciaTipo("CARTA");
    d.setReferenciaId(c.getId());
    d = docs.save(d);

    dispararN8n(c.getId(), d.getId());

    return new EmitirCartaResult(c.getId(), d.getId());
  }

  private void validarRegras(UUID membroId, LocalDate dataPregacao, String turno) {
    LocalDate hoje = LocalDate.now();

    if (dataPregacao.isBefore(hoje)) {
      throw new IllegalArgumentException("Data de pregação não pode ser no passado");
    }

    if (dataPregacao.isAfter(hoje.plusDays(5))) {
      throw new IllegalArgumentException("Data de pregação não pode ser maior que 5 dias à frente");
    }

    if (membroId != null) {
      // semana (segunda..domingo)
      LocalDate start = dataPregacao.with(DayOfWeek.MONDAY);
      LocalDate end = dataPregacao.with(DayOfWeek.SUNDAY);

      long totalSemana = cartas.countByMembroIdAndDataPregacaoBetween(membroId, start, end);
      if (totalSemana >= 5) {
        throw new IllegalArgumentException("Limite de 5 cartas por semana atingido");
      }

      if (cartas.existsByMembroIdAndDataPregacaoAndTurno(membroId, dataPregacao, turno)) {
        throw new IllegalArgumentException("Já existe carta para este membro nesta data e turno");
      }
    }

    if (turno == null || turno.isBlank()) {
      throw new IllegalArgumentException("Turno é obrigatório");
    }
  }

  private void dispararN8n(UUID cartaId, UUID documentoEmitidoId) {
    if (n8nWebhookUrl == null || n8nWebhookUrl.isBlank()) {
      // webhook não configurado ainda; deixa PENDENTE
      return;
    }

    try {
      WebClient.RequestBodySpec req =
          webClient
              .post()
              .uri(URI.create(n8nWebhookUrl))
              .contentType(MediaType.APPLICATION_JSON);

      if (n8nWebhookSecret != null && !n8nWebhookSecret.isBlank()) {
        req = req.header("X-Webhook-Secret", n8nWebhookSecret);
      }

      req.bodyValue(Map.of("carta_id", cartaId.toString(), "documento_emitido_id", documentoEmitidoId.toString()))
          .retrieve()
          .toBodilessEntity()
          .subscribe();

    } catch (Exception ignored) {
      // best effort; errors will be visible by status staying PENDENTE
    }
  }
}
