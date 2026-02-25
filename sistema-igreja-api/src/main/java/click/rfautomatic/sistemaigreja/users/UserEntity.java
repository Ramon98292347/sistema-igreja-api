package click.rfautomatic.sistemaigreja.users;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "usuarios",
    indexes = {
      @Index(name = "idx_usuarios_cpf", columnList = "cpf"),
      @Index(name = "idx_usuarios_email", columnList = "email")
    })
public class UserEntity {
  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(length = 11, unique = true)
  private String cpf; // somente números

  @Column(length = 255, unique = true)
  private String email;

  @Column(name = "senha_hash", nullable = false, length = 100)
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role = UserRole.ADMIN;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "first_access_pending", nullable = false)
  private boolean firstAccessPending = false;

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

  public String getSenhaHash() {
    return senhaHash;
  }

  public void setSenhaHash(String senhaHash) {
    this.senhaHash = senhaHash;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public boolean isFirstAccessPending() {
    return firstAccessPending;
  }

  public void setFirstAccessPending(boolean firstAccessPending) {
    this.firstAccessPending = firstAccessPending;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
