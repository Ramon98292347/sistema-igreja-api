package click.rfautomatic.sistemaigreja.churches;

import click.rfautomatic.sistemaigreja.auth.Authz;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/igrejas")
public class IgrejaController {

  private final IgrejaRepository repo;

  public IgrejaController(IgrejaRepository repo) {
    this.repo = repo;
  }

  public record IgrejaDto(
      UUID id,
      String nome,
      String cidade,
      String estado,
      String nivel,
      UUID parent_id,
      UUID pastor_responsavel_id,
      String foto_r2_bucket,
      String foto_r2_key) {
    static IgrejaDto from(IgrejaEntity e) {
      return new IgrejaDto(
          e.getId(),
          e.getNome(),
          e.getCidade(),
          e.getEstado(),
          e.getNivel(),
          e.getParentId(),
          e.getPastorResponsavelId(),
          e.getFotoR2Bucket(),
          e.getFotoR2Key());
    }
  }

  @GetMapping
  public List<IgrejaDto> list() {
    return repo.findAll().stream().map(IgrejaDto::from).toList();
  }

  @GetMapping("/{id}")
  public IgrejaDto get(@PathVariable UUID id) {
    IgrejaEntity e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return IgrejaDto.from(e);
  }

  public record CreateIgrejaRequest(
      String nome,
      String cidade,
      String estado,
      String nivel,
      UUID parent_id,
      UUID pastor_responsavel_id,
      String foto_r2_bucket,
      String foto_r2_key) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public IgrejaDto create(@RequestBody CreateIgrejaRequest body, Authentication authentication) {
    Authz.requireAdmin(authentication);
    IgrejaEntity e = new IgrejaEntity();
    e.setNome(body.nome());
    e.setCidade(body.cidade());
    e.setEstado(body.estado());
    e.setNivel(body.nivel());
    e.setParentId(body.parent_id());
    e.setPastorResponsavelId(body.pastor_responsavel_id());
    e.setFotoR2Bucket(body.foto_r2_bucket());
    e.setFotoR2Key(body.foto_r2_key());
    return IgrejaDto.from(repo.save(e));
  }

  public record UpdateIgrejaRequest(
      String nome,
      String cidade,
      String estado,
      String nivel,
      UUID parent_id,
      UUID pastor_responsavel_id,
      String foto_r2_bucket,
      String foto_r2_key) {}

  @PutMapping("/{id}")
  public IgrejaDto update(@PathVariable UUID id, @RequestBody UpdateIgrejaRequest body, Authentication authentication) {
    Authz.requireAdmin(authentication);
    IgrejaEntity e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    // PUT = full replace (client must send desired final values; can send null to clear)
    e.setNome(body.nome());
    e.setCidade(body.cidade());
    e.setEstado(body.estado());
    e.setNivel(body.nivel());
    e.setParentId(body.parent_id());
    e.setPastorResponsavelId(body.pastor_responsavel_id());
    e.setFotoR2Bucket(body.foto_r2_bucket());
    e.setFotoR2Key(body.foto_r2_key());

    return IgrejaDto.from(repo.save(e));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Authentication authentication) {
    Authz.requireAdmin(authentication);
    if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    repo.deleteById(id);
  }
}
