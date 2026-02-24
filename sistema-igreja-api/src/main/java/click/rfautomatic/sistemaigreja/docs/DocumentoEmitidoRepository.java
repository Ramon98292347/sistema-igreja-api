package click.rfautomatic.sistemaigreja.docs;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoEmitidoRepository extends JpaRepository<DocumentoEmitidoEntity, UUID> {}
