package br.dev.jady.candidaturas;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "vagas")
public class Vaga {
    @Id @GeneratedValue(strategy = GenerationType.UUID) UUID id;
    @Column(nullable = false, length = 120) String titulo;
    @Column(nullable = false, length = 1000) String descricao;
    @Column(length = 450) String link;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false) Empresa empresa;
    @Version long versao;
    protected Vaga() {}
    public UUID getId() { return id; }
    public String getTitulo() { return titulo; }
    public Empresa getEmpresa() { return empresa; }
}
