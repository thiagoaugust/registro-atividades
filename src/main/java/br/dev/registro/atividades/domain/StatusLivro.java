package br.dev.registro.atividades.domain;

public enum StatusLivro {
    LENDO,
    CONCLUIDO,
    /** Na fila: ainda sem sessao. A primeira leitura registrada passa o livro para LENDO. */
    QUERO_LER,
    ABANDONADO
}
