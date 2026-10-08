package br.dev.jady.candidaturas;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface VagaRepository extends JpaRepository<Vaga, UUID> {
    boolean existsByEmpresaId(UUID empresaId);
    @Query("select v from Vaga v where (:empresaId is null or v.empresa.id = :empresaId)")
    Page<Vaga> filtrar(@Param("empresaId") UUID empresaId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vaga v where v.id = :id")
    Optional<Vaga> bloquear(@Param("id") UUID id);
}
