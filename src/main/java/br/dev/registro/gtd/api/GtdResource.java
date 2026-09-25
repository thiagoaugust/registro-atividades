package br.dev.registro.gtd.api;

import br.dev.registro.atividades.api.RegistroDto;
import br.dev.registro.checkin.domain.CheckinDiario;
import br.dev.registro.checkin.domain.CheckinService;
import br.dev.registro.comum.Relogio;
import br.dev.registro.gtd.api.GtdDtos.AcaoDto;
import br.dev.registro.gtd.api.GtdDtos.ConclusaoDto;
import br.dev.registro.gtd.api.GtdDtos.ContextoDto;
import br.dev.registro.gtd.api.GtdDtos.InboxItemDto;
import br.dev.registro.gtd.api.GtdDtos.ReferenciaDto;
import br.dev.registro.gtd.domain.AcaoService;
import br.dev.registro.gtd.domain.Energia;
import br.dev.registro.gtd.domain.EstadoAcao;
import br.dev.registro.gtd.domain.ProgressoProjeto;
import br.dev.registro.gtd.domain.GtdCatalogoService;
import br.dev.registro.gtd.domain.InboxItem;
import br.dev.registro.gtd.domain.InboxService;
import br.dev.registro.gtd.domain.PassoRevisao;
import br.dev.registro.gtd.domain.RevisaoSemanal;
import br.dev.registro.gtd.domain.RevisaoSemanalService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/api/gtd")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GtdResource {

    private final InboxService inbox;
    private final AcaoService acoes;
    private final GtdCatalogoService catalogo;
    private final CheckinService checkins;
    private final RevisaoSemanalService revisoes;
    private final Relogio relogio;

    public GtdResource(
            InboxService inbox,
            AcaoService acoes,
            GtdCatalogoService catalogo,
            CheckinService checkins,
            RevisaoSemanalService revisoes,
            Relogio relogio) {
        this.inbox = inbox;
        this.acoes = acoes;
        this.catalogo = catalogo;
        this.checkins = checkins;
        this.revisoes = revisoes;
        this.relogio = relogio;
    }

    // ---------- 1. Capturar ----------

    public record InboxDto(long pendentes, List<InboxItemDto> itens) {
    }

    @GET
    @Path("inbox")
    public InboxDto listarInbox() {
        List<InboxItem> pendentes = inbox.pendentes();
        return new InboxDto(pendentes.size(), pendentes.stream().map(InboxItemDto::de).toList());
    }

    @POST
    @Path("inbox")
    public Response capturar(@Valid InboxService.DadosCaptura dados) {
        InboxItemDto item = InboxItemDto.de(inbox.capturar(dados));
        return Response.created(URI.create("/api/gtd/inbox/" + item.id())).entity(item).build();
    }

    /** O proximo item a esclarecer. 204 quando o inbox esta zerado. */
    @GET
    @Path("inbox/proximo")
    public Response proximo() {
        InboxItem item = inbox.proximoPendente();
        return item == null ? Response.noContent().build() : Response.ok(InboxItemDto.de(item)).build();
    }

    @DELETE
    @Path("inbox/{id}")
    public Response descartarDoInbox(@PathParam("id") long id) {
        inbox.descartar(id);
        return Response.noContent().build();
    }

    // ---------- 2. Esclarecer ----------

    @POST
    @Path("inbox/{id}/processar")
    public InboxItemDto processar(@PathParam("id") long id, @Valid InboxService.Decisao decisao) {
        return InboxItemDto.de(inbox.processar(id, decisao));
    }

    // ---------- 3. Organizar ----------

    @GET
    @Path("acoes")
    public List<AcaoDto> listarAcoes(
            @QueryParam("estado") EstadoAcao estado,
            @QueryParam("contexto") Long contextoId,
            @QueryParam("projeto") Long projetoId) {
        return AcaoDto.de(acoes.listar(estado, contextoId, projetoId));
    }

    @GET
    @Path("acoes/{id}")
    public AcaoDto acao(@PathParam("id") long id) {
        return AcaoDto.de(acoes.porId(id));
    }

    @POST
    @Path("acoes")
    public Response criarAcao(@Valid AcaoService.DadosAcao dados) {
        AcaoDto criada = AcaoDto.de(acoes.criar(dados));
        return Response.created(URI.create("/api/gtd/acoes/" + criada.id())).entity(criada).build();
    }

    @PUT
    @Path("acoes/{id}")
    public AcaoDto atualizarAcao(@PathParam("id") long id, @Valid AcaoService.DadosAcao dados) {
        return AcaoDto.de(acoes.atualizar(id, dados));
    }

    @POST
    @Path("acoes/{id}/concluir")
    @Consumes(MediaType.WILDCARD)
    public ConclusaoDto concluir(@PathParam("id") long id) {
        return ConclusaoDto.de(acoes.concluir(id));
    }

    @POST
    @Path("acoes/{id}/reabrir")
    @Consumes(MediaType.WILDCARD)
    public AcaoDto reabrir(@PathParam("id") long id) {
        return AcaoDto.de(acoes.reabrir(id));
    }

    /** Fecha o ciclo: a acao concluida vira registro de atividade pedindo so duracao e esforco. */
    @POST
    @Path("acoes/{id}/registrar")
    public RegistroDto registrarComoAtividade(
            @PathParam("id") long id, @Valid AcaoService.DadosRegistroDaAcao dados) {
        return RegistroDto.de(acoes.registrarComoAtividade(id, dados));
    }

    @DELETE
    @Path("acoes/{id}")
    public Response excluirAcao(@PathParam("id") long id) {
        acoes.excluir(id);
        return Response.noContent().build();
    }

    /** Cria o projeto ja com as tarefas, numa transacao so. */
    @POST
    @Path("projetos")
    public Response criarProjetoComTarefas(@Valid AcaoService.DadosProjetoComTarefas dados) {
        ProgressoProjeto criado = acoes.criarProjetoComTarefas(dados);
        return Response.created(URI.create("/api/projetos/" + criado.projetoId())).entity(criado).build();
    }

    /** Projetos com o progresso das tarefas: a tela de projetos mostra o que falta. */
    @GET
    @Path("projetos/progresso")
    public List<ProgressoProjeto> progressoProjetos() {
        return acoes.progressoProjetos();
    }

    /** Projeto ativo sem nenhuma acao aberta — o alerta que a revisao semanal precisa dar. */
    @GET
    @Path("projetos/sem-proxima-acao")
    public List<GtdDtos.Vinculo> projetosParados() {
        return acoes.projetosParados().stream()
                .map(p -> new GtdDtos.Vinculo(p.id, p.titulo))
                .toList();
    }

    // ---------- 4. Refletir: revisao semanal ----------

    public record RevisaoDto(
            java.time.LocalDate semanaInicio,
            java.time.Instant iniciadaEm,
            java.time.Instant concluidaEm,
            Integer duracaoMin,
            java.util.Map<String, Object> passos,
            boolean completa) {

        static RevisaoDto de(RevisaoSemanal r) {
            return new RevisaoDto(
                    r.semanaInicio, r.iniciadaEm, r.concluidaEm, r.duracaoMin, r.passos,
                    r.todosOsPassosFeitos());
        }
    }

    public record MarcarPasso(boolean feito) {
    }

    /** Os passos do checklist, com titulo e o porque de cada um. */
    @GET
    @Path("revisoes/checklist")
    public List<java.util.Map<String, String>> checklist() {
        return revisoes.checklist();
    }

    @GET
    @Path("revisoes")
    public List<RevisaoDto> listarRevisoes() {
        return revisoes.recentes().stream().map(RevisaoDto::de).toList();
    }

    /** A revisao da semana corrente. 204 quando ela ainda nao foi aberta. */
    @GET
    @Path("revisoes/atual")
    public Response revisaoAtual() {
        RevisaoSemanal revisao = revisoes.atual();
        return revisao == null ? Response.noContent().build() : Response.ok(RevisaoDto.de(revisao)).build();
    }

    /** Abre a revisao da semana, ou devolve a que ja estava em andamento. */
    @POST
    @Path("revisoes")
    @Consumes(MediaType.WILDCARD)
    public RevisaoDto iniciarRevisao(@QueryParam("semana") java.time.LocalDate semana) {
        return RevisaoDto.de(revisoes.iniciar(semana));
    }

    @PUT
    @Path("revisoes/{semana}/passos/{passo}")
    public RevisaoDto marcarPasso(
            @PathParam("semana") java.time.LocalDate semana,
            @PathParam("passo") PassoRevisao passo,
            MarcarPasso dados) {
        return RevisaoDto.de(revisoes.marcarPasso(semana, passo, dados == null || dados.feito()));
    }

    /**
     * Conclui a revisao. O resumo da semana fica com o cliente, que ja chama /api/analytics/periodo
     * para isso — trazer analytics para dentro do GTD acoplaria dois modulos por uma tela so.
     */
    @POST
    @Path("revisoes/{semana}/concluir")
    @Consumes(MediaType.WILDCARD)
    public RevisaoDto concluirRevisao(
            @PathParam("semana") java.time.LocalDate semana,
            @QueryParam("duracaoMin") Integer duracaoMin) {
        return RevisaoDto.de(revisoes.concluir(semana, duracaoMin));
    }

    // ---------- 5. Engajar ----------

    public record EngajarDto(Energia energiaSugerida, List<AcaoDto> acoes) {
    }

    /**
     * "O que fazer agora?". Sem energia informada, a do check-in de hoje serve de palpite: energia 1
     * ou 2 pela manha nao combina com a tarefa que exige alta.
     */
    @GET
    @Path("engajar")
    public EngajarDto engajar(
            @QueryParam("contexto") Long contextoId,
            @QueryParam("tempoDisponivel") Integer tempoDisponivelMin,
            @QueryParam("energia") Energia energia) {
        Energia energiaUsada = energia != null ? energia : energiaDoCheckin();
        return new EngajarDto(energiaUsada, AcaoDto.de(acoes.engajar(contextoId, tempoDisponivelMin, energiaUsada)));
    }

    private Energia energiaDoCheckin() {
        CheckinDiario checkin = checkins.porData(relogio.hoje());
        if (checkin == null || checkin.energia == null) {
            return null;
        }
        return switch (checkin.energia) {
            case 1, 2 -> Energia.BAIXA;
            case 3 -> Energia.MEDIA;
            default -> Energia.ALTA;
        };
    }

    // ---------- Contextos e referencias ----------

    @GET
    @Path("contextos")
    public List<ContextoDto> listarContextos(@QueryParam("todos") boolean todos) {
        return catalogo.listarContextos(!todos).stream().map(ContextoDto::de).toList();
    }

    @POST
    @Path("contextos")
    public Response criarContexto(@Valid GtdCatalogoService.DadosContexto dados) {
        ContextoDto criado = ContextoDto.de(catalogo.criarContexto(dados));
        return Response.created(URI.create("/api/gtd/contextos/" + criado.id())).entity(criado).build();
    }

    @PUT
    @Path("contextos/{id}")
    public ContextoDto atualizarContexto(
            @PathParam("id") long id, @Valid GtdCatalogoService.DadosContexto dados) {
        return ContextoDto.de(catalogo.atualizarContexto(id, dados));
    }

    @DELETE
    @Path("contextos/{id}")
    public Response excluirContexto(@PathParam("id") long id) {
        catalogo.excluirContexto(id);
        return Response.noContent().build();
    }

    @GET
    @Path("referencias")
    public List<ReferenciaDto> listarReferencias(@QueryParam("tag") String tag) {
        return catalogo.listarReferencias(tag).stream().map(ReferenciaDto::de).toList();
    }

    @POST
    @Path("referencias")
    public Response criarReferencia(@Valid InboxService.DadosReferencia dados) {
        ReferenciaDto criada = ReferenciaDto.de(catalogo.criarReferencia(dados));
        return Response.created(URI.create("/api/gtd/referencias/" + criada.id())).entity(criada).build();
    }

    @PUT
    @Path("referencias/{id}")
    public ReferenciaDto atualizarReferencia(
            @PathParam("id") long id, @Valid InboxService.DadosReferencia dados) {
        return ReferenciaDto.de(catalogo.atualizarReferencia(id, dados));
    }

    @DELETE
    @Path("referencias/{id}")
    public Response excluirReferencia(@PathParam("id") long id) {
        catalogo.excluirReferencia(id);
        return Response.noContent().build();
    }
}
