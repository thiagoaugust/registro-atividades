package br.dev.registro.gamificacao.infra;

import br.dev.registro.gamificacao.domain.Conquista;
import br.dev.registro.gamificacao.domain.ConquistaDesbloqueada;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConquistaRepository implements PanacheRepositoryBase<Conquista, String> {

    public List<Conquista> ativas() {
        return list("ativa = true", Sort.by("ordem"));
    }

    public Set<String> codigosDesbloqueados() {
        return getEntityManager()
                .createQuery("select d.conquistaCodigo from ConquistaDesbloqueada d", String.class)
                .getResultList()
                .stream()
                .collect(Collectors.toSet());
    }

    public List<ConquistaDesbloqueada> desbloqueadas() {
        return getEntityManager()
                .createQuery(
                        "select d from ConquistaDesbloqueada d order by d.desbloqueadaEm desc",
                        ConquistaDesbloqueada.class)
                .getResultList();
    }

    public void desbloquear(ConquistaDesbloqueada desbloqueada) {
        getEntityManager().persist(desbloqueada);
    }
}
