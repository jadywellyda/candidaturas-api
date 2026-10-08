CREATE TABLE empresas (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    nome_chave VARCHAR(240) NOT NULL UNIQUE,
    versao BIGINT NOT NULL
);
CREATE TABLE vagas (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES empresas(id),
    titulo VARCHAR(120) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    link VARCHAR(450),
    versao BIGINT NOT NULL
);
CREATE TABLE candidaturas (
    id UUID PRIMARY KEY,
    vaga_id UUID NOT NULL UNIQUE REFERENCES vagas(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ENVIADA','EM_ANALISE','ENTREVISTA','APROVADA','REJEITADA','DESISTENCIA')),
    data_envio DATE NOT NULL,
    entrevista_em TIMESTAMP WITH TIME ZONE,
    observacoes VARCHAR(1000) NOT NULL,
    versao BIGINT NOT NULL,
    CHECK (status <> 'ENTREVISTA' OR entrevista_em IS NOT NULL)
);
CREATE INDEX idx_vagas_empresa ON vagas(empresa_id);
CREATE INDEX idx_candidaturas_status ON candidaturas(status);
