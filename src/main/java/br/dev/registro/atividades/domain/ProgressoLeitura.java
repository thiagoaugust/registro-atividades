package br.dev.registro.atividades.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Progresso de um livro: quanto foi lido, a que velocidade e quando deve acabar. Funcao pura sobre
 * os agregados que o repositorio soma — sem banco, sem CDI.
 *
 * <p>A pergunta que o painel responde e "esta fluindo melhor?", e ela so tem resposta comparando a
 * velocidade recente com a media do livro. Por isso as duas andam juntas.
 */
public record ProgressoLeitura(
        long livroId,
        String titulo,
        String autor,
        Integer totalPaginas,
        StatusLivro status,
        LocalDate concluidoEm,
        String capaUrl,
        Short dificuldade,
        Long categoriaId,
        String categoria,
        boolean retroativo,
        /** Repetidos do cadastro para que a tela possa reenvia-los ao editar. */
        Integer diasLeitura,
        Double horasLeitura,
        int ultimaPagina,
        int paginasLidas,
        Integer paginasRestantes,
        Double percentualLido,
        int sessoes,
        int minutos,
        Double esforcoMedio,
        Double paginasPorHoraRecente,
        Double paginasPorHoraMedia,
        Double ritmoDiario,
        Integer diasRestantes,
        LocalDate previsaoTermino,
        LocalDate primeiraSessao,
        LocalDate ultimaSessao) {

    /** Janela do ritmo: curta o bastante para refletir a fase atual, longa para absorver folgas. */
    public static final int JANELA_RITMO_DIAS = 14;

    /** Agregados crus de um livro, como o banco entrega. */
    public record Agregado(
            long livroId,
            String titulo,
            String autor,
            Integer totalPaginas,
            StatusLivro status,
            LocalDate concluidoEm,
            String capaUrl,
            Short dificuldade,
            Long categoriaId,
            String categoria,
            Integer diasLeitura,
            Double horasLeitura,
            int ultimaPagina,
            int paginasLidas,
            int sessoes,
            int minutos,
            Double esforcoMedio,
            LocalDate primeiraSessao,
            LocalDate ultimaSessao,
            int minutosComPaginas,
            int paginasRecentes,
            int minutosRecentes,
            int paginasNaJanela) {
    }

    public static ProgressoLeitura de(Agregado a, LocalDate hoje) {
        // Livro retroativo nao tem sessao: o que se sabe dele e que foi lido inteiro, em quantos
        // dias e (as vezes) em quantas horas. As contas saem desses tres numeros.
        if (a.diasLeitura() != null) {
            return retroativo(a);
        }

        Integer restantes = a.totalPaginas() == null
                ? null
                : Math.max(0, a.totalPaginas() - a.ultimaPagina());
        Double percentual = a.totalPaginas() == null || a.totalPaginas() == 0
                ? null
                : arredondar(Math.min(100.0, a.ultimaPagina() * 100.0 / a.totalPaginas()), 1);

        Double velocidadeMedia = paginasPorHora(a.paginasLidas(), a.minutosComPaginas());
        Double velocidadeRecente = paginasPorHora(a.paginasRecentes(), a.minutosRecentes());
        Double ritmo = ritmoDiario(a, hoje);

        Integer diasRestantes = null;
        LocalDate previsao = null;
        // Livro terminado nao tem previsao, e sem ritmo nao se inventa data: a tela mostra um traco.
        if (restantes != null && restantes > 0 && ritmo != null && ritmo > 0) {
            diasRestantes = (int) Math.ceil(restantes / ritmo);
            previsao = hoje.plusDays(diasRestantes);
        }

        return new ProgressoLeitura(
                a.livroId(),
                a.titulo(),
                a.autor(),
                a.totalPaginas(),
                a.status(),
                a.concluidoEm(),
                a.capaUrl(),
                a.dificuldade(),
                a.categoriaId(),
                a.categoria(),
                false,
                null,
                null,
                a.ultimaPagina(),
                a.paginasLidas(),
                restantes,
                percentual,
                a.sessoes(),
                a.minutos(),
                a.esforcoMedio() == null ? null : arredondar(a.esforcoMedio(), 1),
                velocidadeRecente,
                velocidadeMedia,
                ritmo,
                diasRestantes,
                previsao,
                a.primeiraSessao(),
                a.ultimaSessao());
    }

    /**
     * Livro lido antes de o sistema existir. Ele conta na estante e nas metricas por categoria; a
     * velocidade so aparece quando as horas foram lembradas, e previsao nao existe — ja acabou.
     */
    private static ProgressoLeitura retroativo(Agregado a) {
        int paginas = a.totalPaginas() == null ? 0 : a.totalPaginas();
        int minutos = a.horasLeitura() == null ? 0 : (int) Math.round(a.horasLeitura() * 60);

        return new ProgressoLeitura(
                a.livroId(),
                a.titulo(),
                a.autor(),
                a.totalPaginas(),
                a.status(),
                a.concluidoEm(),
                a.capaUrl(),
                a.dificuldade(),
                a.categoriaId(),
                a.categoria(),
                true,
                a.diasLeitura(),
                a.horasLeitura(),
                paginas,
                paginas,
                a.totalPaginas() == null ? null : 0,
                a.totalPaginas() == null ? null : 100.0,
                0,
                minutos,
                null,
                null,
                paginasPorHora(paginas, minutos),
                paginas > 0 ? arredondar(paginas / (double) a.diasLeitura(), 2) : null,
                null,
                null,
                null,
                a.concluidoEm());
    }

    /**
     * Ritmo em paginas por dia. A janela recente divide pelos dias corridos, nao pelos dias com
     * leitura: quem le 40 paginas num domingo e para a semana toda avanca 40 por semana, nao 40 por
     * dia — e a previsao precisa contar as folgas.
     */
    private static Double ritmoDiario(Agregado a, LocalDate hoje) {
        if (a.paginasNaJanela() > 0) {
            return arredondar(a.paginasNaJanela() / (double) JANELA_RITMO_DIAS, 2);
        }
        // Sem leitura recente, o ritmo historico do livro ainda diz alguma coisa.
        if (a.primeiraSessao() == null || a.paginasLidas() == 0) {
            return null;
        }
        long dias = Math.max(1, ChronoUnit.DAYS.between(a.primeiraSessao(), hoje) + 1);
        return arredondar(a.paginasLidas() / (double) dias, 2);
    }

    private static Double paginasPorHora(int paginas, int minutos) {
        if (paginas <= 0 || minutos <= 0) {
            return null;
        }
        return arredondar(paginas * 60.0 / minutos, 1);
    }

    /** Positivo quando as ultimas sessoes renderam mais que a media do livro. */
    public Double variacaoDeVelocidade() {
        if (paginasPorHoraRecente == null || paginasPorHoraMedia == null || paginasPorHoraMedia == 0) {
            return null;
        }
        return arredondar((paginasPorHoraRecente - paginasPorHoraMedia) / paginasPorHoraMedia * 100, 1);
    }

    private static double arredondar(double valor, int casas) {
        double fator = Math.pow(10, casas);
        return Math.round(valor * fator) / fator;
    }
}
