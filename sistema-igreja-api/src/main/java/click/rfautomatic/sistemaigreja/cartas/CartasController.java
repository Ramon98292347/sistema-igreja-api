package click.rfautomatic.sistemaigreja.cartas;

import click.rfautomatic.sistemaigreja.docs.DocumentoEmitidoEntity;
import click.rfautomatic.sistemaigreja.docs.DocumentoEmitidoRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cartas")
public class CartasController {

  private final CartasService service;
  private final CartaRepository cartas;
  private final DocumentoEmitidoRepository docs;

  public CartasController(CartasService service, CartaRepository cartas, DocumentoEmitidoRepository docs) {
    this.service = service;
    this.cartas = cartas;
    this.docs = docs;
  }

  public record EmitirCartaRequest(
      String pregador_nome,
      String cargo_ministerial,
      String igreja_origem_nome,
      String igreja_origem_codigo,
      String igreja_destino_nome,
      String igreja_destino_codigo,
      String data_pregacao,
      String turno,
      String membro_id) {}

  public record EmitirCartaResponse(String carta_id, String documento_emitido_id) {}

  @PostMapping("/emitir")
  @ResponseStatus(HttpStatus.CREATED)
  public EmitirCartaResponse emitir(@RequestBody EmitirCartaRequest body) {
    UUID membroId = body.membro_id() == null || body.membro_id().isBlank() ? null : UUID.fromString(body.membro_id());

    CartasService.EmitirCartaResult r =
        service.emitir(
            membroId,
            body.pregador_nome(),
            body.cargo_ministerial(),
            body.igreja_origem_nome(),
            body.igreja_origem_codigo(),
            body.igreja_destino_nome(),
            body.igreja_destino_codigo(),
            LocalDate.parse(body.data_pregacao()),
            body.turno());

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

  public record DocumentoCallbackRequest(String status, String url_documento, String erro_msg) {}

  @PostMapping("/documentos_emitidos/{id}/callback")
  public DocumentoEmitidoEntity callback(@PathVariable("id") UUID id, @RequestBody DocumentoCallbackRequest body) {
    DocumentoEmitidoEntity d = docs.findById(id).orElseThrow();
    if (body.status() != null) d.setStatus(body.status());
    if (body.url_documento() != null) d.setUrlDocumento(body.url_documento());
    if (body.erro_msg() != null) d.setErroMsg(body.erro_msg());
    return docs.save(d);
  }
}
