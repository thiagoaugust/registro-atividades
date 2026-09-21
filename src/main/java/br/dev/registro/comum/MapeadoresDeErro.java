package br.dev.registro.comum;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Traducao centralizada de excecoes para RFC 7807. Resources nunca montam Response de erro na mao.
 */
public final class MapeadoresDeErro {

    private static final Logger LOG = Logger.getLogger(MapeadoresDeErro.class);

    private MapeadoresDeErro() {
    }

    private static Response resposta(ProblemDetail problema) {
        return Response.status(problema.status())
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problema)
                .build();
    }

    @Provider
    public static class Dominio implements ExceptionMapper<DominioException> {

        @Context
        UriInfo uriInfo;

        @Override
        public Response toResponse(DominioException e) {
            return resposta(new ProblemDetail(
                    e.titulo(), e.status(), e.getMessage(), uriInfo.getPath()));
        }
    }

    /** Bean Validation: 400 com a lista de campos recusados. */
    @Provider
    public static class Validacao implements ExceptionMapper<ConstraintViolationException> {

        @Context
        UriInfo uriInfo;

        @Override
        public Response toResponse(ConstraintViolationException e) {
            List<ProblemDetail.ErroCampo> campos = e.getConstraintViolations().stream()
                    .map(v -> new ProblemDetail.ErroCampo(ultimoNo(v.getPropertyPath().toString()), v.getMessage()))
                    .toList();
            return resposta(new ProblemDetail(
                    "about:blank",
                    "Dados invalidos",
                    400,
                    "A requisicao tem %d campo(s) invalido(s)".formatted(campos.size()),
                    uriInfo.getPath(),
                    campos));
        }

        private static String ultimoNo(String caminho) {
            int i = caminho.lastIndexOf('.');
            return i < 0 ? caminho : caminho.substring(i + 1);
        }
    }

    /** Rede de seguranca: nada de stacktrace vazando para o cliente. */
    @Provider
    public static class NaoTratada implements ExceptionMapper<Throwable> {

        @Context
        UriInfo uriInfo;

        @Override
        public Response toResponse(Throwable e) {
            if (e instanceof jakarta.ws.rs.WebApplicationException web) {
                return resposta(new ProblemDetail(
                        web.getResponse().getStatusInfo().getReasonPhrase(),
                        web.getResponse().getStatus(),
                        web.getMessage(),
                        uriInfo.getPath()));
            }
            LOG.error("Erro nao tratado em " + uriInfo.getPath(), e);
            return resposta(new ProblemDetail(
                    "Erro interno", 500, "Erro inesperado ao processar a requisicao", uriInfo.getPath()));
        }
    }
}
