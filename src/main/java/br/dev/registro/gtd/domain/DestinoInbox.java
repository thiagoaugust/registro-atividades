package br.dev.registro.gtd.domain;

/** O que o esclarecimento decidiu sobre um item do inbox. */
public enum DestinoInbox {
    LIXO,
    ALGUM_DIA,
    REFERENCIA,
    ACAO,
    PROJETO,
    /** Levava menos de dois minutos: foi feito na hora. */
    FEITO_2MIN
}
