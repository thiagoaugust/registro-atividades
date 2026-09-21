package br.dev.registro.gamificacao.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class DesafioPeriodicoApiTest {

    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    private static Response painel() {
        return given().when().get("/api/gamificacao/desafios")
                .then().statusCode(200).extract().response();
    }

    /** Posicao do desafio no painel: a ordem vem do catalogo, mas fixar indice quebraria no seed. */
    private static int indiceDe(Response painel, String escopo, String titulo) {
        List<String> titulos = painel.path(escopo + ".titulo");
        int indice = titulos.indexOf(titulo);
        assertThat(indice).as("desafio '%s' em %s", titulo, escopo).isNotNegative();
        return indice;
    }

    private static void registro(LocalDate data, String categoria, int duracao) {
        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", data.toString(), "categoria", categoria,
                        "duracaoMin", duracao, "esforco", 5))
                .when().post("/api/registros").then().statusCode(201);
    }

    @Test
    void os_tres_horizontes_abrem_sozinhos() {
        Response painel = painel();

        assertThat(painel.<List<?>>path("diarios")).isNotEmpty();
        assertThat(painel.<List<?>>path("semanais")).isNotEmpty();
        assertThat(painel.<List<?>>path("mensais")).isNotEmpty();
    }

    @Test
    void periodo_de_cada_desafio_bate_com_o_escopo() {
        Response painel = painel();

        String inicioDiario = painel.path("diarios[0].periodoInicio");
        String fimDiario = painel.path("diarios[0].periodoFim");
        assertThat(inicioDiario).isEqualTo(HOJE.toString()).isEqualTo(fimDiario);

        LocalDate inicioSemana = LocalDate.parse(painel.path("semanais[0].periodoInicio"));
        assertThat(inicioSemana.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        LocalDate inicioMes = LocalDate.parse(painel.path("mensais[0].periodoInicio"));
        assertThat(inicioMes.getDayOfMonth()).isEqualTo(1);
    }

    @Test
    void abrir_o_painel_duas_vezes_nao_duplica_instancia() {
        int antes = painel().<List<?>>path("diarios").size();
        int depois = painel().<List<?>>path("diarios").size();

        assertThat(depois).isEqualTo(antes);
    }

    @Test
    void registrar_o_dia_cumpre_o_desafio_de_presenca_e_rende_xp() {
        registro(HOJE, "TREINO", 30);

        Response painel = painel();
        int indice = indiceDe(painel, "diarios", "Registre o dia");
        String status = painel.path("diarios[" + indice + "].status");
        int xp = painel.path("diarios[" + indice + "].xp");

        assertThat(status).isEqualTo("CUMPRIDO");
        assertThat(xp).isEqualTo(10);

        // O XP do desafio entra no ledger do dia, somado ao XP do proprio registro.
        given().when().get("/api/dias/" + HOJE)
                .then().statusCode(200)
                .body("resumo.xpTotal", greaterThan(30));
    }

    @Test
    void alvo_calibrado_nunca_fica_abaixo_do_piso() {
        Response painel = painel();
        int indice = indiceDe(painel, "semanais", "Horas de estudo");
        float alvo = painel.path("semanais[" + indice + "].alvo");
        String unidade = painel.path("semanais[" + indice + "].unidade");

        // parametros {"fator":1.1,"minimo":60}: o historico pode subir o alvo, nunca derruba-lo.
        assertThat(alvo).isGreaterThanOrEqualTo(60.0f);
        assertThat(unidade).isEqualTo("minutos");
    }

    @Test
    void desafio_de_recorde_pede_mais_que_o_melhor_periodo() {
        Response painel = painel();
        int indice = indiceDe(painel, "mensais", "Recorde de leitura");
        String tipo = painel.path("mensais[" + indice + "].tipo");
        float alvo = painel.path("mensais[" + indice + "].alvo");

        assertThat(tipo).isEqualTo("RECORDE");
        // {"minimo":50}: sem mes anterior o piso segura o primeiro recorde; com mes anterior, sobe.
        assertThat(alvo).isGreaterThanOrEqualTo(50.0f);
    }

    @Test
    void historico_e_trofeus_existem_mesmo_vazios() {
        Response painel = painel();
        int trofeus = painel.path("trofeusDoAno");

        assertThat(painel.<List<?>>path("historico")).isNotNull();
        assertThat(trofeus).isNotNegative();
    }

    @Test
    void catalogo_traz_titulos_e_unidades() {
        given().when().get("/api/gamificacao/desafios")
                .then().statusCode(200)
                .body("diarios.titulo", hasItem("Dia equilibrado"))
                .body("mensais.titulo", hasItem("Recorde de quilometragem"))
                .body("mensais.unidade", hasItem("km"));
    }
}
