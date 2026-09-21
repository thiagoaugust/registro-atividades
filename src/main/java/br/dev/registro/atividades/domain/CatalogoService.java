package br.dev.registro.atividades.domain;

import br.dev.registro.atividades.infra.CategoriaLivroRepository;
import br.dev.registro.atividades.infra.DesafioRepository;
import br.dev.registro.atividades.infra.LivroRepository;
import br.dev.registro.atividades.infra.ProgressoLeituraRepository;
import br.dev.registro.atividades.infra.ProjetoRepository;
import br.dev.registro.atividades.infra.RegistroRepository;
import br.dev.registro.comum.RecursoNaoEncontradoException;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.comum.Relogio;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CRUD das entidades que os registros referenciam. Sao tres cadastros rasos; um servico so evita
 * espalhar o mesmo codigo por tres classes iguais.
 */
@ApplicationScoped
public class CatalogoService {

    private final ProjetoRepository projetos;
    private final DesafioRepository desafios;
    private final LivroRepository livros;
    private final RegistroRepository registros;
    private final ProgressoLeituraRepository progressos;
    private final CategoriaLivroRepository categoriasLivro;
    private final Relogio relogio;

    public CatalogoService(
            ProjetoRepository projetos,
            DesafioRepository desafios,
            LivroRepository livros,
            RegistroRepository registros,
            ProgressoLeituraRepository progressos,
            CategoriaLivroRepository categoriasLivro,
            Relogio relogio) {
        this.projetos = projetos;
        this.desafios = desafios;
        this.livros = livros;
        this.registros = registros;
        this.progressos = progressos;
        this.categoriasLivro = categoriasLivro;
        this.relogio = relogio;
    }

    public record DadosProjeto(
            @NotBlank @Size(max = 200) String titulo,
            @Size(max = 2000) String resultadoDesejado,
            StatusProjeto status) {
    }

    public record DadosDesafio(
            @NotBlank @Size(max = 200) String titulo,
            @NotNull @Positive BigDecimal metaValor,
            @NotBlank @Size(max = 30) String unidade,
            LocalDate inicio,
            LocalDate fim,
            StatusDesafio status) {
    }

    public record DadosLivro(
            @NotBlank @Size(max = 300) String titulo,
            @Size(max = 200) String autor,
            @Positive Integer totalPaginas,
            StatusLivro status,
            @Size(max = 1000) String capaUrl,
            @Min(1) @Max(5) Short dificuldade,
            Long categoriaId,
            /** Preenchidos so para leitura retroativa: livro lido antes de existir registro. */
            @Positive Integer diasLeitura,
            @Positive BigDecimal horasLeitura,
            LocalDate concluidoEm) {
    }

    public record DadosCategoriaLivro(
            @NotBlank @Size(max = 60) String nome, Boolean ativa, Integer ordem) {
    }

    // ---------- Projeto ----------

    public List<Projeto> listarProjetos() {
        return projetos.listAll(Sort.by("status").and("titulo"));
    }

    public Projeto projeto(long id) {
        return projetos.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", id));
    }

    @Transactional
    public Projeto criarProjeto(DadosProjeto dados) {
        Projeto p = new Projeto();
        aplicar(dados, p);
        projetos.persist(p);
        return p;
    }

    @Transactional
    public Projeto atualizarProjeto(long id, DadosProjeto dados) {
        Projeto p = projeto(id);
        aplicar(dados, p);
        return p;
    }

    private void aplicar(DadosProjeto dados, Projeto p) {
        p.titulo = dados.titulo();
        p.resultadoDesejado = dados.resultadoDesejado();
        StatusProjeto novo = dados.status() != null ? dados.status() : p.status;
        if (novo == StatusProjeto.CONCLUIDO && p.status != StatusProjeto.CONCLUIDO) {
            p.concluidoEm = relogio.agora();
        } else if (novo != StatusProjeto.CONCLUIDO) {
            p.concluidoEm = null;
        }
        p.status = novo;
    }

    @Transactional
    public void excluirProjeto(long id) {
        exigirSemRegistros("projeto", id);
        projetos.delete(projeto(id));
    }

    // ---------- Desafio ----------

    public List<Desafio> listarDesafios() {
        return desafios.listAll(Sort.by("inicio").descending());
    }

    public Desafio desafio(long id) {
        return desafios.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Desafio", id));
    }

    /** Progresso somado dos registros vinculados — nunca um contador guardado. */
    public BigDecimal progressoDesafio(long id) {
        desafio(id);
        return registros.progressoDoDesafio(id);
    }

    @Transactional
    public Desafio criarDesafio(DadosDesafio dados) {
        Desafio d = new Desafio();
        aplicar(dados, d);
        desafios.persist(d);
        return d;
    }

    @Transactional
    public Desafio atualizarDesafio(long id, DadosDesafio dados) {
        Desafio d = desafio(id);
        aplicar(dados, d);
        return d;
    }

    private void aplicar(DadosDesafio dados, Desafio d) {
        if (dados.fim() != null && dados.inicio() != null && dados.fim().isBefore(dados.inicio())) {
            throw new RegraNegocioException("o fim do desafio e anterior ao inicio");
        }
        d.titulo = dados.titulo();
        d.metaValor = dados.metaValor();
        d.unidade = dados.unidade();
        d.inicio = dados.inicio() != null ? dados.inicio() : relogio.hoje();
        d.fim = dados.fim();
        if (dados.status() != null) {
            d.status = dados.status();
        }
    }

    @Transactional
    public void excluirDesafio(long id) {
        exigirSemRegistros("desafio", id);
        desafios.delete(desafio(id));
    }

    // ---------- Livro ----------

    public List<Livro> listarLivros() {
        return livros.listAll(Sort.by("status").and("titulo"));
    }

    public Livro livro(long id) {
        return livros.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro", id));
    }

    public record EstatisticasLeitura(
            List<ProgressoLeituraRepository.PorCategoria> porCategoria,
            List<ProgressoLeituraRepository.PorDificuldade> porDificuldade) {
    }

    /** Quantos livros por categoria e a que velocidade cada grau de dificuldade e lido. */
    public EstatisticasLeitura estatisticasLeitura() {
        return new EstatisticasLeitura(progressos.porCategoria(), progressos.porDificuldade());
    }

    /** Cada livro com quanto ja foi lido, a que velocidade e quando deve acabar. */
    public List<ProgressoLeitura> progressoLivros() {
        LocalDate hoje = relogio.hoje();
        return progressos.agregados(hoje).stream()
                .map(agregado -> ProgressoLeitura.de(agregado, hoje))
                .toList();
    }

    @Transactional
    public Livro criarLivro(DadosLivro dados) {
        Livro l = new Livro();
        aplicar(dados, l);
        livros.persist(l);
        return l;
    }

    @Transactional
    public Livro atualizarLivro(long id, DadosLivro dados) {
        Livro l = livro(id);
        aplicar(dados, l);
        return l;
    }

    private void aplicar(DadosLivro dados, Livro l) {
        l.titulo = dados.titulo();
        l.autor = dados.autor();
        l.totalPaginas = dados.totalPaginas();
        l.capaUrl = textoOuNulo(dados.capaUrl());
        l.dificuldade = dados.dificuldade();
        l.diasLeitura = dados.diasLeitura();
        l.horasLeitura = dados.horasLeitura();

        l.categoria = dados.categoriaId() == null
                ? null
                : categoriasLivro.findByIdOptional(dados.categoriaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException(
                                "Categoria de livro", dados.categoriaId()));

        // Livro retroativo ja nasce lido: quem informa quantos dias levou esta cadastrando historico.
        StatusLivro novo = dados.status() != null
                ? dados.status()
                : (l.diasLeitura != null ? StatusLivro.CONCLUIDO : l.status);

        if (novo == StatusLivro.CONCLUIDO) {
            if (dados.concluidoEm() != null) {
                l.concluidoEm = dados.concluidoEm();
            } else if (l.concluidoEm == null) {
                l.concluidoEm = relogio.hoje();
            }
            if (l.concluidoEm.isAfter(relogio.hoje())) {
                throw new RegraNegocioException("a data de conclusao nao pode estar no futuro");
            }
        } else {
            l.concluidoEm = null;
        }
        l.status = novo;
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    // ---------- Categorias de livro ----------

    public List<CategoriaLivro> listarCategoriasLivro(boolean apenasAtivas) {
        return apenasAtivas
                ? categoriasLivro.ativas()
                : categoriasLivro.listAll(Sort.by("ordem").and("nome"));
    }

    @Transactional
    public CategoriaLivro criarCategoriaLivro(DadosCategoriaLivro dados) {
        CategoriaLivro categoria = new CategoriaLivro();
        aplicar(dados, categoria);
        categoriasLivro.persist(categoria);
        return categoria;
    }

    @Transactional
    public CategoriaLivro atualizarCategoriaLivro(long id, DadosCategoriaLivro dados) {
        CategoriaLivro categoria = categoriasLivro.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria de livro", id));
        aplicar(dados, categoria);
        return categoria;
    }

    private void aplicar(DadosCategoriaLivro dados, CategoriaLivro categoria) {
        categoria.nome = dados.nome().trim();
        if (dados.ativa() != null) {
            categoria.ativa = dados.ativa();
        }
        if (dados.ordem() != null) {
            categoria.ordem = dados.ordem();
        }
    }

    /** Categoria em uso e desativada, nao apagada: os livros existentes perderiam a classificacao. */
    @Transactional
    public void excluirCategoriaLivro(long id) {
        CategoriaLivro categoria = categoriasLivro.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria de livro", id));
        long emUso = livros.count("categoria.id = ?1", id);
        if (emUso > 0) {
            throw new RegraNegocioException(
                    "a categoria tem %d livro(s); desative em vez de excluir".formatted(emUso));
        }
        categoriasLivro.delete(categoria);
    }

    @Transactional
    public void excluirLivro(long id) {
        exigirSemRegistros("livro", id);
        livros.delete(livro(id));
    }

    /** Mensagem clara em vez de deixar estourar a violacao de chave estrangeira. */
    private void exigirSemRegistros(String campo, long id) {
        long vinculados = registros.count(campo + ".id = ?1", id);
        if (vinculados > 0) {
            throw new RegraNegocioException(
                    "o %s tem %d registro(s) vinculado(s); exclua ou desvincule antes"
                            .formatted(campo, vinculados));
        }
    }
}
