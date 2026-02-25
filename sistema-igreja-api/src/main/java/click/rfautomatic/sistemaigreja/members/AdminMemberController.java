package click.rfautomatic.sistemaigreja.members;

import click.rfautomatic.sistemaigreja.auth.Authz;
import click.rfautomatic.sistemaigreja.churches.IgrejaEntity;
import click.rfautomatic.sistemaigreja.churches.IgrejaRepository;
import click.rfautomatic.sistemaigreja.users.UserEntity;
import click.rfautomatic.sistemaigreja.users.UserRepository;
import click.rfautomatic.sistemaigreja.users.UserRole;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/membros")
public class AdminMemberController {

  private final MembroRepository membros;
  private final IgrejaRepository igrejas;
  private final UserRepository users;
  private final PasswordEncoder encoder;

  public AdminMemberController(
      MembroRepository membros, IgrejaRepository igrejas, UserRepository users, PasswordEncoder encoder) {
    this.membros = membros;
    this.igrejas = igrejas;
    this.users = users;
    this.encoder = encoder;
  }

  public record CreateUsuarioPayload(String cpf, String email, String senha, String role) {}

  public record CreateAdminMembroRequest(
      String nome,
      String cpf,
      String email,
      String telefone,
      UUID church_id,
      String cargo_lideranca,
      Boolean ativo,
      String foto_r2_bucket,
      String foto_r2_key,
      CreateUsuarioPayload usuario) {}

  public record MembroDto(UUID id, String nome, String cpf, String email, String telefone, UUID church_id, UUID usuario_id) {
    static MembroDto from(MembroEntity e) {
      UUID cid = e.getIgreja() != null ? e.getIgreja().getId() : null;
      return new MembroDto(e.getId(), e.getNome(), e.getCpf(), e.getEmail(), e.getTelefone(), cid, e.getUsuarioId());
    }
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MembroDto create(@RequestBody CreateAdminMembroRequest body, Authentication authentication) {
    Authz.requireAdmin(authentication);

    if (body == null || body.nome() == null || body.nome().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nome é obrigatório");
    }

    UUID usuarioId = null;
    if (body.usuario() != null) {
      String loginCpf = body.usuario().cpf() == null ? null : body.usuario().cpf().replaceAll("\\D", "");
      String email = body.usuario().email();
      if ((loginCpf == null || loginCpf.isBlank()) && (email == null || email.isBlank())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cpf ou email do usuario é obrigatório");
      }
      if (body.usuario().senha() == null || body.usuario().senha().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senha do usuario é obrigatória");
      }

      // prevent duplicate cpf
      if (loginCpf != null && !loginCpf.isBlank() && users.existsByCpf(loginCpf)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
      }

      UserEntity u = new UserEntity();
      u.setCpf(loginCpf);
      u.setEmail(email);
      u.setSenhaHash(encoder.encode(body.usuario().senha()));

      String role = body.usuario().role();
      if (role != null && !role.isBlank()) {
        try {
          u.setRole(UserRole.valueOf(role.trim().toUpperCase()));
        } catch (Exception ignored) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role inválido");
        }
      }

      u = users.save(u);
      usuarioId = u.getId();
    }

    MembroEntity m = new MembroEntity();
    m.setNome(body.nome());
    m.setCpf(body.cpf() == null ? null : body.cpf().replaceAll("\\D", ""));
    m.setEmail(body.email());
    m.setTelefone(body.telefone());
    m.setUsuarioId(usuarioId);
    m.setFotoR2Bucket(body.foto_r2_bucket());
    m.setFotoR2Key(body.foto_r2_key());

    if (body.church_id() != null) {
      IgrejaEntity igreja = igrejas.findById(body.church_id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "church_id inválido"));
      m.setIgreja(igreja);
    }

    m = membros.save(m);
    return MembroDto.from(m);
  }
}
