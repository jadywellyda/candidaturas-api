package br.dev.jady.candidaturas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
    boolean existsByNomeChaveAndIdNot(String nomeChave, UUID id);
}
