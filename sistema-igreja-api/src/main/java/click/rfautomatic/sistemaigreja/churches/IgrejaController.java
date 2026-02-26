package click.rfautomatic.sistemaigreja.churches;

import click.rfautomatic.sistemaigreja.auth.Authz;
import click.rfautomatic.sistemaigreja.auth.JwtPrincipal;
import click.rfautomatic.sistemaigreja.members.MembroEntity;
import click.rfautomatic.sistemaigreja.members.MembroRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/igrejas")
public class IgrejaController {

  private final IgrejaRepository repo;
  private final MembroRepository membros;

  public IgrejaController(IgrejaRepository repo, MembroRepository membros) {
    this.repo = repo;
    this.membros = membros;
  }

  private static final Set<String> LEVELS = Set.of("estadual", "setorial", "central", "regional", "local");

  private static boolean isMemberRole(String role) {
    if (role == null) return true;
    String r = role.trim().toUpperCase();
    return r.isBlank() || r.equals("MEMBRO") || r.equals("OPERADOR") || r.equals("SECRETARIA") || r.equals("DEPOSITO");
  }

  private UUID getUserChurchId(Authentication authentication) {
    JwtPrincipal p = Authz.requirePrincipal(authentication);
    UUID userId = UUID.fromString(p.userId());
    MembroEntity m = membros.findFirstByUsuarioId(userId).orElse(null);
    if (m == null || m.getIgreja() == null) return null;
    return m.getIgreja().getId();
  }

  private String normalizeLevel(String nivel) {
    String s = (nivel == null ? "" : nivel.trim().toLowerCase());
    if (!LEVELS.contains(s)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nível inválido");
    return s;
  }

  private boolean isInSubtree(UUID rootId, UUID candidateId) {
    if (rootId == null || candidateId == null) return false;
    UUID cur = candidateId;
    for (int i = 0; i < 32; i++) { // safety loop
      if (rootId.equals(cur)) return true;
      IgrejaEntity e = repo.findById(cur).orElse(null);
      if (e == null || e.getParentId() == null) return false;
      cur = e.getParentId();
    }
    return false;
  }

  private void requireCanManageChurch(Authentication authentication, UUID targetChurchId) {
    JwtPrincipal p = Authz.requirePrincipal(authentication);
    String role = p.role() == null ? "" : p.role().trim();
    if (role.equalsIgnoreCase("ADMIN")) return;
    if (!role.equalsIgnoreCase("PASTOR")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão");

    UUID myChurchId = getUserChurchId(authentication);
    if (myChurchId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem igreja vinculada");

    if (!isInSubtree(myChurchId, targetChurchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Fora da sua hierarquia");
    }
  }

  private void requireCanCreate(Authentication authentication, String nivel, UUID parentId) {
    JwtPrincipal p = Authz.requirePrincipal(authentication);
    String role = p.role() == null ? "" : p.role().trim();
    if (role.equalsIgnoreCase("ADMIN")) return;
    if (!role.equalsIgnoreCase("PASTOR")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão");

    UUID myChurchId = getUserChurchId(authentication);
    if (myChurchId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem igreja vinculada");

    // Must create under their subtree (we require an explicit parent)
    if (parentId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pastor deve selecionar a igreja superior");
    if (!isInSubtree(myChurchId, parentId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Igreja superior fora da sua hierarquia");

    IgrejaEntity root = repo.findById(myChurchId).orElse(null);
    String rootLevel = root == null ? null : (root.getNivel() == null ? null : root.getNivel().trim().toLowerCase());

    // Allowed child levels based on root level
    boolean ok;
    if (rootLevel == null) {
      ok = false;
    } else if (rootLevel.equals("estadual")) {
      ok = Set.of("setorial", "central", "regional", "local").contains(nivel);
    } else if (rootLevel.equals("setorial")) {
      ok = Set.of("central", "regional", "local").contains(nivel);
    } else if (rootLevel.equals("central")) {
      ok = Set.of("regional", "local").contains(nivel);
    } else if (rootLevel.equals("regional")) {
      ok = Set.of("local").contains(nivel);
    } else {
      ok = false;
    }

    if (!ok) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode cadastrar esse nível de igreja");
    }
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
    String nivel = normalizeLevel(body.nivel());
    requireCanCreate(authentication, nivel, body.parent_id());

    IgrejaEntity e = new IgrejaEntity();
    e.setNome(body.nome());
    e.setCidade(body.cidade());
    e.setEstado(body.estado());
    e.setNivel(nivel);
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
    requireCanManageChurch(authentication, id);
    IgrejaEntity e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    String nivel = normalizeLevel(body.nivel());

    // PUT = full replace (client must send desired final values; can send null to clear)
    e.setNome(body.nome());
    e.setCidade(body.cidade());
    e.setEstado(body.estado());
    e.setNivel(nivel);
    e.setParentId(body.parent_id());
    e.setPastorResponsavelId(body.pastor_responsavel_id());
    e.setFotoR2Bucket(body.foto_r2_bucket());
    e.setFotoR2Key(body.foto_r2_key());

    return IgrejaDto.from(repo.save(e));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Authentication authentication) {
    requireCanManageChurch(authentication, id);
    if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    repo.deleteById(id);
  }
}
