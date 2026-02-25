package click.rfautomatic.sistemaigreja.churches;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "igrejas")
public class IgrejaEntity {
  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false)
  private String nome;

  private String cidade;

  @Column(length = 2)
  private String estado;

  // estadual|setorial|central|regional|local
  private String nivel;

  @Column(name = "parent_id", columnDefinition = "uuid")
  private UUID parentId;

  @Column(name = "pastor_responsavel_id", columnDefinition = "uuid")
  private UUID pastorResponsavelId;

  @Column(name = "foto_r2_bucket")
  private String fotoR2Bucket;

  @Column(name = "foto_r2_key")
  private String fotoR2Key;

  @Column(name = "carimbo_r2_bucket")
  private String carimboR2Bucket;

  @Column(name = "carimbo_r2_key")
  private String carimboR2Key;

  // e.g. CENTRAL | SETORIAL | ESTADUAL | LOCAL ... (string for flexibility)
  private String nivel;

  @Column(name = "parent_id", columnDefinition = "uuid")
  private UUID parentId;

  @Column(name = "pastor_responsavel_id", columnDefinition = "uuid")
  private UUID pastorResponsavelId;

  @Column(name = "foto_r2_bucket")
  private String fotoR2Bucket;

  @Column(name = "foto_r2_key")
  private String fotoR2Key;

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

  public String getCidade() {
    return cidade;
  }

  public void setCidade(String cidade) {
    this.cidade = cidade;
  }

  public String getEstado() {
    return estado;
  }

  public void setEstado(String estado) {
    this.estado = estado;
  }

  public String getNivel() {
    return nivel;
  }

  public void setNivel(String nivel) {
    this.nivel = nivel;
  }

  public UUID getParentId() {
    return parentId;
  }

  public void setParentId(UUID parentId) {
    this.parentId = parentId;
  }

  public UUID getPastorResponsavelId() {
    return pastorResponsavelId;
  }

  public void setPastorResponsavelId(UUID pastorResponsavelId) {
    this.pastorResponsavelId = pastorResponsavelId;
  }

  public String getFotoR2Bucket() {
    return fotoR2Bucket;
  }

  public void setFotoR2Bucket(String fotoR2Bucket) {
    this.fotoR2Bucket = fotoR2Bucket;
  }

  public String getFotoR2Key() {
    return fotoR2Key;
  }

  public void setFotoR2Key(String fotoR2Key) {
    this.fotoR2Key = fotoR2Key;
  }

  public String getNivel() {
    return nivel;
  }

  public void setNivel(String nivel) {
    this.nivel = nivel;
  }

  public UUID getParentId() {
    return parentId;
  }

  public void setParentId(UUID parentId) {
    this.parentId = parentId;
  }

  public UUID getPastorResponsavelId() {
    return pastorResponsavelId;
  }

  public void setPastorResponsavelId(UUID pastorResponsavelId) {
    this.pastorResponsavelId = pastorResponsavelId;
  }

  public String getFotoR2Bucket() {
    return fotoR2Bucket;
  }

  public void setFotoR2Bucket(String fotoR2Bucket) {
    this.fotoR2Bucket = fotoR2Bucket;
  }

  public String getFotoR2Key() {
    return fotoR2Key;
  }

  public void setFotoR2Key(String fotoR2Key) {
    this.fotoR2Key = fotoR2Key;
  }

  public String getCarimboR2Bucket() {
    return carimboR2Bucket;
  }

  public void setCarimboR2Bucket(String carimboR2Bucket) {
    this.carimboR2Bucket = carimboR2Bucket;
  }

  public String getCarimboR2Key() {
    return carimboR2Key;
  }

  public void setCarimboR2Key(String carimboR2Key) {
    this.carimboR2Key = carimboR2Key;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
