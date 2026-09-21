package br.dev.registro.comum;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/** Sem @TestSecurity de proposito: aqui o login de verdade e o que esta sob teste. */
@QuarkusTest
class AutenticacaoApiTest {

    @Test
    void api_recusa_quem_nao_esta_logado() {
        given().when().get("/api/registros").then().statusCode(401);
    }

    @Test
    void health_fica_aberto_para_o_docker_compose() {
        given().when().get("/q/health/ready").then().statusCode(200);
    }

    @Test
    void login_devolve_cookie_de_sessao_que_abre_a_api() {
        String cookie = given()
                .contentType(ContentType.URLENC)
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .when()
                .post("/api/sessao/login")
                .then()
                .statusCode(200)
                .cookie("registro_sessao", notNullValue())
                .extract()
                .cookie("registro_sessao");

        given().cookie("registro_sessao", cookie)
                .when()
                .get("/api/sessao")
                .then()
                .statusCode(200)
                .body("usuario", is("admin"));
    }

    @Test
    void senha_errada_nao_loga() {
        given().contentType(ContentType.URLENC)
                .formParam("j_username", "admin")
                .formParam("j_password", "nao-e-essa")
                .when()
                .post("/api/sessao/login")
                .then()
                .statusCode(401);
    }
}
