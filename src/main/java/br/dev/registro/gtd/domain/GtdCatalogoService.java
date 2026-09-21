package br.dev.registro.gtd.domain;

import br.dev.registro.comum.RecursoNaoEncontradoException;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.gtd.infra.AcaoRepository;
import br.dev.registro.gtd.infra.ContextoRepository;
import br.dev.registro.gtd.infra.ReferenciaRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Contextos (@casa, @computador...) e material de referencia. Dois cadastros rasos. */
@ApplicationScoped
public class GtdCatalogoService {

    private final ContextoRepository contextos;
    private final ReferenciaRepository referencias;
    private final AcaoRepository acoes;

    public GtdCatalogoService(
            ContextoRepository contextos, ReferenciaRepository referencias, AcaoRepository acoes) {
        this.contextos = contextos;
        this.referencias = referencias;
        this.acoes = acoes;
    }

    public record DadosContexto(@NotBlank @Size(max = 40) String nome, Boolean ativo, Integer ordem) {
    }

    public List<Contexto> listarContextos(boolean apenasAtivos) {
        return apenasAtivos ? contextos.ativos() : contextos.listAll(Sort.by("ordem").and("nome"));
    }

    @Transactional
    public Contexto criarContexto(DadosContexto dados) {
        Contexto contexto = new Contexto();
        aplicar(dados, contexto);
        contextos.persist(contexto);
        return contexto;
    }

    @Transactional
    public Contexto atualizarContexto(long id, DadosContexto dados) {
        Contexto contexto = contextos.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contexto", id));
        aplicar(dados, contexto);
        return contexto;
    }

    private void aplicar(DadosContexto dados, Contexto contexto) {
        contexto.nome = dados.nome().startsWith("@") ? dados.nome() : "@" + dados.nome();
        if (dados.ativo() != null) {
            contexto.ativo = dados.ativo();
        }
        if (dados.ordem() != null) {
            contexto.ordem = dados.ordem();
        }
    }

    /**
     * Contexto em uso nao e apagado, e desativado: apagar deixaria as acoes existentes sem a
     * informacao de onde elas podem ser feitas.
     */
    @Transactional
    public void excluirContexto(long id) {
        Contexto contexto = contextos.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contexto", id));
        long emUso = acoes.count("contexto.id = ?1", id);
        if (emUso > 0) {
            throw new RegraNegocioException(
                    "o contexto tem %d acao(oes); desative em vez de excluir".formatted(emUso));
        }
        contextos.delete(contexto);
    }

    // ---------- Referencias ----------

    public List<Referencia> listarReferencias(String tag) {
        if (tag == null || tag.isBlank()) {
            return referencias.listAll(Sort.by("criadaEm").descending());
        }
        return referencias.getEntityManager()
                .createNativeQuery(
                        "select * from referencia where :tag = any(tags) order by criada_em desc",
                        Referencia.class)
                .setParameter("tag", tag)
                .getResultList();
    }

    @Transactional
    public Referencia criarReferencia(InboxService.DadosReferencia dados) {
        Referencia referencia = new Referencia();
        aplicar(dados, referencia);
        referencias.persist(referencia);
        return referencia;
    }

    @Transactional
    public Referencia atualizarReferencia(long id, InboxService.DadosReferencia dados) {
        Referencia referencia = referencias.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Referencia", id));
        aplicar(dados, referencia);
        return referencia;
    }

    private void aplicar(InboxService.DadosReferencia dados, Referencia referencia) {
        referencia.titulo = dados.titulo();
        referencia.conteudo = dados.conteudo();
        referencia.url = dados.url();
        referencia.tags = dados.tags() == null ? new String[0] : dados.tags().toArray(String[]::new);
    }

    @Transactional
    public void excluirReferencia(long id) {
        Referencia referencia = referencias.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Referencia", id));
        referencias.delete(referencia);
    }
}
