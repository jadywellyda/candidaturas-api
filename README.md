# Candidaturas API

API REST para acompanhar candidaturas a vagas: empresas, oportunidades, etapas de seleção e entrevistas. Projeto de estudo em Java 21, Spring Boot 4.1.1, Spring Data JPA, validação e H2, com migração de banco pelo Flyway.

## Executar

Requisitos: **JDK 21**, `JAVA_HOME` configurado para o JDK e acesso à internet na primeira execução. O Maven Wrapper incluído baixa o Maven e as dependências; não é preciso instalar Maven separadamente.

No terminal, dentro da pasta do projeto:

```powershell
# Windows
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

```sh
# Linux/macOS
sh mvnw verify
sh mvnw spring-boot:run
```

A API fica em `http://127.0.0.1:8081/api`. Confira `GET /api/health`. O banco começa vazio e é salvo em `dados/candidaturas.mv.db`. Fechar e abrir a aplicação mantém os dados. O Flyway cria e valida a estrutura do banco; o Hibernate valida o mapeamento sem recriar as tabelas.

Para gerar e executar um JAR:

```powershell
.\mvnw.cmd package
java -jar target/candidaturas-api-0.0.1-SNAPSHOT.jar
```

Com a aplicação aberta, use outro terminal para a demonstração com dados fictícios:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File examples/demo.ps1
```

O exemplo cria uma empresa, uma vaga e uma candidatura e registra uma entrevista futura. Cada execução usa um nome de empresa diferente.

## Endpoints

| Método | Caminho após `/api` | Uso |
| --- | --- | --- |
| GET / POST | `/empresas` | Listar / criar empresas |
| GET / PUT / DELETE | `/empresas/{id}` | Consultar / editar / excluir empresa |
| GET / POST | `/vagas` | Listar / criar vagas |
| GET / PUT / DELETE | `/vagas/{id}` | Consultar / editar / excluir vaga |
| GET / POST | `/candidaturas` | Listar / criar candidaturas |
| GET / DELETE | `/candidaturas/{id}` | Consultar / excluir candidatura |
| PATCH | `/candidaturas/{id}/status` | Mudar etapa e observações; agendar entrevista |
| GET | `/health` | Conferir se a aplicação está disponível |

Listagens aceitam `pagina` (a partir de zero) e `tamanho` (de 1 a 100, padrão 20). `/vagas` aceita `empresaId`; `/candidaturas` aceita `empresaId` e `status`.

```text
GET /api/candidaturas?status=ENVIADA&pagina=0&tamanho=10
```

Resposta de listagem: `conteudo`, `pagina`, `tamanho`, `total` e `totalPaginas`. Os identificadores são UUIDs, retornados no cadastro. Criações retornam `201` e cabeçalho `Location`; exclusões retornam `204`.

## Corpos de exemplo

Empresa:

```json
{ "nome": "Empresa Exemplo" }
```

Vaga — substitua `empresaId` pelo UUID retornado:

```json
{
  "empresaId": "00000000-0000-0000-0000-000000000001",
  "titulo": "Estágio Java",
  "descricao": "Oportunidade fictícia de estudo",
  "link": "https://example.com/vaga"
}
```

Candidatura — substitua `vagaId` pelo UUID retornado e use uma data de envio passada ou atual:

```json
{
  "vagaId": "00000000-0000-0000-0000-000000000002",
  "dataEnvio": "2026-10-08",
  "observacoes": "Currículo enviado"
}
```

Agendar entrevista — use uma data futura, com offset de fuso:

```json
{
  "status": "ENTREVISTA",
  "entrevistaEm": "2099-10-09T10:00:00-03:00",
  "observacoes": "Entrevista técnica"
}
```

Os UUIDs e a data de entrevista acima são exemplos. O script de demonstração gera os valores automaticamente.

## Regras de negócio

- Nomes de empresas não podem repetir após remover espaços externos e normalizar maiúsculas/minúsculas.
- Uma vaga precisa estar vinculada a uma empresa existente. `descricao` e `link` são opcionais; quando informado, o link deve começar com `http://` ou `https://`.
- Este é um organizador pessoal: há **uma candidatura por vaga**, inclusive sob requisições simultâneas. Uma vaga pode ser cadastrada mais de uma vez se representar oportunidades distintas.
- A data de envio não pode ser futura; a referência de calendário é `America/Sao_Paulo`.
- Empresa com vagas e vaga com candidatura não podem ser excluídas antes dos registros dependentes.

Etapas permitidas:

| Etapa atual | Próximas etapas |
| --- | --- |
| ENVIADA | EM_ANALISE, ENTREVISTA, APROVADA, REJEITADA, DESISTENCIA |
| EM_ANALISE | ENTREVISTA, APROVADA, REJEITADA, DESISTENCIA |
| ENTREVISTA | APROVADA, REJEITADA, DESISTENCIA |
| APROVADA / REJEITADA / DESISTENCIA | A mesma etapa, sem reabertura |

A etapa `ENTREVISTA` exige `entrevistaEm` futura e aceita reagendamento. Nas demais etapas, não envie esse campo; uma data de entrevista já registrada fica preservada como histórico. `observacoes` omitida mantém o texto atual; uma string vazia o limpa.

## Erros e arquitetura

Entradas inválidas retornam `400`, registros ausentes retornam `404` e duplicidades, vínculos ou mudanças de etapa incompatíveis retornam `409`. Erros de validação usam o formato `ProblemDetail`, com uma lista `erros` quando aplicável.

O controller recebe e valida DTOs. O service aplica as regras dentro de transações. Os repositories consultam o banco via JPA, e as entidades ficam fora das respostas públicas. A criação de candidatura bloqueia a vaga durante a transação; uma restrição única no banco reforça a regra. Alterações de etapa também usam bloqueio de registro, e as entidades possuem controle de versão.

```text
src/main/java/br/dev/jady/candidaturas/   Controllers, DTOs, regras, entidades e repositories
src/main/resources/db/migration/         Estrutura versionada do banco
src/test/java/br/dev/jady/candidaturas/   Testes HTTP e inicialização
examples/demo.ps1                        Demonstração com dados fictícios
```

## Testes e validação

`verify` executa **11 testes**, com banco em memória separado dos dados de uso e relógio fixo. Eles cobrem cadastro e edição, vínculos, datas, entrevistas, mudanças de etapa, paginação, exclusões, erros e duas candidaturas simultâneas para a mesma vaga. As respostas são testadas por HTTP contra um servidor real em porta aleatória.

Validado no Windows com JDK 21: build completo, testes, JAR, exemplo PowerShell e persistência após reinício. O Maven Wrapper para Linux/macOS está incluído, mas não foi executado neste ambiente.

## Contexto e limites

Projeto de portfólio desenvolvido com assistência de IA e dados fictícios. A API roda localmente, sem autenticação, contas de usuário, anexos ou envio de mensagens. Não importe dados pessoais de candidaturas reais nesta demonstração. O H2 em arquivo é destinado a uma instância local; publicação em servidor exigiria autenticação e uma configuração de banco apropriada.

## Referências

- [Spring Boot](https://docs.spring.io/spring-boot/index.html)
- [Spring Data JPA — bloqueios](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
