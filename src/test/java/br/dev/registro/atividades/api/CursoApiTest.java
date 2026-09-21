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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "user")
class CursoApiTest {

    private static int areaPorNome(String nome) {
        return given().when().get("/api/areas")
                .then().statusCode(200)
                .extract().path("find { it.nome == '%s' }.id".formatted(nome));
    }

    private static int criarCurso(Map<String, Object> corpo) {
        return given().contentType(ContentType.JSON)
                .body(corpo)
                .when().post("/api/cursos")
                .then().statusCode(201)
                .extract().path("id");
    }

    /** Sessao de estudo vinculada ao curso, com parte do tempo como pratica deliberada. */
    private static void estudo(String data, int cursoId, int duracao, Integer minutosPratica, String tema) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("tema", tema);
        if (minutosPratica != null) {
            detalhes.put("minutosPratica", minutosPratica);
        }
        given().contentType(ContentType.JSON)
                .body(Map.of(
                        "dataLocal", data,
                        "categoria", "ESTUDO",
                        "duracaoMin", duracao,
                        "esforco", 6,
                        "cursoId", cursoId,
                        "detalhes", detalhes))
                .when().post("/api/registros")
                .then().statusCode(201);
    }

    private static Map<String, Object> progressoDe(int cursoId) {
        return given().when().get("/api/cursos/progresso")
                .then().statusCode(200)
                .extract().path("find { it.cursoId == %d }".formatted(cursoId));
    }

    @Test
    void areas_sao_as_mesmas_dos_livros() {
        given().when().get("/api/areas")
                .then().statusCode(200)
                .body("nome", org.hamcrest.Matchers.hasItems("Tecnico", "Psicologia", "Negocios"));
    }

    @Test
    void curso_acumula_horas_das_sessoes() {
        int curso = criarCurso(Map.of(
                "titulo", "Kubernetes do zero", "instituicao", "Alura",
                "cargaHoraria", 20, "areaId", areaPorNome("Tecnico")));
        LocalDate hoje = LocalDate.now(br.dev.registro.comum.Relogio.ZONA);

        estudo(hoje.minusDays(3).toString(), curso, 120, 40, "Kubernetes");
        estudo(hoje.toString(), curso, 180, 80, "Kubernetes");

        Map<String, Object> progresso = progressoDe(curso);

        assertThat(progresso, "minutos", 300);
        assertThat(progresso, "sessoes", 2);
        // 5h de uma carga de 20h
        assertThat(progresso, "percentualConcluido", 25.0f);
        assertThat(progresso, "horasRestantes", 15.0f);
        assertThat(progresso, "area", "Tecnico");
    }

    @Test
    void fracao_de_pratica_deliberada_sai_dos_minutos_informados() {
        int curso = criarCurso(Map.of("titulo", "Curso com pratica", "cargaHoraria", 10));
        LocalDate hoje = LocalDate.now(br.dev.registro.comum.Relogio.ZONA);

        estudo(hoje.toString(), curso, 100, 40, "SQL");

        Map<String, Object> progresso = progressoDe(curso);
        assertThat(progresso, "minutosPratica", 40);
        assertThat(progresso, "percentualPratica", 40.0f);
    }

    @Test
    void curso_so_de_video_mostra_pratica_zero() {
        int curso = criarCurso(Map.of("titulo", "So assistindo", "cargaHoraria", 10));
        estudo(LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString(), curso, 90, null, "Teoria");

        assertThat(progressoDe(curso), "percentualPratica", 0.0f);
    }

    @Test
    void pratica_maior_que_a_sessao_e_recusada() {
        int curso = criarCurso(Map.of("titulo", "Curso coerente", "cargaHoraria", 10));

        given().contentType(ContentType.JSON)
                .body(Map.of("dataLocal", "2026-06-01", "categoria", "ESTUDO",
                        "duracaoMin", 60, "esforco", 5, "cursoId", curso,
                        "detalhes", Map.of("minutosPratica", 90)))
                .when().post("/api/registros")
                .then().statusCode(422)
                .body("detail", containsString("nao pode passar da duracao"));
    }

    @Test
    void curso_retroativo_conta_as_horas_sem_gerar_registro() {
        int curso = criarCurso(Map.of(
                "titulo", "Curso antigo", "cargaHoraria", 30, "areaId", areaPorNome("Negocios"),
                "horasRetroativas", 30, "diasRetroativos", 90, "concluidoEm", "2026-04-20"));

        Map<String, Object> progresso = progressoDe(curso);
        assertThat(progresso, "retroativo", true);
        assertThat(progresso, "minutos", 1800);
        assertThat(progresso, "percentualConcluido", 100.0f);
        assertThat(progresso, "sessoes", 0);
        org.assertj.core.api.Assertions.assertThat(progresso.get("percentualPratica")).isNull();

        given().when().get("/api/cursos/" + curso).then().body("status", is("CONCLUIDO"));
    }

    @Test
    void previsao_sai_do_ritmo_semanal() {
        int curso = criarCurso(Map.of("titulo", "Com previsao", "cargaHoraria", 40));
        LocalDate hoje = LocalDate.now(br.dev.registro.comum.Relogio.ZONA);

        // 600 min (10h) dentro da janela de 28 dias = 2,5 h/semana
        estudo(hoje.minusDays(10).toString(), curso, 300, 100, "Tema");
        estudo(hoje.minusDays(2).toString(), curso, 300, 100, "Tema");

        Map<String, Object> progresso = progressoDe(curso);
        assertThat(progresso, "horasPorSemana", 2.5f);
        assertThat(progresso, "diasRestantes", 84);
        org.assertj.core.api.Assertions.assertThat(progresso.get("previsaoTermino")).isNotNull();
    }

    @Test
    void curso_sem_carga_horaria_mede_tempo_mas_nao_percentual() {
        int curso = criarCurso(Map.of("titulo", "Sem carga"));
        estudo(LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString(), curso, 60, 30, "Livre");

        Map<String, Object> progresso = progressoDe(curso);
        assertThat(progresso, "minutos", 60);
        org.assertj.core.api.Assertions.assertThat(progresso.get("percentualConcluido")).isNull();
        org.assertj.core.api.Assertions.assertThat(progresso.get("previsaoTermino")).isNull();
    }

    @Test
    void estudo_mostra_onde_o_tempo_foi_parar() {
        int psicologia = areaPorNome("Psicologia");
        int curso = criarCurso(Map.of("titulo", "Curso de psicologia", "cargaHoraria", 12,
                "areaId", psicologia));
        LocalDate hoje = LocalDate.now(br.dev.registro.comum.Relogio.ZONA);
        estudo(hoje.toString(), curso, 120, 60, "Vieses cognitivos");

        given().when().get("/api/cursos/estudo")
                .then().statusCode(200)
                .body("porArea.find { it.area == 'Psicologia' }.minutosCurso",
                        greaterThanOrEqualTo(120))
                .body("porTema.find { it.tema == 'Vieses cognitivos' }.minutos", is(120))
                .body("porTema.find { it.tema == 'Vieses cognitivos' }.minutosPratica", is(60))
                .body("pratica.minutosPratica", greaterThanOrEqualTo(60))
                .body("pratica.percentual", notNullValue());
    }

    @Test
    void area_soma_o_tempo_de_curso_e_de_livro() {
        int filosofia = areaPorNome("Filosofia");

        int curso = criarCurso(Map.of("titulo", "Curso de filosofia", "cargaHoraria", 10,
                "areaId", filosofia));
        estudo(LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString(), curso, 60, 0, "Etica");

        given().contentType(ContentType.JSON)
                .body(Map.of("titulo", "Livro de filosofia", "totalPaginas", 200, "areaId", filosofia,
                        "diasLeitura", 10, "horasLeitura", 5, "concluidoEm", "2026-05-01"))
                .when().post("/api/livros").then().statusCode(201);

        given().when().get("/api/cursos/estudo")
                .then().statusCode(200)
                .body("porArea.find { it.area == 'Filosofia' }.minutosCurso", is(60))
                .body("porArea.find { it.area == 'Filosofia' }.minutosLivro", is(300))
                .body("porArea.find { it.area == 'Filosofia' }.cursos", is(1))
                .body("porArea.find { it.area == 'Filosofia' }.livros", is(1));
    }

    @Test
    void curso_com_registro_nao_e_excluido() {
        int curso = criarCurso(Map.of("titulo", "Curso ocupado", "cargaHoraria", 5));
        estudo(LocalDate.now(br.dev.registro.comum.Relogio.ZONA).toString(), curso, 30, null, "X");

        given().when().delete("/api/cursos/" + curso)
                .then().statusCode(422)
                .body("detail", containsString("registro(s) vinculado(s)"));
    }

    @Test
    void cursando_aparece_antes_de_concluido() {
        criarCurso(Map.of("titulo", "Zzz cursando", "cargaHoraria", 5));
        given().when().get("/api/cursos")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].status", is("CURSANDO"));
    }

    @Test
    void conquistas_de_curso_e_pratica_aparecem_no_catalogo() {
        given().when().get("/api/gamificacao/conquistas")
                .then().statusCode(200)
                .body("codigo", org.hamcrest.Matchers.hasItems("PRIMEIRO_CURSO", "CEM_HORAS_PRATICA"));
    }

    private static void assertThat(Map<String, Object> progresso, String campo, Object esperado) {
        org.assertj.core.api.Assertions.assertThat(progresso.get(campo)).as(campo).isEqualTo(esperado);
    }
}
