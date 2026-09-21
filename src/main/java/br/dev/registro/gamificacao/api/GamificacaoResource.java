package br.dev.registro.gamificacao.api;

import br.dev.registro.gamificacao.domain.PerfilService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/gamificacao")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
public class GamificacaoResource {

    private final PerfilService service;

    public GamificacaoResource(PerfilService service) {
        this.service = service;
    }

    @GET
    @Path("perfil")
    public PerfilService.PerfilDto perfil() {
        return service.perfil();
    }

    @GET
    @Path("conquistas")
    public List<PerfilService.ConquistaDto> conquistas() {
        return service.conquistas();
    }
}
