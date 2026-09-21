package br.dev.registro.analytics.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Os dados sao semeados uma vez, num ano proprio (2025), para nao esbarrarem nos outros testes de
 * API — que usam 2026 e o dia de hoje.
 */
@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class AnalyticsApiTest {

    private static final String ANO = "2025";
    private static boolean semeado;

    // @BeforeEach com guarda, e nao @BeforeAll: no @QuarkusTest a porta do RestAssured so e
    // configurada depois que o @BeforeAll estatico ja rodou.
    @BeforeEach
    void semear() {
        if (semeado) {
            return;
        }
        semeado = true;

        int livro = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Livro de analytics", "totalPaginas", 300))
                .when().post("/api/livros").then().statusCode(201).extract().path("id");

        int desafio = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "50 km em 2025", "metaValor", 50, "unidade", "km",
                        "inicio", "2025-03-01"))
                .when().post("/api/desafios").then().statusCode(201).extract().path("id");

        // Duas semanas de marco de 2025 com padroes distintos: a primeira boa, a segunda fraca.
        for (int dia = 3; dia <= 7; dia++) {
            String data = "2025-03-%02d".formatted(dia);
            registro(data, "TREINO", 60, 7, Map.of("modalidade", "corrida", "distanciaKm", 8));
            registro(data, "ESTUDO", 45, 6, Map.of("tema", "Quarkus"));
            checkin(data, 5, 8.0, 5, 5, 1, 2);
        }
        for (int dia = 10; dia <= 12; dia++) {
            String data = "2025-03-%02d".formatted(dia);
            registro(data, "ESTUDO", 20, 3, Map.of("tema", "SQL"));
            checkin(data, 2, 5.0, 2, 2, 4, 4);
        }

        registro("2025-03-05", "LEITURA", 40, 4, Map.of("paginaInicial", 1, "paginaFinal", 60), livro, null);
        registro("2025-03-06", "DESAFIO", 50, 8, Map.of("valorProgresso", 12), null, desafio);
    }

    private static void registro(String data, String categoria, int duracao, int esforco,
            Map<String, Object> detalhes) {
        registro(data, categoria, duracao, esforco, detalhes, null, null);
    }

    private static void registro(String data, String categoria, int duracao, int esforco,
            Map<String, Object> detalhes, Integer livroId, Integer desafioId) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("dataLocal", data);
        corpo.put("categoria", categoria);
        corpo.put("duracaoMin", duracao);
        corpo.put("esforco", esforco);
        corpo.put("detalhes", detalhes);
        if (livroId != null) {
            corpo.put("livroId", livroId);
        }
        if (desafioId != null) {
            corpo.put("desafioId", desafioId);
        }
        given().contentType(ContentType.JSON).body(corpo)
                .when().post("/api/registros").then().statusCode(201);
    }

    private static void checkin(String data, int energia, double horasSono, int qualidadeSono,
            int humor, int estresse, int dificuldade) {
        given().contentType(ContentType.JSON)
                .body(Map.of("energia", energia, "horasSono", horasSono, "qualidadeSono", qualidadeSono,
                        "humor", humor, "estresse", estresse, "dificuldadePrevista", dificuldade,
                        "descansoPlanejado", false))
                .when().put("/api/checkins/" + data).then().statusCode(200);
    }

    @Test
    void heatmap_traz_um_ponto_por_dia_com_dados() {
        given().when().get("/api/analytics/heatmap?ano=" + ANO)
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(8))
                .body("find { it.data == '2025-03-03' }.classificacao", notNullValue())
                .body("find { it.data == '2025-03-03' }.xp", greaterThan(0));
    }

    @Test
    void serie_diaria_traz_media_movel_e_distribuicao() {
        given().when().get("/api/analytics/periodo?granularidade=DIA&de=2025-03-01&ate=2025-03-15")
                .then().statusCode(200)
                .body("granularidade", is("DIA"))
                .body("serie.size()", greaterThanOrEqualTo(8))
                .body("serie[0].xpMediaMovel", notNullValue())
                .body("serie.collect { it.dificeis + it.normais + it.bons + it.excelentes }",
                        hasItem(1)); // cada bucket diario classifica exatamente um dia
    }

    @Test
    void granularidade_semanal_junta_os_dias_em_dois_baldes() {
        given().when().get("/api/analytics/periodo?granularidade=SEMANA&de=2025-03-01&ate=2025-03-16")
                .then().statusCode(200)
                // date_trunc('week') comeca na segunda; 01 e 02/03 sao fim de semana sem dados
                .body("serie.size()", is(2))
                .body("serie.find { it.periodo == '2025-03-03' }.diasComPresenca", is(5));
    }

    @Test
    void periodo_compara_com_o_anterior_de_mesmo_tamanho() {
        given().when().get("/api/analytics/periodo?de=2025-03-08&ate=2025-03-14")
                .then().statusCode(200)
                .body("periodoAnterior.de", is("2025-03-01"))
                .body("periodoAnterior.ate", is("2025-03-07"))
                // a semana fraca fica abaixo da semana forte anterior
                .body("xp.percentual", org.hamcrest.Matchers.lessThan(0f));
    }

    @Test
    void sem_base_de_comparacao_o_percentual_e_nulo_em_vez_de_infinito() {
        given().when().get("/api/analytics/periodo?de=2024-01-08&ate=2024-01-14")
                .then().statusCode(200)
                .body("xp.atual", is(0.0f))
                .body("xp.anterior", is(0.0f))
                .body("xp.percentual", nullValue());
    }

    @Test
    void barras_por_categoria_saem_agregadas_no_banco() {
        given().when().get("/api/analytics/periodo?granularidade=SEMANA&de=2025-03-01&ate=2025-03-16")
                .then().statusCode(200)
                .body("porCategoria.categoria", hasItem("TREINO"))
                .body("porCategoria.categoria", hasItem("ESTUDO"))
                .body("porCategoria.find { it.categoria == 'TREINO' }.minutos", is(5 * 60));
    }

    @Test
    void metricas_de_treino_trazem_km_e_pace_ponderado() {
        given().when().get("/api/analytics/categorias?de=2025-03-01&ate=2025-03-31")
                .then().statusCode(200)
                .body("treino.km", is(40.0f)) // 5 corridas de 8 km
                .body("treino.sessoes", is(5))
                // 5 x 60 min para 40 km = 7min30/km = 450 s/km
                .body("treino.paceMedioSegPorKm", is(450));
    }

    @Test
    void metricas_de_leitura_contam_paginas_e_livros() {
        given().when().get("/api/analytics/categorias?de=2025-03-01&ate=2025-03-31")
                .then().statusCode(200)
                .body("leitura.paginas", is(60)) // 1 a 60 inclusive
                .body("leitura.sessoes", is(1));
    }

    @Test
    void estudo_e_agrupado_por_tema() {
        given().when().get("/api/analytics/categorias?de=2025-03-01&ate=2025-03-31")
                .then().statusCode(200)
                .body("temas.find { it.tema == 'Quarkus' }.minutos", is(5 * 45))
                .body("temas.find { it.tema == 'SQL' }.minutos", is(3 * 20));
    }

    @Test
    void progresso_do_desafio_vem_somado_dos_registros() {
        given().when().get("/api/analytics/categorias?de=2025-03-01&ate=2025-03-31")
                .then().statusCode(200)
                .body("desafios.find { it.titulo == '50 km em 2025' }.progresso", is(12))
                .body("desafios.find { it.titulo == '50 km em 2025' }.meta", is(50.0f));
    }

    @Test
    void correlacao_de_sono_com_indice_sai_do_banco() {
        given().when().get("/api/analytics/correlacoes?de=2025-03-01&ate=2025-03-31")
                .then().statusCode(200)
                .body("coeficientes.size()", is(6))
                .body("coeficientes.find { it.nome.startsWith('horas de sono') }.pares", is(8))
                .body("coeficientes.find { it.nome.startsWith('horas de sono') }.coeficiente",
                        notNullValue());
    }

    /**
     * Energia alta acompanhou os dias de mais XP: correlacao positiva forte. (Contra o INDICE a
     * correlacao seria negativa, e nao por erro: com historico curto o componente objetivo fica
     * fixo em 50 e o indice passa a medir sobretudo a adversidade do check-in.)
     */
    @Test
    void energia_correlaciona_positivamente_com_xp() {
        given().when().get("/api/analytics/correlacoes?de=2025-03-01&ate=2025-03-31")
                .then().statusCode(200)
                .body("coeficientes.find { it.nome.startsWith('energia') }.coeficiente",
                        greaterThan(0.8f));
    }

    @Test
    void dia_da_semana_traz_media_por_dia() {
        given().when().get("/api/analytics/correlacoes?de=2025-03-01&ate=2025-03-31")
                .then().statusCode(200)
                .body("porDiaDaSemana.size()", greaterThan(0))
                .body("porDiaDaSemana[0].diaSemana", greaterThanOrEqualTo(1))
                .body("porDiaDaSemana.collect { it.dias }.sum()", is(8));
    }

    @Test
    void dispersao_traz_os_pares_para_o_grafico() {
        given().when().get("/api/analytics/correlacoes?de=2025-03-01&ate=2025-03-31")
                .then().statusCode(200)
                .body("sonoVersusIndice.size()", is(8))
                .body("sonoVersusIndice[0].x", notNullValue())
                .body("energiaVersusXp.size()", is(8));
    }

    @Test
    void metricas_gtd_contam_o_inbox_e_as_acoes() {
        given().when().get("/api/analytics/gtd")
                .then().statusCode(200)
                .body("pendentes", greaterThanOrEqualTo(0))
                .body("acoesAbertas", greaterThanOrEqualTo(0))
                .body("projetosParados", greaterThanOrEqualTo(0));
    }

    @Test
    void idade_media_do_inbox_aparece_quando_ha_pendentes() {
        given().contentType(ContentType.JSON)
                .body(Map.of("texto", "item para medir idade"))
                .when().post("/api/gtd/inbox").then().statusCode(201);

        given().when().get("/api/analytics/gtd")
                .then().statusCode(200)
                .body("pendentes", greaterThan(0))
                .body("idadeMediaPendentesDias", org.hamcrest.Matchers.lessThan(1.0f));
    }

    @Test
    void retrospectiva_resume_o_ano_e_compara_com_o_anterior() {
        given().when().get("/api/analytics/retrospectiva/2025")
                .then().statusCode(200)
                .body("ano", is(2025))
                .body("totais.xp", greaterThan(0))
                .body("porMes.size()", greaterThanOrEqualTo(1))
                .body("melhorDia.data", notNullValue())
                .body("melhorDia.xp", greaterThan(0))
                .body("maiorSequencia", greaterThanOrEqualTo(3)) // 3 a 7 de marco
                .body("categorias.treino.km", is(40.0f))
                .body("gtd", notNullValue())
                .body("xp.anterior", is(0.0f)); // nao havia 2024

        var resposta = given().when().get("/api/analytics/retrospectiva/2025").then().extract();
        int classificados = (int) resposta.path("dificeis")
                + (int) resposta.path("normais")
                + (int) resposta.path("bons")
                + (int) resposta.path("excelentes");
        org.assertj.core.api.Assertions.assertThat(classificados).isEqualTo(8);
    }

    @Test
    void retrospectiva_de_ano_sem_dados_nao_quebra() {
        given().when().get("/api/analytics/retrospectiva/2020")
                .then().statusCode(200)
                .body("totais.xp", is(0))
                .body("melhorDia", nullValue())
                .body("porMes.size()", is(0))
                .body("maiorSequencia", is(0));
    }

    @Test
    void periodo_invertido_e_recusado() {
        given().when().get("/api/analytics/periodo?de=2025-03-10&ate=2025-03-01")
                .then().statusCode(422);
    }

    @Test
    void sem_periodo_usa_os_ultimos_trinta_dias() {
        LocalDate hoje = LocalDate.now(br.dev.registro.comum.Relogio.ZONA);
        given().when().get("/api/analytics/periodo")
                .then().statusCode(200)
                .body("periodo.ate", is(hoje.toString()))
                .body("periodo.de", is(hoje.minusDays(29).toString()));
    }
}
