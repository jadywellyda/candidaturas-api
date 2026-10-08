package br.dev.jady.candidaturas;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import static br.dev.jady.candidaturas.Dtos.*;

@RestController
@RequestMapping("/api")
public class CandidaturasController {
    private final CandidaturasService service;
    public CandidaturasController(CandidaturasService service) { this.service = service; }
    @GetMapping("/health") Map<String, String> health() { return Map.of("status", "UP"); }

    @GetMapping("/empresas") Pagina<EmpresaSaida> empresas(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) { return service.empresas(pagina, tamanho); }
    @GetMapping("/empresas/{id}") EmpresaSaida empresa(@PathVariable UUID id) { return service.empresa(id); }
    @PostMapping("/empresas") ResponseEntity<EmpresaSaida> criarEmpresa(@Valid @RequestBody EmpresaEntrada entrada) {
        var e = service.criarEmpresa(entrada); return ResponseEntity.created(URI.create("/api/empresas/" + e.id())).body(e);
    }
    @PutMapping("/empresas/{id}") EmpresaSaida editarEmpresa(@PathVariable UUID id, @Valid @RequestBody EmpresaEntrada entrada) { return service.editarEmpresa(id, entrada); }
    @DeleteMapping("/empresas/{id}") ResponseEntity<Void> excluirEmpresa(@PathVariable UUID id) { service.excluirEmpresa(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/vagas") Pagina<VagaSaida> vagas(@RequestParam(required = false) UUID empresaId, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) { return service.vagas(empresaId, pagina, tamanho); }
    @GetMapping("/vagas/{id}") VagaSaida vaga(@PathVariable UUID id) { return service.vaga(id); }
    @PostMapping("/vagas") ResponseEntity<VagaSaida> criarVaga(@Valid @RequestBody VagaEntrada entrada) {
        var v = service.criarVaga(entrada); return ResponseEntity.created(URI.create("/api/vagas/" + v.id())).body(v);
    }
    @PutMapping("/vagas/{id}") VagaSaida editarVaga(@PathVariable UUID id, @Valid @RequestBody VagaEntrada entrada) { return service.editarVaga(id, entrada); }
    @DeleteMapping("/vagas/{id}") ResponseEntity<Void> excluirVaga(@PathVariable UUID id) { service.excluirVaga(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/candidaturas") Pagina<CandidaturaSaida> candidaturas(@RequestParam(required = false) Situacao status, @RequestParam(required = false) UUID empresaId, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) { return service.candidaturas(status, empresaId, pagina, tamanho); }
    @GetMapping("/candidaturas/{id}") CandidaturaSaida candidatura(@PathVariable UUID id) { return service.candidatura(id); }
    @PostMapping("/candidaturas") ResponseEntity<CandidaturaSaida> criarCandidatura(@Valid @RequestBody CandidaturaEntrada entrada) {
        var c = service.criarCandidatura(entrada); return ResponseEntity.created(URI.create("/api/candidaturas/" + c.id())).body(c);
    }
    @PatchMapping("/candidaturas/{id}/status") CandidaturaSaida status(@PathVariable UUID id, @Valid @RequestBody StatusEntrada entrada) { return service.atualizarStatus(id, entrada); }
    @DeleteMapping("/candidaturas/{id}") ResponseEntity<Void> excluirCandidatura(@PathVariable UUID id) { service.excluirCandidatura(id); return ResponseEntity.noContent().build(); }
}
