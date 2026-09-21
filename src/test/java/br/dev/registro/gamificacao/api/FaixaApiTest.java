package br.dev.registro.gamificacao.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class FaixaApiTest {

    /** Datas proprias por teste: a faixa le os 90 dias anteriores, e os testes compartilham o banco. */
    private static void registro(LocalDate data, int duracao) {
        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", data.toString(), "categoria", "TREINO",
                        "duracaoMin", duracao, "esforco", 5))
                .when().post("/api/registros").then().statusCode(201);
    }

    private static void checkinComEnergia(LocalDate data, int energia) {
        given().contentType(ContentType.JSON)
                .body(Map.of("energia", energia, "descansoPlanejado", false))
                .when().put("/api/checkins/" + data).then().statusCode(200);
    }

    @Test
    void faixa_sai_dos_dias_de_energia_parecida() {
        LocalDate hoje = LocalDate.of(2025, 6, 30);
        // Dez dias de energia alta rendendo bem, dez de energia baixa rendendo pouco.
        for (int i = 1; i <= 10; i++) {
            registro(hoje.minusDays(i), 60);
            checkinComEnergia(hoje.minusDays(i), 5);

            registro(hoje.minusDays(i + 20), 20);
            checkinComEnergia(hoje.minusDays(i + 20), 1);
        }

        checkinComEnergia(hoje, 5);
        int pisoAlto = given().when().get("/api/gamificacao/faixa/" + hoje)
                .then().statusCode(200)
                .body("banda", is("ALTA"))
                .body("situacao", is("ABAIXO"))
                .extract().path("piso");

        checkinComEnergia(hoje, 1);
        int pisoBaixo = given().when().get("/api/gamificacao/faixa/" + hoje)
                .then().statusCode(200)
                .body("banda", is("BAIXA"))
                .extract().path("piso");

        // A mesma data, duas reguas: o dia de energia baixa cobra menos.
        org.assertj.core.api.Assertions.assertThat(pisoBaixo).isLessThan(pisoAlto);
    }

    @Test
    void dia_dentro_da_faixa_nao_deve_nada() {
        LocalDate hoje = LocalDate.of(2025, 9, 30);
        for (int i = 1; i <= 8; i++) {
            registro(hoje.minusDays(i), 60);
            checkinComEnergia(hoje.minusDays(i), 3);
        }

        checkinComEnergia(hoje, 3);
        registro(hoje, 60);

        given().when().get("/api/gamificacao/faixa/" + hoje)
                .then().statusCode(200)
                .body("situacao", is("DENTRO"))
                .body("faltaParaOPiso", is(0))
                .body("xpDoDia", is(60));
    }

    @Test
    void descanso_planejado_nao_cobra_faixa() {
        LocalDate hoje = LocalDate.of(2025, 11, 20);
        for (int i = 1; i <= 8; i++) {
            registro(hoje.minusDays(i), 60);
            checkinComEnergia(hoje.minusDays(i), 3);
        }

        given().contentType(ContentType.JSON)
                .body(Map.of("energia", 3, "descansoPlanejado", true))
                .when().put("/api/checkins/" + hoje).then().statusCode(200);

        given().when().get("/api/gamificacao/faixa/" + hoje)
                .then().statusCode(200)
                .body("situacao", is("DESCANSO"))
                .body("faltaParaOPiso", is(0));
    }

    @Test
    void sem_historico_a_faixa_fica_calibrando() {
        LocalDate hoje = LocalDate.of(2024, 2, 15);
        checkinComEnergia(hoje, 4);

        given().when().get("/api/gamificacao/faixa/" + hoje)
                .then().statusCode(200)
                .body("situacao", is("CALIBRANDO"))
                .body("diasComparaveis", lessThan(5));
    }

    @Test
    void sono_do_relogio_volta_no_checkin() {
        LocalDate dia = LocalDate.of(2024, 3, 10);
        given().contentType(ContentType.JSON)
                .body(Map.of("dormiuEm", "23:40", "acordouEm", "06:30", "minutosSonoProfundo", 74,
                        "minutosSonoRem", 96, "despertares", 2, "fcRepouso", 54,
                        "pontuacaoSono", 82, "qualidadeSono", 4, "descansoPlanejado", false))
                .when().put("/api/checkins/" + dia).then().statusCode(200)
                // 23h40 as 06h30 atravessa a meia-noite: 410 minutos, nao negativo.
                .body("minutosSono", is(410))
                .body("pontuacaoSono", is(82))
                .body("fcRepouso", is(54));
    }

    @Test
    void duracao_informada_ganha_da_derivada_dos_horarios() {
        LocalDate dia = LocalDate.of(2024, 3, 11);
        given().contentType(ContentType.JSON)
                .body(Map.of("dormiuEm", "23:00", "acordouEm", "07:00", "minutosSono", 430,
                        "descansoPlanejado", false))
                .when().put("/api/checkins/" + dia).then().statusCode(200)
                // 8h na cama, 7h10 dormindo: o relogio desconta os despertares, a subtracao nao.
                .body("minutosSono", is(430));
    }
}
