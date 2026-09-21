package br.dev.registro.gamificacao.domain;

import br.dev.registro.gamificacao.domain.FaixaEsforco.Situacao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class FaixaEsforcoTest {

    /** 10, 20, 30, ..., 100: dez dias comparaveis, quartis faceis de conferir a mao. */
    private static final List<Integer> DEZ_DIAS =
            IntStream.rangeClosed(1, 10).map(i -> i * 10).boxed().toList();

    @Test
    void faixa_sai_dos_quartis_do_historico() {
        FaixaEsforco faixa = FaixaEsforco.de(BandaEnergia.NORMAL, DEZ_DIAS);

        assertThat(faixa.piso()).isEqualTo(30);
        assertThat(faixa.tipico()).isEqualTo(60);
        assertThat(faixa.teto()).isEqualTo(80);
        assertThat(faixa.diasConsiderados()).isEqualTo(10);
        assertThat(faixa.baseCurta()).isFalse();
    }

    @Test
    void ordem_de_chegada_nao_muda_a_faixa() {
        assertThat(FaixaEsforco.de(BandaEnergia.NORMAL, List.of(100, 10, 50, 20, 90, 30, 80, 40, 70, 60)))
                .isEqualTo(FaixaEsforco.de(BandaEnergia.NORMAL, DEZ_DIAS));
    }

    @Test
    void classifica_o_dia_contra_a_faixa() {
        FaixaEsforco faixa = FaixaEsforco.de(BandaEnergia.NORMAL, DEZ_DIAS);

        assertThat(faixa.situacao(20)).isEqualTo(Situacao.ABAIXO);
        assertThat(faixa.situacao(30)).isEqualTo(Situacao.DENTRO);
        assertThat(faixa.situacao(80)).isEqualTo(Situacao.DENTRO);
        assertThat(faixa.situacao(81)).isEqualTo(Situacao.ACIMA);
    }

    @Test
    void historico_curto_nao_cobra_nada() {
        FaixaEsforco faixa = FaixaEsforco.de(BandaEnergia.BAIXA, List.of(10, 20, 30));

        assertThat(faixa.baseCurta()).isTrue();
        assertThat(faixa.situacao(0)).isEqualTo(Situacao.CALIBRANDO);
    }

    @Test
    void sem_historico_a_faixa_e_zerada() {
        FaixaEsforco faixa = FaixaEsforco.de(BandaEnergia.ALTA, List.of());

        assertThat(faixa.piso()).isZero();
        assertThat(faixa.teto()).isZero();
        assertThat(faixa.diasConsiderados()).isZero();
        assertThat(faixa.situacao(500)).isEqualTo(Situacao.CALIBRANDO);
    }

    @Test
    void falta_some_quando_o_dia_entra_na_faixa() {
        FaixaEsforco faixa = FaixaEsforco.de(BandaEnergia.NORMAL, DEZ_DIAS);

        assertThat(faixa.faltaParaOPiso(10)).isEqualTo(20);
        assertThat(faixa.faltaParaOPiso(30)).isZero();
        assertThat(faixa.faltaParaOPiso(90)).isZero();
    }

    @Test
    void banda_sai_da_energia_do_checkin() {
        assertThat(BandaEnergia.de(1)).isEqualTo(BandaEnergia.BAIXA);
        assertThat(BandaEnergia.de(2)).isEqualTo(BandaEnergia.BAIXA);
        assertThat(BandaEnergia.de(3)).isEqualTo(BandaEnergia.NORMAL);
        assertThat(BandaEnergia.de(5)).isEqualTo(BandaEnergia.ALTA);
        assertThat(BandaEnergia.de(null)).isNull();
    }
}
