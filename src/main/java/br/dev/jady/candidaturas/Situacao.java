package br.dev.jady.candidaturas;

import java.util.Set;

public enum Situacao {
    ENVIADA, EM_ANALISE, ENTREVISTA, APROVADA, REJEITADA, DESISTENCIA;

    boolean permite(Situacao destino) {
        if (this == destino) return true;
        return switch (this) {
            case ENVIADA -> Set.of(EM_ANALISE, ENTREVISTA, APROVADA, REJEITADA, DESISTENCIA).contains(destino);
            case EM_ANALISE -> Set.of(ENTREVISTA, APROVADA, REJEITADA, DESISTENCIA).contains(destino);
            case ENTREVISTA -> Set.of(APROVADA, REJEITADA, DESISTENCIA).contains(destino);
            case APROVADA, REJEITADA, DESISTENCIA -> false;
        };
    }
}
