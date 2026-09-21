package br.dev.registro.gtd.domain;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.DadosRegistro;
import br.dev.registro.atividades.domain.RegistroAtividade;
import br.dev.registro.atividades.domain.RegistroService;
import br.dev.registro.atividades.infra.ProjetoRepository;
import br.dev.registro.comum.AcaoAlterada;
import br.dev.registro.comum.RecursoNaoEncontradoException;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.comum.Relogio;
import br.dev.registro.gtd.infra.AcaoRepository;
import br.dev.registro.gtd.infra.ContextoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AcaoService {

    private final AcaoRepository acoes;
    private final ContextoRepository contextos;
    private final ProjetoRepository projetos;
    private final RegistroService registros;
    private final Relogio relogio;
    private final Event<AcaoAlterada> acaoAlterada;

    public AcaoService(
            AcaoRepository acoes,
            ContextoRepository contextos,
            ProjetoRepository projetos,
            RegistroService registros,
            Relogio relogio,
            Event<AcaoAlterada> acaoAlterada) {
        this.acoes = acoes;
        this.contextos = contextos;
        this.projetos = projetos;
        this.registros = registros;
        this.relogio = relogio;
        this.acaoAlterada = acaoAlterada;
    }

    public record DadosAcao(
            @NotBlank @Size(max = 300) String titulo,
            @Size(max = 4000) String notas,
            EstadoAcao estado,
            Long contextoId,
            @Min(1) Integer tempoEstimadoMin,
            Energia energia,
            Categoria categoria,
            Long projetoId,
            Instant agendadaPara,
            @Size(max = 120) String delegadaPara) {
    }

    /** Dados que faltam para a acao concluida virar registro de atividade. */
    public record DadosRegistroDaAcao(@Min(1) int duracaoMin, @Min(1) int esforco, Short satisfacao) {
    }

    public Acao porId(long id) {
        return acoes.porIdComVinculos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acao", id));
    }

    public List<Acao> listar(EstadoAcao estado, Long contextoId, Long projetoId) {
        return acoes.listar(estado, contextoId, projetoId);
    }

    public List<Acao> engajar(Long contextoId, Integer tempoDisponivelMin, Energia energia) {
        return acoes.engajar(contextoId, tempoDisponivelMin, energia);
    }

    /** Projeto ativo sem nenhuma acao aberta: parece vivo na lista, mas nada o move. */
    public List<br.dev.registro.atividades.domain.Projeto> projetosParados() {
        return acoes.projetosAtivosSemProximaAcao();
    }

    @Transactional
    public Acao criar(DadosAcao dados) {
        Acao acao = new Acao();
        aplicar(dados, acao);
        acoes.persist(acao);
        if (acao.estado == EstadoAcao.CONCLUIDA) {
            acao.concluidaEm = relogio.agora();
            acaoAlterada.fire(new AcaoAlterada(acao.id, true));
        }
        return acao;
    }

    @Transactional
    public Acao atualizar(long id, DadosAcao dados) {
        Acao acao = porId(id);
        boolean estavaConcluida = acao.estado == EstadoAcao.CONCLUIDA;
        aplicar(dados, acao);

        boolean agoraConcluida = acao.estado == EstadoAcao.CONCLUIDA;
        if (agoraConcluida && !estavaConcluida) {
            acao.concluidaEm = relogio.agora();
        } else if (!agoraConcluida && estavaConcluida) {
            acao.concluidaEm = null;
        }
        if (agoraConcluida != estavaConcluida) {
            acaoAlterada.fire(new AcaoAlterada(id, agoraConcluida));
        }
        return acao;
    }

    @Transactional
    public Acao concluir(long id) {
        Acao acao = porId(id);
        if (acao.estado == EstadoAcao.CONCLUIDA) {
            return acao;
        }
        acao.estado = EstadoAcao.CONCLUIDA;
        acao.concluidaEm = relogio.agora();
        acaoAlterada.fire(new AcaoAlterada(id, true));
        return acao;
    }

    @Transactional
    public Acao reabrir(long id) {
        Acao acao = porId(id);
        if (acao.aberta()) {
            return acao;
        }
        acao.estado = EstadoAcao.PROXIMA;
        acao.concluidaEm = null;
        acaoAlterada.fire(new AcaoAlterada(id, false));
        return acao;
    }

    @Transactional
    public void excluir(long id) {
        Acao acao = porId(id);
        boolean contavaXp = acao.estado == EstadoAcao.CONCLUIDA;
        acoes.delete(acao);
        if (contavaXp) {
            acaoAlterada.fire(new AcaoAlterada(id, false));
        }
    }

    /**
     * Fecha o ciclo GTD -> registro: a acao ja sabe categoria e titulo, entao so faltam duracao e
     * esforco. O registro criado fica vinculado a acao, e nao ha dois cadastros do mesmo esforco.
     */
    @Transactional
    public RegistroAtividade registrarComoAtividade(long id, DadosRegistroDaAcao dados) {
        Acao acao = porId(id);
        if (acao.categoria == null) {
            throw new RegraNegocioException("a acao nao tem categoria para virar registro");
        }
        if (acao.registro != null) {
            throw new RegraNegocioException("essa acao ja foi registrada como atividade");
        }
        if (acao.estado != EstadoAcao.CONCLUIDA) {
            concluir(id);
        }

        RegistroAtividade registro = registros.criar(new DadosRegistro(
                relogio.hoje(),
                null,
                dados.duracaoMin(),
                acao.categoria,
                acao.titulo,
                (short) dados.esforco(),
                dados.satisfacao(),
                null,
                acao.projeto == null ? null : acao.projeto.id,
                null,
                null,
                null,
                Map.of()));

        acao.registro = registro;
        return registro;
    }

    private void aplicar(DadosAcao dados, Acao acao) {
        EstadoAcao estado = dados.estado() != null ? dados.estado() : EstadoAcao.PROXIMA;

        // O banco tem o mesmo check, mas a mensagem daqui diz o que fazer em vez de citar a constraint.
        if (estado == EstadoAcao.AGENDA && dados.agendadaPara() == null) {
            throw new RegraNegocioException("acao na agenda precisa de data e hora");
        }
        if (estado == EstadoAcao.AGUARDANDO && (dados.delegadaPara() == null || dados.delegadaPara().isBlank())) {
            throw new RegraNegocioException("acao aguardando precisa dizer com quem ela esta");
        }

        acao.titulo = dados.titulo();
        acao.notas = dados.notas();
        acao.estado = estado;
        acao.tempoEstimadoMin = dados.tempoEstimadoMin();
        acao.energia = dados.energia();
        acao.categoria = dados.categoria();
        acao.agendadaPara = dados.agendadaPara();
        acao.delegadaPara = dados.delegadaPara();
        if (estado == EstadoAcao.AGUARDANDO && acao.delegadaEm == null) {
            acao.delegadaEm = relogio.hoje();
        }

        acao.contexto = dados.contextoId() == null
                ? null
                : contextos.findByIdOptional(dados.contextoId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Contexto", dados.contextoId()));
        acao.projeto = dados.projetoId() == null
                ? null
                : projetos.findByIdOptional(dados.projetoId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", dados.projetoId()));
    }
}
