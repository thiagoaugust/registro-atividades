package br.dev.registro.gamificacao.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalibradorDeAlvoTest {

    @Test
    void alvo_e_a_mediana_do_historico() {
        assertThat(CalibradorDeAlvo.calibrar(List.of(2.0, 4.0, 6.0), 1.0, 1)).isEqualTo(4);
    }

    @Test
    void um_pico_isolado_nao_vira_a_expectativa() {
        // A media daria 28; a mediana segura o alvo perto do que realmente se repete.
        assertThat(CalibradorDeAlvo.calibrar(List.of(3.0, 4.0, 5.0, 100.0), 1.0, 1)).isEqualTo(5);
    }

    @Test
    void periodos_zerados_e_nulos_sao_ignorados() {
        assertThat(CalibradorDeAlvo.calibrar(Arrays.asList(0.0, null, 4.0, 4.0), 1.0, 1)).isEqualTo(4);
    }

    @Test
    void fator_pede_acima_do_normal() {
        assertThat(CalibradorDeAlvo.calibrar(List.of(10.0, 10.0, 10.0), 1.5, 1)).isEqualTo(15);
    }

    @Test
    void piso_segura_historico_fraco() {
        assertThat(CalibradorDeAlvo.calibrar(List.of(1.0, 1.0), 1.0, 5)).isEqualTo(5);
    }

    @Test
    void sem_historico_vale_o_piso() {
        assertThat(CalibradorDeAlvo.calibrar(List.of(), 1.0, 30)).isEqualTo(30);
    }

    @Test
    void alvo_arredonda_para_numero_de_meta() {
        assertThat(CalibradorDeAlvo.calibrar(List.of(47.3), 1.0, 1)).isEqualTo(45);
        assertThat(CalibradorDeAlvo.calibrar(List.of(312.0), 1.0, 1)).isEqualTo(310);
        assertThat(CalibradorDeAlvo.calibrar(List.of(7.4), 1.0, 1)).isEqualTo(7);
    }

    @Test
    void recorde_exige_passar_do_melhor() {
        assertThat(CalibradorDeAlvo.recorde(List.of(4.0, 9.0, 2.0), 1)).isEqualTo(10);
    }

    @Test
    void recorde_sem_historico_cai_no_piso() {
        assertThat(CalibradorDeAlvo.recorde(List.of(), 50)).isEqualTo(50);
    }
}
