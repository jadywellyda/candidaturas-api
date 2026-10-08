package br.dev.jady.candidaturas;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import static br.dev.jady.candidaturas.Dtos.*;

@Service
@Transactional(readOnly = true)
public class CandidaturasService {
    private final EmpresaRepository empresas;
    private final VagaRepository vagas;
    private final CandidaturaRepository candidaturas;
    private final Clock relogio;
    public CandidaturasService(EmpresaRepository empresas, VagaRepository vagas,
                               CandidaturaRepository candidaturas, Clock relogio) {
        this.empresas = empresas; this.vagas = vagas; this.candidaturas = candidaturas; this.relogio = relogio;
    }

    public Pagina<EmpresaSaida> empresas(int pagina, int tamanho) {
        return Pagina.de(empresas.findAll(pagina(pagina, tamanho, "nome")).map(CandidaturasService::saida));
    }
    public EmpresaSaida empresa(UUID id) { return saida(buscarEmpresa(id)); }
    @Transactional public EmpresaSaida criarEmpresa(EmpresaEntrada entrada) {
        Empresa e = new Empresa(); definirEmpresa(e, entrada); return saida(empresas.saveAndFlush(e));
    }
    @Transactional public EmpresaSaida editarEmpresa(UUID id, EmpresaEntrada entrada) {
        Empresa e = buscarEmpresa(id); definirEmpresa(e, entrada); return saida(empresas.saveAndFlush(e));
    }
    @Transactional public void excluirEmpresa(UUID id) {
        Empresa e = buscarEmpresa(id);
        if (vagas.existsByEmpresaId(id)) throw new FalhaNegocio(409, "Empresa possui vagas. Exclua-as primeiro.");
        empresas.delete(e); empresas.flush();
    }
    private void definirEmpresa(Empresa e, EmpresaEntrada entrada) {
        String nome = entrada.nome().strip(), chave = nome.toLowerCase(Locale.ROOT);
        if (empresas.existsByNomeChaveAndIdNot(chave, e.id == null ? new UUID(0, 0) : e.id)) {
            throw new FalhaNegocio(409, "Empresa já cadastrada com esse nome.");
        }
        e.nome = nome; e.nomeChave = chave;
    }

    public Pagina<VagaSaida> vagas(UUID empresaId, int pagina, int tamanho) {
        return Pagina.de(vagas.filtrar(empresaId, pagina(pagina, tamanho, "titulo")).map(CandidaturasService::saida));
    }
    public VagaSaida vaga(UUID id) { return saida(buscarVaga(id)); }
    @Transactional public VagaSaida criarVaga(VagaEntrada entrada) {
        Vaga v = new Vaga(); definirVaga(v, entrada); return saida(vagas.saveAndFlush(v));
    }
    @Transactional public VagaSaida editarVaga(UUID id, VagaEntrada entrada) {
        Vaga v = vagas.bloquear(id).orElseThrow(() -> new FalhaNegocio(404, "Vaga não encontrada."));
        definirVaga(v, entrada); return saida(vagas.saveAndFlush(v));
    }
    @Transactional public void excluirVaga(UUID id) {
        Vaga v = vagas.bloquear(id).orElseThrow(() -> new FalhaNegocio(404, "Vaga não encontrada."));
        if (candidaturas.existsByVagaId(id)) throw new FalhaNegocio(409, "Vaga possui candidatura. Exclua-a primeiro.");
        vagas.delete(v); vagas.flush();
    }
    private void definirVaga(Vaga v, VagaEntrada entrada) {
        v.empresa = buscarEmpresa(entrada.empresaId()); v.titulo = entrada.titulo().strip();
        v.descricao = entrada.descricao() == null ? "" : entrada.descricao().strip();
        v.link = entrada.link() == null || entrada.link().isBlank() ? null : entrada.link().strip();
    }

    public Pagina<CandidaturaSaida> candidaturas(Situacao status, UUID empresaId, int pagina, int tamanho) {
        return Pagina.de(candidaturas.filtrar(status, empresaId, pagina(pagina, tamanho, "dataEnvio")).map(CandidaturasService::saida));
    }
    public CandidaturaSaida candidatura(UUID id) {
        return saida(candidaturas.findById(id).orElseThrow(() -> new FalhaNegocio(404, "Candidatura não encontrada.")));
    }
    @Transactional public CandidaturaSaida criarCandidatura(CandidaturaEntrada entrada) {
        if (entrada.dataEnvio().isAfter(LocalDate.now(relogio))) throw new FalhaNegocio(400, "Data de envio não pode ser futura.");
        Vaga vaga = vagas.bloquear(entrada.vagaId()).orElseThrow(() -> new FalhaNegocio(404, "Vaga não encontrada."));
        if (candidaturas.existsByVagaId(vaga.id)) throw new FalhaNegocio(409, "Já existe candidatura para essa vaga.");
        Candidatura c = new Candidatura(); c.vaga = vaga; c.status = Situacao.ENVIADA;
        c.dataEnvio = entrada.dataEnvio(); c.observacoes = entrada.observacoes() == null ? "" : entrada.observacoes().strip();
        return saida(candidaturas.saveAndFlush(c));
    }
    @Transactional public CandidaturaSaida atualizarStatus(UUID id, StatusEntrada entrada) {
        Candidatura c = candidaturas.bloquear(id).orElseThrow(() -> new FalhaNegocio(404, "Candidatura não encontrada."));
        if (!c.status.permite(entrada.status())) throw new FalhaNegocio(409, "Mudança de etapa não permitida.");
        if (entrada.status() == Situacao.ENTREVISTA) {
            if (entrada.entrevistaEm() == null || !entrada.entrevistaEm().toInstant().isAfter(relogio.instant())) {
                throw new FalhaNegocio(400, "Informe uma entrevista com data, hora e fuso futuros.");
            }
            c.entrevistaEm = entrada.entrevistaEm();
        } else if (entrada.entrevistaEm() != null) throw new FalhaNegocio(400, "Data de entrevista é aceita somente na etapa ENTREVISTA.");
        c.status = entrada.status();
        if (entrada.observacoes() != null) c.observacoes = entrada.observacoes().strip();
        return saida(candidaturas.saveAndFlush(c));
    }
    @Transactional public void excluirCandidatura(UUID id) {
        Candidatura c = candidaturas.bloquear(id).orElseThrow(() -> new FalhaNegocio(404, "Candidatura não encontrada."));
        candidaturas.delete(c); candidaturas.flush();
    }

    private Empresa buscarEmpresa(UUID id) { return empresas.findById(id).orElseThrow(() -> new FalhaNegocio(404, "Empresa não encontrada.")); }
    private Vaga buscarVaga(UUID id) { return vagas.findById(id).orElseThrow(() -> new FalhaNegocio(404, "Vaga não encontrada.")); }
    private static PageRequest pagina(int pagina, int tamanho, String campo) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) throw new FalhaNegocio(400, "Página deve ser >= 0 e tamanho entre 1 e 100.");
        return PageRequest.of(pagina, tamanho, Sort.by(campo).ascending().and(Sort.by("id")));
    }
    private static EmpresaSaida saida(Empresa e) { return new EmpresaSaida(e.getId(), e.getNome()); }
    private static VagaSaida saida(Vaga v) { return new VagaSaida(v.getId(), v.empresa.getId(), v.empresa.getNome(), v.getTitulo(), v.descricao, v.link); }
    private static CandidaturaSaida saida(Candidatura c) {
        return new CandidaturaSaida(c.id, c.vaga.getId(), c.vaga.getTitulo(), c.vaga.getEmpresa().getId(), c.vaga.getEmpresa().getNome(),
                c.status, c.dataEnvio, c.entrevistaEm, c.observacoes);
    }
}
