package br.dev.registro.gamificacao.domain;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.gamificacao.domain.CalculadoraXp.Lancamento;
import br.dev.registro.gamificacao.domain.CalculadoraXp.ResumoXp;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraXpTest {

    private static final ParametrosXp P = ParametrosXp.padrao();

    private static Lancamento registro(Categoria categoria, int pontos) {
        return new Lancamento(OrigemXp.REGISTRO, categoria, pontos);
    }

    @Test
    void esforco_medio_nao_altera_a_duracao() {
        // fator 1,0 e multiplicador 1,0: 60 min de treino a esforco 5 valem 60 XP.
        assertThat(CalculadoraXp.pontosBrutos(60, 5, Categoria.TREINO, P)).isEqualTo(60);
    }

    @Test
    void esforco_maximo_vale_quase_o_dobro_do_minimo() {
        int minimo = CalculadoraXp.pontosBrutos(60, 1, Categoria.TREINO, P);
        int maximo = CalculadoraXp.pontosBrutos(60, 10, Categoria.TREINO, P);
        assertThat(minimo).isEqualTo(41);
        assertThat(maximo).isEqualTo(84);
    }

    @Test
    void multiplicador_da_categoria_entra_na_conta() {
        assertThat(CalculadoraXp.pontosBrutos(60, 5, Categoria.LEITURA, P)).isEqualTo(48);
        assertThat(CalculadoraXp.pontosBrutos(60, 5, Categoria.DESAFIO, P)).isEqualTo(72);
    }

    @Test
    void quatro_horas_da_mesma_coisa_batem_no_teto() {
        ResumoXp resumo = CalculadoraXp.consolidar(List.of(registro(Categoria.ESTUDO, 400)), P);

        assertThat(resumo.porCategoria()).containsEntry(Categoria.ESTUDO, 120);
        assertThat(resumo.xpTotal()).isEqualTo(120);
        assertThat(resumo.bonusEquilibrio()).isZero();
    }

    @Test
    void dia_equilibrado_vale_mais_que_o_dia_de_uma_coisa_so() {
        ResumoXp concentrado = CalculadoraXp.consolidar(List.of(registro(Categoria.ESTUDO, 400)), P);
        ResumoXp equilibrado = CalculadoraXp.consolidar(
                List.of(
                        registro(Categoria.ESTUDO, 60),
                        registro(Categoria.TREINO, 60),
                        registro(Categoria.LEITURA, 60)),
                P);

        assertThat(equilibrado.categoriasDistintas()).isEqualTo(3);
        assertThat(equilibrado.bonusEquilibrio()).isEqualTo(27); // 15% de 180
        assertThat(equilibrado.xpTotal()).isEqualTo(207).isGreaterThan(concentrado.xpTotal());
    }

    @Test
    void duas_categorias_ainda_nao_dao_bonus() {
        ResumoXp resumo = CalculadoraXp.consolidar(
                List.of(registro(Categoria.ESTUDO, 60), registro(Categoria.TREINO, 60)), P);
        assertThat(resumo.bonusEquilibrio()).isZero();
        assertThat(resumo.xpTotal()).isEqualTo(120);
    }

    @Test
    void teto_do_gtd_nao_deixa_tarefinha_virar_fazenda_de_xp() {
        List<Lancamento> muitas = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> new Lancamento(OrigemXp.ACAO_GTD, null, 5))
                .toList();

        ResumoXp resumo = CalculadoraXp.consolidar(muitas, P);
        assertThat(resumo.xpTotal()).isEqualTo(30);
    }

    @Test
    void revisao_semanal_soma_por_fora_do_teto_do_gtd() {
        ResumoXp resumo = CalculadoraXp.consolidar(
                List.of(
                        new Lancamento(OrigemXp.ACAO_GTD, null, 100),
                        new Lancamento(OrigemXp.REVISAO_SEMANAL, null, 50)),
                P);
        assertThat(resumo.xpTotal()).isEqualTo(80);
    }

    @Test
    void dia_vazio_nao_rende_nada() {
        ResumoXp resumo = CalculadoraXp.consolidar(List.of(), P);
        assertThat(resumo.xpTotal()).isZero();
        assertThat(resumo.categoriasDistintas()).isZero();
        assertThat(resumo.porCategoria()).isEmpty();
    }

    @Test
    void curva_de_nivel_e_progressiva() {
        assertThat(CalculadoraXp.nivel(0)).isEqualTo(1);
        assertThat(CalculadoraXp.nivel(99)).isEqualTo(1);
        assertThat(CalculadoraXp.nivel(100)).isEqualTo(2);
        assertThat(CalculadoraXp.nivel(800)).isEqualTo(5);
        assertThat(CalculadoraXp.nivel(2700)).isEqualTo(10);
    }

    @Test
    void xp_para_nivel_e_o_inverso_do_nivel() {
        for (int nivel = 1; nivel <= 30; nivel++) {
            long limiar = CalculadoraXp.xpParaNivel(nivel);
            assertThat(CalculadoraXp.nivel(limiar)).as("nivel %d", nivel).isEqualTo(nivel);
            if (nivel > 1) {
                assertThat(CalculadoraXp.nivel(limiar - 1)).isEqualTo(nivel - 1);
            }
        }
    }
}
