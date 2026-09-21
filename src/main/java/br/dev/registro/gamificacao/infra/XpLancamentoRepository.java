package br.dev.registro.gamificacao.infra;

import br.dev.registro.gamificacao.domain.CalculadoraXp.Lancamento;
import br.dev.registro.gamificacao.domain.OrigemXp;
import br.dev.registro.gamificacao.domain.XpLancamento;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class XpLancamentoRepository implements PanacheRepository<XpLancamento> {

    /** Remove o lancamento de uma origem (o registro foi editado ou excluido). */
    public long removerDaOrigem(OrigemXp origem, long origemId) {
        return delete("origem = ?1 and origemId = ?2", origem, origemId);
    }

    /** Data do lancamento atual de uma origem, para saber qual outro dia tambem precisa recalcular. */
    public LocalDate dataDaOrigem(OrigemXp origem, long origemId) {
        return find("origem = ?1 and origemId = ?2", origem, origemId)
                .firstResultOptional()
                .map(l -> l.dataLocal)
                .orElse(null);
    }

    public List<Lancamento> doDia(LocalDate dia) {
        return find("dataLocal", dia).stream()
                .map(l -> new Lancamento(l.origem, l.categoria, l.pontosBrutos))
                .toList();
    }
}
