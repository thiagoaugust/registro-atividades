import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { api, CATEGORIAS, type Granularidade, type Variacao } from "@/api";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/input";
import { Campo, Card } from "@/components/ui/campo";
import { Heatmap } from "@/components/Heatmap";
import { Retrospectiva } from "@/components/Retrospectiva";
import { COR_HEATMAP, DIAS_SEMANA, corDaVariacao, forcaDaCorrelacao, formatarVariacao } from "@/lib/analytics";
import { formatarDuracao } from "@/lib/formato";

const CORES_GRAFICO: Record<string, string> = {
  TREINO: "#10b981",
  ESTUDO: "#0ea5e9",
  LEITURA: "#8b5cf6",
  DESAFIO: "#f59e0b",
  PROJETO: "#f43f5e",
};

const EIXO = { stroke: "#52525b", fontSize: 11 };
const TOOLTIP = {
  contentStyle: { background: "#18181b", border: "1px solid #3f3f46", borderRadius: 6, fontSize: 12 },
};

function hojeLocal(): string {
  return new Date().toLocaleDateString("sv-SE", { timeZone: "America/Sao_Paulo" });
}

function diasAtras(dias: number): string {
  const data = new Date();
  data.setDate(data.getDate() - dias);
  return data.toLocaleDateString("sv-SE", { timeZone: "America/Sao_Paulo" });
}

function Indicador({
  rotulo,
  valor,
  variacao,
}: {
  rotulo: string;
  valor: string;
  variacao: Variacao;
}) {
  return (
    <Card>
      <p className="text-xs uppercase tracking-wide text-zinc-500">{rotulo}</p>
      <p className="mt-1 text-xl font-semibold">{valor}</p>
      <p className={`mt-0.5 text-xs ${corDaVariacao(variacao.percentual)}`}>
        {formatarVariacao(variacao.percentual)} vs periodo anterior
      </p>
    </Card>
  );
}

export function Analytics() {
  const [granularidade, setGranularidade] = useState<Granularidade>("DIA");
  const [de, setDe] = useState(diasAtras(29));
  const [ate, setAte] = useState(hojeLocal());
  const [ano, setAno] = useState(new Date().getFullYear());

  const periodo = useQuery({
    queryKey: ["analytics-periodo", granularidade, de, ate],
    queryFn: () => api.periodoAnalytics(granularidade, de, ate),
  });
  const categorias = useQuery({
    queryKey: ["analytics-categorias", de, ate],
    queryFn: () => api.analyticsCategorias(de, ate),
  });
  const correlacoes = useQuery({
    queryKey: ["analytics-correlacoes", de, ate],
    queryFn: () => api.correlacoes(de, ate),
  });
  const gtd = useQuery({
    queryKey: ["analytics-gtd", de, ate],
    queryFn: () => api.analyticsGtd(de, ate),
  });

  // Recharts quer uma linha por bucket com uma coluna por categoria.
  const barrasEmpilhadas = (() => {
    const porPeriodo = new Map<string, Record<string, number | string>>();
    for (const item of periodo.data?.porCategoria ?? []) {
      const linha = porPeriodo.get(item.periodo) ?? { periodo: item.periodo };
      linha[item.categoria] = item.minutos;
      porPeriodo.set(item.periodo, linha);
    }
    return [...porPeriodo.values()];
  })();

  const distribuicao = (() => {
    const totais = { DIFICIL: 0, NORMAL: 0, BOM: 0, EXCELENTE: 0 };
    for (const ponto of periodo.data?.serie ?? []) {
      totais.DIFICIL += ponto.dificeis;
      totais.NORMAL += ponto.normais;
      totais.BOM += ponto.bons;
      totais.EXCELENTE += ponto.excelentes;
    }
    return Object.entries(totais).map(([classificacao, dias]) => ({ classificacao, dias }));
  })();

  const pace = categorias.data?.treino.paceMedioSegPorKm;

  return (
    <div className="flex flex-col gap-5">
      <Card className="grid gap-3 sm:grid-cols-4">
        <Campo rotulo="De">
          <input
            type="date"
            value={de}
            onChange={(e) => setDe(e.target.value)}
            className="h-9 w-full rounded-md border border-zinc-800 bg-zinc-900 px-2 text-sm"
          />
        </Campo>
        <Campo rotulo="Ate">
          <input
            type="date"
            value={ate}
            onChange={(e) => setAte(e.target.value)}
            className="h-9 w-full rounded-md border border-zinc-800 bg-zinc-900 px-2 text-sm"
          />
        </Campo>
        <Campo rotulo="Agrupar por">
          <Select value={granularidade} onChange={(e) => setGranularidade(e.target.value as Granularidade)}>
            <option value="DIA">dia</option>
            <option value="SEMANA">semana</option>
            <option value="MES">mes</option>
            <option value="ANO">ano</option>
          </Select>
        </Campo>
        <Campo rotulo="Atalhos">
          <div className="flex gap-1">
            <Button variante="secundario" tamanho="sm" onClick={() => { setDe(diasAtras(6)); setAte(hojeLocal()); setGranularidade("DIA"); }}>
              7d
            </Button>
            <Button variante="secundario" tamanho="sm" onClick={() => { setDe(diasAtras(29)); setAte(hojeLocal()); setGranularidade("DIA"); }}>
              30d
            </Button>
            <Button variante="secundario" tamanho="sm" onClick={() => { setDe(diasAtras(364)); setAte(hojeLocal()); setGranularidade("MES"); }}>
              1a
            </Button>
          </div>
        </Campo>
      </Card>

      {periodo.data && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Indicador rotulo="XP" valor={String(periodo.data.xp.atual)} variacao={periodo.data.xp} />
          <Indicador
            rotulo="Tempo"
            valor={formatarDuracao(periodo.data.minutos.atual)}
            variacao={periodo.data.minutos}
          />
          <Indicador
            rotulo="Indice medio"
            valor={periodo.data.indiceMedio.atual.toFixed(1)}
            variacao={periodo.data.indiceMedio}
          />
          <Indicador
            rotulo="Dias dificeis vencidos"
            valor={String(periodo.data.diasDificeisVencidos.atual)}
            variacao={periodo.data.diasDificeisVencidos}
          />
        </div>
      )}

      <Card>
        <p className="mb-3 text-sm font-medium">XP e tendencia</p>
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={periodo.data?.serie ?? []}>
            <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
            <XAxis dataKey="periodo" tick={EIXO} />
            <YAxis tick={EIXO} />
            <Tooltip {...TOOLTIP} />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Line type="monotone" dataKey="xp" name="XP" stroke="#10b981" dot={false} strokeWidth={2} />
            <Line
              type="monotone"
              dataKey="xpMediaMovel"
              name="media movel (7)"
              stroke="#71717a"
              dot={false}
              strokeDasharray="4 4"
            />
          </LineChart>
        </ResponsiveContainer>
      </Card>

      <Card>
        <p className="mb-3 text-sm font-medium">Tempo por categoria</p>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={barrasEmpilhadas}>
            <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
            <XAxis dataKey="periodo" tick={EIXO} />
            <YAxis tick={EIXO} />
            <Tooltip {...TOOLTIP} />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            {CATEGORIAS.map((categoria) => (
              <Bar key={categoria} dataKey={categoria} stackId="tempo" fill={CORES_GRAFICO[categoria]} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </Card>

      <div className="grid gap-5 lg:grid-cols-2">
        <Card>
          <p className="mb-3 text-sm font-medium">Como foram os dias</p>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={distribuicao}>
              <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
              <XAxis dataKey="classificacao" tick={EIXO} />
              <YAxis tick={EIXO} allowDecimals={false} />
              <Tooltip {...TOOLTIP} />
              <Bar dataKey="dias" name="dias">
                {distribuicao.map((item) => (
                  <Cell
                    key={item.classificacao}
                    fill={COR_HEATMAP[item.classificacao as keyof typeof COR_HEATMAP]}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card>
          <p className="mb-3 text-sm font-medium">Produtividade por dia da semana</p>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart
              data={(correlacoes.data?.porDiaDaSemana ?? []).map((d) => ({
                ...d,
                rotulo: DIAS_SEMANA[d.diaSemana - 1],
              }))}
            >
              <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
              <XAxis dataKey="rotulo" tick={EIXO} />
              <YAxis tick={EIXO} />
              <Tooltip {...TOOLTIP} />
              <Bar dataKey="indiceMedio" name="indice medio" fill="#0ea5e9" />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <Card>
        <p className="mb-3 text-sm font-medium">Correlacoes</p>
        <div className="grid gap-2 sm:grid-cols-2">
          {correlacoes.data?.coeficientes.map((item) => (
            <div key={item.nome} className="flex items-baseline justify-between gap-2 text-sm">
              <span className="text-zinc-300">{item.nome}</span>
              <span className="text-xs text-zinc-500">
                {item.coeficiente ?? "—"} · {forcaDaCorrelacao(item.coeficiente)} ({item.pares} dias)
              </span>
            </div>
          ))}
        </div>

        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          <div>
            <p className="mb-2 text-xs text-zinc-500">horas de sono x indice do dia</p>
            <ResponsiveContainer width="100%" height={180}>
              <ScatterChart>
                <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
                <XAxis dataKey="x" name="sono" tick={EIXO} type="number" />
                <YAxis dataKey="y" name="indice" tick={EIXO} type="number" />
                <Tooltip {...TOOLTIP} cursor={{ strokeDasharray: "3 3" }} />
                <Scatter data={correlacoes.data?.sonoVersusIndice ?? []} fill="#8b5cf6" />
              </ScatterChart>
            </ResponsiveContainer>
          </div>
          <div>
            <p className="mb-2 text-xs text-zinc-500">energia x XP</p>
            <ResponsiveContainer width="100%" height={180}>
              <ScatterChart>
                <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
                <XAxis dataKey="x" name="energia" tick={EIXO} type="number" />
                <YAxis dataKey="y" name="XP" tick={EIXO} type="number" />
                <Tooltip {...TOOLTIP} cursor={{ strokeDasharray: "3 3" }} />
                <Scatter data={correlacoes.data?.energiaVersusXp ?? []} fill="#10b981" />
              </ScatterChart>
            </ResponsiveContainer>
          </div>
        </div>
      </Card>

      <div className="grid gap-5 lg:grid-cols-2">
        <Card>
          <p className="mb-3 text-sm font-medium">Por categoria</p>
          <dl className="grid grid-cols-2 gap-y-2 text-sm">
            <dt className="text-zinc-500">Distancia</dt>
            <dd>{categorias.data?.treino.km ?? 0} km</dd>
            <dt className="text-zinc-500">Pace medio</dt>
            <dd>
              {pace ? `${Math.floor(pace / 60)}:${String(pace % 60).padStart(2, "0")} /km` : "—"}
            </dd>
            <dt className="text-zinc-500">Paginas lidas</dt>
            <dd>{categorias.data?.leitura.paginas ?? 0}</dd>
            <dt className="text-zinc-500">Livros concluidos</dt>
            <dd>{categorias.data?.leitura.livrosConcluidos ?? 0}</dd>
          </dl>

          {(categorias.data?.temas.length ?? 0) > 0 && (
            <>
              <p className="mt-4 text-xs uppercase tracking-wide text-zinc-500">Horas por tema</p>
              <ul className="mt-1 flex flex-col gap-1 text-sm">
                {categorias.data?.temas.slice(0, 6).map((tema) => (
                  <li key={tema.tema} className="flex justify-between">
                    <span className="text-zinc-300">{tema.tema}</span>
                    <span className="text-zinc-500">{formatarDuracao(tema.minutos)}</span>
                  </li>
                ))}
              </ul>
            </>
          )}

          {(categorias.data?.desafios.length ?? 0) > 0 && (
            <>
              <p className="mt-4 text-xs uppercase tracking-wide text-zinc-500">Desafios</p>
              <ul className="mt-1 flex flex-col gap-2">
                {categorias.data?.desafios.map((desafio) => (
                  <li key={desafio.id}>
                    <div className="flex justify-between text-sm">
                      <span className="text-zinc-300">{desafio.titulo}</span>
                      <span className="text-zinc-500">
                        {desafio.progresso}/{desafio.meta} {desafio.unidade}
                      </span>
                    </div>
                    <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-zinc-800">
                      <div
                        className="h-full rounded-full bg-amber-500"
                        style={{
                          width: `${Math.min((desafio.progresso / desafio.meta) * 100, 100)}%`,
                        }}
                      />
                    </div>
                  </li>
                ))}
              </ul>
            </>
          )}
        </Card>

        <Card>
          <p className="mb-3 text-sm font-medium">GTD</p>
          <dl className="grid grid-cols-2 gap-y-2 text-sm">
            <dt className="text-zinc-500">Capturados</dt>
            <dd>{gtd.data?.capturados ?? 0}</dd>
            <dt className="text-zinc-500">Processados</dt>
            <dd>{gtd.data?.processados ?? 0}</dd>
            <dt className="text-zinc-500">No inbox agora</dt>
            <dd>{gtd.data?.pendentes ?? 0}</dd>
            <dt className="text-zinc-500">Idade media do inbox</dt>
            <dd>
              {gtd.data?.idadeMediaPendentesDias != null
                ? `${gtd.data.idadeMediaPendentesDias} dias`
                : "—"}
            </dd>
            <dt className="text-zinc-500">Acoes concluidas</dt>
            <dd>{gtd.data?.acoesConcluidas ?? 0}</dd>
            <dt className="text-zinc-500">Acoes abertas</dt>
            <dd>{gtd.data?.acoesAbertas ?? 0}</dd>
            <dt className="text-zinc-500">Projetos parados</dt>
            <dd className={gtd.data?.projetosParados ? "text-amber-400" : ""}>
              {gtd.data?.projetosParados ?? 0}
            </dd>
          </dl>
        </Card>
      </div>

      <div className="mt-2 flex items-center gap-2 border-t border-zinc-800 pt-5">
        <Button variante="secundario" tamanho="sm" onClick={() => setAno(ano - 1)}>
          {ano - 1}
        </Button>
        <span className="text-sm text-zinc-400">o ano de {ano}</span>
        <Button
          variante="secundario"
          tamanho="sm"
          onClick={() => setAno(ano + 1)}
          disabled={ano >= new Date().getFullYear()}
        >
          {ano + 1}
        </Button>
      </div>
      <Heatmap ano={ano} />
      <Retrospectiva ano={ano} />
    </div>
  );
}
