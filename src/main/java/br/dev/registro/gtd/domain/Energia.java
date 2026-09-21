package br.dev.registro.gtd.domain;

public enum Energia {
    BAIXA,
    MEDIA,
    ALTA;

    /** Com energia media da para fazer o que exige media ou baixa, nao o que exige alta. */
    public boolean cabeEm(Energia disponivel) {
        return ordinal() <= disponivel.ordinal();
    }
}
