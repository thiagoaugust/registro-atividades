package br.dev.registro.gamificacao.domain;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.DadosRegistro;
import br.dev.registro.atividades.domain.RegistroService;
import br.dev.registro.comum.Relogio;
import br.dev.registro.gamificacao.infra.DiaResumoRepository;
import br.dev.registro.gamificacao.infra.XpLancamentoRepository;
import br.dev.registro.gtd.domain.AcaoService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O ledger e reconstruivel a partir do que esta no banco. E o que permite semear historico sem
 * passar pelos servicos (perfil demo) e recuperar a pontuacao se algum lancamento se perder.
 */
@QuarkusTest
class ReconstrucaoLedgerTest {

    @Inject
    RecalculoDiaService recalculo;

    @Inject
    RegistroService registros;

    @Inject
    AcaoService acoes;

    @Inject
    XpLancamentoRepository lancamentos;

    @Inject
    DiaResumoRepository resumos;

    @Inject
    Relogio relogio;

    @Test
    void reconstruir_devolve_o_mesmo_xp_que_o_caminho_normal() {
        LocalDate dia = LocalDate.of(2025, 11, 4);

        registros.criar(new DadosRegistro(
                dia, null, 60, Categoria.TREINO, "corrida", (short) 7, null, null, null, null, null, null, Map.of()));
        registros.criar(new DadosRegistro(
                dia, null, 45, Categoria.ESTUDO, "estudo", (short) 5, null, null, null, null, null, null, Map.of()));

        int xpOriginal = resumos.findById(dia).xpTotal;
        assertThat(xpOriginal).isGreaterThan(0);
        long lancamentosOriginais = lancamentos.count("dataLocal", dia);

        // reconstruirLancamentos limpa o periodo e refaz tudo a partir do que esta no banco
        recalculo.reconstruirLancamentos(dia, dia);
        recalculo.recalcular(dia);

        assertThat(resumos.findById(dia).xpTotal).isEqualTo(xpOriginal);
        // e nao duplica: continua havendo um lancamento por registro
        assertThat(lancamentos.count("dataLocal", dia)).isEqualTo(lancamentosOriginais);
    }

    @Test
    void reconstrucao_inclui_o_xp_das_acoes_gtd_concluidas() {
        LocalDate hoje = relogio.hoje();
        var acao = acoes.criar(new AcaoService.DadosAcao(
                "acao para reconstruir", null, null, null, null, null, null, null, null, null));
        acoes.concluir(acao.id);

        int comAcao = resumos.findById(hoje).xpTotal;

        recalculo.reconstruirLancamentos(hoje, hoje);
        recalculo.recalcular(hoje);

        assertThat(resumos.findById(hoje).xpTotal).isEqualTo(comAcao);
    }
}
