package br.dev.registro.atividades.domain;

import br.dev.registro.atividades.domain.ProgressoCurso.Agregado;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressoCursoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 21);

    private static Agregado agregado(
            Double carga, int minutos, int minutosPratica, int minutosNaJanela, LocalDate primeira) {
        return new Agregado(
                1L, "Curso", "Plataforma", null, carga, 1L, "Tecnico", StatusCurso.CURSANDO, null,
                null, null, minutos, minutosPratica, 8, 6.0, primeira, HOJE, minutosNaJanela);
    }

    @Test
    void percentual_sai_das_horas_dedicadas_sobre_a_carga() {
        // 600 min = 10h de uma carga de 40h
        ProgressoCurso p = ProgressoCurso.de(agregado(40.0, 600, 240, 600, HOJE.minusDays(20)), HOJE);

        assertThat(p.percentualConcluido()).isEqualTo(25.0);
        assertThat(p.horasRestantes()).isEqualTo(30.0);
    }

    @Test
    void fracao_de_pratica_separa_estudar_de_assistir() {
        ProgressoCurso p = ProgressoCurso.de(agregado(40.0, 600, 240, 600, HOJE.minusDays(20)), HOJE);

        assertThat(p.percentualPratica()).isEqualTo(40.0);
    }

    @Test
    void curso_so_de_video_tem_pratica_zero_e_nao_nulo() {
        ProgressoCurso p = ProgressoCurso.de(agregado(40.0, 600, 0, 600, HOJE.minusDays(20)), HOJE);

        assertThat(p.percentualPratica()).isZero();
    }

    @Test
    void sem_tempo_registrado_a_fracao_de_pratica_nao_existe() {
        ProgressoCurso p = ProgressoCurso.de(agregado(40.0, 0, 0, 0, null), HOJE);

        assertThat(p.percentualPratica()).isNull();
        assertThat(p.percentualConcluido()).isZero();
    }

    @Test
    void ritmo_semanal_conta_os_dias_de_folga() {
        // 600 min (10h) na janela de 28 dias = 2,5 h/semana
        ProgressoCurso p = ProgressoCurso.de(agregado(40.0, 600, 240, 600, HOJE.minusDays(20)), HOJE);

        assertThat(p.horasPorSemana()).isEqualTo(2.5);
        // faltam 30h a 2,5h/semana = 12 semanas = 84 dias
        assertThat(p.diasRestantes()).isEqualTo(84);
        assertThat(p.previsaoTermino()).isEqualTo(HOJE.plusDays(84));
    }

    @Test
    void sem_estudo_recente_cai_para_o_ritmo_historico() {
        // 1400 min (23,3h) entre a primeira sessao e hoje (98 dias) = ~1,67 h/semana
        ProgressoCurso p = ProgressoCurso.de(agregado(40.0, 1400, 0, 0, HOJE.minusDays(97)), HOJE);

        assertThat(p.horasPorSemana()).isEqualTo(1.67);
        assertThat(p.previsaoTermino()).isNotNull();
    }

    @Test
    void curso_sem_carga_horaria_mede_tempo_mas_nao_percentual() {
        ProgressoCurso p = ProgressoCurso.de(agregado(null, 600, 300, 600, HOJE.minusDays(10)), HOJE);

        assertThat(p.percentualConcluido()).isNull();
        assertThat(p.horasRestantes()).isNull();
        assertThat(p.previsaoTermino()).isNull();
        assertThat(p.percentualPratica()).isEqualTo(50.0);
    }

    @Test
    void curso_cumprido_nao_tem_previsao() {
        ProgressoCurso p = ProgressoCurso.de(agregado(10.0, 600, 300, 600, HOJE.minusDays(20)), HOJE);

        assertThat(p.percentualConcluido()).isEqualTo(100.0);
        assertThat(p.horasRestantes()).isZero();
        assertThat(p.previsaoTermino()).isNull();
    }

    @Test
    void passar_da_carga_horaria_nao_passa_de_cem_por_cento() {
        ProgressoCurso p = ProgressoCurso.de(agregado(5.0, 600, 0, 600, HOJE.minusDays(20)), HOJE);

        assertThat(p.percentualConcluido()).isEqualTo(100.0);
        assertThat(p.horasRestantes()).isZero();
    }

    @Test
    void curso_retroativo_conta_as_horas_informadas() {
        Agregado retro = new Agregado(
                9L, "Curso antigo", "Alura", null, 30.0, 2L, "Negocios", StatusCurso.CONCLUIDO,
                HOJE.minusDays(60), 30.0, 90, 0, 0, 0, null, null, null, 0);

        ProgressoCurso p = ProgressoCurso.de(retro, HOJE);

        assertThat(p.retroativo()).isTrue();
        assertThat(p.minutos()).isEqualTo(1800);
        assertThat(p.percentualConcluido()).isEqualTo(100.0);
        // 30h em 90 dias = 2,33 h/semana
        assertThat(p.horasPorSemana()).isEqualTo(2.33);
        assertThat(p.previsaoTermino()).isNull();
        // sem sessao nao ha como saber o que foi pratica
        assertThat(p.percentualPratica()).isNull();
    }
}
