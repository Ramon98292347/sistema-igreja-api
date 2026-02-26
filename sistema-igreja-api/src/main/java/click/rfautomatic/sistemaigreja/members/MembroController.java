package click.rfautomatic.sistemaigreja.members;

// (imports removed)
import click.rfautomatic.sistemaigreja.churches.IgrejaRepository;
import click.rfautomatic.sistemaigreja.users.UserEntity;
import click.rfautomatic.sistemaigreja.users.UserRepository;
import click.rfautomatic.sistemaigreja.users.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/membros")
public class MembroController {

  private final MembroRepository membros;
  private final IgrejaRepository igrejas;
  private final UserRepository users;
  private final PasswordEncoder encoder;

  public MembroController(MembroRepository membros, IgrejaRepository igrejas, UserRepository users, PasswordEncoder encoder) {
    this.membros = membros;
    this.igrejas = igrejas;
    this.users = users;
    this.encoder = encoder;
  }

  public record MembroDto(
      UUID id,
      String nome,
      String cpf,
      String email,
      String telefone,
      UUID church_id,
      UUID usuario_id,
      String cargo_ministerial,
      Boolean ativo,
      com.fasterxml.jackson.databind.JsonNode ficha_json,
      String foto_r2_bucket,
      String foto_r2_key) {
    static MembroDto from(MembroEntity e) {
      UUID cid = e.getIgreja() != null ? e.getIgreja().getId() : null;
      return new MembroDto(
          e.getId(),
          e.getNome(),
          e.getCpf(),
          e.getEmail(),
          e.getTelefone(),
          cid,
          e.getUsuarioId(),
          e.getCargoMinisterial(),
          e.isAtivo(),
          e.getFichaJson(),
          e.getFotoR2Bucket(),
          e.getFotoR2Key());
    }
  }

  @GetMapping
  public List<MembroDto> list() {
    return membros.findAll().stream().map(MembroDto::from).toList();
  }

  @GetMapping("/{id}")
  public MembroDto get(@PathVariable UUID id) {
    MembroEntity e = membros.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return MembroDto.from(e);
  }

  public record CreateUsuarioInlineRequest(String cpf, String email, String senha, String role) {}

  public record CreateMembroRequest(
      String nome,
      String cpf,
      String email,
      String telefone,
      UUID church_id,
      UUID usuario_id,
      String cargo_ministerial,
      Boolean ativo,
      com.fasterxml.jackson.databind.JsonNode ficha_json,
      String foto_r2_bucket,
      String foto_r2_key,
      CreateUsuarioInlineRequest usuario) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MembroDto create(@RequestBody CreateMembroRequest body, Authentication authentication) {
    requireAdmin(authentication);
    MembroEntity e = new MembroEntity();
    e.setNome(body.nome());
    e.setCpf(body.cpf() == null ? null : onlyDigits(body.cpf()));
    e.setEmail(body.email());
    e.setTelefone(body.telefone());
    e.setFotoR2Bucket(body.foto_r2_bucket());
    e.setFotoR2Key(body.foto_r2_key());
    e.setCargoMinisterial(body.cargo_ministerial() == null || body.cargo_ministerial().isBlank() ? "membro" : body.cargo_ministerial());
    e.setAtivo(body.ativo() == null ? true : body.ativo());
    e.setFichaJson(body.ficha_json());

    if (body.church_id() != null) {
      e.setIgreja(igrejas.findById(body.church_id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "church_id inválido")));
    }

    if (body.usuario() != null) {
      UserEntity u = new UserEntity();
      u.setCpf(body.usuario().cpf() == null ? null : onlyDigits(body.usuario().cpf()));
      u.setEmail(body.usuario().email());
      u.setSenhaHash(encoder.encode(body.usuario().senha()));
      u.setRole(parseRole(body.usuario().role()));

      // Uniqueness checks (fail fast with 409)
      if (u.getCpf() != null && users.existsByCpf(u.getCpf())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
      }
      if (u.getEmail() != null && users.existsByEmailIgnoreCase(u.getEmail())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
      }

      u = users.save(u);
      e.setUsuarioId(u.getId());
    } else if (body.usuario_id() != null) {
      // allow linking an existing user to a member (admin only)
      if (!users.existsById(body.usuario_id())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuario_id inválido");
      }
      e.setUsuarioId(body.usuario_id());
    }

    return MembroDto.from(membros.save(e));
  }

  public record UpdateMembroRequest(
      String nome,
      String cpf,
      String email,
      String telefone,
      UUID church_id,
      UUID usuario_id,
      String cargo_ministerial,
      Boolean ativo,
      com.fasterxml.jackson.databind.JsonNode ficha_json,
      String foto_r2_bucket,
      String foto_r2_key) {}

  @PutMapping("/{id}")
  public MembroDto update(@PathVariable UUID id, @RequestBody UpdateMembroRequest body, Authentication authentication) {
    requireAdmin(authentication);
    MembroEntity e = membros.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    e.setNome(body.nome());
    e.setCpf(body.cpf() == null ? null : onlyDigits(body.cpf()));
    e.setEmail(body.email());
    e.setTelefone(body.telefone());
    e.setFotoR2Bucket(body.foto_r2_bucket());
    e.setFotoR2Key(body.foto_r2_key());
    e.setCargoMinisterial(body.cargo_ministerial());
    if (body.ativo() != null) e.setAtivo(body.ativo());
    e.setFichaJson(body.ficha_json());

    if (body.church_id() != null) {
      e.setIgreja(igrejas.findById(body.church_id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "church_id inválido")));
    } else {
      e.setIgreja(null);
    }

    // linking/unlinking user is admin-only (already required for this endpoint)
    if (body.usuario_id() != null && !users.existsById(body.usuario_id())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuario_id inválido");
    }
    e.setUsuarioId(body.usuario_id());

    // if member has login user linked, allow updating their role based on cargo (admin only)
    if (e.getUsuarioId() != null && body.cargo_ministerial() != null && !body.cargo_ministerial().isBlank()) {
      users.findById(e.getUsuarioId()).ifPresent(u -> {
        try {
          u.setRole(UserRole.valueOf(body.cargo_ministerial().trim().toUpperCase()));
          users.save(u);
        } catch (Exception ignored) {
          // ignore invalid cargo values
        }
      });
    }

    return MembroDto.from(membros.save(e));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Authentication authentication) {
    requireAdmin(authentication);
    if (!membros.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    membros.deleteById(id);
  }

  private static String onlyDigits(String s) {
    return s.replaceAll("\\D", "");
  }

  private static UserRole parseRole(String role) {
    if (role == null || role.isBlank()) return UserRole.OPERADOR;
    return UserRole.valueOf(role.trim().toUpperCase());
  }

  private static void requireAdmin(Authentication authentication) {
    if (authentication == null || authentication.getAuthorities() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
    }
    boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    if (!isAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente ADMIN");
    }
  }
}
