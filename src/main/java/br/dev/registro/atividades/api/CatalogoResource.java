package br.dev.registro.atividades.api;

import br.dev.registro.atividades.domain.CatalogoService;
import br.dev.registro.atividades.domain.Desafio;
import br.dev.registro.atividades.domain.Livro;
import br.dev.registro.atividades.domain.Projeto;
import br.dev.registro.atividades.domain.StatusDesafio;
import br.dev.registro.atividades.domain.StatusLivro;
import br.dev.registro.atividades.domain.StatusProjeto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Cadastros que os registros referenciam: projeto, desafio e livro. Tres CRUDs rasos num recurso so —
 * tres classes identicas nao pagariam o proprio arquivo.
 */
@Path("/api")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    private final CatalogoService service;

    public CatalogoResource(CatalogoService service) {
        this.service = service;
    }

    public record ProjetoDto(
            Long id, String titulo, String resultadoDesejado, StatusProjeto status,
            Instant criadoEm, Instant concluidoEm) {

        static ProjetoDto de(Projeto p) {
            return new ProjetoDto(p.id, p.titulo, p.resultadoDesejado, p.status, p.criadoEm, p.concluidoEm);
        }
    }

    public record DesafioDto(
            Long id, String titulo, BigDecimal metaValor, String unidade, LocalDate inicio,
            LocalDate fim, StatusDesafio status, BigDecimal progresso) {

        static DesafioDto de(Desafio d, BigDecimal progresso) {
            return new DesafioDto(d.id, d.titulo, d.metaValor, d.unidade, d.inicio, d.fim, d.status, progresso);
        }
    }

    public record LivroDto(
            Long id, String titulo, String autor, Integer totalPaginas, StatusLivro status,
            LocalDate concluidoEm, String capaUrl, Short dificuldade, Long categoriaId,
            String categoria, Integer diasLeitura, BigDecimal horasLeitura) {

        static LivroDto de(Livro l) {
            return new LivroDto(
                    l.id, l.titulo, l.autor, l.totalPaginas, l.status, l.concluidoEm,
                    l.capaUrl, l.dificuldade, l.categoria == null ? null : l.categoria.id,
                    l.categoria == null ? null : l.categoria.nome, l.diasLeitura, l.horasLeitura);
        }
    }

    public record CategoriaLivroDto(Long id, String nome, boolean ativa, int ordem) {

        static CategoriaLivroDto de(br.dev.registro.atividades.domain.CategoriaLivro c) {
            return new CategoriaLivroDto(c.id, c.nome, c.ativa, c.ordem);
        }
    }

    // ---------- Projetos ----------

    @GET
    @Path("projetos")
    public List<ProjetoDto> listarProjetos() {
        return service.listarProjetos().stream().map(ProjetoDto::de).toList();
    }

    @GET
    @Path("projetos/{id}")
    public ProjetoDto projeto(@PathParam("id") long id) {
        return ProjetoDto.de(service.projeto(id));
    }

    @POST
    @Path("projetos")
    public Response criarProjeto(@Valid CatalogoService.DadosProjeto dados) {
        ProjetoDto criado = ProjetoDto.de(service.criarProjeto(dados));
        return Response.created(java.net.URI.create("/api/projetos/" + criado.id())).entity(criado).build();
    }

    @PUT
    @Path("projetos/{id}")
    public ProjetoDto atualizarProjeto(@PathParam("id") long id, @Valid CatalogoService.DadosProjeto dados) {
        return ProjetoDto.de(service.atualizarProjeto(id, dados));
    }

    @DELETE
    @Path("projetos/{id}")
    public Response excluirProjeto(@PathParam("id") long id) {
        service.excluirProjeto(id);
        return Response.noContent().build();
    }

    // ---------- Desafios ----------

    @GET
    @Path("desafios")
    public List<DesafioDto> listarDesafios() {
        return service.listarDesafios().stream()
                .map(d -> DesafioDto.de(d, service.progressoDesafio(d.id)))
                .toList();
    }

    @GET
    @Path("desafios/{id}")
    public DesafioDto desafio(@PathParam("id") long id) {
        return DesafioDto.de(service.desafio(id), service.progressoDesafio(id));
    }

    @POST
    @Path("desafios")
    public Response criarDesafio(@Valid CatalogoService.DadosDesafio dados) {
        DesafioDto criado = DesafioDto.de(service.criarDesafio(dados), BigDecimal.ZERO);
        return Response.created(java.net.URI.create("/api/desafios/" + criado.id())).entity(criado).build();
    }

    @PUT
    @Path("desafios/{id}")
    public DesafioDto atualizarDesafio(@PathParam("id") long id, @Valid CatalogoService.DadosDesafio dados) {
        return DesafioDto.de(service.atualizarDesafio(id, dados), service.progressoDesafio(id));
    }

    @DELETE
    @Path("desafios/{id}")
    public Response excluirDesafio(@PathParam("id") long id) {
        service.excluirDesafio(id);
        return Response.noContent().build();
    }

    // ---------- Livros ----------

    /**
     * Livros com progresso, velocidade e previsao. E a tela de acompanhamento; /api/livros continua
     * servindo a lista enxuta que o formulario de registro precisa.
     */
    @GET
    @Path("livros/progresso")
    public List<br.dev.registro.atividades.domain.ProgressoLeitura> progressoLivros() {
        return service.progressoLivros();
    }

    @GET
    @Path("livros/estatisticas")
    public CatalogoService.EstatisticasLeitura estatisticasLeitura() {
        return service.estatisticasLeitura();
    }

    @GET
    @Path("categorias-livro")
    public List<CategoriaLivroDto> listarCategoriasLivro(@QueryParam("todas") boolean todas) {
        return service.listarCategoriasLivro(!todas).stream().map(CategoriaLivroDto::de).toList();
    }

    @POST
    @Path("categorias-livro")
    public Response criarCategoriaLivro(@Valid CatalogoService.DadosCategoriaLivro dados) {
        CategoriaLivroDto criada = CategoriaLivroDto.de(service.criarCategoriaLivro(dados));
        return Response.created(java.net.URI.create("/api/categorias-livro/" + criada.id()))
                .entity(criada)
                .build();
    }

    @PUT
    @Path("categorias-livro/{id}")
    public CategoriaLivroDto atualizarCategoriaLivro(
            @PathParam("id") long id, @Valid CatalogoService.DadosCategoriaLivro dados) {
        return CategoriaLivroDto.de(service.atualizarCategoriaLivro(id, dados));
    }

    @DELETE
    @Path("categorias-livro/{id}")
    public Response excluirCategoriaLivro(@PathParam("id") long id) {
        service.excluirCategoriaLivro(id);
        return Response.noContent().build();
    }

    @GET
    @Path("livros")
    public List<LivroDto> listarLivros() {
        return service.listarLivros().stream().map(LivroDto::de).toList();
    }

    @GET
    @Path("livros/{id}")
    public LivroDto livro(@PathParam("id") long id) {
        return LivroDto.de(service.livro(id));
    }

    @POST
    @Path("livros")
    public Response criarLivro(@Valid CatalogoService.DadosLivro dados) {
        LivroDto criado = LivroDto.de(service.criarLivro(dados));
        return Response.created(java.net.URI.create("/api/livros/" + criado.id())).entity(criado).build();
    }

    @PUT
    @Path("livros/{id}")
    public LivroDto atualizarLivro(@PathParam("id") long id, @Valid CatalogoService.DadosLivro dados) {
        return LivroDto.de(service.atualizarLivro(id, dados));
    }

    @DELETE
    @Path("livros/{id}")
    public Response excluirLivro(@PathParam("id") long id) {
        service.excluirLivro(id);
        return Response.noContent().build();
    }
}
