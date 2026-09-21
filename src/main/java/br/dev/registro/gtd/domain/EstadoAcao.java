package br.dev.registro.gtd.domain;

/** As listas do GTD sao estados de uma acao, nao tabelas diferentes. */
public enum EstadoAcao {
    /** Proximas acoes, filtradas por contexto. */
    PROXIMA,
    /** Tem data ou hora marcada. */
    AGENDA,
    /** Delegada: esperando outra pessoa. */
    AGUARDANDO,
    /** Incubada. */
    ALGUM_DIA,
    CONCLUIDA,
    DESCARTADA
}
