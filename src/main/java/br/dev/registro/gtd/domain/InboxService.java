package br.dev.registro.gtd.domain;

import br.dev.registro.atividades.domain.CatalogoService;
import br.dev.registro.atividades.domain.Projeto;
import br.dev.registro.comum.RecursoNaoEncontradoException;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.comum.Relogio;
import br.dev.registro.gtd.infra.InboxRepository;
import br.dev.registro.gtd.infra.ReferenciaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Capturar e esclarecer. A captura e deliberadamente burra — texto e Enter; toda a decisao acontece
 * depois, no processamento guiado, um item por vez.
 */
@ApplicationScoped
public class InboxService {

    private final InboxRepository inbox;
    private final ReferenciaRepository referencias;
    private final AcaoService acoes;
    private final CatalogoService catalogo;
    private final Relogio relogio;

    public InboxService(
            InboxRepository inbox,
            ReferenciaRepository referencias,
            AcaoService acoes,
            CatalogoService catalogo,
            Relogio relogio) {
        this.inbox = inbox;
        this.referencias = referencias;
        this.acoes = acoes;
        this.catalogo = catalogo;
        this.relogio = relogio;
    }

    public record DadosCaptura(@NotBlank @Size(max = 2000) String texto) {
    }

    public record DadosReferencia(
            @NotBlank @Size(max = 300) String titulo,
            String conteudo,
            @Size(max = 2000) String url,
            List<String> tags) {
    }

    /**
     * A decisao do fluxograma de esclarecimento. O que cada destino exige:
     * ACAO/ALGUM_DIA/FEITO_2MIN precisam de {@code acao}; PROJETO precisa de {@code projeto} (e
     * aceita a primeira proxima acao junto); REFERENCIA precisa de {@code referencia}; LIXO, nada.
     */
    public record Decisao(
            @NotNull DestinoInbox destino,
            @Valid AcaoService.DadosAcao acao,
            @Valid CatalogoService.DadosProjeto projeto,
            @Valid DadosReferencia referencia) {
    }

    public List<InboxItem> pendentes() {
        return inbox.pendentes();
    }

    public long contarPendentes() {
        return inbox.contarPendentes();
    }

    public InboxItem proximoPendente() {
        return inbox.proximoPendente();
    }

    @Transactional
    public InboxItem capturar(DadosCaptura dados) {
        InboxItem item = new InboxItem();
        item.texto = dados.texto().trim();
        inbox.persist(item);
        return item;
    }

    @Transactional
    public void descartar(long id) {
        inbox.delete(porId(id));
    }

    @Transactional
    public InboxItem processar(long id, Decisao decisao) {
        InboxItem item = porId(id);
        if (item.processadoEm != null) {
            throw new RegraNegocioException("esse item ja foi processado");
        }

        item.destinoId = switch (decisao.destino()) {
            case LIXO -> null;
            case REFERENCIA -> criarReferencia(decisao, item).id;
            case ACAO -> acoes.criar(exigirAcao(decisao, null)).id;
            case ALGUM_DIA -> acoes.criar(exigirAcao(decisao, EstadoAcao.ALGUM_DIA)).id;
            case FEITO_2MIN -> acoes.criar(exigirAcao(decisao, EstadoAcao.CONCLUIDA)).id;
            case PROJETO -> criarProjetoComPrimeiraAcao(decisao).id;
        };
        item.destino = decisao.destino();
        item.processadoEm = relogio.agora();
        return item;
    }

    private InboxItem porId(long id) {
        InboxItem item = inbox.findById(id);
        if (item == null) {
            throw new RecursoNaoEncontradoException("Item do inbox", id);
        }
        return item;
    }

    /** Se o destino nao trouxe titulo de acao, o texto capturado e o titulo — um campo a menos. */
    private AcaoService.DadosAcao exigirAcao(Decisao decisao, EstadoAcao estadoForcado) {
        AcaoService.DadosAcao dados = decisao.acao();
        if (dados == null) {
            throw new RegraNegocioException("destino %s precisa dos dados da acao".formatted(decisao.destino()));
        }
        EstadoAcao estado = estadoForcado != null ? estadoForcado : dados.estado();
        return new AcaoService.DadosAcao(
                dados.titulo(), dados.notas(), estado, dados.contextoId(), dados.tempoEstimadoMin(),
                dados.energia(), dados.categoria(), dados.projetoId(), dados.agendadaPara(),
                dados.delegadaPara());
    }

    /**
     * Item que exige mais de uma acao vira projeto. A primeira proxima acao pode vir junto — e o que
     * evita nascer um projeto ja parado.
     */
    private Projeto criarProjetoComPrimeiraAcao(Decisao decisao) {
        if (decisao.projeto() == null) {
            throw new RegraNegocioException("destino PROJETO precisa dos dados do projeto");
        }
        Projeto projeto = catalogo.criarProjeto(decisao.projeto());

        if (decisao.acao() != null) {
            AcaoService.DadosAcao dados = decisao.acao();
            acoes.criar(new AcaoService.DadosAcao(
                    dados.titulo(), dados.notas(), dados.estado(), dados.contextoId(),
                    dados.tempoEstimadoMin(), dados.energia(), dados.categoria(), projeto.id,
                    dados.agendadaPara(), dados.delegadaPara()));
        }
        return projeto;
    }

    private Referencia criarReferencia(Decisao decisao, InboxItem item) {
        DadosReferencia dados = decisao.referencia();
        if (dados == null) {
            throw new RegraNegocioException("destino REFERENCIA precisa dos dados da referencia");
        }
        Referencia referencia = new Referencia();
        referencia.titulo = dados.titulo();
        referencia.conteudo = dados.conteudo() != null ? dados.conteudo() : item.texto;
        referencia.url = dados.url();
        referencia.tags = dados.tags() == null ? new String[0] : dados.tags().toArray(String[]::new);
        referencias.persist(referencia);
        return referencia;
    }
}
