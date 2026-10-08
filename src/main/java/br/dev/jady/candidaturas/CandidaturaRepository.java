package br.dev.jady.candidaturas;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {
    boolean existsByVagaId(UUID vagaId);
    @Query("select c from Candidatura c where (:status is null or c.status = :status) and (:empresaId is null or c.vaga.empresa.id = :empresaId)")
    Page<Candidatura> filtrar(@Param("status") Situacao status, @Param("empresaId") UUID empresaId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Candidatura c where c.id = :id")
    Optional<Candidatura> bloquear(@Param("id") UUID id);
}
