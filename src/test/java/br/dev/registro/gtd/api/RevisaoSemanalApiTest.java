package br.dev.registro.gtd.api;

import br.dev.registro.comum.Relogio;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class RevisaoSemanalApiTest {

    private static String semanaDe(LocalDate data) {
        return data.with(DayOfWeek.MONDAY).toString();
    }

    private static final LocalDate HOJE = LocalDate.now(Relogio.ZONA);

    private static void marcarTodos(String semana) {
        for (String passo : new String[] {
            "ESVAZIAR_INBOX", "REVISAR_PROXIMAS_ACOES", "REVISAR_PROJETOS",
            "REVISAR_AGUARDANDO", "REVISAR_AGENDA", "REVISAR_ALGUM_DIA"
        }) {
            given().contentType(ContentType.JSON)
                    .body(Map.of("feito", true))
                    .when()
                    .put("/api/gtd/revisoes/" + semana + "/passos/" + passo)
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    void checklist_traz_os_seis_passos_na_ordem() {
        given().when().get("/api/gtd/revisoes/checklist")
                .then().statusCode(200)
                .body("size()", is(6))
                .body("[0].passo", is("ESVAZIAR_INBOX"))
                .body("[0].titulo", notNullValue())
                .body("[5].passo", is("REVISAR_ALGUM_DIA"));
    }

    @Test
    void iniciar_duas_vezes_retoma_a_mesma_revisao() {
        String semana = semanaDe(HOJE);

        given().when().post("/api/gtd/revisoes")
                .then().statusCode(200).body("semanaInicio", is(semana));
        given().when().post("/api/gtd/revisoes")
                .then().statusCode(200)
                .body("semanaInicio", is(semana))
                .body("concluidaEm", org.hamcrest.Matchers.nullValue());

        given().when().get("/api/gtd/revisoes")
                .then().statusCode(200)
                .body("findAll { it.semanaInicio == '%s' }.size()".formatted(semana), is(1));
    }

    @Test
    void revisao_incompleta_nao_conclui() {
        String semana = semanaDe(HOJE.minusWeeks(5));
        given().when().post("/api/gtd/revisoes?semana=" + semana).then().statusCode(200);

        given().contentType(ContentType.JSON)
                .body(Map.of("feito", true))
                .when().put("/api/gtd/revisoes/" + semana + "/passos/ESVAZIAR_INBOX")
                .then().statusCode(200).body("completa", is(false));

        given().when().post("/api/gtd/revisoes/" + semana + "/concluir")
                .then().statusCode(422)
                .body("detail", containsString("faltam passos"));
    }

    @Test
    void revisao_completa_conclui_e_rende_xp() {
        String semana = semanaDe(HOJE.minusWeeks(6));
        given().when().post("/api/gtd/revisoes?semana=" + semana).then().statusCode(200);
        marcarTodos(semana);

        given().when().post("/api/gtd/revisoes/" + semana + "/concluir")
                .then().statusCode(200)
                .body("concluidaEm", notNullValue())
                .body("completa", is(true))
                .body("duracaoMin", greaterThanOrEqualTo(0));

        // o XP da revisao cai no dia em que ela foi concluida
        given().when().get("/api/dias/" + HOJE)
                .then().statusCode(200)
                .body("resumo.xpTotal", greaterThanOrEqualTo(50));
    }

    @Test
    void nao_conclui_a_mesma_revisao_duas_vezes() {
        String semana = semanaDe(HOJE.minusWeeks(7));
        given().when().post("/api/gtd/revisoes?semana=" + semana).then().statusCode(200);
        marcarTodos(semana);
        given().when().post("/api/gtd/revisoes/" + semana + "/concluir").then().statusCode(200);

        given().when().post("/api/gtd/revisoes/" + semana + "/concluir")
                .then().statusCode(422)
                .body("detail", containsString("ja foi concluida"));
    }

    @Test
    void desmarcar_passo_derruba_a_completude() {
        String semana = semanaDe(HOJE.minusWeeks(8));
        given().when().post("/api/gtd/revisoes?semana=" + semana).then().statusCode(200);
        marcarTodos(semana);

        given().contentType(ContentType.JSON)
                .body(Map.of("feito", false))
                .when().put("/api/gtd/revisoes/" + semana + "/passos/REVISAR_PROJETOS")
                .then().statusCode(200).body("completa", is(false));
    }

    @Test
    void semana_futura_e_recusada() {
        given().when().post("/api/gtd/revisoes?semana=" + HOJE.plusWeeks(2))
                .then().statusCode(422)
                .body("detail", containsString("ainda nao comecou"));
    }
}
