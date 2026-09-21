package br.dev.registro.atividades.api;

import br.dev.registro.atividades.domain.Curso;
import br.dev.registro.atividades.domain.CursoService;
import br.dev.registro.atividades.domain.ProgressoCurso;
import br.dev.registro.atividades.domain.StatusCurso;
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
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Path("/api/cursos")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CursoResource {

    private final CursoService service;

    public CursoResource(CursoService service) {
        this.service = service;
    }

    public record CursoDto(
            Long id, String titulo, String instituicao, String url, BigDecimal cargaHoraria,
            Long areaId, String area, StatusCurso status, LocalDate concluidoEm,
            BigDecimal horasRetroativas, Integer diasRetroativos) {

        static CursoDto de(Curso c) {
            return new CursoDto(
                    c.id, c.titulo, c.instituicao, c.url, c.cargaHoraria,
                    c.area == null ? null : c.area.id, c.area == null ? null : c.area.nome,
                    c.status, c.concluidoEm, c.horasRetroativas, c.diasRetroativos);
        }
    }

    @GET
    public List<CursoDto> listar() {
        return service.listar().stream().map(CursoDto::de).toList();
    }

    /** Cursos com dedicacao, ritmo, previsao e fracao de pratica deliberada. */
    @GET
    @Path("progresso")
    public List<ProgressoCurso> progresso() {
        return service.progresso();
    }

    /** Onde o tempo de estudo foi parar: por area, por tema, e quanto foi pratica. */
    @GET
    @Path("estudo")
    public CursoService.EstudoPorArea estudo(
            @QueryParam("de") LocalDate de, @QueryParam("ate") LocalDate ate) {
        return service.onde(de, ate);
    }

    @GET
    @Path("{id}")
    public CursoDto porId(@PathParam("id") long id) {
        return CursoDto.de(service.porId(id));
    }

    @POST
    public Response criar(@Valid CursoService.DadosCurso dados) {
        CursoDto criado = CursoDto.de(service.criar(dados));
        return Response.created(URI.create("/api/cursos/" + criado.id())).entity(criado).build();
    }

    @PUT
    @Path("{id}")
    public CursoDto atualizar(@PathParam("id") long id, @Valid CursoService.DadosCurso dados) {
        return CursoDto.de(service.atualizar(id, dados));
    }

    @DELETE
    @Path("{id}")
    public Response excluir(@PathParam("id") long id) {
        service.excluir(id);
        return Response.noContent().build();
    }
}
