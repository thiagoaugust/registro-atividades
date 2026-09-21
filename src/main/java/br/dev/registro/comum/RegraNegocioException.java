package br.dev.registro.comum;

public class RegraNegocioException extends DominioException {

    public RegraNegocioException(String mensagem) {
        super(422, "Regra de negocio violada", mensagem);
    }
}
