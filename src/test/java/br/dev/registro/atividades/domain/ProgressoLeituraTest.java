package br.dev.registro.atividades.domain;

import br.dev.registro.atividades.domain.ProgressoLeitura.Agregado;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressoLeituraTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 21);

    private static Agregado agregado(
            int total, int ultimaPagina, int paginasLidas, int minutosComPaginas,
            int paginasRecentes, int minutosRecentes, int paginasNaJanela, LocalDate primeira) {
        return new Agregado(
                1L, "Livro", "Autor", total, StatusLivro.LENDO, null,
                null, null, null, null, null, null,
                ultimaPagina, paginasLidas, 10, minutosComPaginas, 5.0,
                primeira, HOJE, minutosComPaginas, paginasRecentes, minutosRecentes, paginasNaJanela);
    }

    @Test
    void calcula_percentual_e_paginas_restantes() {
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(300, 120, 120, 240, 40, 60, 70, HOJE.minusDays(20)), HOJE);

        assertThat(p.percentualLido()).isEqualTo(40.0);
        assertThat(p.paginasRestantes()).isEqualTo(180);
    }

    @Test
    void velocidade_sai_em_paginas_por_hora() {
        // 120 paginas em 240 min = 30 paginas/hora; as ultimas sessoes: 40 em 60 min = 40/hora
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(300, 120, 120, 240, 40, 60, 70, HOJE.minusDays(20)), HOJE);

        assertThat(p.paginasPorHoraMedia()).isEqualTo(30.0);
        assertThat(p.paginasPorHoraRecente()).isEqualTo(40.0);
        assertThat(p.variacaoDeVelocidade()).isEqualTo(33.3); // esta fluindo melhor
    }

    @Test
    void leitura_travando_da_variacao_negativa() {
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(300, 120, 120, 240, 10, 60, 30, HOJE.minusDays(20)), HOJE);

        assertThat(p.paginasPorHoraRecente()).isEqualTo(10.0);
        assertThat(p.variacaoDeVelocidade()).isEqualTo(-66.7);
    }

    @Test
    void previsao_usa_o_ritmo_da_janela_recente() {
        // 70 paginas em 14 dias = 5 paginas/dia; faltam 180 -> 36 dias
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(300, 120, 120, 240, 40, 60, 70, HOJE.minusDays(20)), HOJE);

        assertThat(p.ritmoDiario()).isEqualTo(5.0);
        assertThat(p.diasRestantes()).isEqualTo(36);
        assertThat(p.previsaoTermino()).isEqualTo(HOJE.plusDays(36));
    }

    @Test
    void ritmo_conta_os_dias_de_folga_e_nao_so_os_de_leitura() {
        // 40 paginas lidas num unico domingo, ha 6 dias, e nada depois: 40/7 = 5,71 por dia
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(300, 40, 40, 60, 40, 60, 40, HOJE.minusDays(6)), HOJE);

        assertThat(p.ritmoDiario()).isEqualTo(5.71);
    }

    @Test
    void livro_comecado_hoje_nao_conta_os_dias_antes_de_abrir_o_livro() {
        // 45 paginas no primeiro dia: o ritmo e 45/dia, nao 45/14
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(200, 45, 45, 60, 45, 60, 45, HOJE), HOJE);

        assertThat(p.ritmoDiario()).isEqualTo(45.0);
        assertThat(p.diasRestantes()).isEqualTo(4);
    }

    @Test
    void sem_leitura_recente_cai_para_a_media_do_livro() {
        // 100 paginas desde 99 dias atras (100 dias corridos) = 1 pagina/dia
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(300, 100, 100, 300, 0, 0, 0, HOJE.minusDays(99)), HOJE);

        assertThat(p.ritmoDiario()).isEqualTo(1.0);
        assertThat(p.diasRestantes()).isEqualTo(200);
    }

    @Test
    void livro_sem_nenhuma_sessao_nao_inventa_numero() {
        Agregado vazio = new Agregado(
                1L, "Novo", "Autor", 300, StatusLivro.LENDO, null,
                null, null, null, null, null, null,
                0, 0, 0, 0, null, null, null, 0, 0, 0, 0);

        ProgressoLeitura p = ProgressoLeitura.de(vazio, HOJE);

        assertThat(p.percentualLido()).isZero();
        assertThat(p.paginasRestantes()).isEqualTo(300);
        assertThat(p.ritmoDiario()).isNull();
        assertThat(p.previsaoTermino()).isNull();
        assertThat(p.paginasPorHoraMedia()).isNull();
        assertThat(p.variacaoDeVelocidade()).isNull();
    }

    @Test
    void livro_sem_total_de_paginas_nao_tem_percentual_nem_previsao() {
        Agregado semTotal = new Agregado(
                1L, "Sem total", "Autor", null, StatusLivro.LENDO, null,
                null, null, null, null, null, null,
                80, 80, 4, 120, 5.0, HOJE.minusDays(10), HOJE, 120, 30, 45, 50);

        ProgressoLeitura p = ProgressoLeitura.de(semTotal, HOJE);

        assertThat(p.percentualLido()).isNull();
        assertThat(p.paginasRestantes()).isNull();
        assertThat(p.previsaoTermino()).isNull();
        // a velocidade continua fazendo sentido mesmo sem saber o tamanho do livro
        assertThat(p.paginasPorHoraMedia()).isEqualTo(40.0);
    }

    @Test
    void livro_terminado_nao_tem_previsao() {
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(300, 300, 300, 600, 50, 60, 60, HOJE.minusDays(30)), HOJE);

        assertThat(p.percentualLido()).isEqualTo(100.0);
        assertThat(p.paginasRestantes()).isZero();
        assertThat(p.previsaoTermino()).isNull();
    }

    /** Livro lido antes de o sistema existir: sem sessao, mas com dias (e as vezes horas). */
    private static Agregado retroativo(int total, int dias, Double horas) {
        return new Agregado(
                9L, "Lido antes", "Autor", total, StatusLivro.CONCLUIDO, HOJE.minusDays(40),
                null, (short) 3, 1L, "Historia", dias, horas,
                0, 0, 0, 0, null, null, null, 0, 0, 0, 0);
    }

    @Test
    void livro_retroativo_conta_como_lido_por_inteiro() {
        ProgressoLeitura p = ProgressoLeitura.de(retroativo(320, 40, 16.0), HOJE);

        assertThat(p.retroativo()).isTrue();
        assertThat(p.diasLeitura()).isEqualTo(40);
        assertThat(p.percentualLido()).isEqualTo(100.0);
        assertThat(p.paginasLidas()).isEqualTo(320);
        assertThat(p.paginasRestantes()).isZero();
        assertThat(p.area()).isEqualTo("Historia");
        assertThat(p.previsaoTermino()).isNull();
    }

    @Test
    void retroativo_com_horas_entra_na_conta_de_velocidade() {
        // 320 paginas em 16 horas = 20 paginas/hora; 320 em 40 dias = 8 por dia
        ProgressoLeitura p = ProgressoLeitura.de(retroativo(320, 40, 16.0), HOJE);

        assertThat(p.paginasPorHoraMedia()).isEqualTo(20.0);
        assertThat(p.ritmoDiario()).isEqualTo(8.0);
    }

    @Test
    void retroativo_sem_horas_nao_inventa_velocidade() {
        ProgressoLeitura p = ProgressoLeitura.de(retroativo(320, 40, null), HOJE);

        assertThat(p.paginasPorHoraMedia()).isNull();
        assertThat(p.paginasPorHoraRecente()).isNull();
        assertThat(p.minutos()).isZero();
        // o ritmo em paginas/dia continua valendo: dias foi informado
        assertThat(p.ritmoDiario()).isEqualTo(8.0);
    }

    @Test
    void pagina_alem_do_total_nao_passa_de_cem_por_cento() {
        ProgressoLeitura p = ProgressoLeitura.de(
                agregado(300, 320, 320, 600, 50, 60, 60, HOJE.minusDays(30)), HOJE);

        assertThat(p.percentualLido()).isEqualTo(100.0);
        assertThat(p.paginasRestantes()).isZero();
    }
}
