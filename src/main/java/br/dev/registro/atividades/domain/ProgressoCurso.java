package br.dev.registro.atividades.domain;

import java.time.LocalDate;

/**
 * Progresso de um curso: quanto da carga horaria ja foi cumprido, em que ritmo, e quanto desse tempo
 * foi pratica deliberada em vez de consumo passivo. Funcao pura sobre os agregados do banco.
 *
 * <p>A fracao de pratica e o numero que diferencia estudar de assistir: duas pessoas com as mesmas
 * 40 horas num curso chegam a lugares diferentes se uma passou 30 delas fazendo exercicio.
 */
public record ProgressoCurso(
        long cursoId,
        String titulo,
        String instituicao,
        String url,
        Double cargaHoraria,
        Long areaId,
        String area,
        StatusCurso status,
        LocalDate concluidoEm,
        boolean retroativo,
        int minutos,
        int minutosPratica,
        Double percentualConcluido,
        Double horasRestantes,
        Double percentualPratica,
        int sessoes,
        Double esforcoMedio,
        Double horasPorSemana,
        Integer diasRestantes,
        LocalDate previsaoTermino,
        LocalDate primeiraSessao,
        LocalDate ultimaSessao) {

    /** Janela do ritmo, em dias. Mesma ideia do livro: reflete a fase atual sem ignorar as folgas. */
    public static final int JANELA_RITMO_DIAS = 28;

    public record Agregado(
            long cursoId,
            String titulo,
            String instituicao,
            String url,
            Double cargaHoraria,
            Long areaId,
            String area,
            StatusCurso status,
            LocalDate concluidoEm,
            Double horasRetroativas,
            Integer diasRetroativos,
            int minutos,
            int minutosPratica,
            int sessoes,
            Double esforcoMedio,
            LocalDate primeiraSessao,
            LocalDate ultimaSessao,
            int minutosNaJanela) {
    }

    public static ProgressoCurso de(Agregado a, LocalDate hoje) {
        if (a.horasRetroativas() != null) {
            return retroativo(a);
        }

        Double percentual = a.cargaHoraria() == null || a.cargaHoraria() == 0
                ? null
                : arredondar(Math.min(100.0, a.minutos() / 60.0 / a.cargaHoraria() * 100), 1);
        Double restantes = a.cargaHoraria() == null
                ? null
                : arredondar(Math.max(0, a.cargaHoraria() - a.minutos() / 60.0), 1);

        Double horasPorSemana = ritmoSemanal(a);

        Integer diasRestantes = null;
        LocalDate previsao = null;
        if (restantes != null && restantes > 0 && horasPorSemana != null && horasPorSemana > 0) {
            diasRestantes = (int) Math.ceil(restantes / horasPorSemana * 7);
            previsao = hoje.plusDays(diasRestantes);
        }

        return new ProgressoCurso(
                a.cursoId(),
                a.titulo(),
                a.instituicao(),
                a.url(),
                a.cargaHoraria(),
                a.areaId(),
                a.area(),
                a.status(),
                a.concluidoEm(),
                false,
                a.minutos(),
                a.minutosPratica(),
                percentual,
                restantes,
                fracaoDePratica(a.minutos(), a.minutosPratica()),
                a.sessoes(),
                a.esforcoMedio() == null ? null : arredondar(a.esforcoMedio(), 1),
                horasPorSemana,
                diasRestantes,
                previsao,
                a.primeiraSessao(),
                a.ultimaSessao());
    }

    /** Curso feito antes de o sistema existir: conta na estante e nas areas, sem sessao registrada. */
    private static ProgressoCurso retroativo(Agregado a) {
        int minutos = (int) Math.round(a.horasRetroativas() * 60);
        Double horasPorSemana = a.diasRetroativos() == null || a.diasRetroativos() == 0
                ? null
                : arredondar(a.horasRetroativas() / a.diasRetroativos() * 7, 2);

        return new ProgressoCurso(
                a.cursoId(),
                a.titulo(),
                a.instituicao(),
                a.url(),
                a.cargaHoraria(),
                a.areaId(),
                a.area(),
                a.status(),
                a.concluidoEm(),
                true,
                minutos,
                0,
                a.cargaHoraria() == null ? null : 100.0,
                a.cargaHoraria() == null ? null : 0.0,
                null,
                0,
                null,
                horasPorSemana,
                null,
                null,
                null,
                a.concluidoEm());
    }

    /**
     * Horas por semana na janela recente. Divide pelos dias corridos e nao pelos dias estudados:
     * quem faz 4 horas num sabado e para a semana avanca 4 por semana, nao 4 por dia.
     */
    private static Double ritmoSemanal(Agregado a) {
        if (a.minutosNaJanela() > 0) {
            return arredondar(a.minutosNaJanela() / 60.0 / JANELA_RITMO_DIAS * 7, 2);
        }
        if (a.primeiraSessao() == null || a.minutos() == 0) {
            return null;
        }
        long dias = Math.max(
                1, java.time.temporal.ChronoUnit.DAYS.between(a.primeiraSessao(), a.ultimaSessao()) + 1);
        return arredondar(a.minutos() / 60.0 / dias * 7, 2);
    }

    /** Sem tempo registrado nao ha fracao — 0% e uma afirmacao, "sem dados" e outra. */
    private static Double fracaoDePratica(int minutos, int minutosPratica) {
        if (minutos <= 0) {
            return null;
        }
        return arredondar(minutosPratica * 100.0 / minutos, 1);
    }

    private static double arredondar(double valor, int casas) {
        double fator = Math.pow(10, casas);
        return Math.round(valor * fator) / fator;
    }
}
