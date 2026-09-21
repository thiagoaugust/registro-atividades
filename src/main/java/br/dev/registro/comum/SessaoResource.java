package br.dev.registro.comum;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Quem esta logado. O login (POST /api/sessao/login com j_username/j_password) e o logout
 * (/api/sessao/logout) sao tratados pelo proprio form auth do Quarkus — nao existe codigo nosso ali.
 */
@Path("/api/sessao")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
public class SessaoResource {

    private final SecurityIdentity identity;

    public SessaoResource(SecurityIdentity identity) {
        this.identity = identity;
    }

    public record SessaoDto(String usuario) {
    }

    @GET
    public SessaoDto atual() {
        return new SessaoDto(identity.getPrincipal().getName());
    }
}
