package br.dev.jady.candidaturas;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:candidaturas-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
@Import(CandidaturasApiTest.Tempo.class)
class CandidaturasApiTest {
    @TestConfiguration static class Tempo {
        @Bean @Primary Clock relogioTeste() { return Clock.fixed(Instant.parse("2030-06-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo")); }
    }
    @LocalServerPort int porta;
    @Autowired JsonMapper json;
    @Autowired CandidaturaRepository candidaturas;
    @Autowired VagaRepository vagas;
    @Autowired EmpresaRepository empresas;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    record Resposta(int status, JsonNode body) {}
    @BeforeEach void limparBanco() { candidaturas.deleteAllInBatch(); vagas.deleteAllInBatch(); empresas.deleteAllInBatch(); }
    @AfterAll static void fecharCliente() { HTTP.close(); }
    private Resposta enviar(String metodo, String caminho, Object body) throws Exception {
        String texto = body == null ? "" : body instanceof String s ? s : json.writeValueAsString(body);
        HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/api" + caminho))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                .method(metodo, texto.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(texto)).build();
        var r = HTTP.send(pedido, HttpResponse.BodyHandlers.ofString());
        return new Resposta(r.statusCode(), r.body().isBlank() ? null : json.readTree(r.body()));
    }
    private String empresa(String nome) throws Exception {
        var r = enviar("POST", "/empresas", Map.of("nome", nome)); assertEquals(201, r.status()); return r.body().get("id").stringValue();
    }
    private String vaga(String empresa) throws Exception {
        var r = enviar("POST", "/vagas", Map.of("empresaId", empresa, "titulo", "Estágio Java", "descricao", "Exemplo fictício"));
        assertEquals(201, r.status()); return r.body().get("id").stringValue();
    }
    private Map<String, String> entrada(String vaga) { return Map.of("vagaId", vaga, "dataEnvio", "2030-06-01"); }
    private String candidatura(String vaga) throws Exception {
        var r = enviar("POST", "/candidaturas", entrada(vaga)); assertEquals(201, r.status()); return r.body().get("id").stringValue();
    }
    @Test void empresaPodeSerEditadaEExcluida() throws Exception {
        String id = empresa("Empresa Exemplo");
        var r = enviar("PUT", "/empresas/" + id, Map.of("nome", "Novo Nome"));
        assertEquals(200, r.status()); assertEquals("Novo Nome", r.body().get("nome").stringValue());
        assertEquals(200, enviar("GET", "/empresas/" + id, null).status());
        assertEquals(204, enviar("DELETE", "/empresas/" + id, null).status());
        assertEquals(404, enviar("GET", "/empresas/" + id, null).status());
    }
    @Test void validaNomeEDuplicidadeDeEmpresa() throws Exception {
        empresa(" Empresa Exemplo ");
        assertEquals(409, enviar("POST", "/empresas", Map.of("nome", "empresa exemplo")).status());
        assertEquals(400, enviar("POST", "/empresas", Map.of("nome", " ")).status());
        assertEquals(400, enviar("POST", "/empresas", Map.of("nome", "x".repeat(121))).status());
        assertEquals(1, empresas.count());
    }
    @Test void vagasExigemEmpresaEValidamLink() throws Exception {
        String empresa = empresa("Empresa Exemplo");
        assertEquals(404, enviar("POST", "/vagas", Map.of("empresaId", UUID.randomUUID(), "titulo", "Vaga")).status());
        assertEquals(400, enviar("POST", "/vagas", Map.of("empresaId", empresa, "titulo", " ")).status());
        assertEquals(400, enviar("POST", "/vagas", Map.of("empresaId", empresa, "titulo", "Vaga", "link", "javascript:alert(1)")).status());
        String id = vaga(empresa);
        assertEquals(409, enviar("DELETE", "/empresas/" + empresa, null).status());
        var editada = enviar("PUT", "/vagas/" + id, Map.of("empresaId", empresa, "titulo", "Desenvolvimento Java"));
        assertEquals(200, editada.status()); assertEquals("Desenvolvimento Java", editada.body().get("titulo").stringValue());
        assertEquals(204, enviar("DELETE", "/vagas/" + id, null).status());
        assertEquals(204, enviar("DELETE", "/empresas/" + empresa, null).status());
    }
    @Test void candidaturaUnicaPreservaVinculos() throws Exception {
        String empresa = empresa("Empresa Exemplo"), vaga = vaga(empresa), id = candidatura(vaga);
        var r = enviar("GET", "/candidaturas/" + id, null);
        assertEquals(200, r.status()); assertEquals("ENVIADA", r.body().get("status").stringValue());
        assertEquals(empresa, r.body().get("empresaId").stringValue());
        assertEquals(409, enviar("POST", "/candidaturas", entrada(vaga)).status());
        assertEquals(409, enviar("DELETE", "/vagas/" + vaga, null).status());
        assertEquals(1, candidaturas.count());
    }
    @Test void rejeitaDataDeEnvioFuturaEVagaAusente() throws Exception {
        String vaga = vaga(empresa("Empresa Exemplo"));
        assertEquals(400, enviar("POST", "/candidaturas", Map.of("vagaId", vaga, "dataEnvio", "2030-06-02")).status());
        assertEquals(404, enviar("POST", "/candidaturas", entrada(UUID.randomUUID().toString())).status());
        assertEquals(0, candidaturas.count());
    }
    @Test void etapasEEntrevistasRespeitamRegras() throws Exception {
        String id = candidatura(vaga(empresa("Empresa Exemplo"))), caminho = "/candidaturas/" + id + "/status";
        assertEquals(400, enviar("PATCH", caminho, Map.of("status", "ENTREVISTA")).status());
        assertEquals(400, enviar("PATCH", caminho, Map.of("status", "ENTREVISTA", "entrevistaEm", "2030-05-31T10:00:00-03:00")).status());
        assertEquals(200, enviar("PATCH", caminho, Map.of("status", "EM_ANALISE")).status());
        assertEquals(409, enviar("PATCH", caminho, Map.of("status", "ENVIADA")).status());
        assertEquals(200, enviar("PATCH", caminho, Map.of("status", "ENTREVISTA", "entrevistaEm", "2030-06-02T10:00:00-03:00")).status());
        assertEquals(200, enviar("PATCH", caminho, Map.of("status", "APROVADA", "observacoes", "Entrevista concluída")).status());
        assertEquals(409, enviar("PATCH", caminho, Map.of("status", "REJEITADA")).status());
        var finalizada = enviar("GET", "/candidaturas/" + id, null);
        assertEquals("APROVADA", finalizada.body().get("status").stringValue());
        assertEquals("Entrevista concluída", finalizada.body().get("observacoes").stringValue());
        assertNotNull(finalizada.body().get("entrevistaEm"));
    }
    @Test void filtrosEPaginacaoRetornamApenasResultadosEsperados() throws Exception {
        String e1 = empresa("Empresa Um"), e2 = empresa("Empresa Dois");
        candidatura(vaga(e1)); String segunda = candidatura(vaga(e2));
        assertEquals(200, enviar("PATCH", "/candidaturas/" + segunda + "/status", Map.of("status", "REJEITADA")).status());
        var filtro = enviar("GET", "/candidaturas?status=ENVIADA&empresaId=" + e1, null);
        assertEquals(200, filtro.status()); assertEquals(1, filtro.body().get("total").intValue());
        var page = enviar("GET", "/candidaturas?tamanho=1&pagina=1", null);
        assertEquals(2, page.body().get("total").intValue()); assertEquals(1, page.body().get("conteudo").size());
        assertEquals(2, page.body().get("totalPaginas").intValue());
        assertEquals(400, enviar("GET", "/candidaturas?pagina=-1", null).status());
        assertEquals(400, enviar("GET", "/candidaturas?tamanho=101", null).status());
        assertEquals(400, enviar("GET", "/candidaturas?status=INVALIDO", null).status());
    }
    @Test void exclusaoDaCandidaturaLiberaVaga() throws Exception {
        String e = empresa("Empresa Exemplo"), v = vaga(e), c = candidatura(v);
        assertEquals(204, enviar("DELETE", "/candidaturas/" + c, null).status());
        assertEquals(404, enviar("GET", "/candidaturas/" + c, null).status());
        assertEquals(204, enviar("DELETE", "/vagas/" + v, null).status());
        assertEquals(204, enviar("DELETE", "/empresas/" + e, null).status());
    }
    @Test void dadosMalformadosRetornam400() throws Exception {
        assertEquals(400, enviar("POST", "/empresas", "{invalid").status());
        assertEquals(400, enviar("GET", "/empresas/nao-e-uuid", null).status());
        assertEquals(404, enviar("GET", "/empresas/" + UUID.randomUUID(), null).status());
        String v = vaga(empresa("Empresa Exemplo"));
        assertEquals(400, enviar("POST", "/candidaturas", Map.of("vagaId", v, "dataEnvio", "2030-02-30")).status());
    }
    @Test void candidaturasSimultaneasNaoGeramDuplicidade() throws Exception {
        String v = vaga(empresa("Empresa Exemplo")); CountDownLatch iniciar = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> pedido = () -> { iniciar.await(); return enviar("POST", "/candidaturas", entrada(v)).status(); };
            Future<Integer> a = executor.submit(pedido), b = executor.submit(pedido); iniciar.countDown();
            assertEquals(List.of(201, 409), java.util.stream.Stream.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)).sorted().toList());
        }
        assertEquals(1, candidaturas.count());
    }
}
