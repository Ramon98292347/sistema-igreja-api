package click.rfautomatic.sistemaigreja.cartas;

import click.rfautomatic.sistemaigreja.auth.JwtAuthenticationToken;
import click.rfautomatic.sistemaigreja.auth.JwtPrincipal;
import click.rfautomatic.sistemaigreja.docs.DocumentoEmitidoEntity;
import click.rfautomatic.sistemaigreja.docs.DocumentoEmitidoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cartas")
public class CartasController {

  private final CartasService service;
  private final CartasRpcService rpc;
  private final CartaRepository cartas;
  private final DocumentoEmitidoRepository docs;

  public CartasController(CartasService service, CartasRpcService rpc, CartaRepository cartas, DocumentoEmitidoRepository docs) {
    this.service = service;
    this.rpc = rpc;
    this.cartas = cartas;
    this.docs = docs;
  }

  public record EmitirCartaRequest(
      String membro_id,
      String igreja_destino_id,
      String data_pregacao,
      String turno,
      String horario_pregacao,
      String observacao) {}

  public record EmitirCartaResponse(String carta_id, String documento_emitido_id) {}

  @PostMapping("/emitir")
  @ResponseStatus(HttpStatus.CREATED)
  public EmitirCartaResponse emitir(@RequestBody EmitirCartaRequest body, Authentication authentication) {
    JwtPrincipal principal = ((JwtAuthenticationToken) authentication).getPrincipal() instanceof JwtPrincipal p ? p : null;

    UUID membroId = UUID.fromString(body.membro_id());
    UUID igrejaDestinoId = UUID.fromString(body.igreja_destino_id());

    UUID cartaId =
        rpc.emitirCartaValidada(
            principal,
            membroId,
            igrejaDestinoId,
            LocalDate.parse(body.data_pregacao()),
            body.turno(),
            LocalTime.parse(body.horario_pregacao()),
            body.observacao());

    CartasService.EmitirCartaResult r = service.criarDocumentoEmitidoEChamarN8n(cartaId);
    return new EmitirCartaResponse(r.cartaId().toString(), r.documentoEmitidoId().toString());
  }

  @GetMapping("/{id}/payload")
  public Map<String, Object> payload(@PathVariable("id") UUID id) {
    CartaEntity c = cartas.findById(id).orElseThrow();

    // Map.of does not allow null values; build manually.
    Map<String, Object> payload = new java.util.LinkedHashMap<>();
    payload.put("carta_id", c.getId().toString());
    payload.put("membro_id", c.getMembroId() == null ? null : c.getMembroId().toString());
    payload.put("pregador_nome", c.getPregadorNome());
    payload.put("cargo_ministerial", c.getCargoMinisterial());

    Map<String, Object> origem = new java.util.LinkedHashMap<>();
    origem.put("nome", c.getIgrejaOrigemNome());
    origem.put("codigo", c.getIgrejaOrigemCodigo());
    payload.put("igreja_origem", origem);

    Map<String, Object> destino = new java.util.LinkedHashMap<>();
    destino.put("nome", c.getIgrejaDestinoNome());
    destino.put("codigo", c.getIgrejaDestinoCodigo());
    payload.put("igreja_destino", destino);

    payload.put("data_emissao", c.getDataEmissao().toString());
    payload.put("data_pregacao", c.getDataPregacao().toString());
    payload.put("turno", c.getTurno());

    return payload;
  }

  public record DocumentoCallbackRequest(
      String status,
      String url_documento,
      String r2_bucket,
      String r2_key,
      String erro_msg) {}

  @PostMapping("/documentos_emitidos/{id}/callback")
  public DocumentoEmitidoEntity callback(@PathVariable("id") UUID id, @RequestBody DocumentoCallbackRequest body) {
    DocumentoEmitidoEntity d = docs.findById(id).orElseThrow();
    if (body.status() != null) d.setStatus(body.status());
    if (body.url_documento() != null) d.setUrlDocumento(body.url_documento());
    if (body.r2_bucket() != null) d.setR2Bucket(body.r2_bucket());
    if (body.r2_key() != null) d.setR2Key(body.r2_key());
    if (body.erro_msg() != null) d.setErroMsg(body.erro_msg());
    return docs.save(d);
  }
}
