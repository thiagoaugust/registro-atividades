package br.dev.registro.comum;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Corpo de erro no padrao RFC 7807. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        List<ErroCampo> errors) {

    public static final String MEDIA_TYPE = "application/problem+json";

    public ProblemDetail(String title, int status, String detail, String instance) {
        this("about:blank", title, status, detail, instance, null);
    }

    public record ErroCampo(String campo, String mensagem) {
    }
}
