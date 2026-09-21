package br.dev.registro.gamificacao.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class GamificacaoApiTest {

    private static int criarRegistro(String data, String categoria, int duracao, int esforco) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("dataLocal", data);
        corpo.put("categoria", categoria);
        corpo.put("duracaoMin", duracao);
        corpo.put("esforco", esforco);
        return given().contentType(ContentType.JSON)
                .body(corpo)
                .when()
                .post("/api/registros")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private static void checkin(String data, Map<String, Object> dados) {
        given().contentType(ContentType.JSON)
                .body(dados)
                .when()
                .put("/api/checkins/" + data)
                .then()
                .statusCode(200);
    }

    @Test
    void registro_vira_xp_no_resumo_do_dia() {
        String dia = "2026-03-02";
        criarRegistro(dia, "TREINO", 60, 5); // 60 x 1,0 x 1,0

        given().when().get("/api/dias/" + dia)
                .then().statusCode(200)
                .body("resumo.xpTotal", is(60))
                .body("resumo.presenca", is(true))
                .body("resumo.baselineInsuficiente", is(true)); // sem 7 dias de historico
    }

    @Test
    void editar_e_excluir_recalculam_o_dia() {
        String dia = "2026-03-05";
        int id = criarRegistro(dia, "ESTUDO", 30, 5);

        given().when().get("/api/dias/" + dia).then().body("resumo.xpTotal", is(30));

        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", dia, "categoria", "ESTUDO", "duracaoMin", 80, "esforco", 10))
                .when().put("/api/registros/" + id).then().statusCode(200);

        // 80 x 1,4 = 112, abaixo do teto de 120: aqui o que esta sob teste e o recalculo
        given().when().get("/api/dias/" + dia).then().body("resumo.xpTotal", is(112));

        given().when().delete("/api/registros/" + id).then().statusCode(204);
        given().when().get("/api/dias/" + dia)
                .then().body("resumo.xpTotal", is(0))
                .body("resumo.presenca", is(false));
    }

    @Test
    void mudar_a_data_do_registro_recalcula_os_dois_dias() {
        String origem = "2026-03-08";
        String destino = "2026-03-09";
        int id = criarRegistro(origem, "TREINO", 60, 5);

        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", destino, "categoria", "TREINO", "duracaoMin", 60, "esforco", 5))
                .when().put("/api/registros/" + id).then().statusCode(200);

        given().when().get("/api/dias/" + origem).then().body("resumo.xpTotal", is(0));
        given().when().get("/api/dias/" + destino).then().body("resumo.xpTotal", is(60));
    }

    @Test
    void teto_por_categoria_limita_o_dia() {
        String dia = "2026-03-12";
        criarRegistro(dia, "ESTUDO", 240, 5); // 240 brutos, teto 120

        given().when().get("/api/dias/" + dia).then().body("resumo.xpTotal", is(120));
    }

    @Test
    void tres_categorias_no_dia_rendem_bonus_de_equilibrio() {
        String dia = "2026-03-15";
        criarRegistro(dia, "TREINO", 60, 5);
        criarRegistro(dia, "ESTUDO", 60, 5);
        criarRegistro(dia, "LEITURA", 60, 5); // 48 (multiplicador 0,8)

        // base 168 + 15% = 193
        given().when().get("/api/dias/" + dia)
                .then().body("resumo.xpTotal", is(193));
    }

    @Test
    void check_in_ruim_melhora_a_classificacao_do_mesmo_xp() {
        String facil = "2026-04-02";
        String dificil = "2026-04-03";
        criarRegistro(facil, "TREINO", 60, 5);
        criarRegistro(dificil, "TREINO", 60, 5);

        checkin(facil, Map.of("energia", 5, "qualidadeSono", 5, "humor", 5, "estresse", 1,
                "dificuldadePrevista", 1, "descansoPlanejado", false));
        checkin(dificil, Map.of("energia", 1, "qualidadeSono", 1, "humor", 2, "estresse", 5,
                "dificuldadePrevista", 5, "descansoPlanejado", false));

        float indiceFacil = given().when().get("/api/dias/" + facil)
                .then().statusCode(200).extract().path("resumo.indiceProdutividade");
        float indiceDificil = given().when().get("/api/dias/" + dificil)
                .then().statusCode(200).extract().path("resumo.indiceProdutividade");

        org.assertj.core.api.Assertions.assertThat(indiceDificil).isGreaterThan(indiceFacil);
    }

    @Test
    void fechamento_do_dia_substitui_a_dificuldade_prevista() {
        String dia = "2026-04-06";
        criarRegistro(dia, "ESTUDO", 60, 5);
        checkin(dia, Map.of("dificuldadePrevista", 1, "descansoPlanejado", false));

        float antes = given().when().get("/api/dias/" + dia).then().extract().path("resumo.indiceProdutividade");

        given().contentType(ContentType.JSON)
                .body(Map.of("dificuldadeFinal", 5, "atrapalhou", "reuniao o dia inteiro"))
                .when().put("/api/checkins/" + dia + "/fechamento")
                .then().statusCode(200);

        float depois = given().when().get("/api/dias/" + dia).then().extract().path("resumo.indiceProdutividade");
        org.assertj.core.api.Assertions.assertThat(depois).isGreaterThan(antes);
    }

    @Test
    void descanso_planejado_e_dia_neutro() {
        String dia = "2026-04-10";
        checkin(dia, Map.of("energia", 2, "descansoPlanejado", true, "frase", "folga"));

        given().when().get("/api/dias/" + dia)
                .then().statusCode(200)
                .body("resumo.descanso", is(true))
                .body("resumo.classificacao", is("NORMAL"))
                .body("resumo.indiceProdutividade", is(50.0f))
                .body("resumo.presenca", is(true)); // check-in sozinho ja e presenca
    }

    @Test
    void check_in_de_dia_futuro_e_recusado() {
        String amanha = java.time.LocalDate.now(br.dev.registro.comum.Relogio.ZONA).plusDays(1).toString();

        given().contentType(ContentType.JSON)
                .body(Map.of("energia", 3, "descansoPlanejado", false))
                .when().put("/api/checkins/" + amanha)
                .then().statusCode(422);
    }

    @Test
    void dia_sem_check_in_devolve_204() {
        given().when().get("/api/checkins/2026-04-20").then().statusCode(204);
    }

    @Test
    void perfil_traz_nivel_xp_e_streaks() {
        criarRegistro("2026-05-04", "TREINO", 60, 5);

        given().when().get("/api/gamificacao/perfil")
                .then().statusCode(200)
                .body("geral.nivel", greaterThanOrEqualTo(1))
                .body("geral.xp", greaterThan(0))
                .body("geral.xpParaOProximo", greaterThan(0))
                .body("porCategoria.TREINO.xp", greaterThan(0))
                .body("streakGeral", greaterThanOrEqualTo(0));

        // O total do perfil tem de bater com o catalogo; numero fixo aqui quebraria a cada
        // conquista nova no seed, sem apontar defeito nenhum.
        int noCatalogo = given().when().get("/api/gamificacao/conquistas")
                .then().statusCode(200).extract().path("size()");
        given().when().get("/api/gamificacao/perfil")
                .then().body("conquistasTotais", is(noCatalogo));
    }

    @Test
    void primeiro_registro_desbloqueia_conquista() {
        criarRegistro("2026-05-07", "ESTUDO", 30, 5);

        given().when().get("/api/gamificacao/conquistas")
                .then().statusCode(200)
                .body("findAll { it.desbloqueada == true }.codigo", hasItem("PRIMEIRO_REGISTRO"))
                .body("find { it.codigo == 'PRIMEIRO_REGISTRO' }.dataLocal", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void conquista_distante_mostra_progresso_parcial() {
        criarRegistro("2026-05-11", "TREINO", 30, 5);

        given().when().get("/api/gamificacao/conquistas")
                .then().statusCode(200)
                .body("find { it.codigo == 'QUINHENTOS_KM' }.desbloqueada", is(false))
                .body("find { it.codigo == 'QUINHENTOS_KM' }.alvo", is(500.0f));
    }

    @Test
    void dia_nunca_tocado_nao_tem_resumo() {
        given().when().get("/api/dias/2026-01-15")
                .then().statusCode(200)
                .body("resumo", nullValue())
                .body("checkin", nullValue());
    }
}
