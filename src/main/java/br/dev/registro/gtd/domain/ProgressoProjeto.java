package br.dev.registro.gtd.domain;

import br.dev.registro.atividades.domain.StatusProjeto;

/**
 * Quanto de um projeto ja foi feito e o que falta. As tarefas sao as acoes do projeto, sem as
 * descartadas: o que saiu do escopo nao e o que falta (docs/PLANO.md 5.9).
 */
public record ProgressoProjeto(
        long projetoId,
        String titulo,
        String resultadoDesejado,
        StatusProjeto status,
        int tarefas,
        int concluidas,
        int faltam,
        /** Nulo sem tarefa: projeto recem criado nao esta 0% feito, so ainda nao foi planejado. */
        Double percentual) {

    public static ProgressoProjeto de(
            long projetoId, String titulo, String resultadoDesejado, StatusProjeto status,
            int tarefas, int concluidas) {
        Double percentual = tarefas == 0
                ? null
                : Math.round(concluidas * 1000.0 / tarefas) / 10.0;
        return new ProgressoProjeto(
                projetoId, titulo, resultadoDesejado, status,
                tarefas, concluidas, tarefas - concluidas, percentual);
    }
}
