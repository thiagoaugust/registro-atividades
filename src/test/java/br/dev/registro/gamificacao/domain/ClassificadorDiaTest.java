package br.dev.registro.gamificacao.domain;

import br.dev.registro.gamificacao.domain.ClassificadorDia.Baseline;
import br.dev.registro.gamificacao.domain.ClassificadorDia.ContextoDia;
import br.dev.registro.gamificacao.domain.ClassificadorDia.ResultadoDia;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificadorDiaTest {

    private static final ParametrosIndice P = ParametrosIndice.padrao();
    private static final Baseline BASELINE = new Baseline(100, 20, 28);

    /** Check-in de um dia comum: tudo no meio da escala. */
    private static ContextoDia neutro() {
        return new ContextoDia(3, 3, 3, 3, 3);
    }

    /** Dormiu mal, sem energia, humor no chao, dia previsto como dificil. */
    private static ContextoDia ruim() {
        return new ContextoDia(5, 1, 1, 2, 5);
    }

    private static ResultadoDia classificar(int xp, ContextoDia contexto) {
        return ClassificadorDia.classificar(xp, BASELINE, contexto, false, P);
    }

    @Test
    void dia_na_media_com_contexto_neutro_e_normal() {
        ResultadoDia r = classificar(100, neutro());

        assertThat(r.objetivo()).isEqualTo(50);
        assertThat(r.adversidade()).isEqualTo(0.5);
        assertThat(r.indice()).isEqualTo(60); // 50 x (1 + 0,4 x 0,5)
        assertThat(r.classificacao()).isEqualTo(Classificacao.BOM);
    }

    @Test
    void dia_muito_acima_da_media_e_excelente() {
        ResultadoDia r = classificar(160, neutro());

        assertThat(r.objetivo()).isEqualTo(95); // z = +3 -> 50 + 45
        assertThat(r.classificacao()).isEqualTo(Classificacao.EXCELENTE);
    }

    @Test
    void dia_fraco_sem_nada_atenuando_e_dificil() {
        ResultadoDia r = classificar(40, new ContextoDia(1, 5, 5, 5, 1));

        assertThat(r.adversidade()).isZero();
        assertThat(r.objetivo()).isEqualTo(5); // z = -3
        assertThat(r.indice()).isEqualTo(5);
        assertThat(r.classificacao()).isEqualTo(Classificacao.DIFICIL);
        assertThat(r.diaDificilVencido()).isFalse();
    }

    /** O caso central do sistema: mesmo XP, contexto adverso, classificacao melhor. */
    @Test
    void mesmo_xp_em_dia_adverso_sobe_de_classificacao_e_conta_como_vitoria() {
        ResultadoDia facil = classificar(100, new ContextoDia(1, 5, 5, 5, 1));
        ResultadoDia dificil = classificar(100, ruim());

        assertThat(facil.indice()).isEqualTo(50);
        assertThat(facil.classificacao()).isEqualTo(Classificacao.NORMAL);
        assertThat(facil.diaDificilVencido()).isFalse();

        assertThat(dificil.adversidade()).isEqualTo(0.9625);
        assertThat(dificil.indice()).isEqualTo(69.25); // 50 x (1 + 0,4 x 0,9625)
        assertThat(dificil.classificacao()).isEqualTo(Classificacao.BOM);
        assertThat(dificil.diaDificilVencido()).isTrue();
    }

    @Test
    void dia_ruim_de_verdade_nao_vira_vitoria_so_por_ser_dificil() {
        // Contexto pessimo, mas o desempenho tambem ficou abaixo do minimo: nao ha vitoria.
        ResultadoDia r = classificar(50, ruim());

        assertThat(r.adversidade()).isEqualTo(0.9625);
        assertThat(r.objetivo()).isEqualTo(12.5);
        assertThat(r.diaDificilVencido()).isFalse();
    }

    @Test
    void primeiros_dias_nao_inventam_z_score() {
        ResultadoDia r = ClassificadorDia.classificar(500, new Baseline(80, 10, 3), neutro(), false, P);

        assertThat(r.baselineInsuficiente()).isTrue();
        assertThat(r.objetivo()).isEqualTo(50);
        assertThat(r.indice()).isEqualTo(60);
    }

    @Test
    void dia_sem_check_in_fica_so_com_o_objetivo() {
        ResultadoDia r = classificar(100, ContextoDia.AUSENTE);

        assertThat(r.adversidade()).isZero();
        assertThat(r.indice()).isEqualTo(r.objetivo()).isEqualTo(50);
        assertThat(r.classificacao()).isEqualTo(Classificacao.NORMAL);
        assertThat(r.diaDificilVencido()).isFalse();
    }

    @Test
    void check_in_parcial_renormaliza_os_pesos() {
        // So a energia foi preenchida, e no pior valor: adversidade maxima, nao 0,25.
        ResultadoDia r = classificar(100, new ContextoDia(null, 1, null, null, null));

        assertThat(r.adversidade()).isEqualTo(1.0);
        assertThat(r.indice()).isEqualTo(70);
    }

    @Test
    void descanso_planejado_e_dia_neutro_e_nunca_vitoria() {
        ResultadoDia r = ClassificadorDia.classificar(0, BASELINE, ruim(), true, P);

        assertThat(r.indice()).isEqualTo(50);
        assertThat(r.classificacao()).isEqualTo(Classificacao.NORMAL);
        assertThat(r.diaDificilVencido()).isFalse();
    }

    @Test
    void historico_sem_variacao_deixa_o_dia_no_meio_da_escala() {
        ResultadoDia r = ClassificadorDia.classificar(
                500, new Baseline(100, 0, 28), ContextoDia.AUSENTE, false, P);

        assertThat(r.objetivo()).isEqualTo(50);
        assertThat(r.classificacao()).isEqualTo(Classificacao.NORMAL);
    }

    @Test
    void indice_nunca_sai_da_escala() {
        ResultadoDia altissimo = ClassificadorDia.classificar(10_000, BASELINE, ruim(), false, P);
        ResultadoDia baixissimo = ClassificadorDia.classificar(0, BASELINE, ContextoDia.AUSENTE, false, P);

        assertThat(altissimo.indice()).isEqualTo(100);
        assertThat(baixissimo.indice()).isZero();
    }
}
