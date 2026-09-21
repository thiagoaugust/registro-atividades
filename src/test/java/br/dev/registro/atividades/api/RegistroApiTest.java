package br.dev.registro.atividades.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class RegistroApiTest {

    private static Map<String, Object> registro(String data, String categoria, Object... extras) {
        Map<String, Object> corpo = new java.util.LinkedHashMap<>(
                Map.of("dataLocal", data, "categoria", categoria, "duracaoMin", 45, "esforco", 7));
        for (int i = 0; i < extras.length; i += 2) {
            corpo.put((String) extras[i], extras[i + 1]);
        }
        return corpo;
    }

    private static int criar(Map<String, Object> corpo) {
        return given().contentType(ContentType.JSON)
                .body(corpo)
                .when()
                .post("/api/registros")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private static int criarLivro(String titulo, int totalPaginas) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("titulo", titulo, "autor", "Autor", "totalPaginas", totalPaginas))
                .when()
                .post("/api/livros")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    @Test
    void registro_rapido_so_precisa_de_categoria_duracao_e_esforco() {
        given().contentType(ContentType.JSON)
                .body(Map.of("categoria", "TREINO", "duracaoMin", 30, "esforco", 5))
                .when()
                .post("/api/registros")
                .then()
                .statusCode(201)
                .body("dataLocal", is(java.time.LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString()))
                .body("titulo", nullValue());
    }

    @Test
    void cria_as_cinco_categorias_com_seus_detalhes() {
        String dia = "2026-02-02";
        int projeto = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Sistema de registro"))
                .when().post("/api/projetos").then().statusCode(201).extract().path("id");
        int desafio = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "1000 km", "metaValor", 1000, "unidade", "km", "inicio", "2026-01-01"))
                .when().post("/api/desafios").then().statusCode(201).extract().path("id");
        int livro = criarLivro("Domain-Driven Design", 560);

        criar(registro(dia, "TREINO", "titulo", "Corrida leve",
                "detalhes", Map.of("modalidade", "corrida", "distanciaKm", 8.2, "paceSegPorKm", 330)));
        criar(registro(dia, "ESTUDO", "detalhes", Map.of("tema", "Quarkus", "tecnica", "EXERCICIO", "foco", 4)));
        criar(registro(dia, "LEITURA", "livroId", livro,
                "detalhes", Map.of("paginaInicial", 1, "paginaFinal", 40)));
        criar(registro(dia, "DESAFIO", "desafioId", desafio, "detalhes", Map.of("valorProgresso", 8.2)));
        criar(registro(dia, "PROJETO", "projetoId", projeto,
                "detalhes", Map.of("marco", "CRUD de registros", "statusApos", "em andamento")));

        given().when().get("/api/registros?de=" + dia + "&ate=" + dia)
                .then().statusCode(200).body("$", hasSize(5));

        given().when().get("/api/registros?de=" + dia + "&ate=" + dia + "&categoria=TREINO")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].detalhes.distanciaKm", is(8.2f));

        given().when().get("/api/dias/" + dia)
                .then().statusCode(200)
                .body("totalMinutos", is(5 * 45))
                .body("minutosPorCategoria.TREINO", is(45));

        // progresso do desafio e derivado dos registros, nao um contador guardado
        given().when().get("/api/desafios/" + desafio)
                .then().statusCode(200).body("progresso", is(8.2f));
    }

    @Test
    void sem_periodo_a_listagem_traz_o_dia_de_hoje() {
        criar(Map.of("categoria", "ESTUDO", "duracaoMin", 20, "esforco", 3, "titulo", "de hoje"));
        given().when().get("/api/registros")
                .then().statusCode(200)
                .body("findAll { it.dataLocal == '%s' }.size()"
                        .formatted(java.time.LocalDate.now(br.dev.registro.comum.Relogio.ZONA)),
                        org.hamcrest.Matchers.greaterThan(0));
    }

    @Test
    void bean_validation_recusa_esforco_fora_da_faixa_em_rfc7807() {
        given().contentType(ContentType.JSON)
                .body(Map.of("categoria", "TREINO", "duracaoMin", 30, "esforco", 11))
                .when()
                .post("/api/registros")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("title", is("Dados invalidos"))
                .body("errors[0].campo", is("esforco"));
    }

    @Test
    void detalhe_que_nao_pertence_a_categoria_volta_422() {
        given().contentType(ContentType.JSON)
                .body(registro("2026-02-03", "ESTUDO", "detalhes", Map.of("distanciaKm", 5)))
                .when()
                .post("/api/registros")
                .then()
                .statusCode(422)
                .contentType("application/problem+json")
                .body("detail", containsString("nao e um campo de ESTUDO"));
    }

    @Test
    void nao_aceita_registro_no_futuro() {
        given().contentType(ContentType.JSON)
                .body(registro(java.time.LocalDate.now(br.dev.registro.comum.Relogio.ZONA).plusDays(1).toString(),
                        "TREINO"))
                .when()
                .post("/api/registros")
                .then()
                .statusCode(422)
                .body("detail", containsString("futuro"));
    }

    @Test
    void vinculo_inexistente_volta_404() {
        given().contentType(ContentType.JSON)
                .body(registro("2026-02-04", "PROJETO", "projetoId", 999999))
                .when()
                .post("/api/registros")
                .then()
                .statusCode(404)
                .body("detail", containsString("Projeto 999999"));
    }

    @Test
    void edita_e_exclui_um_registro() {
        int id = criar(registro("2026-02-05", "ESTUDO", "titulo", "antes"));

        given().contentType(ContentType.JSON)
                .body(registro("2026-02-05", "ESTUDO", "titulo", "depois", "satisfacao", 5))
                .when()
                .put("/api/registros/" + id)
                .then()
                .statusCode(200)
                .body("titulo", is("depois"))
                .body("satisfacao", is(5));

        given().when().delete("/api/registros/" + id).then().statusCode(204);
        given().when().get("/api/registros/" + id).then().statusCode(404);
        given().when().delete("/api/registros/" + id).then().statusCode(404);
    }

    @Test
    void leitura_ate_a_ultima_pagina_fecha_o_livro() {
        int livro = criarLivro("Livro curto", 100);
        given().when().get("/api/livros/" + livro).then().body("status", is("LENDO"));

        criar(registro("2026-02-06", "LEITURA", "livroId", livro,
                "detalhes", Map.of("paginaInicial", 80, "paginaFinal", 100)));

        given().when().get("/api/livros/" + livro)
                .then().statusCode(200)
                .body("status", is("CONCLUIDO"))
                .body("concluidoEm", is("2026-02-06"));
    }

    @Test
    void nao_exclui_livro_com_registro_vinculado() {
        int livro = criarLivro("Livro ocupado", 300);
        criar(registro("2026-02-07", "LEITURA", "livroId", livro,
                "detalhes", Map.of("paginaInicial", 1, "paginaFinal", 10)));

        given().when().delete("/api/livros/" + livro)
                .then().statusCode(422)
                .body("detail", containsString("registro(s) vinculado(s)"));
    }
}
