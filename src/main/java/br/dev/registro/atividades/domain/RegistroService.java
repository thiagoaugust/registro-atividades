package br.dev.registro.atividades.domain;

import br.dev.registro.atividades.infra.DesafioRepository;
import br.dev.registro.atividades.infra.LivroRepository;
import br.dev.registro.atividades.infra.ProgressoLeituraRepository;
import br.dev.registro.atividades.infra.ProjetoRepository;
import br.dev.registro.atividades.infra.RegistroRepository;
import br.dev.registro.comum.RecursoNaoEncontradoException;
import br.dev.registro.comum.RegistroAlterado;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.comum.Relogio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

@ApplicationScoped
public class RegistroService {

    private final RegistroRepository registros;
    private final ProjetoRepository projetos;
    private final DesafioRepository desafios;
    private final LivroRepository livros;
    private final ProgressoLeituraRepository progressos;
    private final Relogio relogio;
    private final Event<RegistroAlterado> registroAlterado;

    public RegistroService(
            RegistroRepository registros,
            ProjetoRepository projetos,
            DesafioRepository desafios,
            LivroRepository livros,
            ProgressoLeituraRepository progressos,
            Relogio relogio,
            Event<RegistroAlterado> registroAlterado) {
        this.registros = registros;
        this.projetos = projetos;
        this.desafios = desafios;
        this.livros = livros;
        this.progressos = progressos;
        this.relogio = relogio;
        this.registroAlterado = registroAlterado;
    }

    public RegistroAtividade porId(long id) {
        return registros.porIdComVinculos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Registro", id));
    }

    public List<RegistroAtividade> buscar(LocalDate de, LocalDate ate, Categoria categoria) {
        LocalDate inicio = de != null ? de : relogio.hoje();
        LocalDate fim = ate != null ? ate : inicio;
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("o fim do periodo e anterior ao inicio");
        }
        return registros.buscar(inicio, fim, categoria);
    }

    @Transactional
    public RegistroAtividade criar(DadosRegistro dados) {
        RegistroAtividade registro = new RegistroAtividade();
        aplicar(dados, registro);
        registros.persist(registro);
        fecharLivroSeTerminou(registro);
        registroAlterado.fire(new RegistroAlterado(registro.id, false));
        return registro;
    }

    @Transactional
    public RegistroAtividade atualizar(long id, DadosRegistro dados) {
        RegistroAtividade registro = porId(id);
        aplicar(dados, registro);
        fecharLivroSeTerminou(registro);
        registroAlterado.fire(new RegistroAlterado(registro.id, false));
        return registro;
    }

    @Transactional
    public void excluir(long id) {
        registros.delete(porId(id));
        registroAlterado.fire(new RegistroAlterado(id, true));
    }

    private void aplicar(DadosRegistro dados, RegistroAtividade registro) {
        registro.dataLocal = dados.dataLocal() != null ? dados.dataLocal() : relogio.hoje();
        registro.inicioEm = dados.inicioEm();
        registro.duracaoMin = dados.duracaoMin();
        registro.categoria = dados.categoria();
        registro.titulo = dados.titulo();
        registro.esforco = dados.esforco();
        registro.satisfacao = dados.satisfacao();
        registro.notas = dados.notas();
        registro.detalhes = dados.detalhes() != null ? new LinkedHashMap<>(dados.detalhes()) : new LinkedHashMap<>();

        registro.projeto = dados.projetoId() == null
                ? null
                : projetos.findByIdOptional(dados.projetoId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", dados.projetoId()));
        registro.desafio = dados.desafioId() == null
                ? null
                : desafios.findByIdOptional(dados.desafioId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Desafio", dados.desafioId()));
        registro.livro = dados.livroId() == null
                ? null
                : livros.findByIdOptional(dados.livroId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Livro", dados.livroId()));

        if (registro.dataLocal.isAfter(relogio.hoje())) {
            throw new RegraNegocioException("nao da para registrar uma atividade no futuro");
        }
        derivarPaginaInicial(registro);
        ValidadorDetalhes.validar(registro);
    }

    /**
     * Registrar leitura pede uma informacao so: em que pagina voce parou. O inicio do trecho e a
     * pagina seguinte a ultima ja registrada daquele livro — quem esta lendo sabe onde parou, nao de
     * onde comecou.
     */
    private void derivarPaginaInicial(RegistroAtividade registro) {
        if (registro.categoria != Categoria.LEITURA || registro.livro == null) {
            return;
        }
        Object paginaFinal = registro.detalhes.get("paginaFinal");
        if (!(paginaFinal instanceof Number fim) || registro.detalhes.containsKey("paginaInicial")) {
            return;
        }

        int ultima = progressos.ultimaPaginaLida(registro.livro.id, registro.id);
        if (fim.intValue() <= ultima) {
            throw new RegraNegocioException(
                    ("voce ja tinha registrado ate a pagina %d desse livro; informe uma pagina maior"
                            + " ou corrija o registro anterior").formatted(ultima));
        }
        registro.detalhes.put("paginaInicial", ultima + 1);
    }

    /** Leitura que chega na ultima pagina fecha o livro — senao o status ficaria eternamente LENDO. */
    private void fecharLivroSeTerminou(RegistroAtividade registro) {
        if (registro.categoria != Categoria.LEITURA || registro.livro == null) {
            return;
        }
        Livro livro = registro.livro;
        Object paginaFinal = registro.detalhes.get("paginaFinal");
        if (livro.totalPaginas == null || !(paginaFinal instanceof Number pagina)) {
            return;
        }
        if (pagina.intValue() >= livro.totalPaginas && livro.status == StatusLivro.LENDO) {
            livro.status = StatusLivro.CONCLUIDO;
            livro.concluidoEm = registro.dataLocal;
        }
    }
}
