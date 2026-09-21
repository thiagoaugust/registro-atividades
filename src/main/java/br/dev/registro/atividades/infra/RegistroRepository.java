package br.dev.registro.atividades.infra;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.RegistroAtividade;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RegistroRepository implements PanacheRepository<RegistroAtividade> {

    /**
     * Os vinculos vem com join fetch: o DTO e montado depois do commit, e um proxy lazy ali estouraria
     * LazyInitializationException. Assim a transacao continua sendo so do servico.
     */
    private static final String SELECT_COM_VINCULOS =
            """
            select r from RegistroAtividade r
            left join fetch r.projeto
            left join fetch r.desafio
            left join fetch r.livro
            """;

    public Optional<RegistroAtividade> porIdComVinculos(long id) {
        return getEntityManager()
                .createQuery(SELECT_COM_VINCULOS + " where r.id = :id", RegistroAtividade.class)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst();
    }

    public List<RegistroAtividade> buscar(LocalDate de, LocalDate ate, Categoria categoria) {
        String filtroCategoria = categoria == null ? "" : " and r.categoria = :categoria";
        TypedQuery<RegistroAtividade> query = getEntityManager()
                .createQuery(
                        SELECT_COM_VINCULOS
                                + " where r.dataLocal between :de and :ate"
                                + filtroCategoria
                                + " order by r.dataLocal desc, r.id asc",
                        RegistroAtividade.class)
                .setParameter("de", de)
                .setParameter("ate", ate);
        if (categoria != null) {
            query.setParameter("categoria", categoria);
        }
        return query.getResultList();
    }

    /**
     * Progresso de um desafio = soma do valorProgresso dos registros vinculados. Derivado, nunca
     * armazenado — assim editar um registro nao deixa um contador desatualizado para tras.
     *
     * <p>SQL nativo porque o HQL nao alcanca o JSONB. jsonb_exists no lugar do operador `?`, que o
     * driver JDBC leria como placeholder.
     */
    public BigDecimal progressoDoDesafio(long desafioId) {
        Object soma = getEntityManager()
                .createNativeQuery(
                        """
                        select coalesce(sum((detalhes->>'valorProgresso')::numeric), 0)
                          from registro_atividade
                         where desafio_id = :id
                           and jsonb_exists(detalhes, 'valorProgresso')
                        """)
                .setParameter("id", desafioId)
                .getSingleResult();
        return (BigDecimal) soma;
    }
}
