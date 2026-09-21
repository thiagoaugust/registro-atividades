package br.dev.registro.comum;

public class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(404, "Recurso nao encontrado", "%s %s nao existe".formatted(recurso, id));
    }
}
