package click.rfautomatic.sistemaigreja.docs;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documentos_emitidos")
public class DocumentoEmitidoEntity {
  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false)
  private String tipo;

  @Column(nullable = false)
  private String status = "PENDENTE";

  @Column(name = "url_documento")
  private String urlDocumento;

  @Column(name = "r2_bucket")
  private String r2Bucket;

  @Column(name = "r2_key")
  private String r2Key;

  @Column(name = "erro_msg")
  private String erroMsg;

  @Column(name = "referencia_tipo")
  private String referenciaTipo;

  @Column(name = "referencia_id", columnDefinition = "uuid")
  private UUID referenciaId;

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

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getUrlDocumento() {
    return urlDocumento;
  }

  public void setUrlDocumento(String urlDocumento) {
    this.urlDocumento = urlDocumento;
  }

  public String getR2Bucket() {
    return r2Bucket;
  }

  public void setR2Bucket(String r2Bucket) {
    this.r2Bucket = r2Bucket;
  }

  public String getR2Key() {
    return r2Key;
  }

  public void setR2Key(String r2Key) {
    this.r2Key = r2Key;
  }

  public String getErroMsg() {
    return erroMsg;
  }

  public void setErroMsg(String erroMsg) {
    this.erroMsg = erroMsg;
  }

  public String getReferenciaTipo() {
    return referenciaTipo;
  }

  public void setReferenciaTipo(String referenciaTipo) {
    this.referenciaTipo = referenciaTipo;
  }

  public UUID getReferenciaId() {
    return referenciaId;
  }

  public void setReferenciaId(UUID referenciaId) {
    this.referenciaId = referenciaId;
  }
}
