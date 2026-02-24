package click.rfautomatic.sistemaigreja.members;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembroRepository extends JpaRepository<MembroEntity, UUID> {
  Optional<MembroEntity> findFirstByUsuarioId(UUID usuarioId);
}

