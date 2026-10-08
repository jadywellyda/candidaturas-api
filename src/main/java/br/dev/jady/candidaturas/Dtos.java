package br.dev.jady.candidaturas;

import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}
    public record EmpresaEntrada(@NotBlank @Size(max = 120) String nome) {}
    public record EmpresaSaida(UUID id, String nome) {}
    public record VagaEntrada(@NotNull UUID empresaId, @NotBlank @Size(max = 120) String titulo,
                              @Size(max = 1000) String descricao,
                              @Size(max = 450) @Pattern(regexp = "^$|https?://\\S+") String link) {}
    public record VagaSaida(UUID id, UUID empresaId, String empresa, String titulo, String descricao, String link) {}
    public record CandidaturaEntrada(@NotNull UUID vagaId, @NotNull LocalDate dataEnvio,
                                     @Size(max = 1000) String observacoes) {}
    public record StatusEntrada(@NotNull Situacao status, OffsetDateTime entrevistaEm,
                                @Size(max = 1000) String observacoes) {}
    public record CandidaturaSaida(UUID id, UUID vagaId, String vaga, UUID empresaId, String empresa,
                                   Situacao status, LocalDate dataEnvio, OffsetDateTime entrevistaEm, String observacoes) {}
    public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long total, int totalPaginas) {
        static <T> Pagina<T> de(Page<T> page) {
            return new Pagina<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
