package br.dev.jady.candidaturas;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidaturas")
public class Candidatura {
    @Id @GeneratedValue(strategy = GenerationType.UUID) UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vaga_id", nullable = false, unique = true) Vaga vaga;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Situacao status;
    @Column(name = "data_envio", nullable = false) LocalDate dataEnvio;
    @Column(name = "entrevista_em") OffsetDateTime entrevistaEm;
    @Column(nullable = false, length = 1000) String observacoes;
    @Version long versao;
    protected Candidatura() {}
}
