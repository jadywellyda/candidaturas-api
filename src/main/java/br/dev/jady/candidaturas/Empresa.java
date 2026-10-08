package br.dev.jady.candidaturas;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "empresas")
public class Empresa {
    @Id @GeneratedValue(strategy = GenerationType.UUID) UUID id;
    @Column(nullable = false, length = 120) String nome;
    @Column(name = "nome_chave", nullable = false, unique = true, length = 240) String nomeChave;
    @Version long versao;
    protected Empresa() {}
    public UUID getId() { return id; }
    public String getNome() { return nome; }
}
