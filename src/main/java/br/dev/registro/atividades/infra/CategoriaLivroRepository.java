package br.dev.registro.atividades.infra;

import br.dev.registro.atividades.domain.CategoriaLivro;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CategoriaLivroRepository implements PanacheRepository<CategoriaLivro> {

    public List<CategoriaLivro> ativas() {
        return list("ativa = true", Sort.by("ordem").and("nome"));
    }
}
