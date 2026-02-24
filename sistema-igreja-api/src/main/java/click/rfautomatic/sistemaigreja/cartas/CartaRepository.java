package click.rfautomatic.sistemaigreja.cartas;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartaRepository extends JpaRepository<CartaEntity, UUID> {
  long countByMembroIdAndDataPregacaoBetween(UUID membroId, LocalDate start, LocalDate end);

  boolean existsByMembroIdAndDataPregacaoAndTurno(UUID membroId, LocalDate dataPregacao, String turno);
}
