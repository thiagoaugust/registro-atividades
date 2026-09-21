package br.dev.registro.atividades.api;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.DadosRegistro;
import br.dev.registro.atividades.domain.RegistroService;
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
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Path("/api/registros")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegistroResource {

    private final RegistroService service;

    public RegistroResource(RegistroService service) {
        this.service = service;
    }

    /** Sem parametros, devolve o dia de hoje. */
    @GET
    public List<RegistroDto> buscar(
            @Parameter(description = "Inicio do periodo (dia local). Padrao: hoje") @QueryParam("de") LocalDate de,
            @Parameter(description = "Fim do periodo. Padrao: igual a 'de'") @QueryParam("ate") LocalDate ate,
            @QueryParam("categoria") Categoria categoria) {
        return RegistroDto.de(service.buscar(de, ate, categoria));
    }

    @GET
    @Path("{id}")
    public RegistroDto porId(@PathParam("id") long id) {
        return RegistroDto.de(service.porId(id));
    }

    @POST
    public Response criar(@Valid DadosRegistro dados) {
        RegistroDto criado = RegistroDto.de(service.criar(dados));
        return Response.created(URI.create("/api/registros/" + criado.id())).entity(criado).build();
    }

    @PUT
    @Path("{id}")
    public RegistroDto atualizar(@PathParam("id") long id, @Valid DadosRegistro dados) {
        return RegistroDto.de(service.atualizar(id, dados));
    }

    @DELETE
    @Path("{id}")
    public Response excluir(@PathParam("id") long id) {
        service.excluir(id);
        return Response.noContent().build();
    }
}
