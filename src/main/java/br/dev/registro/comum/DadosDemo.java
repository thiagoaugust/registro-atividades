package br.dev.registro.comum;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.Desafio;
import br.dev.registro.atividades.domain.Livro;
import br.dev.registro.atividades.domain.Projeto;
import br.dev.registro.atividades.domain.RegistroAtividade;
import br.dev.registro.atividades.domain.AreaConhecimento;
import br.dev.registro.atividades.domain.StatusLivro;
import br.dev.registro.atividades.domain.StatusProjeto;
import br.dev.registro.atividades.infra.DesafioRepository;
import br.dev.registro.atividades.infra.AreaConhecimentoRepository;
import br.dev.registro.atividades.infra.LivroRepository;
import br.dev.registro.atividades.infra.ProjetoRepository;
import br.dev.registro.atividades.infra.RegistroRepository;
import br.dev.registro.checkin.domain.CheckinDiario;
import br.dev.registro.checkin.infra.CheckinRepository;
import br.dev.registro.gamificacao.domain.RecalculoDiaService;
import br.dev.registro.gtd.domain.Acao;
import br.dev.registro.gtd.domain.Contexto;
import br.dev.registro.gtd.domain.DestinoInbox;
import br.dev.registro.gtd.domain.Energia;
import br.dev.registro.gtd.domain.EstadoAcao;
import br.dev.registro.gtd.domain.InboxItem;
import br.dev.registro.gtd.domain.PassoRevisao;
import br.dev.registro.gtd.domain.Referencia;
import br.dev.registro.gtd.domain.RevisaoSemanal;
import br.dev.registro.gtd.infra.AcaoRepository;
import br.dev.registro.gtd.infra.ContextoRepository;
import br.dev.registro.gtd.infra.InboxRepository;
import br.dev.registro.gtd.infra.ReferenciaRepository;
import br.dev.registro.gtd.infra.RevisaoSemanalRepository;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Popula ~120 dias de historico plausivel no perfil demo, para que os dashboards possam ser
 * conferidos de olho. Semente fixa: duas execucoes geram exatamente o mesmo historico.
 *
 * <p>Escreve pelos repositorios, sem passar pelos servicos, e chama o recalculo uma vez no fim — 120
 * dias disparando evento a cada registro seriam centenas de recalculos para o mesmo resultado.
 */
@ApplicationScoped
public class DadosDemo {

    private static final Logger LOG = Logger.getLogger(DadosDemo.class);
    private static final int DIAS = 120;

    private final Random sorteio = new Random(42);

    private final RegistroRepository registros;
    private final CheckinRepository checkins;
    private final LivroRepository livros;
    private final AreaConhecimentoRepository areas;
    private final DesafioRepository desafios;
    private final ProjetoRepository projetos;
    private final InboxRepository inbox;
    private final AcaoRepository acoes;
    private final ContextoRepository contextos;
    private final ReferenciaRepository referencias;
    private final RevisaoSemanalRepository revisoes;
    private final RecalculoDiaService recalculo;
    private final Relogio relogio;

    public DadosDemo(
            RegistroRepository registros,
            CheckinRepository checkins,
            LivroRepository livros,
            AreaConhecimentoRepository areas,
            DesafioRepository desafios,
            ProjetoRepository projetos,
            InboxRepository inbox,
            AcaoRepository acoes,
            ContextoRepository contextos,
            ReferenciaRepository referencias,
            RevisaoSemanalRepository revisoes,
            RecalculoDiaService recalculo,
            Relogio relogio) {
        this.registros = registros;
        this.checkins = checkins;
        this.livros = livros;
        this.areas = areas;
        this.desafios = desafios;
        this.projetos = projetos;
        this.inbox = inbox;
        this.acoes = acoes;
        this.contextos = contextos;
        this.referencias = referencias;
        this.revisoes = revisoes;
        this.recalculo = recalculo;
        this.relogio = relogio;
    }

    void aoIniciar(@Observes StartupEvent evento) {
        if (!ConfigUtils.getProfiles().contains("demo")) {
            return;
        }
        if (registros.count() > 0) {
            LOG.info("Perfil demo: ja existem dados, nada a semear");
            return;
        }
        long inicio = System.currentTimeMillis();
        gerar();
        LOG.infof("Perfil demo: %d dias semeados em %d ms", DIAS, System.currentTimeMillis() - inicio);
    }

    /** Como foi o dia. O peso de cada tipo muda ao longo da semana e de fases do periodo. */
    private enum TipoDeDia {
        DESCANSO,
        FRACO,
        NORMAL,
        FORTE
    }

    @Transactional
    void gerar() {
        LocalDate hoje = relogio.hoje();
        LocalDate inicio = hoje.minusDays(DIAS - 1L);

        Livro lidoUm = livro("Domain-Driven Design", "Eric Evans", 560, "Tecnico", 5);
        Livro lidoDois = livro("Refactoring", "Martin Fowler", 448, "Tecnico", 4);
        Livro lendo = livro("A Philosophy of Software Design", "John Ousterhout", 190, "Tecnico", 3);

        // Estante anterior ao sistema: sem ela, "livros por area" seria uma barra so de Tecnico.
        livroRetroativo("O Homem do Castelo Alto", "Philip K. Dick", 288, "Literatura", 2,
                14, 9.5, inicio.minusDays(40));
        livroRetroativo("O Corpo Guarda as Marcas", "Bessel van der Kolk", 464, "Psicologia", 4,
                38, 21, inicio.minusDays(15));
        livroRetroativo("Sapiens", "Yuval Harari", 464, "Historia", 3,
                26, 16.5, inicio.plusDays(20));
        livroRetroativo("Meditacoes", "Marco Aurelio", 176, "Filosofia", 4,
                21, 7, inicio.plusDays(55));

        Desafio corrida = desafio("200 km no trimestre", 200, "km", inicio);

        Projeto sistema = projeto("Sistema de registro", "app rodando e em uso diario", StatusProjeto.ATIVO);
        Projeto casa = projeto("Reforma da cozinha", "cozinha pronta e usavel", StatusProjeto.ATIVO);
        Projeto curso = projeto("Curso de Kubernetes", "certificacao CKA", StatusProjeto.CONCLUIDO);
        curso.concluidoEm = paraInstante(hoje.minusDays(30));

        int paginasUm = 1;
        int paginasDois = 1;
        int paginasTres = 1;

        for (int i = 0; i < DIAS; i++) {
            LocalDate dia = inicio.plusDays(i);
            TipoDeDia tipo = sortearTipo(dia, i);

            // Um dia em doze e de superacao: acordou mal e entregou mesmo assim. Sem isso o gerador
            // amarra contexto ruim a producao baixa, e "dia dificil vencido" nunca acontece — que e
            // justamente a situacao que o indice existe para reconhecer.
            boolean superacao = tipo != TipoDeDia.DESCANSO && sorteio.nextInt(12) == 0;
            if (superacao) {
                tipo = TipoDeDia.FORTE;
            }

            if (tipo != TipoDeDia.DESCANSO) {
                // Duas semanas sem treino no meio do periodo: uma lesao, uma viagem.
                boolean semTreino = (i >= 40 && i < 54) || tipo == TipoDeDia.FRACO;

                if (!semTreino) {
                    double km = 5 + sorteio.nextInt(8) + sorteio.nextDouble();
                    int minutos = (int) Math.round(km * (5.5 + sorteio.nextDouble()));
                    registro(dia, Categoria.TREINO, minutos, 5 + sorteio.nextInt(5), "Corrida",
                            Map.of("modalidade", "corrida",
                                    "distanciaKm", Math.round(km * 10) / 10.0,
                                    "paceSegPorKm", (int) Math.round(minutos * 60 / km)),
                            null, corrida, null);
                } else if (tipo == TipoDeDia.FORTE) {
                    registro(dia, Categoria.TREINO, 45 + sorteio.nextInt(30), 6 + sorteio.nextInt(4),
                            "Musculacao",
                            Map.of("modalidade", "musculacao",
                                    "series", List.of(
                                            Map.of("exercicio", "agachamento", "reps", 10, "cargaKg", 80),
                                            Map.of("exercicio", "supino", "reps", 10, "cargaKg", 60))),
                            null, null, null);
                }

                if (tipo != TipoDeDia.FRACO || sorteio.nextBoolean()) {
                    String tema = switch (sorteio.nextInt(4)) {
                        case 0 -> "Quarkus";
                        case 1 -> "Postgres";
                        case 2 -> "Arquitetura";
                        default -> "Kubernetes";
                    };
                    int minutosEstudo = 25 + sorteio.nextInt(70);
                    // Dia forte estuda praticando; dia fraco assiste. A fracao de pratica e o que o
                    // painel de estudo compara entre temas.
                    double fracaoPratica = tipo == TipoDeDia.FORTE ? 0.5 : tipo == TipoDeDia.FRACO ? 0.1 : 0.3;
                    int pratica = (int) Math.round(minutosEstudo * fracaoPratica * (0.5 + sorteio.nextDouble()));
                    registro(dia, Categoria.ESTUDO, minutosEstudo, 4 + sorteio.nextInt(6),
                            "Estudo de " + tema,
                            Map.of("tema", tema, "fonte", "doc oficial",
                                    "tecnica", pratica > 0 ? "EXERCICIO" : "LEITURA",
                                    "foco", 2 + sorteio.nextInt(4),
                                    "minutosPratica", Math.min(pratica, minutosEstudo)),
                            null, null, null);
                }

                if (sorteio.nextInt(10) < 6) {
                    Livro alvo = paginasUm <= lidoUm.totalPaginas ? lidoUm
                            : paginasDois <= lidoDois.totalPaginas ? lidoDois : lendo;
                    int de = alvo == lidoUm ? paginasUm : alvo == lidoDois ? paginasDois : paginasTres;
                    // O ultimo livro para perto de dois tercos: velocidade, ritmo e previsao de
                    // termino so tem o que mostrar enquanto existe leitura em andamento.
                    int limite = alvo == lendo ? alvo.totalPaginas * 2 / 3 : alvo.totalPaginas;
                    int ate = Math.min(de + 10 + sorteio.nextInt(25), limite);

                    // Com todos os livros terminados nao ha o que ler; sem esta condicao a ultima
                    // pilha gerava paginaInicial > paginaFinal, dado que a propria API recusaria.
                    if (de <= limite) {
                        registro(dia, Categoria.LEITURA, 20 + sorteio.nextInt(40), 3 + sorteio.nextInt(4),
                                alvo.titulo, Map.of("paginaInicial", de, "paginaFinal", ate),
                                alvo, null, null);

                        if (alvo == lidoUm) {
                            paginasUm = ate + 1;
                        } else if (alvo == lidoDois) {
                            paginasDois = ate + 1;
                        } else {
                            paginasTres = ate + 1;
                        }
                        if (ate >= alvo.totalPaginas && alvo.status == StatusLivro.LENDO) {
                            alvo.status = StatusLivro.CONCLUIDO;
                            alvo.concluidoEm = dia;
                        }
                    }
                }

                if (tipo == TipoDeDia.FORTE && sorteio.nextBoolean()) {
                    Projeto alvo = sorteio.nextBoolean() ? sistema : casa;
                    registro(dia, Categoria.PROJETO, 40 + sorteio.nextInt(90), 5 + sorteio.nextInt(5),
                            alvo.titulo,
                            Map.of("marco", "avanco do dia", "statusApos", "em andamento"),
                            null, null, alvo);
                }
            }

            // ~85% dos dias tem check-in; os outros ficam sem contexto de proposito.
            if (superacao || sorteio.nextInt(100) < 85) {
                checkin(dia, superacao ? TipoDeDia.FRACO : tipo);
            }
        }

        gerarGtd(hoje, sistema, casa);
        gerarRevisoes(inicio, hoje);

        // Escrever pelos repositorios pula os eventos que alimentam o ledger de XP; reconstruir e o
        // que faz o historico semeado ter a mesma pontuacao que teria se tivesse sido digitado.
        recalculo.reconstruirLancamentos(inicio, hoje);
        recalculo.recalcularIntervalo(inicio, hoje);
    }

    private TipoDeDia sortearTipo(LocalDate dia, int indice) {
        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY && sorteio.nextInt(10) < 7) {
            return TipoDeDia.DESCANSO;
        }
        // Uma semana ruim de verdade, para o heatmap nao ficar uniforme.
        if (indice >= 75 && indice < 82) {
            return sorteio.nextBoolean() ? TipoDeDia.FRACO : TipoDeDia.DESCANSO;
        }
        int sorte = sorteio.nextInt(100);
        if (sorte < 10) {
            return TipoDeDia.DESCANSO;
        }
        if (sorte < 35) {
            return TipoDeDia.FRACO;
        }
        return sorte < 80 ? TipoDeDia.NORMAL : TipoDeDia.FORTE;
    }

    private void checkin(LocalDate dia, TipoDeDia tipo) {
        CheckinDiario checkin = new CheckinDiario();
        checkin.dataLocal = dia;
        checkin.descansoPlanejado = tipo == TipoDeDia.DESCANSO && dia.getDayOfWeek() == DayOfWeek.SUNDAY;

        // Dia forte anda junto de sono e energia bons; dia fraco, o contrario. E o que faz as
        // correlacoes do painel terem o que mostrar.
        int base = switch (tipo) {
            case FORTE -> 4;
            case NORMAL -> 3;
            case FRACO -> 2;
            case DESCANSO -> 3;
        };
        checkin.energia = (short) limitar(base + sorteio.nextInt(2) - (tipo == TipoDeDia.FRACO ? 1 : 0));
        checkin.qualidadeSono = (short) limitar(base + sorteio.nextInt(2) - 1);
        checkin.humor = (short) limitar(base + sorteio.nextInt(2));
        checkin.estresse = (short) limitar(6 - base + sorteio.nextInt(2) - 1);
        checkin.dificuldadePrevista = (short) limitar(6 - base + sorteio.nextInt(2) - 1);
        // Sono no formato que o relogio entrega: duracao ao minuto, fases e a nota da noite.
        checkin.minutosSono = (int) Math.round((4.5 + base * 0.8 + sorteio.nextInt(2) * 0.5) * 60);
        checkin.acordouEm = java.time.LocalTime.of(6, 0).plusMinutes(sorteio.nextInt(90));
        checkin.dormiuEm = checkin.acordouEm.minusMinutes(checkin.minutosSono + 20L + sorteio.nextInt(40));
        checkin.minutosSonoProfundo = (int) Math.round(checkin.minutosSono * (0.10 + base * 0.02));
        checkin.minutosSonoRem = (int) Math.round(checkin.minutosSono * (0.15 + base * 0.02));
        checkin.despertares = Math.max(0, 5 - base + sorteio.nextInt(2));
        checkin.fcRepouso = 68 - base * 2 + sorteio.nextInt(5);
        checkin.pontuacaoSono = limitar100(40 + base * 11 + sorteio.nextInt(10));

        if (sorteio.nextInt(10) < 3) {
            checkin.frase = switch (tipo) {
                case FORTE -> "dia que rendeu";
                case FRACO -> "empurrei o dia com a barriga";
                case DESCANSO -> "folga merecida";
                case NORMAL -> "dia comum";
            };
        }
        if (sorteio.nextInt(10) < 4) {
            checkin.dificuldadeFinal = (short) limitar(checkin.dificuldadePrevista + sorteio.nextInt(3) - 1);
            checkin.fechadoEm = paraInstante(dia).plus(20, ChronoUnit.HOURS);
        }
        checkins.persist(checkin);
    }

    private void gerarGtd(LocalDate hoje, Projeto sistema, Projeto casa) {
        List<Contexto> lista = contextos.ativos();
        Contexto computador = lista.stream().filter(c -> c.nome.equals("@computador")).findFirst().orElse(null);
        Contexto rua = lista.stream().filter(c -> c.nome.equals("@rua")).findFirst().orElse(null);
        Contexto telefone = lista.stream().filter(c -> c.nome.equals("@telefone")).findFirst().orElse(null);

        String[] acoesAbertas = {
            "escrever os testes de integracao do modulo de analytics",
            "revisar o PR do colega",
            "comprar material de pintura",
            "ligar para o eletricista",
            "escolher o proximo livro",
            "atualizar o curriculo",
            "planejar a corrida longa de domingo",
        };
        for (int i = 0; i < acoesAbertas.length; i++) {
            Acao acao = new Acao();
            acao.titulo = acoesAbertas[i];
            acao.estado = EstadoAcao.PROXIMA;
            acao.contexto = i % 3 == 0 ? computador : i % 3 == 1 ? rua : telefone;
            acao.tempoEstimadoMin = 15 + sorteio.nextInt(4) * 15;
            acao.energia = Energia.values()[sorteio.nextInt(3)];
            acao.projeto = i % 4 == 0 ? sistema : i % 4 == 1 ? casa : null;
            acao.criadaEm = paraInstante(hoje.minusDays(sorteio.nextInt(30)));
            acoes.persist(acao);
        }

        Acao agendada = new Acao();
        agendada.titulo = "consulta com o dentista";
        agendada.estado = EstadoAcao.AGENDA;
        agendada.agendadaPara = paraInstante(hoje.plusDays(3)).plus(14, ChronoUnit.HOURS);
        acoes.persist(agendada);

        Acao aguardando = new Acao();
        aguardando.titulo = "orcamento da marcenaria";
        aguardando.estado = EstadoAcao.AGUARDANDO;
        aguardando.delegadaPara = "marcenaria do bairro";
        aguardando.delegadaEm = hoje.minusDays(9);
        aguardando.projeto = casa;
        acoes.persist(aguardando);

        for (String titulo : new String[] {"aprender violao", "morar fora por um ano", "escrever um livro"}) {
            Acao alguemDia = new Acao();
            alguemDia.titulo = titulo;
            alguemDia.estado = EstadoAcao.ALGUM_DIA;
            acoes.persist(alguemDia);
        }

        // 34 acoes concluidas espalhadas pelo periodo, para as metricas do GTD terem historia.
        for (int i = 0; i < 34; i++) {
            LocalDate quando = hoje.minusDays(sorteio.nextInt(DIAS));
            Acao concluida = new Acao();
            concluida.titulo = "tarefa concluida #" + (i + 1);
            concluida.estado = EstadoAcao.CONCLUIDA;
            concluida.concluidaEm = paraInstante(quando).plus(sorteio.nextInt(12) + 8, ChronoUnit.HOURS);
            concluida.criadaEm = paraInstante(quando.minusDays(sorteio.nextInt(10) + 1));
            concluida.contexto = computador;
            acoes.persist(concluida);
        }

        // Inbox com itens velhos: e o que faz a metrica de idade media significar alguma coisa.
        String[] pendentes = {
            "ver aquele video sobre indices no Postgres",
            "trocar o oleo do carro",
            "responder o e-mail da faculdade",
            "pesquisar tenis novo para corrida",
            "ideia: exportar os dados para CSV",
        };
        for (int i = 0; i < pendentes.length; i++) {
            InboxItem item = new InboxItem();
            item.texto = pendentes[i];
            item.capturadoEm = paraInstante(hoje.minusDays(i * 6L + 1));
            inbox.persist(item);
        }
        for (int i = 0; i < 28; i++) {
            LocalDate quando = hoje.minusDays(sorteio.nextInt(DIAS));
            InboxItem item = new InboxItem();
            item.texto = "item processado #" + (i + 1);
            item.capturadoEm = paraInstante(quando);
            item.processadoEm = paraInstante(quando).plus(sorteio.nextInt(48) + 1, ChronoUnit.HOURS);
            item.destino = DestinoInbox.values()[sorteio.nextInt(DestinoInbox.values().length)];
            inbox.persist(item);
        }

        // @CreationTimestamp sobrescreve capturado_em no persist: sem este ajuste, todo item do
        // inbox nasceria com a data de hoje e a metrica de idade media seria sempre zero.
        inbox.getEntityManager().flush();
        inbox.getEntityManager()
                .createNativeQuery(
                        """
                        update inbox_item
                           set capturado_em = coalesce(processado_em, now()) - (id % 40) * interval '1 day'
                        """)
                .executeUpdate();

        Referencia referencia = new Referencia();
        referencia.titulo = "Guia de Panache";
        referencia.url = "https://quarkus.io/guides/hibernate-orm-panache";
        referencia.conteudo = "padrao repository fica na secao do meio";
        referencia.tags = new String[] {"quarkus", "jpa"};
        referencias.persist(referencia);
    }

    /** Doze das ultimas semanas revisadas, com duas falhas no meio. */
    private void gerarRevisoes(LocalDate inicio, LocalDate hoje) {
        LocalDate semana = inicio.with(DayOfWeek.MONDAY);
        int contador = 0;
        while (semana.isBefore(hoje)) {
            contador++;
            boolean pulou = contador == 6 || contador == 11;
            if (!pulou) {
                RevisaoSemanal revisao = new RevisaoSemanal();
                revisao.semanaInicio = semana;
                revisao.iniciadaEm = paraInstante(semana.plusDays(6)).plus(10, ChronoUnit.HOURS);
                revisao.concluidaEm = revisao.iniciadaEm.plus(25 + sorteio.nextInt(40), ChronoUnit.MINUTES);
                revisao.duracaoMin = (int) ChronoUnit.MINUTES.between(revisao.iniciadaEm, revisao.concluidaEm);
                Map<String, Object> passos = new LinkedHashMap<>();
                for (PassoRevisao passo : PassoRevisao.values()) {
                    passos.put(passo.name(), true);
                }
                revisao.passos = passos;
                revisoes.persist(revisao);
            }
            semana = semana.plusWeeks(1);
        }
    }

    // ---------- auxiliares ----------

    private void registro(
            LocalDate dia,
            Categoria categoria,
            int duracao,
            int esforco,
            String titulo,
            Map<String, Object> detalhes,
            Livro livro,
            Desafio desafio,
            Projeto projeto) {
        RegistroAtividade registro = new RegistroAtividade();
        registro.dataLocal = dia;
        registro.duracaoMin = duracao;
        registro.esforco = (short) esforco;
        registro.categoria = categoria;
        registro.titulo = titulo;
        registro.satisfacao = (short) (2 + sorteio.nextInt(4));
        registro.detalhes = new LinkedHashMap<>(detalhes);
        registro.livro = livro;
        registro.projeto = projeto;

        // O desafio de corrida soma os km do treino; o progresso sai desses registros.
        if (desafio != null && detalhes.containsKey("distanciaKm")) {
            registro.desafio = desafio;
            registro.detalhes.put("valorProgresso", detalhes.get("distanciaKm"));
        }
        registros.persist(registro);
    }

    private Livro livro(String titulo, String autor, int paginas, String area, int dificuldade) {
        Livro livro = new Livro();
        livro.titulo = titulo;
        livro.autor = autor;
        livro.totalPaginas = paginas;
        livro.area = area(area);
        livro.dificuldade = (short) dificuldade;
        livros.persist(livro);
        return livro;
    }

    /**
     * Livro lido antes de o sistema existir: conta na estante e nas areas, sem gerar registro nem
     * XP. E o que faz "quantos livros de psicologia eu li" ter resposta ja no primeiro dia.
     */
    private void livroRetroativo(
            String titulo, String autor, int paginas, String area, int dificuldade,
            int dias, double horas, LocalDate concluidoEm) {
        Livro livro = livro(titulo, autor, paginas, area, dificuldade);
        livro.status = StatusLivro.CONCLUIDO;
        livro.concluidoEm = concluidoEm;
        livro.diasLeitura = dias;
        livro.horasLeitura = BigDecimal.valueOf(horas);
    }

    private AreaConhecimento area(String nome) {
        return areas.find("nome", nome).firstResult();
    }

    private Desafio desafio(String titulo, int meta, String unidade, LocalDate inicio) {
        Desafio desafio = new Desafio();
        desafio.titulo = titulo;
        desafio.metaValor = BigDecimal.valueOf(meta);
        desafio.unidade = unidade;
        desafio.inicio = inicio;
        desafio.fim = inicio.plusDays(90);
        desafios.persist(desafio);
        return desafio;
    }

    private Projeto projeto(String titulo, String resultado, StatusProjeto status) {
        Projeto projeto = new Projeto();
        projeto.titulo = titulo;
        projeto.resultadoDesejado = resultado;
        projeto.status = status;
        projetos.persist(projeto);
        return projeto;
    }

    private static Instant paraInstante(LocalDate dia) {
        return dia.atStartOfDay(Relogio.ZONA).toInstant();
    }

    private static int limitar(int valor) {
        return Math.max(1, Math.min(5, valor));
    }

    private static int limitar100(int valor) {
        return Math.max(0, Math.min(100, valor));
    }
}
