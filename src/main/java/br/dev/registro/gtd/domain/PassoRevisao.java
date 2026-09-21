package br.dev.registro.gtd.domain;

/** O checklist da revisao semanal, na ordem em que o metodo manda percorrer. */
public enum PassoRevisao {
    ESVAZIAR_INBOX("Esvaziar o inbox", "Processar tudo que foi capturado na semana"),
    REVISAR_PROXIMAS_ACOES("Revisar proximas acoes", "O que ja nao faz sentido sai da lista"),
    REVISAR_PROJETOS("Revisar projetos", "Todo projeto ativo tem uma proxima acao?"),
    REVISAR_AGUARDANDO("Revisar aguardando", "O que esta parado com outra pessoa ha tempo demais?"),
    REVISAR_AGENDA("Revisar a agenda", "A semana que passou e a que vem"),
    REVISAR_ALGUM_DIA("Revisar algum dia/talvez", "O que amadureceu e virou acao?");

    private final String titulo;
    private final String descricao;

    PassoRevisao(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
    }

    public String titulo() {
        return titulo;
    }

    public String descricao() {
        return descricao;
    }
}
