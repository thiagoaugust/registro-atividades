package br.dev.registro.gamificacao.domain;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.RegistroAtividade;
import br.dev.registro.atividades.infra.RegistroRepository;
import br.dev.registro.checkin.domain.CheckinDiario;
import br.dev.registro.checkin.infra.CheckinRepository;
import br.dev.registro.comum.AcaoAlterada;
import br.dev.registro.comum.DiaAlterado;
import br.dev.registro.comum.RegistroAlterado;
import br.dev.registro.comum.RevisaoConcluida;
import br.dev.registro.comum.Relogio;
import br.dev.registro.gtd.domain.Acao;
import br.dev.registro.gtd.domain.EstadoAcao;
import br.dev.registro.gtd.domain.RevisaoSemanal;
import br.dev.registro.gtd.infra.RevisaoSemanalRepository;
import br.dev.registro.gtd.infra.AcaoRepository;
import br.dev.registro.gamificacao.domain.CalculadoraXp.Lancamento;
import br.dev.registro.gamificacao.domain.CalculadoraXp.ResumoXp;
import br.dev.registro.gamificacao.domain.ClassificadorDia.Baseline;
import br.dev.registro.gamificacao.domain.ClassificadorDia.ContextoDia;
import br.dev.registro.gamificacao.domain.ClassificadorDia.ResultadoDia;
import br.dev.registro.gamificacao.infra.DiaResumoRepository;
import br.dev.registro.gamificacao.infra.GamificacaoConfig;
import br.dev.registro.gamificacao.infra.MetricasRepository;
import br.dev.registro.gamificacao.infra.XpLancamentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mantem o resumo materializado do dia em dia. Observa as alteracoes dos outros modulos, reescreve o
 * lancamento de XP da origem e recalcula os dias afetados.
 */
@ApplicationScoped
public class RecalculoDiaService {

    private final EntityManager em;
    private final RegistroRepository registros;
    private final CheckinRepository checkins;
    private final XpLancamentoRepository lancamentos;
    private final DiaResumoRepository resumos;
    private final MetricasRepository metricas;
    private final AvaliadorConquistas conquistas;
    private final GamificacaoConfig config;
    private final AcaoRepository acoes;
    private final RevisaoSemanalRepository revisoes;
    private final Relogio relogio;

    public RecalculoDiaService(
            EntityManager em,
            RegistroRepository registros,
            CheckinRepository checkins,
            XpLancamentoRepository lancamentos,
            DiaResumoRepository resumos,
            MetricasRepository metricas,
            AvaliadorConquistas conquistas,
            GamificacaoConfig config,
            AcaoRepository acoes,
            RevisaoSemanalRepository revisoes,
            Relogio relogio) {
        this.em = em;
        this.registros = registros;
        this.checkins = checkins;
        this.lancamentos = lancamentos;
        this.resumos = resumos;
        this.metricas = metricas;
        this.conquistas = conquistas;
        this.config = config;
        this.acoes = acoes;
        this.revisoes = revisoes;
        this.relogio = relogio;
    }

    /**
     * Reescreve o lancamento do registro e recalcula os dias envolvidos. Se a edicao mudou a data, os
     * dois dias precisam ser recalculados — o antigo perdeu XP, o novo ganhou.
     */
    @Transactional
    public void aoAlterarRegistro(@Observes RegistroAlterado evento) {
        LocalDate diaAnterior = lancamentos.dataDaOrigem(OrigemXp.REGISTRO, evento.registroId());
        lancamentos.removerDaOrigem(OrigemXp.REGISTRO, evento.registroId());

        LocalDate diaNovo = null;
        if (!evento.excluido()) {
            RegistroAtividade registro = registros.findById(evento.registroId());
            if (registro != null) {
                int pontos = CalculadoraXp.pontosBrutos(
                        registro.duracaoMin, registro.esforco, registro.categoria,
                        registro.tempoAproveitado(), config.parametrosXp());
                lancamentos.persist(XpLancamento.de(
                        registro.dataLocal, OrigemXp.REGISTRO, registro.id, registro.categoria, pontos));
                diaNovo = registro.dataLocal;
            }
        }

        if (diaAnterior != null) {
            recalcular(diaAnterior);
        }
        if (diaNovo != null && !diaNovo.equals(diaAnterior)) {
            recalcular(diaNovo);
        }
    }

    /** Acao GTD concluida rende XP no dia em que foi concluida; reabri-la desfaz o lancamento. */
    @Transactional
    public void aoAlterarAcao(@Observes AcaoAlterada evento) {
        LocalDate diaAnterior = lancamentos.dataDaOrigem(OrigemXp.ACAO_GTD, evento.acaoId());
        lancamentos.removerDaOrigem(OrigemXp.ACAO_GTD, evento.acaoId());

        LocalDate diaNovo = null;
        if (evento.concluida()) {
            Acao acao = acoes.findById(evento.acaoId());
            if (acao != null && acao.concluidaEm != null) {
                diaNovo = relogio.diaLocal(acao.concluidaEm);
                lancamentos.persist(XpLancamento.de(
                        diaNovo, OrigemXp.ACAO_GTD, acao.id, null, config.xp().pontosPorAcaoGtd()));
            }
        }

        if (diaAnterior != null) {
            recalcular(diaAnterior);
        }
        if (diaNovo != null && !diaNovo.equals(diaAnterior)) {
            recalcular(diaNovo);
        }
    }

    /**
     * A revisao semanal rende XP no dia em que foi concluida. O origem_id e a semana em dias desde a
     * epoca: cabe em long e mantem a unicidade de (origem, origem_id) por semana.
     */
    @Transactional
    public void aoConcluirRevisao(@Observes RevisaoConcluida evento) {
        long origemId = evento.semanaInicio().toEpochDay();
        lancamentos.removerDaOrigem(OrigemXp.REVISAO_SEMANAL, origemId);
        lancamentos.persist(XpLancamento.de(
                evento.dia(), OrigemXp.REVISAO_SEMANAL, origemId, null,
                config.xp().pontosRevisaoSemanal()));
        recalcular(evento.dia());
    }

    @Transactional
    public void aoAlterarDia(@Observes DiaAlterado evento) {
        recalcular(evento.dia());
    }

    /**
     * Recalcula um dia inteiro a partir do ledger. O flush e explicito porque as somas sao queries
     * nativas, e o Hibernate nao sincroniza a sessao sozinho antes delas.
     */
    @Transactional
    public DiaResumo recalcular(LocalDate dia) {
        em.flush();

        List<Lancamento> doDia = lancamentos.doDia(dia);
        ResumoXp xp = CalculadoraXp.consolidar(doDia, config.parametrosXp());
        Map<Categoria, Integer> minutos = minutosPorCategoria(dia);
        CheckinDiario checkin = checkins.findById(dia);

        boolean descanso = checkin != null && checkin.descansoPlanejado;
        boolean presenca = !doDia.isEmpty() || !minutos.isEmpty() || checkin != null;
        ContextoDia contexto = checkin != null ? checkin.contexto() : ContextoDia.AUSENTE;
        Baseline baseline = resumos.baseline(dia, config.indice().diasJanela());

        ResultadoDia resultado = ClassificadorDia.classificar(
                xp.xpTotal(), baseline, contexto, descanso, config.parametrosIndice());

        DiaResumo resumo = resumos.findById(dia);
        if (resumo == null) {
            resumo = new DiaResumo();
            resumo.dataLocal = dia;
            resumos.persist(resumo);
        }
        resumo.xpTotal = xp.xpTotal();
        resumo.minutosTotal = minutos.values().stream().mapToInt(Integer::intValue).sum();
        resumo.categoriasDistintas = (short) xp.categoriasDistintas();
        resumo.indiceProdutividade = BigDecimal.valueOf(resultado.indice());
        resumo.classificacao = resultado.classificacao();
        resumo.diaDificilVencido = resultado.diaDificilVencido();
        resumo.descanso = descanso;
        resumo.presenca = presenca;
        resumo.baselineInsuficiente = resultado.baselineInsuficiente();
        resumo.calculadoEm = Instant.now();

        em.flush();
        resumos.substituirCategorias(dia, xp.porCategoria(), minutos);
        conquistas.avaliar(dia);
        return resumo;
    }

    /**
     * Reconstroi o ledger de XP de um intervalo a partir do que existe no banco: cada registro,
     * cada acao concluida e cada revisao viram seu lancamento de novo.
     *
     * <p>Serve para duas coisas: semear dados sem passar pelos servicos (perfil demo) e recuperar o
     * ledger se algum lancamento se perder. A formula continua valendo num lugar so — este metodo
     * reusa a mesma {@link CalculadoraXp}.
     */
    @Transactional
    public void reconstruirLancamentos(LocalDate de, LocalDate ate) {
        em.flush();

        lancamentos.delete("dataLocal >= ?1 and dataLocal <= ?2", de, ate);

        for (RegistroAtividade registro : registros.buscar(de, ate, null)) {
            int pontos = CalculadoraXp.pontosBrutos(
                    registro.duracaoMin, registro.esforco, registro.categoria,
                    registro.tempoAproveitado(), config.parametrosXp());
            lancamentos.persist(XpLancamento.de(
                    registro.dataLocal, OrigemXp.REGISTRO, registro.id, registro.categoria, pontos));
        }

        for (Acao acao : acoes.list("estado = ?1 and concluidaEm is not null", EstadoAcao.CONCLUIDA)) {
            LocalDate dia = relogio.diaLocal(acao.concluidaEm);
            if (!dia.isBefore(de) && !dia.isAfter(ate)) {
                lancamentos.persist(XpLancamento.de(
                        dia, OrigemXp.ACAO_GTD, acao.id, null, config.xp().pontosPorAcaoGtd()));
            }
        }

        for (RevisaoSemanal revisao : revisoes.list("concluidaEm is not null")) {
            LocalDate dia = relogio.diaLocal(revisao.concluidaEm);
            if (!dia.isBefore(de) && !dia.isAfter(ate)) {
                lancamentos.persist(XpLancamento.de(
                        dia, OrigemXp.REVISAO_SEMANAL, revisao.semanaInicio.toEpochDay(), null,
                        config.xp().pontosRevisaoSemanal()));
            }
        }
        em.flush();
    }

    /**
     * Recalcula um intervalo, em ordem cronologica — cada dia depende da baseline dos anteriores. Usado
     * pelo job noturno para preencher os dias sem nenhum registro, que precisam existir como zero para
     * nao inflarem a media movel.
     */
    @Transactional
    public int recalcularIntervalo(LocalDate de, LocalDate ate) {
        int total = 0;
        for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1)) {
            recalcular(dia);
            total++;
        }
        return total;
    }

    private Map<Categoria, Integer> minutosPorCategoria(LocalDate dia) {
        Map<Categoria, Integer> minutos = new EnumMap<>(Categoria.class);
        for (Object[] linha : metricas.minutosPorCategoria(dia)) {
            minutos.put(Categoria.valueOf((String) linha[0]), ((Number) linha[1]).intValue());
        }
        return minutos;
    }
}
