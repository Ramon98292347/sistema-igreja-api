package click.rfautomatic.sistemaigreja.cartas;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cartas")
public class CartaEntity {
  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "membro_id", columnDefinition = "uuid")
  private UUID membroId;

  @Column(name = "pregador_nome", nullable = false)
  private String pregadorNome;

  @Column(name = "cargo_ministerial")
  private String cargoMinisterial;

  @Column(name = "igreja_origem_nome")
  private String igrejaOrigemNome;

  @Column(name = "igreja_origem_codigo")
  private String igrejaOrigemCodigo;

  @Column(name = "igreja_destino_nome")
  private String igrejaDestinoNome;

  @Column(name = "igreja_destino_codigo")
  private String igrejaDestinoCodigo;

  @Column(name = "data_emissao", nullable = false)
  private LocalDate dataEmissao = LocalDate.now();

  @Column(name = "data_pregacao", nullable = false)
  private LocalDate dataPregacao;

  @Column(name = "turno", nullable = false)
  private String turno;

  @Column(name = "status", nullable = false)
  private String status = "ATIVA";

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

  public UUID getMembroId() {
    return membroId;
  }

  public void setMembroId(UUID membroId) {
    this.membroId = membroId;
  }

  public String getPregadorNome() {
    return pregadorNome;
  }

  public void setPregadorNome(String pregadorNome) {
    this.pregadorNome = pregadorNome;
  }

  public String getCargoMinisterial() {
    return cargoMinisterial;
  }

  public void setCargoMinisterial(String cargoMinisterial) {
    this.cargoMinisterial = cargoMinisterial;
  }

  public String getIgrejaOrigemNome() {
    return igrejaOrigemNome;
  }

  public void setIgrejaOrigemNome(String igrejaOrigemNome) {
    this.igrejaOrigemNome = igrejaOrigemNome;
  }

  public String getIgrejaOrigemCodigo() {
    return igrejaOrigemCodigo;
  }

  public void setIgrejaOrigemCodigo(String igrejaOrigemCodigo) {
    this.igrejaOrigemCodigo = igrejaOrigemCodigo;
  }

  public String getIgrejaDestinoNome() {
    return igrejaDestinoNome;
  }

  public void setIgrejaDestinoNome(String igrejaDestinoNome) {
    this.igrejaDestinoNome = igrejaDestinoNome;
  }

  public String getIgrejaDestinoCodigo() {
    return igrejaDestinoCodigo;
  }

  public void setIgrejaDestinoCodigo(String igrejaDestinoCodigo) {
    this.igrejaDestinoCodigo = igrejaDestinoCodigo;
  }

  public LocalDate getDataEmissao() {
    return dataEmissao;
  }

  public LocalDate getDataPregacao() {
    return dataPregacao;
  }

  public void setDataPregacao(LocalDate dataPregacao) {
    this.dataPregacao = dataPregacao;
  }

  public String getTurno() {
    return turno;
  }

  public void setTurno(String turno) {
    this.turno = turno;
  }
}
