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
import { Campo, Secao } from "@/components/ui/campo";
import { Heatmap } from "@/components/Heatmap";
import { Retrospectiva } from "@/components/Retrospectiva";
import { COR_HEATMAP, DIAS_SEMANA, corDaVariacao, forcaDaCorrelacao, formatarVariacao } from "@/lib/analytics";
import { formatarDuracao } from "@/lib/formato";
import { COR_SERIE, EIXO_GRAFICO, GRADE_GRAFICO, PALETA, TOOLTIP_GRAFICO } from "@/lib/paleta";

const CORES_GRAFICO = COR_SERIE;
const EIXO = EIXO_GRAFICO;
const TOOLTIP = TOOLTIP_GRAFICO;

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
    <div>
      <p className="text-[0.8125rem] text-giz-fraco">{rotulo}</p>
      <p className="leitura mt-0.5 text-2xl leading-none text-giz">{valor}</p>
      <p className={`medida mt-1 text-xs ${corDaVariacao(variacao.percentual)}`}>
        {formatarVariacao(variacao.percentual)} vs periodo anterior
      </p>
    </div>
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
      <div className="grid gap-x-6 gap-y-4 border-b border-risco pb-4 sm:grid-cols-4">
        <Campo rotulo="De">
          <input
            type="date"
            value={de}
            onChange={(e) => setDe(e.target.value)}
            className="h-9 w-full rounded-md border border-risco bg-placa px-2 text-sm"
          />
        </Campo>
        <Campo rotulo="Ate">
          <input
            type="date"
            value={ate}
            onChange={(e) => setAte(e.target.value)}
            className="h-9 w-full rounded-md border border-risco bg-placa px-2 text-sm"
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
      </div>

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

      <section className="flex flex-col gap-3">
        <Secao>xp e tendencia</Secao>
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={periodo.data?.serie ?? []}>
            <CartesianGrid stroke={GRADE_GRAFICO} strokeDasharray="3 3" />
            <XAxis dataKey="periodo" tick={EIXO} />
            <YAxis tick={EIXO} />
            <Tooltip {...TOOLTIP} />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Line type="monotone" dataKey="xp" name="XP" stroke={PALETA.aferido} dot={false} strokeWidth={2} />
            <Line
              type="monotone"
              dataKey="xpMediaMovel"
              name="media movel (7)"
              stroke={PALETA.gizApagado}
              dot={false}
              strokeDasharray="4 4"
            />
          </LineChart>
        </ResponsiveContainer>
      </section>

      <section className="flex flex-col gap-3">
        <Secao>tempo por categoria</Secao>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={barrasEmpilhadas}>
            <CartesianGrid stroke={GRADE_GRAFICO} strokeDasharray="3 3" />
            <XAxis dataKey="periodo" tick={EIXO} />
            <YAxis tick={EIXO} />
            <Tooltip {...TOOLTIP} />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            {CATEGORIAS.map((categoria) => (
              <Bar key={categoria} dataKey={categoria} stackId="tempo" fill={CORES_GRAFICO[categoria]} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </section>

      <div className="grid gap-5 lg:grid-cols-2">
        <section className="flex flex-col gap-3">
          <Secao>como foram os dias</Secao>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={distribuicao}>
              <CartesianGrid stroke={GRADE_GRAFICO} strokeDasharray="3 3" />
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
        </section>

        <section className="flex flex-col gap-3">
          <Secao>produtividade por dia da semana</Secao>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart
              data={(correlacoes.data?.porDiaDaSemana ?? []).map((d) => ({
                ...d,
                rotulo: DIAS_SEMANA[d.diaSemana - 1],
              }))}
            >
              <CartesianGrid stroke={GRADE_GRAFICO} strokeDasharray="3 3" />
              <XAxis dataKey="rotulo" tick={EIXO} />
              <YAxis tick={EIXO} />
              <Tooltip {...TOOLTIP} />
              <Bar dataKey="indiceMedio" name="indice medio" fill={PALETA.frio} />
            </BarChart>
          </ResponsiveContainer>
        </section>
      </div>

      <section className="flex flex-col gap-3">
        <Secao>correlacoes</Secao>
        <div className="grid gap-2 sm:grid-cols-2">
          {correlacoes.data?.coeficientes.map((item) => (
            <div key={item.nome} className="flex items-baseline justify-between gap-2 text-sm">
              <span className="text-giz">{item.nome}</span>
              <span className="text-xs text-giz-apagado">
                {item.coeficiente ?? "—"} · {forcaDaCorrelacao(item.coeficiente)} ({item.pares} dias)
              </span>
            </div>
          ))}
        </div>

        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          <div>
            <p className="mb-2 text-xs text-giz-apagado">horas de sono x indice do dia</p>
            <ResponsiveContainer width="100%" height={180}>
              <ScatterChart>
                <CartesianGrid stroke={GRADE_GRAFICO} strokeDasharray="3 3" />
                <XAxis dataKey="x" name="sono" tick={EIXO} type="number" />
                <YAxis dataKey="y" name="indice" tick={EIXO} type="number" />
                <Tooltip {...TOOLTIP} cursor={{ strokeDasharray: "3 3" }} />
                <Scatter data={correlacoes.data?.sonoVersusIndice ?? []} fill={PALETA.giz} />
              </ScatterChart>
            </ResponsiveContainer>
          </div>
          <div>
            <p className="mb-2 text-xs text-giz-apagado">energia x XP</p>
            <ResponsiveContainer width="100%" height={180}>
              <ScatterChart>
                <CartesianGrid stroke={GRADE_GRAFICO} strokeDasharray="3 3" />
                <XAxis dataKey="x" name="energia" tick={EIXO} type="number" />
                <YAxis dataKey="y" name="XP" tick={EIXO} type="number" />
                <Tooltip {...TOOLTIP} cursor={{ strokeDasharray: "3 3" }} />
                <Scatter data={correlacoes.data?.energiaVersusXp ?? []} fill={PALETA.aferido} />
              </ScatterChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      <div className="grid gap-5 lg:grid-cols-2">
        <section className="flex flex-col gap-3">
          <Secao>por categoria</Secao>
          <dl className="grid grid-cols-2 gap-y-2 text-sm">
            <dt className="text-giz-apagado">Distancia</dt>
            <dd>{categorias.data?.treino.km ?? 0} km</dd>
            <dt className="text-giz-apagado">Pace medio</dt>
            <dd>
              {pace ? `${Math.floor(pace / 60)}:${String(pace % 60).padStart(2, "0")} /km` : "—"}
            </dd>
            <dt className="text-giz-apagado">Paginas lidas</dt>
            <dd>{categorias.data?.leitura.paginas ?? 0}</dd>
            <dt className="text-giz-apagado">Livros concluidos</dt>
            <dd>{categorias.data?.leitura.livrosConcluidos ?? 0}</dd>
          </dl>

          {(categorias.data?.temas.length ?? 0) > 0 && (
            <>
              <p className="mt-4 text-[0.8125rem] text-giz-apagado">Horas por tema</p>
              <ul className="mt-1 flex flex-col gap-1 text-sm">
                {categorias.data?.temas.slice(0, 6).map((tema) => (
                  <li key={tema.tema} className="flex justify-between">
                    <span className="text-giz">{tema.tema}</span>
                    <span className="text-giz-apagado">{formatarDuracao(tema.minutos)}</span>
                  </li>
                ))}
              </ul>
            </>
          )}

          {(categorias.data?.desafios.length ?? 0) > 0 && (
            <>
              <p className="mt-4 text-[0.8125rem] text-giz-apagado">Desafios</p>
              <ul className="mt-1 flex flex-col gap-2">
                {categorias.data?.desafios.map((desafio) => (
                  <li key={desafio.id}>
                    <div className="flex justify-between text-sm">
                      <span className="text-giz">{desafio.titulo}</span>
                      <span className="text-giz-apagado">
                        {desafio.progresso}/{desafio.meta} {desafio.unidade}
                      </span>
                    </div>
                    <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-placa-alta">
                      <div
                        className="h-full rounded-full bg-latao"
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
        </section>

        <section className="flex flex-col gap-3">
          <Secao>gtd</Secao>
          <dl className="grid grid-cols-2 gap-y-2 text-sm">
            <dt className="text-giz-apagado">Capturados</dt>
            <dd>{gtd.data?.capturados ?? 0}</dd>
            <dt className="text-giz-apagado">Processados</dt>
            <dd>{gtd.data?.processados ?? 0}</dd>
            <dt className="text-giz-apagado">No inbox agora</dt>
            <dd>{gtd.data?.pendentes ?? 0}</dd>
            <dt className="text-giz-apagado">Idade media do inbox</dt>
            <dd>
              {gtd.data?.idadeMediaPendentesDias != null
                ? `${gtd.data.idadeMediaPendentesDias} dias`
                : "—"}
            </dd>
            <dt className="text-giz-apagado">Acoes concluidas</dt>
            <dd>{gtd.data?.acoesConcluidas ?? 0}</dd>
            <dt className="text-giz-apagado">Acoes abertas</dt>
            <dd>{gtd.data?.acoesAbertas ?? 0}</dd>
            <dt className="text-giz-apagado">Projetos parados</dt>
            <dd className={gtd.data?.projetosParados ? "text-latao" : ""}>
              {gtd.data?.projetosParados ?? 0}
            </dd>
          </dl>
        </section>
      </div>

      <div className="mt-2 flex items-center gap-2 border-t border-risco pt-5">
        <Button variante="secundario" tamanho="sm" onClick={() => setAno(ano - 1)}>
          {ano - 1}
        </Button>
        <span className="text-sm text-giz-fraco">o ano de {ano}</span>
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
