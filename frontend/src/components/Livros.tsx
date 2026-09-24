import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  BookOpen,
  History,
  LayoutGrid,
  List,
  Pencil,
  Plus,
  TrendingDown,
  TrendingUp,
  Trash2,
} from "lucide-react";
import {
  api,
  ErroApi,
  type DadosLivro,
  type ProgressoLeituraDto,
  type StatusLivro,
} from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Select } from "@/components/ui/input";
import { Campo, Card, Secao } from "@/components/ui/campo";
import { formatarDuracao } from "@/lib/formato";
import { cn } from "@/lib/utils";
import {
  formatarData,
  formatarVelocidade,
  placarDaEstante,
  progressoNaEstante,
  rotuloDificuldade,
  variacaoDeVelocidade,
} from "@/lib/leitura";

function Numero({ rotulo, valor, detalhe }: { rotulo: string; valor: string; detalhe?: string }) {
  return (
    <div>
      <p className="text-[0.8125rem] text-giz-fraco">{rotulo}</p>
      <p className="medida text-sm text-giz">{valor}</p>
      {detalhe && <p className="text-xs text-giz-apagado">{detalhe}</p>}
    </div>
  );
}

function Capa({
  livro,
  className = "h-24 w-16",
  comTitulo = false,
}: {
  livro: ProgressoLeituraDto;
  className?: string;
  /** Na estante o marcador leva titulo e autor: o mesmo icone repetido na grade nao diz qual e qual. */
  comTitulo?: boolean;
}) {
  const [quebrou, setQuebrou] = useState(false);

  // Capa por URL depende de um site de fora; quando some, o cartao mostra um marcador em vez de
  // um icone de imagem quebrada.
  if (!livro.capaUrl || quebrou) {
    return (
      <div
        className={cn(
          "flex shrink-0 flex-col items-center justify-center gap-1 overflow-hidden rounded border border-risco bg-placa p-2 text-center",
          className,
        )}
      >
        {comTitulo ? (
          <>
            <p className="line-clamp-4 text-xs font-medium text-giz">{livro.titulo}</p>
            {livro.autor && (
              <p className="line-clamp-2 text-[0.6875rem] text-giz-apagado">{livro.autor}</p>
            )}
          </>
        ) : (
          <BookOpen className="size-5 text-giz-apagado" />
        )}
      </div>
    );
  }
  return (
    <img
      src={livro.capaUrl}
      alt={`Capa de ${livro.titulo}`}
      onError={() => setQuebrou(true)}
      className={cn("shrink-0 rounded border border-risco object-cover", className)}
    />
  );
}

function Estante({
  livros,
  selecionado,
  aoSelecionar,
}: {
  livros: ProgressoLeituraDto[];
  selecionado: number | null;
  aoSelecionar: (id: number) => void;
}) {
  return (
    <ul className="grid grid-cols-3 gap-x-4 gap-y-5 sm:grid-cols-6">
      {livros.map((livro) => {
        const progresso = progressoNaEstante(livro);
        const ativo = selecionado === livro.livroId;
        return (
          <li key={livro.livroId}>
            <button
              type="button"
              onClick={() => aoSelecionar(livro.livroId)}
              aria-pressed={ativo}
              title={livro.autor ? `${livro.titulo} — ${livro.autor}` : livro.titulo}
              className={cn(
                "group flex w-full flex-col gap-1.5 rounded text-left outline-none focus-visible:ring-2 focus-visible:ring-giz/40",
                livro.status === "ABANDONADO" && "opacity-45 grayscale",
              )}
            >
              <Capa
                livro={livro}
                comTitulo
                className={cn(
                  "aspect-[2/3] w-full transition group-hover:border-risco-forte",
                  ativo && "border-giz ring-1 ring-giz",
                )}
              />
              <div className="h-0.5 overflow-hidden bg-risco">
                {progresso !== null && (
                  <div className="h-full bg-aferido" style={{ width: `${progresso}%` }} />
                )}
              </div>
            </button>
          </li>
        );
      })}
    </ul>
  );
}

function CartaoLivro({
  livro,
  aoEditar,
  aoExcluir,
  aoMudarStatus,
}: {
  livro: ProgressoLeituraDto;
  aoEditar: () => void;
  aoExcluir: (id: number) => void;
  aoMudarStatus: (livro: ProgressoLeituraDto, status: string) => void;
}) {
  const variacao = variacaoDeVelocidade(livro);
  // Livro na fila nao tem sessao: progresso e numeros seriam so tracos.
  const naFila = livro.status === "QUERO_LER";

  return (
    <div className="flex gap-4 border-b border-risco/60 pb-4">
      <Capa livro={livro} />

      <div className="flex min-w-0 flex-1 flex-col gap-3">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-sm font-medium">{livro.titulo}</p>
            <p className="text-xs text-giz-apagado">
              {livro.autor ?? "sem autor"}
              {livro.totalPaginas ? ` · ${livro.totalPaginas} paginas` : ""}
            </p>
            <div className="mt-1 flex flex-wrap items-center gap-1.5">
              {livro.area && (
                <span className="rounded-full border border-risco-forte bg-giz/10 px-2 py-0.5 text-xs text-giz">
                  {livro.area}
                </span>
              )}
              {livro.dificuldade && (
                <span
                  className="rounded-full border border-risco-forte px-2 py-0.5 text-xs text-giz-fraco"
                  title={`Dificuldade ${livro.dificuldade} de 5`}
                >
                  {rotuloDificuldade(livro.dificuldade)}
                </span>
              )}
              {livro.retroativo && (
                <span
                  className="flex items-center gap-1 rounded-full border border-risco-forte px-2 py-0.5 text-xs text-giz-apagado"
                  title="Cadastrado como leitura antiga; nao entra nos graficos de evolucao"
                >
                  <History className="size-3" />
                  retroativo
                </span>
              )}
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-1">
            <Select
              value={livro.status}
              onChange={(e) => aoMudarStatus(livro, e.target.value)}
              className="h-8 w-32 text-xs"
              aria-label={`Status de ${livro.titulo}`}
            >
              <option value="LENDO">lendo</option>
              <option value="CONCLUIDO">concluido</option>
              <option value="QUERO_LER">quero ler</option>
              <option value="ABANDONADO">abandonado</option>
            </Select>
            <Button
              variante="fantasma"
              tamanho="icone"
              aria-label={`Editar ${livro.titulo}`}
              onClick={aoEditar}
            >
              <Pencil className="size-4" />
            </Button>
            <Button
              variante="perigo"
              tamanho="icone"
              aria-label={`Excluir ${livro.titulo}`}
              onClick={() => aoExcluir(livro.livroId)}
            >
              <Trash2 className="size-4" />
            </Button>
          </div>
        </div>

        {!naFila && livro.percentualLido !== null && (
          <div>
            <div className="flex items-baseline justify-between text-xs">
              <span className="text-giz-fraco">
                pagina {livro.ultimaPagina}
                {livro.totalPaginas ? ` de ${livro.totalPaginas}` : ""}
              </span>
              <span className="text-giz-fraco">{livro.percentualLido}%</span>
            </div>
            <div className="mt-1 h-0.5 overflow-hidden bg-risco">
              <div
                className="h-full rounded-full bg-aferido"
                style={{ width: `${livro.percentualLido}%` }}
              />
            </div>
          </div>
        )}

        {!naFila && (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Numero
              rotulo="Velocidade"
              valor={formatarVelocidade(livro.paginasPorHoraRecente ?? livro.paginasPorHoraMedia)}
              detalhe={
                livro.paginasPorHoraRecente !== null && !livro.retroativo
                  ? `media do livro ${formatarVelocidade(livro.paginasPorHoraMedia)}`
                  : undefined
              }
            />
            <Numero
              rotulo="Ritmo"
              valor={livro.ritmoDiario !== null ? `${livro.ritmoDiario} pag/dia` : "—"}
              detalhe={livro.retroativo ? "no periodo informado" : "ultimos 14 dias"}
            />
            <Numero
              rotulo={livro.retroativo ? "Concluido" : "Previsao"}
              valor={
                livro.retroativo
                  ? livro.concluidoEm
                    ? formatarData(livro.concluidoEm)
                    : "—"
                  : livro.previsaoTermino
                    ? formatarData(livro.previsaoTermino)
                    : "—"
              }
              detalhe={
                livro.retroativo
                  ? undefined
                  : livro.diasRestantes !== null
                    ? `em ${livro.diasRestantes} dias`
                    : undefined
              }
            />
            {/* Retroativo nao tem esforco por sessao — o que se sabe dele e o tempo total. */}
            {livro.retroativo ? (
              <Numero
                rotulo="Tempo total"
                valor={livro.minutos > 0 ? formatarDuracao(livro.minutos) : "—"}
                detalhe={livro.minutos > 0 ? undefined : "nao informado"}
              />
            ) : (
              <Numero
                rotulo="Esforco medio"
                valor={livro.esforcoMedio !== null ? String(livro.esforcoMedio) : "—"}
                detalhe={`${livro.sessoes} sessao(oes) · ${formatarDuracao(livro.minutos)}`}
              />
            )}
          </div>
        )}

        {variacao !== null && (
          <p
            className={`flex items-center gap-1 text-xs ${
              variacao > 0 ? "text-aferido" : "text-latao"
            }`}
          >
            {variacao > 0 ? <TrendingUp className="size-3.5" /> : <TrendingDown className="size-3.5" />}
            {variacao > 0
              ? `esta fluindo melhor: ${variacao}% acima da media do livro`
              : `mais devagar que a media do livro (${variacao}%)`}
          </p>
        )}
      </div>
    </div>
  );
}

type Modo = "lendo" | "fila" | "retroativo";

/**
 * Cadastro e edicao no mesmo formulario: editar e o cadastro preenchido. Na edicao o status fica de
 * fora — ele muda pelo seletor do cartao — e o modo so decide se os campos do retroativo aparecem.
 */
function FormularioLivro({
  livro,
  aoTerminar,
}: {
  livro?: ProgressoLeituraDto;
  aoTerminar: () => void;
}) {
  const editando = livro !== undefined;
  const [modo, setModo] = useState<Modo>(livro?.retroativo ? "retroativo" : "lendo");
  const [titulo, setTitulo] = useState(livro?.titulo ?? "");
  const [autor, setAutor] = useState(livro?.autor ?? "");
  const [totalPaginas, setTotalPaginas] = useState(livro?.totalPaginas?.toString() ?? "");
  const [capaUrl, setCapaUrl] = useState(livro?.capaUrl ?? "");
  const [dificuldade, setDificuldade] = useState(livro?.dificuldade?.toString() ?? "");
  const [areaId, setAreaId] = useState(livro?.areaId?.toString() ?? "");
  const [diasLeitura, setDiasLeitura] = useState(livro?.diasLeitura?.toString() ?? "");
  const [horasLeitura, setHorasLeitura] = useState(livro?.horasLeitura?.toString() ?? "");
  const [concluidoEm, setConcluidoEm] = useState(livro?.concluidoEm ?? "");
  const [erro, setErro] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const areas = useQuery({ queryKey: ["areas"], queryFn: api.areas });

  const retroativo = modo === "retroativo";

  const salvar = useMutation({
    mutationFn: () => {
      const dados: DadosLivro = {
        titulo: titulo.trim(),
        autor: autor.trim() || null,
        totalPaginas: totalPaginas === "" ? null : Number(totalPaginas),
        capaUrl: capaUrl.trim() || null,
        dificuldade: dificuldade === "" ? null : Number(dificuldade),
        areaId: areaId === "" ? null : Number(areaId),
        diasLeitura: retroativo && diasLeitura !== "" ? Number(diasLeitura) : null,
        horasLeitura: retroativo && horasLeitura !== "" ? Number(horasLeitura) : null,
        concluidoEm: retroativo && concluidoEm !== "" ? concluidoEm : null,
      };
      if (livro) {
        return api.atualizarLivro(livro.livroId, {
          ...dados,
          status: livro.status,
          // Livro lido pelo sistema guarda a data em que a ultima sessao o fechou.
          concluidoEm: retroativo ? dados.concluidoEm : livro.concluidoEm,
        });
      }
      return api.criarLivro({ ...dados, status: modo === "fila" ? "QUERO_LER" : undefined });
    },
    onSuccess: () => {
      queryClient.invalidateQueries();
      aoTerminar();
    },
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para salvar o livro."),
  });

  const rotuloSalvar = editando
    ? "Salvar"
    : { lendo: "Cadastrar", fila: "Por na fila", retroativo: "Cadastrar leitura antiga" }[modo];

  return (
    <Card>
      {!editando && (
        <div className="mb-3 flex flex-wrap gap-1">
          {(
            [
              ["lendo", "Vou ler agora"],
              ["fila", "Quero ler"],
              ["retroativo", "Ja li (retroativo)"],
            ] as const
          ).map(([valor, rotulo]) => (
            <Button
              key={valor}
              variante={modo === valor ? "secundario" : "fantasma"}
              tamanho="sm"
              onClick={() => setModo(valor)}
            >
              {rotulo}
            </Button>
          ))}
        </div>
      )}

      <form
        className="grid gap-3 sm:grid-cols-4"
        onSubmit={(evento) => {
          evento.preventDefault();
          salvar.mutate();
        }}
      >
        <Campo rotulo="Titulo" className="sm:col-span-2">
          <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} autoFocus required />
        </Campo>
        <Campo rotulo="Autor">
          <Input value={autor} onChange={(e) => setAutor(e.target.value)} />
        </Campo>
        <Campo
          rotulo="Total de paginas"
          dica="da a previsao e o percentual"
          ajuda="Sem o total, o livro ainda mede velocidade, mas nao ha percentual lido nem previsao de termino."
        >
          <Input
            type="number"
            min={1}
            value={totalPaginas}
            onChange={(e) => setTotalPaginas(e.target.value)}
          />
        </Campo>

        <Campo
          rotulo="Area"
          ajuda="O assunto do livro. A mesma lista vale para cursos, entao o painel Estudo soma livro e curso ao responder quanto tempo foi para cada area."
        >
          <Select value={areaId} onChange={(e) => setAreaId(e.target.value)}>
            <option value="">-</option>
            {areas.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
              </option>
            ))}
          </Select>
        </Campo>
        <Campo
          rotulo="Dificuldade"
          ajuda="O quanto o texto e denso, na sua leitura. Cruzado com a velocidade real, mostra quanto um livro puxado custa a mais de tempo que um leve."
        >
          <Select value={dificuldade} onChange={(e) => setDificuldade(e.target.value)}>
            <option value="">-</option>
            {[1, 2, 3, 4, 5].map((n) => (
              <option key={n} value={n}>
                {rotuloDificuldade(n)}
              </option>
            ))}
          </Select>
        </Campo>
        <Campo
          rotulo="URL da capa"
          className="sm:col-span-2"
          ajuda="Endereco da imagem da capa, copiado de qualquer site de livros. Se o link sair do ar, a estante mostra titulo e autor no lugar."
        >
          <Input
            value={capaUrl}
            onChange={(e) => setCapaUrl(e.target.value)}
            placeholder="https://..."
          />
        </Campo>

        {retroativo && (
          <>
            <Campo
              rotulo="Dias que levou"
              dica="obrigatorio no retroativo"
              ajuda="Quantos dias corridos entre comecar e terminar. Da o ritmo em paginas por dia deste livro antigo."
            >
              <Input
                type="number"
                min={1}
                value={diasLeitura}
                onChange={(e) => setDiasLeitura(e.target.value)}
                required
              />
            </Campo>
            <Campo
              rotulo="Horas totais"
              dica="opcional; da a velocidade"
              ajuda="Quanto tempo voce passou lendo, se lembrar. So com isso o livro entra na comparacao de velocidade por dificuldade."
            >
              <Input
                type="number"
                step="0.5"
                min={0.5}
                value={horasLeitura}
                onChange={(e) => setHorasLeitura(e.target.value)}
              />
            </Campo>
            <Campo
              rotulo="Terminei em"
              className="sm:col-span-2"
              ajuda="Quando voce fechou o livro. E a data que posiciona ele na retrospectiva do ano."
            >
              <input
                type="date"
                value={concluidoEm}
                onChange={(e) => setConcluidoEm(e.target.value)}
                className="h-9 w-full rounded-md border border-risco bg-placa px-2 text-sm"
              />
            </Campo>
          </>
        )}

        <div className="flex flex-wrap items-center gap-2 sm:col-span-4">
          <Button type="submit" disabled={salvar.isPending || titulo.trim() === ""}>
            {rotuloSalvar}
          </Button>
          <Button type="button" variante="fantasma" onClick={aoTerminar}>
            Cancelar
          </Button>
          {!editando && retroativo && (
            <span className="text-xs text-giz-apagado">
              livros retroativos contam na estante e nas categorias, mas nao entram nos graficos
              de evolucao
            </span>
          )}
          {!editando && modo === "fila" && (
            <span className="text-xs text-giz-apagado">
              a primeira leitura registrada passa o livro para "lendo"
            </span>
          )}
        </div>
        {erro && <p className="text-sm text-giz-fraco sm:col-span-4">{erro}</p>}
      </form>
    </Card>
  );
}

function Placar({ livros }: { livros: ProgressoLeituraDto[] }) {
  const placar = placarDaEstante(livros);
  const itens = [
    { rotulo: "Livros lidos", valor: placar.lidos },
    { rotulo: "Paginas lidas", valor: placar.paginasLidas },
    { rotulo: "Lendo", valor: placar.lendo },
    { rotulo: "Quero ler", valor: placar.naFila },
  ];
  return (
    <dl className="grid grid-cols-2 gap-x-6 gap-y-4 border-y border-risco py-4 sm:grid-cols-4">
      {itens.map((item) => (
        <div key={item.rotulo}>
          <dt className="text-[0.8125rem] text-giz-fraco">{item.rotulo}</dt>
          <dd className="leitura text-2xl leading-none text-giz">
            {item.valor.toLocaleString("pt-BR")}
          </dd>
        </div>
      ))}
    </dl>
  );
}

type Vista = "capas" | "lista";

const CHAVE_VISTA = "livros.vista";

// A vista e conveniencia de quem olha: sem storage (aba privada, site bloqueado) cai nas capas.
function vistaLembrada(): Vista {
  try {
    return localStorage.getItem(CHAVE_VISTA) === "lista" ? "lista" : "capas";
  } catch {
    return "capas";
  }
}

function lembrarVista(vista: Vista) {
  try {
    localStorage.setItem(CHAVE_VISTA, vista);
  } catch {
    // sem storage, a escolha vale ate recarregar
  }
}

/** A ordem em que se abre a estante: o que esta na mao, o que ja foi, o que vem. */
const PRATELEIRAS: { status: StatusLivro; rotulo: string }[] = [
  { status: "LENDO", rotulo: "Lendo" },
  { status: "CONCLUIDO", rotulo: "Ja lidos" },
  { status: "QUERO_LER", rotulo: "Quero ler" },
  { status: "ABANDONADO", rotulo: "Abandonados" },
];

export function Livros() {
  const [vista, setVista] = useState<Vista>(vistaLembrada);
  const [selecionado, setSelecionado] = useState<number | null>(null);
  const [cadastrando, setCadastrando] = useState(false);
  const [editando, setEditando] = useState<number | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const livros = useQuery({ queryKey: ["progresso-livros"], queryFn: api.progressoLivros });

  const invalidar = () => queryClient.invalidateQueries();

  const atualizar = useMutation({
    mutationFn: ({ livro, status }: { livro: ProgressoLeituraDto; status: string }) =>
      api.atualizarLivro(livro.livroId, {
        titulo: livro.titulo,
        autor: livro.autor,
        totalPaginas: livro.totalPaginas,
        capaUrl: livro.capaUrl,
        dificuldade: livro.dificuldade,
        areaId: livro.areaId,
        diasLeitura: livro.diasLeitura,
        horasLeitura: livro.horasLeitura,
        concluidoEm: livro.concluidoEm,
        status: status as StatusLivro,
      }),
    onSuccess: invalidar,
  });

  const excluir = useMutation({
    mutationFn: api.excluirLivro,
    onSuccess: invalidar,
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para excluir."),
  });

  const prateleiras = PRATELEIRAS.map((p) => ({
    ...p,
    livros: livros.data?.filter((l) => l.status === p.status) ?? [],
  })).filter((p) => p.livros.length > 0);
  // Excluido ou sumido da lista, o detalhe fecha sozinho em vez de apontar para nada.
  const detalhe = livros.data?.find((l) => l.livroId === selecionado) ?? null;

  function trocarVista(nova: Vista) {
    setVista(nova);
    lembrarVista(nova);
  }

  function selecionar(id: number) {
    setEditando(null);
    setSelecionado((atual) => (atual === id ? null : id));
  }

  const cartao = (livro: ProgressoLeituraDto) =>
    editando === livro.livroId ? (
      <FormularioLivro key={livro.livroId} livro={livro} aoTerminar={() => setEditando(null)} />
    ) : (
      <CartaoLivro
        key={livro.livroId}
        livro={livro}
        aoEditar={() => setEditando(livro.livroId)}
        aoExcluir={(id) => excluir.mutate(id)}
        aoMudarStatus={(l, status) => atualizar.mutate({ livro: l, status })}
      />
    );

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="flex items-center gap-2 text-sm font-medium">
          <BookOpen className="size-4 text-giz" />
          Livros
        </p>
        <div className="flex items-center gap-2">
          <div className="flex rounded-md border border-risco p-0.5" role="group" aria-label="Vista">
            <Button
              variante={vista === "capas" ? "secundario" : "fantasma"}
              tamanho="sm"
              aria-pressed={vista === "capas"}
              onClick={() => trocarVista("capas")}
            >
              <LayoutGrid className="size-4" />
              capas
            </Button>
            <Button
              variante={vista === "lista" ? "secundario" : "fantasma"}
              tamanho="sm"
              aria-pressed={vista === "lista"}
              onClick={() => trocarVista("lista")}
            >
              <List className="size-4" />
              lista
            </Button>
          </div>
          <Button tamanho="sm" onClick={() => setCadastrando(!cadastrando)}>
            <Plus className="size-4" />
            Novo livro
          </Button>
        </div>
      </div>

      {(livros.data?.length ?? 0) > 0 && <Placar livros={livros.data ?? []} />}

      {cadastrando && <FormularioLivro aoTerminar={() => setCadastrando(false)} />}

      {erro && <p className="text-sm text-giz-fraco">{erro}</p>}

      {livros.isLoading && <p className="text-sm text-giz-apagado">Carregando...</p>}
      {!livros.isLoading && (livros.data?.length ?? 0) === 0 && (
        <p className="py-2 text-sm text-giz-apagado">
          Nenhum livro cadastrado. Cadastre um para registrar leituras e acompanhar o ritmo.
        </p>
      )}

      {prateleiras.map((prateleira, indice) => (
        <section key={prateleira.status} className={cn("flex flex-col gap-3", indice > 0 && "mt-2")}>
          <Secao>{prateleira.rotulo}</Secao>
          {vista === "capas" ? (
            <>
              <Estante
                livros={prateleira.livros}
                selecionado={selecionado}
                aoSelecionar={selecionar}
              />
              {detalhe?.status === prateleira.status && cartao(detalhe)}
            </>
          ) : (
            prateleira.livros.map(cartao)
          )}
        </section>
      ))}
    </div>
  );
}
