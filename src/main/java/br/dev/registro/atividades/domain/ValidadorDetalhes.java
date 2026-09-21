package br.dev.registro.atividades.domain;

import br.dev.registro.comum.RegraNegocioException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Valida o JSONB `detalhes` conforme a categoria do registro. Funcao pura: sem CDI, sem banco,
 * testavel direto com JUnit.
 *
 * <p>Chaves desconhecidas sao recusadas de proposito — um typo em "distanciaKm" viraria um campo
 * fantasma que nenhum grafico encontraria depois.
 */
public final class ValidadorDetalhes {

    private static final Map<Categoria, Set<String>> CHAVES = Map.of(
            Categoria.TREINO, Set.of("modalidade", "distanciaKm", "paceSegPorKm", "series", "fcMedia"),
            Categoria.ESTUDO, Set.of("tema", "fonte", "tecnica", "foco"),
            Categoria.LEITURA, Set.of("paginaInicial", "paginaFinal"),
            Categoria.DESAFIO, Set.of("valorProgresso"),
            Categoria.PROJETO, Set.of("marco", "statusApos"));

    private static final Set<String> TECNICAS_ESTUDO =
            Set.of("LEITURA", "EXERCICIO", "FLASHCARD", "PROJETO_PRATICO");

    private ValidadorDetalhes() {
    }

    public static void validar(RegistroAtividade registro) {
        Map<String, Object> d = registro.detalhes == null ? Map.of() : registro.detalhes;
        List<String> erros = new ArrayList<>();

        for (String chave : d.keySet()) {
            if (!CHAVES.get(registro.categoria).contains(chave)) {
                erros.add("detalhes.%s nao e um campo de %s".formatted(chave, registro.categoria));
            }
        }

        switch (registro.categoria) {
            case TREINO -> validarTreino(d, erros);
            case ESTUDO -> validarEstudo(d, erros);
            case LEITURA -> validarLeitura(registro, d, erros);
            case DESAFIO -> validarDesafio(registro, d, erros);
            case PROJETO -> validarProjeto(d, erros);
        }

        if (!erros.isEmpty()) {
            throw new RegraNegocioException(String.join("; ", erros));
        }
    }

    private static void validarTreino(Map<String, Object> d, List<String> erros) {
        exigirTextoSePresente(d, "modalidade", erros);
        positivo(d, "distanciaKm", erros);
        inteiroPositivo(d, "paceSegPorKm", erros);
        intervaloInteiro(d, "fcMedia", 20, 250, erros);

        Object series = d.get("series");
        if (series != null) {
            if (!(series instanceof List<?> lista)) {
                erros.add("detalhes.series deve ser uma lista");
            } else {
                for (int i = 0; i < lista.size(); i++) {
                    if (!(lista.get(i) instanceof Map<?, ?> serie)) {
                        erros.add("detalhes.series[%d] deve ser um objeto".formatted(i));
                    } else if (serie.get("exercicio") == null) {
                        erros.add("detalhes.series[%d].exercicio e obrigatorio".formatted(i));
                    }
                }
            }
        }
    }

    private static void validarEstudo(Map<String, Object> d, List<String> erros) {
        exigirTextoSePresente(d, "tema", erros);
        exigirTextoSePresente(d, "fonte", erros);
        Object tecnica = d.get("tecnica");
        if (tecnica != null && !TECNICAS_ESTUDO.contains(String.valueOf(tecnica))) {
            erros.add("detalhes.tecnica deve ser uma de " + TECNICAS_ESTUDO);
        }
        intervaloInteiro(d, "foco", 1, 5, erros);
    }

    private static void validarLeitura(RegistroAtividade r, Map<String, Object> d, List<String> erros) {
        Integer inicial = inteiroPositivo(d, "paginaInicial", erros);
        Integer fin = inteiroPositivo(d, "paginaFinal", erros);

        // Sem livro, a causa e essa — apontar o par de paginas mandaria consertar a coisa errada.
        // (Com livro, o servico ja derivou a pagina inicial a partir de onde a leitura parou.)
        if ((inicial != null || fin != null) && r.livro == null) {
            erros.add("registro com paginas precisa de um livro vinculado");
            return;
        }
        if ((inicial == null) != (fin == null)) {
            erros.add("detalhes.paginaInicial e detalhes.paginaFinal andam juntas");
        } else if (inicial != null && fin < inicial) {
            erros.add("detalhes.paginaFinal nao pode ser menor que paginaInicial");
        }
        if (r.livro != null && r.livro.totalPaginas != null && fin != null && fin > r.livro.totalPaginas) {
            erros.add("detalhes.paginaFinal passa do total de paginas do livro");
        }
    }

    private static void validarDesafio(RegistroAtividade r, Map<String, Object> d, List<String> erros) {
        BigDecimal progresso = positivo(d, "valorProgresso", erros);
        if (progresso != null && r.desafio == null) {
            erros.add("registro com valorProgresso precisa de um desafio vinculado");
        }
    }

    private static void validarProjeto(Map<String, Object> d, List<String> erros) {
        exigirTextoSePresente(d, "marco", erros);
        exigirTextoSePresente(d, "statusApos", erros);
    }

    private static void exigirTextoSePresente(Map<String, Object> d, String chave, List<String> erros) {
        Object v = d.get(chave);
        if (v != null && String.valueOf(v).isBlank()) {
            erros.add("detalhes.%s nao pode ser vazio".formatted(chave));
        }
    }

    private static BigDecimal positivo(Map<String, Object> d, String chave, List<String> erros) {
        Object v = d.get(chave);
        if (v == null) {
            return null;
        }
        if (!(v instanceof Number n)) {
            erros.add("detalhes.%s deve ser numerico".formatted(chave));
            return null;
        }
        BigDecimal valor = new BigDecimal(n.toString());
        if (valor.signum() <= 0) {
            erros.add("detalhes.%s deve ser maior que zero".formatted(chave));
            return null;
        }
        return valor;
    }

    private static Integer inteiroPositivo(Map<String, Object> d, String chave, List<String> erros) {
        BigDecimal valor = positivo(d, chave, erros);
        if (valor == null) {
            return null;
        }
        if (valor.stripTrailingZeros().scale() > 0) {
            erros.add("detalhes.%s deve ser inteiro".formatted(chave));
            return null;
        }
        return valor.intValue();
    }

    private static void intervaloInteiro(
            Map<String, Object> d, String chave, int min, int max, List<String> erros) {
        Object v = d.get(chave);
        if (v == null) {
            return;
        }
        if (!(v instanceof Number n)) {
            erros.add("detalhes.%s deve ser numerico".formatted(chave));
            return;
        }
        int valor = n.intValue();
        if (valor < min || valor > max) {
            erros.add("detalhes.%s deve estar entre %d e %d".formatted(chave, min, max));
        }
    }
}
