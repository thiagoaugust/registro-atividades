package br.dev.registro.gamificacao.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;

/**
 * O tempo resgatado de um deslocamento: bonus no XP da sessao, conquista no acumulado e trofeu
 * mensal repetindo. Os tres apontam para o mesmo campo `detalhes.local`.
 */
@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class TempoAproveitadoApiTest {

    private static int xpDoDia(LocalDate dia, String local) {
        Map<String, Object> detalhes = local == null ? Map.of() : Map.of("local", local);
        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", dia.toString(), "categoria", "ESTUDO",
                        "duracaoMin", 60, "esforco", 5, "detalhes", detalhes))
                .when().post("/api/registros").then().statusCode(201);

        return given().when().get("/api/dias/" + dia)
                .then().statusCode(200).extract().path("resumo.xpTotal");
    }

    @Test
    void estudo_no_transporte_rende_mais_que_o_mesmo_estudo_em_casa() {
        int emCasa = xpDoDia(LocalDate.of(2025, 4, 8), "CASA");
        int noOnibus = xpDoDia(LocalDate.of(2025, 4, 9), "TRANSPORTE_PUBLICO");

        // Mesma duracao, mesmo esforco, mesma categoria: a diferenca e so o bonus de 30%.
        assertThat(emCasa).isEqualTo(60);
        assertThat(noOnibus).isEqualTo(78);
    }

    @Test
    void local_ausente_nao_bonifica() {
        assertThat(xpDoDia(LocalDate.of(2025, 4, 10), null)).isEqualTo(60);
    }

    @Test
    void local_desconhecido_e_recusado() {
        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", "2025-04-11", "categoria", "LEITURA",
                        "duracaoMin", 30, "esforco", 5, "detalhes", Map.of("local", "METRO")))
                .when().post("/api/registros").then().statusCode(422);
    }

    @Test
    void transporte_publico_e_um_contexto_do_gtd() {
        given().when().get("/api/gtd/contextos")
                .then().statusCode(200)
                .body("nome", hasItem("@transporte publico"));
    }

    @Test
    void trofeu_mensal_do_tempo_aproveitado_esta_no_painel() {
        given().when().get("/api/gamificacao/desafios")
                .then().statusCode(200)
                .body("mensais.titulo", hasItem("Trofeu do tempo aproveitado"));
    }

    @Test
    void conquista_do_tempo_aproveitado_mede_os_minutos_no_transporte() {
        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", "2025-04-12", "categoria", "LEITURA",
                        "duracaoMin", 45, "esforco", 4, "detalhes", Map.of("local", "TRANSPORTE_PUBLICO")))
                .when().post("/api/registros").then().statusCode(201);

        float progresso = given().when().get("/api/gamificacao/conquistas")
                .then().statusCode(200)
                .extract().path("find { it.codigo == 'TEMPO_APROVEITADO' }.progresso");

        // Os 45 minutos desta leitura, mais o que os outros testes deste arquivo deixaram.
        assertThat(progresso).isGreaterThanOrEqualTo(45);
    }
}
