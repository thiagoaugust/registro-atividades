package br.dev.registro.atividades.domain;

import br.dev.registro.comum.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Dominio puro: nada de Quarkus, nada de banco. */
class ValidadorDetalhesTest {

    private static RegistroAtividade registro(Categoria categoria, Object... paresChaveValor) {
        RegistroAtividade r = new RegistroAtividade();
        r.categoria = categoria;
        r.duracaoMin = 30;
        r.esforco = 5;
        r.detalhes = new LinkedHashMap<>();
        for (int i = 0; i < paresChaveValor.length; i += 2) {
            r.detalhes.put((String) paresChaveValor[i], paresChaveValor[i + 1]);
        }
        return r;
    }

    @Test
    void aceita_registro_rapido_sem_nenhum_detalhe() {
        for (Categoria c : Categoria.values()) {
            assertThatCode(() -> ValidadorDetalhes.validar(registro(c))).doesNotThrowAnyException();
        }
    }

    @Test
    void recusa_campo_que_nao_pertence_a_categoria() {
        assertThatThrownBy(() -> ValidadorDetalhes.validar(registro(Categoria.ESTUDO, "distanciaKm", 10)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("distanciaKm nao e um campo de ESTUDO");
    }

    @Test
    void treino_aceita_series_e_recusa_serie_sem_exercicio() {
        assertThatCode(() -> ValidadorDetalhes.validar(registro(
                        Categoria.TREINO,
                        "modalidade", "musculacao",
                        "series", List.of(Map.of("exercicio", "supino", "reps", 10, "cargaKg", 60)))))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> ValidadorDetalhes.validar(registro(
                        Categoria.TREINO, "series", List.of(Map.of("reps", 10)))))
                .hasMessageContaining("series[0].exercicio");
    }

    @Test
    void treino_recusa_distancia_nao_positiva_e_fc_fora_da_faixa() {
        assertThatThrownBy(() -> ValidadorDetalhes.validar(registro(Categoria.TREINO, "distanciaKm", 0)))
                .hasMessageContaining("distanciaKm deve ser maior que zero");
        assertThatThrownBy(() -> ValidadorDetalhes.validar(registro(Categoria.TREINO, "fcMedia", 400)))
                .hasMessageContaining("fcMedia deve estar entre 20 e 250");
    }

    @Test
    void estudo_valida_tecnica_e_foco() {
        assertThatCode(() -> ValidadorDetalhes.validar(registro(
                        Categoria.ESTUDO, "tema", "Quarkus", "tecnica", "FLASHCARD", "foco", 4)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ValidadorDetalhes.validar(registro(Categoria.ESTUDO, "tecnica", "OSMOSE")))
                .hasMessageContaining("tecnica deve ser uma de");
        assertThatThrownBy(() -> ValidadorDetalhes.validar(registro(Categoria.ESTUDO, "foco", 9)))
                .hasMessageContaining("foco deve estar entre 1 e 5");
    }

    @Test
    void leitura_exige_livro_quando_ha_paginas() {
        assertThatThrownBy(() -> ValidadorDetalhes.validar(registro(
                        Categoria.LEITURA, "paginaInicial", 10, "paginaFinal", 30)))
                .hasMessageContaining("precisa de um livro vinculado");
    }

    @Test
    void leitura_recusa_pagina_final_menor_que_inicial_e_alem_do_total() {
        RegistroAtividade r = registro(Categoria.LEITURA, "paginaInicial", 30, "paginaFinal", 10);
        r.livro = livro(200);
        assertThatThrownBy(() -> ValidadorDetalhes.validar(r))
                .hasMessageContaining("paginaFinal nao pode ser menor que paginaInicial");

        RegistroAtividade alem = registro(Categoria.LEITURA, "paginaInicial", 10, "paginaFinal", 900);
        alem.livro = livro(200);
        assertThatThrownBy(() -> ValidadorDetalhes.validar(alem))
                .hasMessageContaining("passa do total de paginas");
    }

    @Test
    void leitura_recusa_pagina_solitaria() {
        RegistroAtividade r = registro(Categoria.LEITURA, "paginaInicial", 10);
        r.livro = livro(200);
        assertThatThrownBy(() -> ValidadorDetalhes.validar(r)).hasMessageContaining("andam juntas");
    }

    @Test
    void desafio_exige_vinculo_quando_ha_progresso() {
        assertThatThrownBy(() -> ValidadorDetalhes.validar(registro(Categoria.DESAFIO, "valorProgresso", 5)))
                .hasMessageContaining("precisa de um desafio vinculado");
    }

    @Test
    void acumula_todos_os_erros_numa_mensagem_so() {
        RegistroAtividade r = registro(Categoria.TREINO, "distanciaKm", -1, "fcMedia", 500);
        assertThatThrownBy(() -> ValidadorDetalhes.validar(r))
                .satisfies(e -> assertThat(e.getMessage().split("; ")).hasSize(2));
    }

    private static Livro livro(int totalPaginas) {
        Livro l = new Livro();
        l.id = 1L;
        l.titulo = "Livro";
        l.totalPaginas = totalPaginas;
        return l;
    }
}
