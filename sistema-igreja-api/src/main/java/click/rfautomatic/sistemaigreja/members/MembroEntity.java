package click.rfautomatic.sistemaigreja.members;

import click.rfautomatic.sistemaigreja.churches.IgrejaEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "membros",
    indexes = {
      @Index(name = "idx_membros_church_id", columnList = "church_id"),
      @Index(name = "idx_membros_cpf", columnList = "cpf")
    })
public class MembroEntity {
  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false)
  private String nome;

  @Column(length = 11)
  private String cpf;

  private String email;

  private String telefone;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "church_id")
  private IgrejaEntity igreja;

  @Column(name = "usuario_id", columnDefinition = "uuid")
  private UUID usuarioId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getCpf() {
    return cpf;
  }

  public void setCpf(String cpf) {
    this.cpf = cpf;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getTelefone() {
    return telefone;
  }

  public void setTelefone(String telefone) {
    this.telefone = telefone;
  }

  public IgrejaEntity getIgreja() {
    return igreja;
  }

  public void setIgreja(IgrejaEntity igreja) {
    this.igreja = igreja;
  }

  public UUID getUsuarioId() {
    return usuarioId;
  }

  public void setUsuarioId(UUID usuarioId) {
    this.usuarioId = usuarioId;
  }
}

