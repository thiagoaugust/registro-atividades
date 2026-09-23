package br.dev.registro.atividades.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class LeituraApiTest {

    private static int criarLivro(String titulo, Integer totalPaginas) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("titulo", titulo);
        corpo.put("autor", "Autor");
        if (totalPaginas != null) {
            corpo.put("totalPaginas", totalPaginas);
        }
        return given().contentType(ContentType.JSON)
                .body(corpo)
                .when()
                .post("/api/livros")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    /** Registra uma sessao informando apenas onde a leitura parou. */
    private static io.restassured.response.ValidatableResponse leitura(
            String data, int livroId, int duracao, int esforco, int parouNaPagina) {
        return given().contentType(ContentType.JSON)
                .body(Map.of(
                        "dataLocal", data,
                        "categoria", "LEITURA",
                        "duracaoMin", duracao,
                        "esforco", esforco,
                        "livroId", livroId,
                        "detalhes", Map.of("paginaFinal", parouNaPagina)))
                .when()
                .post("/api/registros")
                .then();
    }

    private static Map<String, Object> progressoDe(int livroId) {
        return given().when().get("/api/livros/progresso")
                .then().statusCode(200)
                .extract()
                .path("find { it.livroId == %d }".formatted(livroId));
    }

    @Test
    void primeira_sessao_comeca_na_pagina_um() {
        int livro = criarLivro("Comeco do zero", 300);

        leitura("2026-07-01", livro, 30, 4, 25)
                .statusCode(201)
                .body("detalhes.paginaInicial", is(1))
                .body("detalhes.paginaFinal", is(25));
    }

    @Test
    void sessao_seguinte_comeca_onde_a_anterior_parou() {
        int livro = criarLivro("Continuado", 300);

        leitura("2026-07-02", livro, 30, 4, 40).statusCode(201);
        leitura("2026-07-03", livro, 30, 4, 75)
                .statusCode(201)
                .body("detalhes.paginaInicial", is(41))
                .body("detalhes.paginaFinal", is(75));
    }

    @Test
    void pagina_anterior_a_ultima_e_recusada_com_mensagem_util() {
        int livro = criarLivro("Nao volta", 300);
        leitura("2026-07-04", livro, 30, 4, 80).statusCode(201);

        leitura("2026-07-05", livro, 30, 4, 50)
                .statusCode(422)
                .body("detail", containsString("ate a pagina 80"));
    }

    @Test
    void editar_a_ultima_sessao_nao_conflita_com_ela_mesma() {
        int livro = criarLivro("Editavel", 300);
        leitura("2026-07-06", livro, 30, 4, 60).statusCode(201);
        int id = leitura("2026-07-07", livro, 30, 4, 90).statusCode(201).extract().path("id");

        // corrigindo para 85: a comparacao ignora o proprio registro, entao 85 > 60 basta
        given().contentType(ContentType.JSON)
                .body(Map.of(
                        "dataLocal", "2026-07-07",
                        "categoria", "LEITURA",
                        "duracaoMin", 30,
                        "esforco", 4,
                        "livroId", livro,
                        "detalhes", Map.of("paginaFinal", 85)))
                .when()
                .put("/api/registros/" + id)
                .then()
                .statusCode(200)
                .body("detalhes.paginaInicial", is(61))
                .body("detalhes.paginaFinal", is(85));
    }

    @Test
    void progresso_traz_percentual_velocidade_e_previsao() {
        int livro = criarLivro("Com progresso", 200);
        LocalDate hoje = LocalDate.now(br.dev.registro.comum.Relogio.ZONA);

        // 3 sessoes recentes: 60 paginas em 90 min = 40 paginas/hora
        leitura(hoje.minusDays(4).toString(), livro, 30, 5, 20).statusCode(201);
        leitura(hoje.minusDays(2).toString(), livro, 30, 5, 40).statusCode(201);
        leitura(hoje.toString(), livro, 30, 5, 60).statusCode(201);

        Map<String, Object> progresso = progressoDe(livro);

        assertThat(progresso, "ultimaPagina", 60);
        assertThat(progresso, "paginasLidas", 60);
        assertThat(progresso, "paginasRestantes", 140);
        assertThat(progresso, "percentualLido", 30.0f);
        assertThat(progresso, "paginasPorHoraMedia", 40.0f);
        assertThat(progresso, "paginasPorHoraRecente", 40.0f);
        // 60 paginas nos 5 dias desde a primeira sessao = 12/dia; 140 restantes -> 12 dias
        assertThat(progresso, "ritmoDiario", 12.0f);
        assertThat(progresso, "diasRestantes", 12);
        org.assertj.core.api.Assertions.assertThat(progresso.get("previsaoTermino"))
                .isEqualTo(hoje.plusDays(12).toString());
    }

    @Test
    void livro_sem_sessao_aparece_no_progresso_sem_numeros_inventados() {
        int livro = criarLivro("Ainda na estante", 400);

        Map<String, Object> progresso = progressoDe(livro);

        assertThat(progresso, "ultimaPagina", 0);
        assertThat(progresso, "paginasRestantes", 400);
        assertThat(progresso, "percentualLido", 0.0f);
        org.assertj.core.api.Assertions.assertThat(progresso.get("ritmoDiario")).isNull();
        org.assertj.core.api.Assertions.assertThat(progresso.get("previsaoTermino")).isNull();
        org.assertj.core.api.Assertions.assertThat(progresso.get("paginasPorHoraMedia")).isNull();
    }

    @Test
    void sessao_sem_pagina_conta_tempo_mas_nao_atrapalha_a_velocidade() {
        int livro = criarLivro("So tempo", 200);
        LocalDate hoje = LocalDate.now(br.dev.registro.comum.Relogio.ZONA);

        leitura(hoje.minusDays(1).toString(), livro, 30, 5, 30).statusCode(201);
        // sessao sem informar pagina: so duracao
        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", hoje.toString(), "categoria", "LEITURA",
                        "duracaoMin", 20, "esforco", 5, "livroId", livro))
                .when().post("/api/registros").then().statusCode(201);

        Map<String, Object> progresso = progressoDe(livro);

        assertThat(progresso, "sessoes", 2);
        assertThat(progresso, "minutos", 50);
        // 30 paginas em 30 min continua sendo 60 paginas/hora
        assertThat(progresso, "paginasPorHoraMedia", 60.0f);
    }

    @Test
    void chegar_na_ultima_pagina_fecha_o_livro_e_zera_a_previsao() {
        int livro = criarLivro("Terminando", 100);
        leitura("2026-07-10", livro, 40, 5, 100).statusCode(201);

        given().when().get("/api/livros/" + livro).then().body("status", is("CONCLUIDO"));

        Map<String, Object> progresso = progressoDe(livro);
        assertThat(progresso, "percentualLido", 100.0f);
        assertThat(progresso, "paginasRestantes", 0);
        org.assertj.core.api.Assertions.assertThat(progresso.get("previsaoTermino")).isNull();
    }

    @Test
    void livro_sem_total_de_paginas_ainda_mede_velocidade() {
        int livro = criarLivro("Sem total", null);
        leitura("2026-07-12", livro, 30, 5, 45).statusCode(201);

        Map<String, Object> progresso = progressoDe(livro);
        assertThat(progresso, "paginasPorHoraMedia", 90.0f);
        org.assertj.core.api.Assertions.assertThat(progresso.get("percentualLido")).isNull();
        org.assertj.core.api.Assertions.assertThat(progresso.get("paginasRestantes")).isNull();
    }

    @Test
    void rota_de_progresso_nao_colide_com_a_de_livro_por_id() {
        given().when().get("/api/livros/progresso").then().statusCode(200).body("$", notNullValue());
    }

    @Test
    void leitura_sem_livro_vinculado_continua_valendo_como_tempo() {
        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", "2026-07-15", "categoria", "LEITURA",
                        "duracaoMin", 25, "esforco", 3))
                .when().post("/api/registros")
                .then().statusCode(201)
                .body("livro", nullValue())
                .body("duracaoMin", is(25));
    }

    @Test
    void pagina_sem_livro_continua_recusada() {
        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", "2026-07-16", "categoria", "LEITURA",
                        "duracaoMin", 25, "esforco", 3,
                        "detalhes", Map.of("paginaFinal", 30)))
                .when().post("/api/registros")
                .then().statusCode(422)
                .body("detail", containsString("livro vinculado"));
    }

    @Test
    void progresso_ordena_livros_em_leitura_antes_dos_concluidos() {
        criarLivro("Zzz em leitura", 100);
        given().when().get("/api/livros/progresso")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].status", is("LENDO"));
    }

    // ---------- categoria, dificuldade, capa e leitura retroativa ----------

    private static int areaPorNome(String nome) {
        return given().when().get("/api/areas")
                .then().statusCode(200)
                .extract().path("find { it.nome == '%s' }.id".formatted(nome));
    }

    @Test
    void areas_vem_semeadas() {
        given().when().get("/api/areas")
                .then().statusCode(200)
                .body("nome", org.hamcrest.Matchers.hasItems(
                        "Tecnico", "Literatura", "Historia", "Filosofia", "Psicologia"));
    }

    @Test
    void livro_guarda_capa_dificuldade_e_area() {
        int tecnico = areaPorNome("Tecnico");

        int livro = given().contentType(ContentType.JSON)
                .body(Map.of(
                        "titulo", "Designing Data-Intensive Applications",
                        "autor", "Kleppmann",
                        "totalPaginas", 590,
                        "capaUrl", "https://exemplo.com/capa.jpg",
                        "dificuldade", 5,
                        "areaId", tecnico))
                .when().post("/api/livros")
                .then().statusCode(201)
                .body("capaUrl", is("https://exemplo.com/capa.jpg"))
                .body("dificuldade", is(5))
                .body("area", is("Tecnico"))
                .extract().path("id");

        Map<String, Object> progresso = progressoDe(livro);
        assertThat(progresso, "area", "Tecnico");
        assertThat(progresso, "dificuldade", 5);
        assertThat(progresso, "capaUrl", "https://exemplo.com/capa.jpg");
    }

    @Test
    void dificuldade_fora_da_escala_e_recusada() {
        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Impossivel", "dificuldade", 9))
                .when().post("/api/livros")
                .then().statusCode(400)
                .body("errors[0].campo", is("dificuldade"));
    }

    @Test
    void livro_retroativo_nasce_concluido_e_conta_inteiro() {
        int historia = areaPorNome("Historia");

        int livro = given().contentType(ContentType.JSON)
                .body(Map.of(
                        "titulo", "Sapiens",
                        "autor", "Harari",
                        "totalPaginas", 464,
                        "areaId", historia,
                        "dificuldade", 3,
                        "diasLeitura", 30,
                        "horasLeitura", 20.0,
                        "concluidoEm", "2026-05-10"))
                .when().post("/api/livros")
                .then().statusCode(201)
                .body("status", is("CONCLUIDO"))
                .body("concluidoEm", is("2026-05-10"))
                .extract().path("id");

        Map<String, Object> progresso = progressoDe(livro);
        assertThat(progresso, "retroativo", true);
        assertThat(progresso, "percentualLido", 100.0f);
        assertThat(progresso, "paginasLidas", 464);
        // 464 paginas em 20 horas = 23,2 pag/h; em 30 dias = 15,47 por dia
        assertThat(progresso, "paginasPorHoraMedia", 23.2f);
        assertThat(progresso, "ritmoDiario", 15.47f);
        org.assertj.core.api.Assertions.assertThat(progresso.get("previsaoTermino")).isNull();
    }

    @Test
    void retroativo_sem_horas_conta_na_estante_mas_nao_na_velocidade() {
        int livro = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Lido ha anos", "totalPaginas", 200,
                        "diasLeitura", 20, "concluidoEm", "2026-04-01"))
                .when().post("/api/livros").then().statusCode(201).extract().path("id");

        Map<String, Object> progresso = progressoDe(livro);
        assertThat(progresso, "percentualLido", 100.0f);
        assertThat(progresso, "ritmoDiario", 10.0f);
        org.assertj.core.api.Assertions.assertThat(progresso.get("paginasPorHoraMedia")).isNull();
    }

    @Test
    void conclusao_no_futuro_e_recusada() {
        String amanha = LocalDate.now(br.dev.registro.comum.Relogio.ZONA).plusDays(1).toString();
        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Do futuro", "totalPaginas", 100,
                        "diasLeitura", 5, "concluidoEm", amanha))
                .when().post("/api/livros")
                .then().statusCode(422)
                .body("detail", containsString("futuro"));
    }

    @Test
    void estatisticas_contam_livros_por_categoria() {
        int filosofia = areaPorNome("Filosofia");
        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Meditacoes", "totalPaginas", 180, "areaId", filosofia,
                        "diasLeitura", 12, "horasLeitura", 6.0, "concluidoEm", "2026-03-01"))
                .when().post("/api/livros").then().statusCode(201);

        given().when().get("/api/livros/estatisticas")
                .then().statusCode(200)
                .body("porCategoria.find { it.categoria == 'Filosofia' }.concluidos",
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1))
                .body("porCategoria.find { it.categoria == 'Filosofia' }.paginas",
                        org.hamcrest.Matchers.greaterThanOrEqualTo(180));
    }

    @Test
    void estatisticas_mostram_velocidade_por_dificuldade() {
        given().when().get("/api/livros/estatisticas")
                .then().statusCode(200)
                .body("porDificuldade", notNullValue())
                .body("porDificuldade.dificuldade", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.greaterThanOrEqualTo(1),
                                org.hamcrest.Matchers.lessThanOrEqualTo(5))));
    }

    @Test
    void area_em_uso_nao_e_excluida() {
        int nova = given().contentType(ContentType.JSON)
                .body(Map.of("nome", "Poesia"))
                .when().post("/api/areas").then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Livro de poesia", "areaId", nova))
                .when().post("/api/livros").then().statusCode(201);

        given().when().delete("/api/areas/" + nova)
                .then().statusCode(422)
                .body("detail", containsString("desative em vez de excluir"));
    }

    private static void assertThat(Map<String, Object> progresso, String campo, Object esperado) {
        org.assertj.core.api.Assertions.assertThat(progresso.get(campo)).as(campo).isEqualTo(esperado);
    }
}
