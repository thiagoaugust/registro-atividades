package br.dev.registro.gamificacao.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 21);

    /** Dias do mais recente para o mais antigo, como o repositorio entrega. */
    private static List<DiaPresenca> dias(String... marcas) {
        return java.util.stream.IntStream.range(0, marcas.length)
                .mapToObj(i -> new DiaPresenca(
                        HOJE.minusDays(i),
                        marcas[i].equals("presente"),
                        marcas[i].equals("descanso")))
                .toList();
    }

    @Test
    void conta_dias_seguidos_de_presenca() {
        assertThat(StreakCalculator.corrente(dias("presente", "presente", "presente"), HOJE)).isEqualTo(3);
    }

    @Test
    void dia_vazio_no_meio_quebra() {
        assertThat(StreakCalculator.corrente(dias("presente", "vazio", "presente"), HOJE)).isEqualTo(1);
    }

    @Test
    void descanso_planejado_segura_a_streak_sem_aumenta_la() {
        assertThat(StreakCalculator.corrente(dias("presente", "descanso", "presente", "presente"), HOJE))
                .isEqualTo(3);
    }

    @Test
    void hoje_ainda_vazio_nao_zera_a_sequencia() {
        // As 8h da manha o dia ainda nao acabou: a streak de ontem para tras continua valendo.
        assertThat(StreakCalculator.corrente(dias("vazio", "presente", "presente"), HOJE)).isEqualTo(2);
    }

    @Test
    void buraco_no_calendario_quebra() {
        List<DiaPresenca> comBuraco = List.of(
                new DiaPresenca(HOJE, true, false),
                new DiaPresenca(HOJE.minusDays(3), true, false),
                new DiaPresenca(HOJE.minusDays(4), true, false));

        assertThat(StreakCalculator.corrente(comBuraco, HOJE)).isEqualTo(1);
    }

    @Test
    void semana_inteira_de_descanso_nao_e_streak() {
        assertThat(StreakCalculator.corrente(dias("descanso", "descanso", "descanso"), HOJE)).isZero();
    }

    @Test
    void historico_vazio_nao_tem_streak() {
        assertThat(StreakCalculator.corrente(List.of(), HOJE)).isZero();
    }

    @Test
    void maior_sequencia_acha_o_recorde_e_nao_a_streak_atual() {
        // do mais recente para o mais antigo: 1 dia agora, e 4 dias seguidos antes
        List<DiaPresenca> dias = dias(
                "presente", "vazio", "presente", "presente", "presente", "presente", "vazio");

        assertThat(StreakCalculator.corrente(dias, HOJE)).isEqualTo(1);
        assertThat(StreakCalculator.maiorSequencia(dias)).isEqualTo(4);
    }

    @Test
    void maior_sequencia_atravessa_o_descanso_planejado() {
        assertThat(StreakCalculator.maiorSequencia(dias("presente", "descanso", "presente", "presente")))
                .isEqualTo(3);
    }

    @Test
    void maior_sequencia_de_historico_vazio_e_zero() {
        assertThat(StreakCalculator.maiorSequencia(List.of())).isZero();
    }
}
