package br.dev.registro.gtd.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class GtdApiTest {

    private static int capturar(String texto) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("texto", texto))
                .when()
                .post("/api/gtd/inbox")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private static io.restassured.response.ValidatableResponse processar(int id, Map<String, Object> decisao) {
        return given().contentType(ContentType.JSON)
                .body(decisao)
                .when()
                .post("/api/gtd/inbox/" + id + "/processar")
                .then();
    }

    private static int contextoComputador() {
        return given().when().get("/api/gtd/contextos")
                .then().statusCode(200)
                .extract()
                .path("find { it.nome == '@computador' }.id");
    }

    @Test
    void captura_e_so_texto_e_entra_no_inbox() {
        capturar("ligar para o dentista");

        given().when().get("/api/gtd/inbox")
                .then().statusCode(200)
                .body("pendentes", greaterThanOrEqualTo(1))
                .body("itens.texto", hasItem("ligar para o dentista"))
                .body("itens[0].destino", nullValue());
    }

    @Test
    void contextos_vem_semeados() {
        given().when().get("/api/gtd/contextos")
                .then().statusCode(200)
                .body("nome", hasItem("@casa"))
                .body("nome", hasItem("@computador"));
    }

    @Test
    void item_nao_acionavel_vira_lixo() {
        int id = capturar("ideia que nao presta");
        processar(id, Map.of("destino", "LIXO")).statusCode(200).body("destino", is("LIXO"));

        given().when().get("/api/gtd/inbox")
                .then().body("itens.id", org.hamcrest.Matchers.not(hasItem(id)));
    }

    @Test
    void item_de_referencia_vai_para_o_arquivo() {
        int id = capturar("link do artigo sobre Panache");
        processar(id, Map.of(
                        "destino", "REFERENCIA",
                        "referencia", Map.of(
                                "titulo", "Panache repository",
                                "url", "https://quarkus.io/guides/hibernate-orm-panache",
                                "tags", List.of("quarkus", "jpa"))))
                .statusCode(200)
                .body("destino", is("REFERENCIA"))
                .body("destinoId", notNullValue());

        given().when().get("/api/gtd/referencias?tag=quarkus")
                .then().statusCode(200)
                .body("titulo", hasItem("Panache repository"));
    }

    @Test
    void item_incubado_vira_acao_em_algum_dia() {
        int id = capturar("aprender violao");
        processar(id, Map.of("destino", "ALGUM_DIA", "acao", Map.of("titulo", "aprender violao")))
                .statusCode(200);

        given().when().get("/api/gtd/acoes?estado=ALGUM_DIA")
                .then().statusCode(200)
                .body("titulo", hasItem("aprender violao"));
    }

    @Test
    void item_de_dois_minutos_ja_nasce_concluido_e_rende_xp() {
        int id = capturar("responder o e-mail do cliente");
        processar(id, Map.of("destino", "FEITO_2MIN", "acao", Map.of("titulo", "responder o e-mail")))
                .statusCode(200)
                .body("destino", is("FEITO_2MIN"));

        String hoje = java.time.LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString();
        given().when().get("/api/dias/" + hoje)
                .then().statusCode(200)
                .body("resumo.xpTotal", greaterThanOrEqualTo(5));
    }

    @Test
    void item_que_exige_mais_de_uma_acao_vira_projeto_com_a_primeira_acao() {
        int id = capturar("organizar a mudanca");
        int projetoId = processar(id, Map.of(
                        "destino", "PROJETO",
                        "projeto", Map.of("titulo", "Mudanca", "resultadoDesejado", "morar no apartamento novo"),
                        "acao", Map.of("titulo", "pedir orcamento de caminhao", "estado", "PROXIMA")))
                .statusCode(200)
                .extract()
                .path("destinoId");

        given().when().get("/api/gtd/acoes?projeto=" + projetoId)
                .then().statusCode(200)
                .body("titulo", hasItem("pedir orcamento de caminhao"));

        // o projeto nasceu com proxima acao, entao nao entra no alerta
        given().when().get("/api/gtd/projetos/sem-proxima-acao")
                .then().body("id", org.hamcrest.Matchers.not(hasItem(projetoId)));
    }

    @Test
    void projeto_sem_proxima_acao_entra_no_alerta() {
        int id = capturar("reformar o quarto");
        int projetoId = processar(id, Map.of(
                        "destino", "PROJETO",
                        "projeto", Map.of("titulo", "Reforma do quarto")))
                .statusCode(200)
                .extract()
                .path("destinoId");

        given().when().get("/api/gtd/projetos/sem-proxima-acao")
                .then().statusCode(200)
                .body("id", hasItem(projetoId));
    }

    @Test
    void acao_delegada_vai_para_aguardando_com_dono_e_data() {
        int id = capturar("assinatura do contrato");
        processar(id, Map.of(
                        "destino", "ACAO",
                        "acao", Map.of(
                                "titulo", "contrato assinado",
                                "estado", "AGUARDANDO",
                                "delegadaPara", "juridico")))
                .statusCode(200);

        given().when().get("/api/gtd/acoes?estado=AGUARDANDO")
                .then().statusCode(200)
                .body("find { it.titulo == 'contrato assinado' }.delegadaPara", is("juridico"))
                .body("find { it.titulo == 'contrato assinado' }.delegadaEm", notNullValue());
    }

    @Test
    void aguardando_sem_dono_e_recusado() {
        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "algo delegado", "estado", "AGUARDANDO"))
                .when().post("/api/gtd/acoes")
                .then().statusCode(422)
                .body("detail", containsString("com quem ela esta"));
    }

    @Test
    void agenda_sem_data_e_recusada() {
        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "reuniao", "estado", "AGENDA"))
                .when().post("/api/gtd/acoes")
                .then().statusCode(422)
                .body("detail", containsString("data e hora"));
    }

    @Test
    void item_ja_processado_nao_processa_de_novo() {
        int id = capturar("item unico");
        processar(id, Map.of("destino", "LIXO")).statusCode(200);
        processar(id, Map.of("destino", "LIXO"))
                .statusCode(422)
                .body("detail", containsString("ja foi processado"));
    }

    @Test
    void engajar_filtra_por_contexto_tempo_e_energia() {
        int contexto = contextoComputador();

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "revisar PR rapido", "contextoId", contexto,
                        "tempoEstimadoMin", 15, "energia", "BAIXA"))
                .when().post("/api/gtd/acoes").then().statusCode(201);

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "refatorar o modulo inteiro", "contextoId", contexto,
                        "tempoEstimadoMin", 180, "energia", "ALTA"))
                .when().post("/api/gtd/acoes").then().statusCode(201);

        // 30 minutos e energia baixa: so a tarefa curta cabe
        given().when().get("/api/gtd/engajar?contexto=" + contexto + "&tempoDisponivel=30&energia=BAIXA")
                .then().statusCode(200)
                .body("acoes.titulo", hasItem("revisar PR rapido"))
                .body("acoes.titulo", org.hamcrest.Matchers.not(hasItem("refatorar o modulo inteiro")));

        // com tempo e energia de sobra, as duas aparecem
        given().when().get("/api/gtd/engajar?contexto=" + contexto + "&tempoDisponivel=240&energia=ALTA")
                .then().statusCode(200)
                .body("acoes.titulo", hasItem("refatorar o modulo inteiro"));
    }

    @Test
    void engajar_sugere_a_energia_do_check_in_do_dia() {
        String hoje = java.time.LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString();
        given().contentType(ContentType.JSON)
                .body(Map.of("energia", 1, "descansoPlanejado", false))
                .when().put("/api/checkins/" + hoje).then().statusCode(200);

        given().when().get("/api/gtd/engajar")
                .then().statusCode(200)
                .body("energiaSugerida", is("BAIXA"));
    }

    @Test
    void acao_concluida_com_categoria_sugere_virar_registro() {
        int acaoId = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "estudar Panache", "categoria", "ESTUDO"))
                .when().post("/api/gtd/acoes").then().statusCode(201).extract().path("id");

        given().when().post("/api/gtd/acoes/" + acaoId + "/concluir")
                .then().statusCode(200)
                .body("acao.estado", is("CONCLUIDA"))
                .body("sugestaoRegistro.categoria", is("ESTUDO"))
                .body("sugestaoRegistro.titulo", is("estudar Panache"));

        // so faltam duracao e esforco
        int registroId = given().contentType(ContentType.JSON)
                .body(Map.of("duracaoMin", 50, "esforco", 6))
                .when().post("/api/gtd/acoes/" + acaoId + "/registrar")
                .then().statusCode(200)
                .body("categoria", is("ESTUDO"))
                .body("titulo", is("estudar Panache"))
                .extract().path("id");

        given().when().get("/api/gtd/acoes/" + acaoId)
                .then().body("registroId", is(registroId));

        // e nao da para registrar duas vezes
        given().contentType(ContentType.JSON)
                .body(Map.of("duracaoMin", 50, "esforco", 6))
                .when().post("/api/gtd/acoes/" + acaoId + "/registrar")
                .then().statusCode(422)
                .body("detail", containsString("ja foi registrada"));
    }

    @Test
    void acao_sem_categoria_nao_vira_registro() {
        int acaoId = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "pagar a conta de luz"))
                .when().post("/api/gtd/acoes").then().statusCode(201).extract().path("id");

        given().when().post("/api/gtd/acoes/" + acaoId + "/concluir")
                .then().statusCode(200)
                .body("sugestaoRegistro", nullValue());

        given().contentType(ContentType.JSON)
                .body(Map.of("duracaoMin", 10, "esforco", 2))
                .when().post("/api/gtd/acoes/" + acaoId + "/registrar")
                .then().statusCode(422)
                .body("detail", containsString("nao tem categoria"));
    }

    @Test
    void reabrir_acao_devolve_o_xp_ganho() {
        String hoje = java.time.LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString();
        int acaoId = given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "acao que vai e volta"))
                .when().post("/api/gtd/acoes").then().statusCode(201).extract().path("id");

        given().when().post("/api/gtd/acoes/" + acaoId + "/concluir").then().statusCode(200);
        int comAcao = given().when().get("/api/dias/" + hoje).then().extract().path("resumo.xpTotal");

        given().when().post("/api/gtd/acoes/" + acaoId + "/reabrir")
                .then().statusCode(200).body("estado", is("PROXIMA"));

        int semAcao = given().when().get("/api/dias/" + hoje).then().extract().path("resumo.xpTotal");
        org.assertj.core.api.Assertions.assertThat(semAcao).isEqualTo(comAcao - 5);
    }

    @Test
    void contexto_em_uso_nao_e_excluido() {
        int contexto = given().contentType(ContentType.JSON)
                .body(Map.of("nome", "@academia"))
                .when().post("/api/gtd/contextos").then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "treino de perna", "contextoId", contexto))
                .when().post("/api/gtd/acoes").then().statusCode(201);

        given().when().delete("/api/gtd/contextos/" + contexto)
                .then().statusCode(422)
                .body("detail", containsString("desative em vez de excluir"));
    }

    @Test
    void contexto_ganha_arroba_sozinho() {
        given().contentType(ContentType.JSON)
                .body(Map.of("nome", "escritorio"))
                .when().post("/api/gtd/contextos")
                .then().statusCode(201)
                .body("nome", is("@escritorio"));
    }

    // ---------- projetos com tarefas e progresso ----------

    private static int criarProjeto(String titulo) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("titulo", titulo, "resultadoDesejado", "entregue"))
                .when().post("/api/projetos")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static int tarefa(int projeto, String titulo, String estado) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("titulo", titulo, "estado", estado, "projetoId", projeto))
                .when().post("/api/gtd/acoes")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static Map<String, Object> progressoDoProjeto(int projeto) {
        return given().when().get("/api/gtd/projetos/progresso")
                .then().statusCode(200)
                .extract()
                .path("find { it.projetoId == %d }".formatted(projeto));
    }

    @Test
    void progresso_do_projeto_conta_concluidas_e_ignora_descartadas() {
        int projeto = criarProjeto("Mudar de apartamento");
        int caixas = tarefa(projeto, "comprar caixas", "PROXIMA");
        tarefa(projeto, "contratar frete", "PROXIMA");
        tarefa(projeto, "pintar a sala", "ALGUM_DIA");
        tarefa(projeto, "trocar o piso", "DESCARTADA");
        given().when().post("/api/gtd/acoes/" + caixas + "/concluir").then().statusCode(200);

        Map<String, Object> progresso = progressoDoProjeto(projeto);

        org.assertj.core.api.Assertions.assertThat(progresso)
                .containsEntry("titulo", "Mudar de apartamento")
                .containsEntry("tarefas", 3)
                .containsEntry("concluidas", 1)
                .containsEntry("faltam", 2)
                .containsEntry("percentual", 33.3f);
    }

    @Test
    void projeto_sem_tarefa_aparece_sem_percentual_e_arquivado_some() {
        int vazio = criarProjeto("Projeto recem criado");
        int arquivado = criarProjeto("Projeto arquivado");
        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Projeto arquivado", "status", "ARQUIVADO"))
                .when().put("/api/projetos/" + arquivado)
                .then().statusCode(200);

        org.assertj.core.api.Assertions.assertThat(progressoDoProjeto(vazio))
                .containsEntry("tarefas", 0)
                .containsEntry("percentual", null);
        org.assertj.core.api.Assertions.assertThat(progressoDoProjeto(arquivado)).isNull();
    }

    @Test
    void concluir_a_ultima_tarefa_nao_conclui_o_projeto() {
        int projeto = criarProjeto("Projeto de uma tarefa");
        int unica = tarefa(projeto, "fazer tudo", "PROXIMA");
        given().when().post("/api/gtd/acoes/" + unica + "/concluir").then().statusCode(200);

        org.assertj.core.api.Assertions.assertThat(progressoDoProjeto(projeto))
                .containsEntry("status", "ATIVO")
                .containsEntry("faltam", 0);
    }
}
