package br.dev.registro.atividades.infra;

import br.dev.registro.atividades.domain.Projeto;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjetoRepository implements PanacheRepository<Projeto> {
}
