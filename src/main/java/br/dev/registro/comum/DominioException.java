package br.dev.registro.comum;

/** Base das excecoes de negocio. Cada subclasse carrega o status HTTP que o mapper deve usar. */
public abstract class DominioException extends RuntimeException {

    private final int status;
    private final String titulo;

    protected DominioException(int status, String titulo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.titulo = titulo;
    }

    public int status() {
        return status;
    }

    public String titulo() {
        return titulo;
    }
}
