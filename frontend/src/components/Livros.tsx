import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BookOpen, History, Plus, TrendingDown, TrendingUp, Trash2 } from "lucide-react";
import { api, ErroApi, type ProgressoLeituraDto } from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Select } from "@/components/ui/input";
import { Campo, Card } from "@/components/ui/campo";
import { formatarDuracao } from "@/lib/formato";
import {
  formatarData,
  formatarVelocidade,
  rotuloDificuldade,
  variacaoDeVelocidade,
} from "@/lib/leitura";

function Numero({ rotulo, valor, detalhe }: { rotulo: string; valor: string; detalhe?: string }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-zinc-500">{rotulo}</p>
      <p className="text-sm font-medium">{valor}</p>
      {detalhe && <p className="text-xs text-zinc-600">{detalhe}</p>}
    </div>
  );
}

function Capa({ livro }: { livro: ProgressoLeituraDto }) {
  const [quebrou, setQuebrou] = useState(false);

  // Capa por URL depende de um site de fora; quando some, o cartao mostra um marcador em vez de
  // um icone de imagem quebrada.
  if (!livro.capaUrl || quebrou) {
    return (
      <div className="flex h-24 w-16 shrink-0 items-center justify-center rounded border border-zinc-800 bg-zinc-900">
        <BookOpen className="size-5 text-zinc-700" />
      </div>
    );
  }
  return (
    <img
      src={livro.capaUrl}
      alt={`Capa de ${livro.titulo}`}
      onError={() => setQuebrou(true)}
      className="h-24 w-16 shrink-0 rounded border border-zinc-800 object-cover"
    />
  );
}

function CartaoLivro({
  livro,
  aoExcluir,
  aoMudarStatus,
}: {
  livro: ProgressoLeituraDto;
  aoExcluir: (id: number) => void;
  aoMudarStatus: (livro: ProgressoLeituraDto, status: string) => void;
}) {
  const variacao = variacaoDeVelocidade(livro);
  const concluido = livro.status === "CONCLUIDO";

  return (
    <Card className="flex gap-4">
      <Capa livro={livro} />

      <div className="flex min-w-0 flex-1 flex-col gap-3">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-sm font-medium">{livro.titulo}</p>
            <p className="text-xs text-zinc-500">
              {livro.autor ?? "sem autor"}
              {livro.totalPaginas ? ` · ${livro.totalPaginas} paginas` : ""}
            </p>
            <div className="mt-1 flex flex-wrap items-center gap-1.5">
              {livro.area && (
                <span className="rounded-full border border-violet-500/30 bg-violet-500/15 px-2 py-0.5 text-xs text-violet-300">
                  {livro.area}
                </span>
              )}
              {livro.dificuldade && (
                <span
                  className="rounded-full border border-zinc-700 px-2 py-0.5 text-xs text-zinc-400"
                  title={`Dificuldade ${livro.dificuldade} de 5`}
                >
                  {rotuloDificuldade(livro.dificuldade)}
                </span>
              )}
              {livro.retroativo && (
                <span
                  className="flex items-center gap-1 rounded-full border border-zinc-700 px-2 py-0.5 text-xs text-zinc-500"
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
              <option value="ABANDONADO">abandonado</option>
            </Select>
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

        {livro.percentualLido !== null && (
          <div>
            <div className="flex items-baseline justify-between text-xs">
              <span className="text-zinc-400">
                pagina {livro.ultimaPagina}
                {livro.totalPaginas ? ` de ${livro.totalPaginas}` : ""}
              </span>
              <span className="text-zinc-400">{livro.percentualLido}%</span>
            </div>
            <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-zinc-800">
              <div
                className={`h-full rounded-full ${concluido ? "bg-violet-500" : "bg-emerald-500"}`}
                style={{ width: `${livro.percentualLido}%` }}
              />
            </div>
          </div>
        )}

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

        {variacao !== null && (
          <p
            className={`flex items-center gap-1 text-xs ${
              variacao > 0 ? "text-emerald-400" : "text-amber-400"
            }`}
          >
            {variacao > 0 ? <TrendingUp className="size-3.5" /> : <TrendingDown className="size-3.5" />}
            {variacao > 0
              ? `esta fluindo melhor: ${variacao}% acima da media do livro`
              : `mais devagar que a media do livro (${variacao}%)`}
          </p>
        )}
      </div>
    </Card>
  );
}

function Estatisticas() {
  const dados = useQuery({ queryKey: ["estatisticas-leitura"], queryFn: api.estatisticasLeitura });

  const categorias = (dados.data?.porCategoria ?? []).filter((c) => c.concluidos + c.lendo > 0);
  const dificuldades = dados.data?.porDificuldade ?? [];

  if (categorias.length === 0) {
    return null;
  }
  const maior = Math.max(...categorias.map((c) => c.concluidos + c.lendo));

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <p className="mb-3 text-sm font-medium">Livros por categoria</p>
        <ul className="flex flex-col gap-2">
          {categorias.map((categoria) => (
            <li key={categoria.categoria}>
              <div className="flex items-baseline justify-between text-sm">
                <span className="text-zinc-300">{categoria.categoria}</span>
                <span className="text-xs text-zinc-500">
                  {categoria.concluidos} lido(s)
                  {categoria.lendo > 0 && ` · ${categoria.lendo} em leitura`}
                </span>
              </div>
              <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-zinc-800">
                <div
                  className="h-full rounded-full bg-violet-500"
                  style={{ width: `${((categoria.concluidos + categoria.lendo) / maior) * 100}%` }}
                />
              </div>
            </li>
          ))}
        </ul>
      </Card>

      <Card>
        <p className="mb-1 text-sm font-medium">Velocidade por dificuldade</p>
        <p className="mb-3 text-xs text-zinc-500">quanto um livro denso custa a mais de tempo</p>
        {dificuldades.length === 0 ? (
          <p className="text-sm text-zinc-500">
            Informe a dificuldade dos livros para ver a comparacao.
          </p>
        ) : (
          <ul className="flex flex-col gap-2 text-sm">
            {dificuldades.map((nivel) => (
              <li key={nivel.dificuldade} className="flex items-baseline justify-between">
                <span className="text-zinc-300">{rotuloDificuldade(nivel.dificuldade)}</span>
                <span className="text-xs text-zinc-500">
                  {formatarVelocidade(nivel.paginasPorHora)} · {nivel.livros} livro(s)
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

export function Livros() {
  const [aberto, setAberto] = useState(false);
  const [retroativo, setRetroativo] = useState(false);
  const [titulo, setTitulo] = useState("");
  const [autor, setAutor] = useState("");
  const [totalPaginas, setTotalPaginas] = useState("");
  const [capaUrl, setCapaUrl] = useState("");
  const [dificuldade, setDificuldade] = useState("");
  const [areaId, setAreaId] = useState("");
  const [diasLeitura, setDiasLeitura] = useState("");
  const [horasLeitura, setHorasLeitura] = useState("");
  const [concluidoEm, setConcluidoEm] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const livros = useQuery({ queryKey: ["progresso-livros"], queryFn: api.progressoLivros });
  const areas = useQuery({ queryKey: ["areas"], queryFn: api.areas });

  const invalidar = () => queryClient.invalidateQueries();

  function limpar() {
    setTitulo("");
    setAutor("");
    setTotalPaginas("");
    setCapaUrl("");
    setDificuldade("");
    setAreaId("");
    setDiasLeitura("");
    setHorasLeitura("");
    setConcluidoEm("");
    setAberto(false);
    setErro(null);
  }

  const criar = useMutation({
    mutationFn: () =>
      api.criarLivro({
        titulo: titulo.trim(),
        autor: autor.trim() || null,
        totalPaginas: totalPaginas === "" ? null : Number(totalPaginas),
        capaUrl: capaUrl.trim() || null,
        dificuldade: dificuldade === "" ? null : Number(dificuldade),
        areaId: areaId === "" ? null : Number(areaId),
        diasLeitura: retroativo && diasLeitura !== "" ? Number(diasLeitura) : null,
        horasLeitura: retroativo && horasLeitura !== "" ? Number(horasLeitura) : null,
        concluidoEm: retroativo && concluidoEm !== "" ? concluidoEm : null,
      }),
    onSuccess: () => {
      limpar();
      invalidar();
    },
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para salvar o livro."),
  });

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
        status: status as "LENDO" | "CONCLUIDO" | "ABANDONADO",
      }),
    onSuccess: invalidar,
  });

  const excluir = useMutation({
    mutationFn: api.excluirLivro,
    onSuccess: invalidar,
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para excluir."),
  });

  const lendo = livros.data?.filter((l) => l.status === "LENDO") ?? [];
  const outros = livros.data?.filter((l) => l.status !== "LENDO") ?? [];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <p className="flex items-center gap-2 text-sm font-medium">
          <BookOpen className="size-4 text-violet-400" />
          Livros
        </p>
        <Button tamanho="sm" onClick={() => setAberto(!aberto)}>
          <Plus className="size-4" />
          Novo livro
        </Button>
      </div>

      {aberto && (
        <Card>
          <div className="mb-3 flex gap-1">
            <Button
              variante={retroativo ? "fantasma" : "secundario"}
              tamanho="sm"
              onClick={() => setRetroativo(false)}
            >
              Vou ler agora
            </Button>
            <Button
              variante={retroativo ? "secundario" : "fantasma"}
              tamanho="sm"
              onClick={() => setRetroativo(true)}
            >
              Ja li (retroativo)
            </Button>
          </div>

          <form
            className="grid gap-3 sm:grid-cols-4"
            onSubmit={(evento) => {
              evento.preventDefault();
              criar.mutate();
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
              ajuda="Endereco da imagem da capa, copiado de qualquer site de livros. Se o link sair do ar, o cartao mostra um marcador no lugar."
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
                    className="h-9 w-full rounded-md border border-zinc-800 bg-zinc-900 px-2 text-sm"
                  />
                </Campo>
              </>
            )}

            <div className="sm:col-span-4 flex items-center gap-2">
              <Button type="submit" disabled={criar.isPending || titulo.trim() === ""}>
                {retroativo ? "Cadastrar leitura antiga" : "Cadastrar"}
              </Button>
              <Button type="button" variante="fantasma" onClick={limpar}>
                Cancelar
              </Button>
              {retroativo && (
                <span className="text-xs text-zinc-500">
                  livros retroativos contam na estante e nas categorias, mas nao entram nos graficos
                  de evolucao
                </span>
              )}
            </div>
          </form>
        </Card>
      )}

      {erro && <p className="text-sm text-rose-400">{erro}</p>}

      <Estatisticas />

      {livros.isLoading && <p className="text-sm text-zinc-500">Carregando...</p>}
      {!livros.isLoading && (livros.data?.length ?? 0) === 0 && (
        <Card className="text-sm text-zinc-500">
          Nenhum livro cadastrado. Cadastre um para registrar leituras e acompanhar o ritmo.
        </Card>
      )}

      {lendo.map((livro) => (
        <CartaoLivro
          key={livro.livroId}
          livro={livro}
          aoExcluir={(id) => excluir.mutate(id)}
          aoMudarStatus={(l, status) => atualizar.mutate({ livro: l, status })}
        />
      ))}

      {outros.length > 0 && (
        <>
          <p className="mt-2 text-xs uppercase tracking-wide text-zinc-500">Ja lidos</p>
          {outros.map((livro) => (
            <CartaoLivro
              key={livro.livroId}
              livro={livro}
              aoExcluir={(id) => excluir.mutate(id)}
              aoMudarStatus={(l, status) => atualizar.mutate({ livro: l, status })}
            />
          ))}
        </>
      )}
    </div>
  );
}
