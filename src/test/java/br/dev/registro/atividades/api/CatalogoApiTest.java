package br.dev.registro.atividades.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/** Caminhos do cadastro de projeto e desafio que os testes de registro nao exercitavam. */
@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class CatalogoApiTest {

    private static int criarProjeto(String titulo) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("titulo", titulo, "resultadoDesejado", "pronto"))
                .when().post("/api/projetos")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    void concluir_projeto_marca_a_data_e_reabrir_limpa() {
        int id = criarProjeto("Projeto que conclui");

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Projeto que conclui", "status", "CONCLUIDO"))
                .when().put("/api/projetos/" + id)
                .then().statusCode(200)
                .body("status", is("CONCLUIDO"))
                .body("concluidoEm", notNullValue());

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Projeto que conclui", "status", "ATIVO"))
                .when().put("/api/projetos/" + id)
                .then().statusCode(200)
                .body("concluidoEm", nullValue());
    }

    @Test
    void concluir_duas_vezes_mantem_a_data_da_primeira() {
        int id = criarProjeto("Projeto teimoso");

        String primeira = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Projeto teimoso", "status", "CONCLUIDO"))
                .when().put("/api/projetos/" + id)
                .then().statusCode(200).extract().path("concluidoEm");

        String segunda = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Projeto teimoso", "status", "CONCLUIDO"))
                .when().put("/api/projetos/" + id)
                .then().statusCode(200).extract().path("concluidoEm");

        // Ate os segundos: o Postgres guarda microssegundos e a resposta do POST vem com nanos,
        // entao comparar a string inteira falharia por precisao, nao por comportamento.
        org.assertj.core.api.Assertions.assertThat(segunda).startsWith(primeira.substring(0, 19));
    }

    @Test
    void projeto_sem_registro_pode_ser_excluido() {
        int id = criarProjeto("Projeto descartavel");
        given().when().delete("/api/projetos/" + id).then().statusCode(204);
        given().when().get("/api/projetos/" + id).then().statusCode(404);
    }

    @Test
    void projeto_inexistente_volta_404() {
        given().when().get("/api/projetos/999999").then().statusCode(404);
        given().when().delete("/api/projetos/999999").then().statusCode(404);
    }

    @Test
    void desafio_com_fim_antes_do_inicio_e_recusado() {
        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Invertido", "metaValor", 10, "unidade", "km",
                        "inicio", "2026-05-10", "fim", "2026-05-01"))
                .when().post("/api/desafios")
                .then().statusCode(422)
                .body("detail", containsString("anterior ao inicio"));
    }

    @Test
    void desafio_sem_inicio_comeca_hoje() {
        String hoje = java.time.LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString();
        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Sem inicio", "metaValor", 10, "unidade", "km"))
                .when().post("/api/desafios")
                .then().statusCode(201)
                .body("inicio", is(hoje))
                .body("progresso", is(0));
    }

    @Test
    void desafio_pode_ser_atualizado_e_concluido() {
        int id = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Desafio editavel", "metaValor", 50, "unidade", "km",
                        "inicio", "2026-01-01"))
                .when().post("/api/desafios").then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Desafio editado", "metaValor", 80, "unidade", "km",
                        "inicio", "2026-01-01", "status", "CONCLUIDO"))
                .when().put("/api/desafios/" + id)
                .then().statusCode(200)
                .body("titulo", is("Desafio editado"))
                .body("metaValor", is(80))
                .body("status", is("CONCLUIDO"));
    }

    @Test
    void desafio_sem_registro_pode_ser_excluido() {
        int id = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Desafio descartavel", "metaValor", 5, "unidade", "km"))
                .when().post("/api/desafios").then().statusCode(201).extract().path("id");

        given().when().delete("/api/desafios/" + id).then().statusCode(204);
    }

    @Test
    void livro_sem_registro_pode_ser_excluido_e_status_volta_para_lendo() {
        int id = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Livro reversivel", "totalPaginas", 100, "status", "CONCLUIDO"))
                .when().post("/api/livros").then().statusCode(201).extract().path("id");

        given().when().get("/api/livros/" + id).then().body("concluidoEm", notNullValue());

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Livro reversivel", "totalPaginas", 100, "status", "LENDO"))
                .when().put("/api/livros/" + id)
                .then().statusCode(200)
                .body("concluidoEm", nullValue());

        given().when().delete("/api/livros/" + id).then().statusCode(204);
    }

    @Test
    void area_pode_ser_renomeada_e_desativada() {
        int id = given().contentType(ContentType.JSON)
                .body(Map.of("nome", "Area temporaria"))
                .when().post("/api/areas").then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("nome", "Area renomeada", "ativa", false, "ordem", 999))
                .when().put("/api/areas/" + id)
                .then().statusCode(200)
                .body("nome", is("Area renomeada"))
                .body("ativa", is(false));

        // desativada some da lista padrao, mas continua acessivel com ?todas=true
        given().when().get("/api/areas")
                .then().body("find { it.id == %d }".formatted(id), nullValue());
        given().when().get("/api/areas?todas=true")
                .then().body("find { it.id == %d }.nome".formatted(id), is("Area renomeada"));

        given().when().delete("/api/areas/" + id).then().statusCode(204);
    }

    @Test
    void periodo_invertido_na_listagem_de_registros_e_recusado() {
        given().when().get("/api/registros?de=2026-03-10&ate=2026-03-01")
                .then().statusCode(422)
                .body("detail", containsString("anterior ao inicio"));
    }
}
