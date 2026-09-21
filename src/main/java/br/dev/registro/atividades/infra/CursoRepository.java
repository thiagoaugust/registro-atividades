package br.dev.registro.atividades.infra;

import br.dev.registro.atividades.domain.Curso;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CursoRepository implements PanacheRepository<Curso> {

    private static final String SELECT = "select c from Curso c left join fetch c.area";

    public Optional<Curso> porIdComArea(long id) {
        return getEntityManager()
                .createQuery(SELECT + " where c.id = :id", Curso.class)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst();
    }

    /** Cursando primeiro; ordenar pelo status cru daria ordem alfabetica. */
    public List<Curso> listar() {
        TypedQuery<Curso> query = getEntityManager()
                .createQuery(
                        SELECT
                                + " order by case c.status"
                                + "   when br.dev.registro.atividades.domain.StatusCurso.CURSANDO then 0"
                                + "   when br.dev.registro.atividades.domain.StatusCurso.CONCLUIDO then 1"
                                + "   else 2 end, c.concluidoEm desc nulls last, c.titulo",
                        Curso.class);
        return query.getResultList();
    }
}
