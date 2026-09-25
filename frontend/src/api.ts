export type Categoria = "TREINO" | "ESTUDO" | "LEITURA" | "DESAFIO" | "PROJETO";

export const CATEGORIAS: Categoria[] = ["TREINO", "ESTUDO", "LEITURA", "DESAFIO", "PROJETO"];

export interface Vinculo {
  id: number;
  titulo: string;
}

export interface RegistroDto {
  id: number;
  dataLocal: string;
  inicioEm: string | null;
  duracaoMin: number;
  categoria: Categoria;
  titulo: string | null;
  esforco: number;
  satisfacao: number | null;
  notas: string | null;
  projeto: Vinculo | null;
  desafio: Vinculo | null;
  livro: Vinculo | null;
  curso: Vinculo | null;
  detalhes: Record<string, unknown>;
  criadoEm: string;
  atualizadoEm: string;
}

export interface DadosRegistro {
  dataLocal?: string;
  duracaoMin: number;
  categoria: Categoria;
  titulo?: string | null;
  esforco: number;
  satisfacao?: number | null;
  notas?: string | null;
  projetoId?: number | null;
  desafioId?: number | null;
  livroId?: number | null;
  cursoId?: number | null;
  detalhes?: Record<string, unknown>;
}

export type Classificacao = "DIFICIL" | "NORMAL" | "BOM" | "EXCELENTE";

export interface CheckinDto {
  dataLocal: string;
  energia: number | null;
  minutosSono: number | null;
  dormiuEm: string | null;
  acordouEm: string | null;
  minutosSonoProfundo: number | null;
  minutosSonoRem: number | null;
  despertares: number | null;
  fcRepouso: number | null;
  pontuacaoSono: number | null;
  qualidadeSono: number | null;
  humor: number | null;
  estresse: number | null;
  dificuldadePrevista: number | null;
  descansoPlanejado: boolean;
  frase: string | null;
  dificuldadeFinal: number | null;
  atrapalhou: string | null;
  fechadoEm: string | null;
}

export interface DadosCheckin {
  energia?: number | null;
  minutosSono?: number | null;
  dormiuEm?: string | null;
  acordouEm?: string | null;
  minutosSonoProfundo?: number | null;
  minutosSonoRem?: number | null;
  despertares?: number | null;
  fcRepouso?: number | null;
  pontuacaoSono?: number | null;
  qualidadeSono?: number | null;
  humor?: number | null;
  estresse?: number | null;
  dificuldadePrevista?: number | null;
  descansoPlanejado: boolean;
  frase?: string | null;
}

export type BandaEnergia = "BAIXA" | "NORMAL" | "ALTA";
export type SituacaoFaixa = "CALIBRANDO" | "DESCANSO" | "ABAIXO" | "DENTRO" | "ACIMA";

/** Quanto e um dia justo hoje, medido contra os seus dias de energia parecida. */
export interface FaixaDto {
  data: string;
  banda: BandaEnergia | null;
  rotulo: string;
  piso: number;
  tipico: number;
  teto: number;
  xpDoDia: number;
  faltaParaOPiso: number;
  situacao: SituacaoFaixa;
  diasComparaveis: number;
}

export type EscopoDesafio = "DIARIO" | "SEMANAL" | "MENSAL";
export type StatusDesafioPeriodico = "ABERTO" | "CUMPRIDO" | "PERDIDO";

/** Um desafio de um periodo concreto, com o alvo ja calibrado sobre os periodos anteriores. */
export interface DesafioPeriodicoDto {
  id: number;
  escopo: EscopoDesafio;
  titulo: string;
  descricao: string | null;
  unidade: string;
  tipo: "META" | "RECORDE";
  periodoInicio: string;
  periodoFim: string;
  alvo: number;
  progresso: number;
  fracao: number;
  status: StatusDesafioPeriodico;
  xp: number;
}

export interface PainelDesafiosDto {
  diarios: DesafioPeriodicoDto[];
  semanais: DesafioPeriodicoDto[];
  mensais: DesafioPeriodicoDto[];
  historico: DesafioPeriodicoDto[];
  trofeusDoAno: number;
}

export interface ResumoDto {
  xpTotal: number;
  indiceProdutividade: number;
  classificacao: Classificacao;
  diaDificilVencido: boolean;
  descanso: boolean;
  presenca: boolean;
  baselineInsuficiente: boolean;
}

export interface NivelDto {
  nivel: number;
  xp: number;
  xpNesteNivel: number;
  xpParaOProximo: number;
}

export interface PerfilDto {
  geral: NivelDto;
  porCategoria: Record<Categoria, NivelDto>;
  streakGeral: number;
  streakPorCategoria: Record<Categoria, number>;
  conquistasDesbloqueadas: number;
  conquistasTotais: number;
}

export interface ConquistaDto {
  codigo: string;
  titulo: string;
  descricao: string | null;
  alvo: number;
  progresso: number;
  desbloqueada: boolean;
  dataLocal: string | null;
  desbloqueadaEm: string | null;
}

export interface DiaDto {
  data: string;
  totalMinutos: number;
  minutosPorCategoria: Record<string, number>;
  registros: RegistroDto[];
  checkin: CheckinDto | null;
  resumo: ResumoDto | null;
}

export interface LivroDto {
  id: number;
  titulo: string;
  autor: string | null;
  totalPaginas: number | null;
  status: StatusLivro;
  concluidoEm: string | null;
}

export interface DesafioDto {
  id: number;
  titulo: string;
  metaValor: number;
  unidade: string;
  inicio: string;
  fim: string | null;
  status: "ATIVO" | "CONCLUIDO" | "ABANDONADO";
  progresso: number;
}

export type StatusProjeto = "ATIVO" | "PAUSADO" | "CONCLUIDO" | "ARQUIVADO";

export interface ProjetoDto {
  id: number;
  titulo: string;
  resultadoDesejado: string | null;
  status: StatusProjeto;
}

export interface DadosProjeto {
  titulo: string;
  resultadoDesejado?: string | null;
  status?: StatusProjeto;
}

/** Tarefas sao as acoes do projeto, sem as descartadas. Percentual nulo quando nao ha tarefa. */
export interface ProgressoProjetoDto {
  projetoId: number;
  titulo: string;
  resultadoDesejado: string | null;
  status: StatusProjeto;
  tarefas: number;
  concluidas: number;
  faltam: number;
  percentual: number | null;
}

export type EstadoAcao = "PROXIMA" | "AGENDA" | "AGUARDANDO" | "ALGUM_DIA" | "CONCLUIDA" | "DESCARTADA";
export type Energia = "BAIXA" | "MEDIA" | "ALTA";
export type DestinoInbox = "LIXO" | "ALGUM_DIA" | "REFERENCIA" | "ACAO" | "PROJETO" | "FEITO_2MIN";

/** Vinculo do GTD: contexto e projeto sao exibidos pelo nome. Registros usam Vinculo (titulo). */
export interface VinculoGtd {
  id: number;
  nome: string;
}

export interface ContextoDto {
  id: number;
  nome: string;
  ativo: boolean;
  ordem: number;
}

export interface InboxItemDto {
  id: number;
  texto: string;
  capturadoEm: string;
  processadoEm: string | null;
  destino: DestinoInbox | null;
  destinoId: number | null;
}

export interface AcaoDto {
  id: number;
  titulo: string;
  notas: string | null;
  estado: EstadoAcao;
  contexto: VinculoGtd | null;
  tempoEstimadoMin: number | null;
  energia: Energia | null;
  categoria: Categoria | null;
  projeto: VinculoGtd | null;
  agendadaPara: string | null;
  delegadaPara: string | null;
  delegadaEm: string | null;
  registroId: number | null;
  criadaEm: string;
  concluidaEm: string | null;
}

export interface DadosAcao {
  titulo: string;
  notas?: string | null;
  estado?: EstadoAcao;
  contextoId?: number | null;
  tempoEstimadoMin?: number | null;
  energia?: Energia | null;
  categoria?: Categoria | null;
  projetoId?: number | null;
  agendadaPara?: string | null;
  delegadaPara?: string | null;
}

export interface DadosReferencia {
  titulo: string;
  conteudo?: string | null;
  url?: string | null;
  tags?: string[];
}

export interface ReferenciaDto {
  id: number;
  titulo: string;
  conteudo: string | null;
  url: string | null;
  tags: string[];
  criadaEm: string;
}

export interface Decisao {
  destino: DestinoInbox;
  acao?: DadosAcao;
  projeto?: { titulo: string; resultadoDesejado?: string | null };
  referencia?: DadosReferencia;
}

export interface ConclusaoDto {
  acao: AcaoDto;
  sugestaoRegistro: { categoria: Categoria; titulo: string; projetoId: number | null } | null;
}

export interface EngajarDto {
  energiaSugerida: Energia | null;
  acoes: AcaoDto[];
}

export type Granularidade = "DIA" | "SEMANA" | "MES" | "ANO";

export interface DiaHeatmap {
  data: string;
  xp: number;
  minutos: number;
  indice: number;
  classificacao: Classificacao;
  diaDificilVencido: boolean;
  descanso: boolean;
}

export interface Variacao {
  atual: number;
  anterior: number;
  percentual: number | null;
}

export interface PontoSerie {
  periodo: string;
  xp: number;
  minutos: number;
  indiceMedio: number | null;
  xpMediaMovel: number | null;
  diasComPresenca: number;
  diasDificeisVencidos: number;
  dificeis: number;
  normais: number;
  bons: number;
  excelentes: number;
}

export interface CategoriaNoPeriodo {
  periodo: string;
  categoria: Categoria;
  xp: number;
  minutos: number;
}

export interface ComparacaoPeriodo {
  periodo: { de: string; ate: string };
  periodoAnterior: { de: string; ate: string };
  granularidade: Granularidade;
  xp: Variacao;
  minutos: Variacao;
  indiceMedio: Variacao;
  diasComPresenca: Variacao;
  diasDificeisVencidos: Variacao;
  registros: Variacao;
  serie: PontoSerie[];
  porCategoria: CategoriaNoPeriodo[];
}

export interface PorCategoriaDto {
  treino: { km: number; paceMedioSegPorKm: number | null; sessoes: number; minutos: number };
  leitura: { paginas: number; livrosConcluidos: number; sessoes: number; minutos: number };
  temas: { tema: string; minutos: number; sessoes: number }[];
  desafios: {
    id: number;
    titulo: string;
    unidade: string;
    meta: number;
    progresso: number;
    inicio: string;
    fim: string | null;
    status: string;
  }[];
}

export interface CorrelacoesDto {
  coeficientes: { nome: string; coeficiente: number | null; pares: number }[];
  porDiaDaSemana: { diaSemana: number; indiceMedio: number; xpMedio: number; dias: number }[];
  sonoVersusIndice: { x: number; y: number }[];
  energiaVersusXp: { x: number; y: number }[];
}

export interface MetricasGtdDto {
  capturados: number;
  processados: number;
  pendentes: number;
  idadeMediaPendentesDias: number | null;
  acoesConcluidas: number;
  acoesAbertas: number;
  projetosParados: number;
}

export type PassoRevisao =
  | "ESVAZIAR_INBOX"
  | "REVISAR_PROXIMAS_ACOES"
  | "REVISAR_PROJETOS"
  | "REVISAR_AGUARDANDO"
  | "REVISAR_AGENDA"
  | "REVISAR_ALGUM_DIA";

export interface RevisaoDto {
  semanaInicio: string;
  iniciadaEm: string;
  concluidaEm: string | null;
  duracaoMin: number | null;
  passos: Record<string, boolean>;
  completa: boolean;
}

export interface ItemChecklist {
  passo: PassoRevisao;
  titulo: string;
  descricao: string;
}

export interface RetrospectivaDto {
  ano: number;
  totais: {
    xp: number;
    minutos: number;
    indiceMedio: number;
    diasComPresenca: number;
    diasDificeisVencidos: number;
    registros: number;
  };
  xp: Variacao;
  minutos: Variacao;
  diasComPresenca: Variacao;
  dificeis: number;
  normais: number;
  bons: number;
  excelentes: number;
  maiorSequencia: number;
  melhorDia: { data: string; xp: number; indice: number; classificacao: Classificacao } | null;
  porMes: PontoSerie[];
  categorias: PorCategoriaDto;
  conquistas: { codigo: string; titulo: string; dataLocal: string }[];
  revisoesConcluidas: number;
  gtd: MetricasGtdDto;
}

export type StatusLivro = "LENDO" | "CONCLUIDO" | "QUERO_LER" | "ABANDONADO";

export interface AreaDto {
  id: number;
  nome: string;
  ativa: boolean;
  ordem: number;
}

export interface ProgressoLeituraDto {
  livroId: number;
  titulo: string;
  autor: string | null;
  totalPaginas: number | null;
  status: StatusLivro;
  concluidoEm: string | null;
  capaUrl: string | null;
  dificuldade: number | null;
  areaId: number | null;
  area: string | null;
  retroativo: boolean;
  diasLeitura: number | null;
  horasLeitura: number | null;
  ultimaPagina: number;
  paginasLidas: number;
  paginasRestantes: number | null;
  percentualLido: number | null;
  sessoes: number;
  minutos: number;
  esforcoMedio: number | null;
  paginasPorHoraRecente: number | null;
  paginasPorHoraMedia: number | null;
  ritmoDiario: number | null;
  diasRestantes: number | null;
  previsaoTermino: string | null;
  primeiraSessao: string | null;
  ultimaSessao: string | null;
}

export interface DadosLivro {
  titulo: string;
  autor?: string | null;
  totalPaginas?: number | null;
  status?: StatusLivro;
  capaUrl?: string | null;
  dificuldade?: number | null;
  areaId?: number | null;
  /** Preenchidos so no cadastro de leitura retroativa. */
  diasLeitura?: number | null;
  horasLeitura?: number | null;
  concluidoEm?: string | null;
}

export type StatusCurso = "CURSANDO" | "CONCLUIDO" | "ABANDONADO";

export interface CursoDto {
  id: number;
  titulo: string;
  instituicao: string | null;
  url: string | null;
  cargaHoraria: number | null;
  areaId: number | null;
  area: string | null;
  status: StatusCurso;
  concluidoEm: string | null;
  horasRetroativas: number | null;
  diasRetroativos: number | null;
}

export interface DadosCurso {
  titulo: string;
  instituicao?: string | null;
  url?: string | null;
  cargaHoraria?: number | null;
  areaId?: number | null;
  status?: StatusCurso;
  horasRetroativas?: number | null;
  diasRetroativos?: number | null;
  concluidoEm?: string | null;
}

export interface ProgressoCursoDto {
  cursoId: number;
  titulo: string;
  instituicao: string | null;
  url: string | null;
  cargaHoraria: number | null;
  areaId: number | null;
  area: string | null;
  status: StatusCurso;
  concluidoEm: string | null;
  retroativo: boolean;
  minutos: number;
  minutosPratica: number;
  percentualConcluido: number | null;
  horasRestantes: number | null;
  percentualPratica: number | null;
  sessoes: number;
  esforcoMedio: number | null;
  horasPorSemana: number | null;
  diasRestantes: number | null;
  previsaoTermino: string | null;
  primeiraSessao: string | null;
  ultimaSessao: string | null;
}

export interface EstudoPorAreaDto {
  porArea: {
    areaId: number | null;
    area: string;
    minutosCurso: number;
    minutosLivro: number;
    minutosPratica: number;
    cursos: number;
    livros: number;
  }[];
  porTema: { tema: string; minutos: number; minutosPratica: number; sessoes: number }[];
  pratica: { minutosEstudo: number; minutosPratica: number; percentual: number | null };
}

/** Erro vindo do backend em RFC 7807. */
export class ErroApi extends Error {
  constructor(
    readonly status: number,
    readonly detalhe: string,
    readonly campos?: { campo: string; mensagem: string }[],
  ) {
    super(detalhe);
  }
}

export class NaoAutenticado extends Error {}

async function requisicao<T>(url: string, init?: RequestInit): Promise<T> {
  const resposta = await fetch(url, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });

  if (resposta.status === 401) {
    throw new NaoAutenticado("sessao expirada");
  }
  if (!resposta.ok) {
    const problema = await resposta.json().catch(() => null);
    throw new ErroApi(
      resposta.status,
      problema?.detail ?? `Erro ${resposta.status}`,
      problema?.errors,
    );
  }
  return resposta.status === 204 ? (undefined as T) : ((await resposta.json()) as T);
}

export const api = {
  sessao: () => requisicao<{ usuario: string }>("/api/sessao"),

  login: async (usuario: string, senha: string) => {
    const resposta = await fetch("/api/sessao/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ j_username: usuario, j_password: senha }),
    });
    if (!resposta.ok) {
      throw new ErroApi(resposta.status, "usuario ou senha invalidos");
    }
  },

  logout: () => fetch("/api/sessao/logout"),

  dia: (data: string) => requisicao<DiaDto>(`/api/dias/${data}`),

  criarRegistro: (dados: DadosRegistro) =>
    requisicao<RegistroDto>("/api/registros", { method: "POST", body: JSON.stringify(dados) }),

  atualizarRegistro: (id: number, dados: DadosRegistro) =>
    requisicao<RegistroDto>(`/api/registros/${id}`, { method: "PUT", body: JSON.stringify(dados) }),

  excluirRegistro: (id: number) => requisicao<void>(`/api/registros/${id}`, { method: "DELETE" }),

  salvarCheckin: (data: string, dados: DadosCheckin) =>
    requisicao<CheckinDto>(`/api/checkins/${data}`, { method: "PUT", body: JSON.stringify(dados) }),

  fecharDia: (data: string, dados: { dificuldadeFinal?: number | null; atrapalhou?: string | null }) =>
    requisicao<CheckinDto>(`/api/checkins/${data}/fechamento`, {
      method: "PUT",
      body: JSON.stringify(dados),
    }),

  perfil: () => requisicao<PerfilDto>("/api/gamificacao/perfil"),

  faixa: (data: string) => requisicao<FaixaDto>(`/api/gamificacao/faixa/${data}`),

  desafiosPeriodicos: () => requisicao<PainelDesafiosDto>("/api/gamificacao/desafios"),

  conquistas: () => requisicao<ConquistaDto[]>("/api/gamificacao/conquistas"),

  inbox: () => requisicao<{ pendentes: number; itens: InboxItemDto[] }>("/api/gtd/inbox"),

  capturar: (texto: string) =>
    requisicao<InboxItemDto>("/api/gtd/inbox", { method: "POST", body: JSON.stringify({ texto }) }),

  descartarDoInbox: (id: number) =>
    requisicao<void>(`/api/gtd/inbox/${id}`, { method: "DELETE" }),

  processar: (id: number, decisao: Decisao) =>
    requisicao<InboxItemDto>(`/api/gtd/inbox/${id}/processar`, {
      method: "POST",
      body: JSON.stringify(decisao),
    }),

  acoes: (filtros: { estado?: EstadoAcao; contexto?: number; projeto?: number } = {}) => {
    const query = new URLSearchParams();
    if (filtros.estado) query.set("estado", filtros.estado);
    if (filtros.contexto) query.set("contexto", String(filtros.contexto));
    if (filtros.projeto) query.set("projeto", String(filtros.projeto));
    return requisicao<AcaoDto[]>(`/api/gtd/acoes?${query}`);
  },

  criarAcao: (dados: DadosAcao) =>
    requisicao<AcaoDto>("/api/gtd/acoes", { method: "POST", body: JSON.stringify(dados) }),

  atualizarAcao: (id: number, dados: DadosAcao) =>
    requisicao<AcaoDto>(`/api/gtd/acoes/${id}`, { method: "PUT", body: JSON.stringify(dados) }),

  concluirAcao: (id: number) =>
    requisicao<ConclusaoDto>(`/api/gtd/acoes/${id}/concluir`, { method: "POST" }),

  reabrirAcao: (id: number) =>
    requisicao<AcaoDto>(`/api/gtd/acoes/${id}/reabrir`, { method: "POST" }),

  excluirAcao: (id: number) => requisicao<void>(`/api/gtd/acoes/${id}`, { method: "DELETE" }),

  registrarAcao: (id: number, dados: { duracaoMin: number; esforco: number; satisfacao?: number | null }) =>
    requisicao<RegistroDto>(`/api/gtd/acoes/${id}/registrar`, {
      method: "POST",
      body: JSON.stringify(dados),
    }),

  engajar: (filtros: { contexto?: number; tempoDisponivel?: number; energia?: Energia }) => {
    const query = new URLSearchParams();
    if (filtros.contexto) query.set("contexto", String(filtros.contexto));
    if (filtros.tempoDisponivel) query.set("tempoDisponivel", String(filtros.tempoDisponivel));
    if (filtros.energia) query.set("energia", filtros.energia);
    return requisicao<EngajarDto>(`/api/gtd/engajar?${query}`);
  },

  contextos: () => requisicao<ContextoDto[]>("/api/gtd/contextos"),

  referencias: (tag?: string) =>
    requisicao<ReferenciaDto[]>(`/api/gtd/referencias${tag ? `?tag=${encodeURIComponent(tag)}` : ""}`),

  projetosParados: () => requisicao<VinculoGtd[]>("/api/gtd/projetos/sem-proxima-acao"),

  heatmap: (ano: number) => requisicao<DiaHeatmap[]>(`/api/analytics/heatmap?ano=${ano}`),

  periodoAnalytics: (granularidade: Granularidade, de: string, ate: string) =>
    requisicao<ComparacaoPeriodo>(
      `/api/analytics/periodo?granularidade=${granularidade}&de=${de}&ate=${ate}`,
    ),

  analyticsCategorias: (de: string, ate: string) =>
    requisicao<PorCategoriaDto>(`/api/analytics/categorias?de=${de}&ate=${ate}`),

  correlacoes: (de: string, ate: string) =>
    requisicao<CorrelacoesDto>(`/api/analytics/correlacoes?de=${de}&ate=${ate}`),

  analyticsGtd: (de: string, ate: string) =>
    requisicao<MetricasGtdDto>(`/api/analytics/gtd?de=${de}&ate=${ate}`),

  checklistRevisao: () => requisicao<ItemChecklist[]>("/api/gtd/revisoes/checklist"),

  revisoes: () => requisicao<RevisaoDto[]>("/api/gtd/revisoes"),

  iniciarRevisao: (semana?: string) =>
    requisicao<RevisaoDto>(`/api/gtd/revisoes${semana ? `?semana=${semana}` : ""}`, { method: "POST" }),

  marcarPasso: (semana: string, passo: PassoRevisao, feito: boolean) =>
    requisicao<RevisaoDto>(`/api/gtd/revisoes/${semana}/passos/${passo}`, {
      method: "PUT",
      body: JSON.stringify({ feito }),
    }),

  concluirRevisao: (semana: string, duracaoMin?: number) =>
    requisicao<RevisaoDto>(
      `/api/gtd/revisoes/${semana}/concluir${duracaoMin ? `?duracaoMin=${duracaoMin}` : ""}`,
      { method: "POST" },
    ),

  retrospectiva: (ano: number) => requisicao<RetrospectivaDto>(`/api/analytics/retrospectiva/${ano}`),

  progressoLivros: () => requisicao<ProgressoLeituraDto[]>("/api/livros/progresso"),

  cursos: () => requisicao<CursoDto[]>("/api/cursos"),

  progressoCursos: () => requisicao<ProgressoCursoDto[]>("/api/cursos/progresso"),

  estudoPorArea: (de?: string, ate?: string) =>
    requisicao<EstudoPorAreaDto>(
      `/api/cursos/estudo${de && ate ? `?de=${de}&ate=${ate}` : ""}`,
    ),

  criarCurso: (dados: DadosCurso) =>
    requisicao<CursoDto>("/api/cursos", { method: "POST", body: JSON.stringify(dados) }),

  atualizarCurso: (id: number, dados: DadosCurso) =>
    requisicao<CursoDto>(`/api/cursos/${id}`, { method: "PUT", body: JSON.stringify(dados) }),

  excluirCurso: (id: number) => requisicao<void>(`/api/cursos/${id}`, { method: "DELETE" }),


  areas: () => requisicao<AreaDto[]>("/api/areas"),

  criarArea: (nome: string) =>
    requisicao<AreaDto>("/api/areas", { method: "POST", body: JSON.stringify({ nome }) }),

  criarLivro: (dados: DadosLivro) =>
    requisicao<LivroDto>("/api/livros", { method: "POST", body: JSON.stringify(dados) }),

  atualizarLivro: (id: number, dados: DadosLivro) =>
    requisicao<LivroDto>(`/api/livros/${id}`, { method: "PUT", body: JSON.stringify(dados) }),

  excluirLivro: (id: number) => requisicao<void>(`/api/livros/${id}`, { method: "DELETE" }),

  livros: () => requisicao<LivroDto[]>("/api/livros"),
  desafios: () => requisicao<DesafioDto[]>("/api/desafios"),
  projetos: () => requisicao<ProjetoDto[]>("/api/projetos"),

  criarProjeto: (dados: DadosProjeto) =>
    requisicao<ProjetoDto>("/api/projetos", { method: "POST", body: JSON.stringify(dados) }),

  atualizarProjeto: (id: number, dados: DadosProjeto) =>
    requisicao<ProjetoDto>(`/api/projetos/${id}`, { method: "PUT", body: JSON.stringify(dados) }),

  progressoProjetos: () => requisicao<ProgressoProjetoDto[]>("/api/gtd/projetos/progresso"),
};
