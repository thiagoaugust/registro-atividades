package br.dev.registro.gamificacao.infra;

import br.dev.registro.gamificacao.domain.DesafioInstancia;
import br.dev.registro.gamificacao.domain.StatusDesafio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class DesafioInstanciaRepository implements PanacheRepository<DesafioInstancia> {

    /**
     * As instancias do periodo que contem a data, com o catalogo junto: os DTOs sao montados depois
     * do commit, e sem o join fetch cada titulo viraria uma consulta a mais (ou um lazy estourado).
     */
    public List<DesafioInstancia> abertasEm(LocalDate hoje) {
        return list("""
                select i from DesafioInstancia i join fetch i.desafio d
                 where i.periodoInicio <= ?1 and i.periodoFim >= ?1
                 order by d.escopo, d.ordem
                """, hoje);
    }

    /** Instancias que ja venceram e ninguem fechou — o job da madrugada, ou a primeira visita do dia. */
    public List<DesafioInstancia> vencidasSemFechar(LocalDate hoje) {
        return list("status = ?1 and periodoFim < ?2", StatusDesafio.ABERTO, hoje);
    }

    public boolean existe(long desafioId, LocalDate periodoInicio) {
        return count("desafio.id = ?1 and periodoInicio = ?2", desafioId, periodoInicio) > 0;
    }

    /** Historico fechado, do mais recente para o mais antigo. */
    public List<DesafioInstancia> concluidas(LocalDate ate, int limite) {
        return find("""
                select i from DesafioInstancia i join fetch i.desafio d
                 where i.status <> ?1 and i.periodoFim < ?2
                 order by i.periodoInicio desc, d.ordem
                """, StatusDesafio.ABERTO, ate)
                .page(0, limite)
                .list();
    }
}
