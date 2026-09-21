import { useQuery } from "@tanstack/react-query";
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Trophy } from "lucide-react";
import { api } from "@/api";
import { Card } from "@/components/ui/campo";
import { COR_HEATMAP, corDaVariacao, formatarVariacao } from "@/lib/analytics";
import { ROTULO_CLASSIFICACAO, formatarDuracao } from "@/lib/formato";

const MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];

function Destaque({
  rotulo,
  valor,
  detalhe,
}: {
  rotulo: string;
  valor: string;
  detalhe?: string;
}) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-zinc-500">{rotulo}</p>
      <p className="text-xl font-semibold">{valor}</p>
      {detalhe && <p className="text-xs text-zinc-500">{detalhe}</p>}
    </div>
  );
}

/** O ano inteiro numa pagina: o que foi feito, quando rendeu, e o que ficou de recorde. */
export function Retrospectiva({ ano }: { ano: number }) {
  const retro = useQuery({ queryKey: ["retrospectiva", ano], queryFn: () => api.retrospectiva(ano) });

  if (retro.isLoading) {
    return <Card className="text-sm text-zinc-500">Montando a retrospectiva...</Card>;
  }
  if (!retro.data || retro.data.totais.registros === 0) {
    return <Card className="text-sm text-zinc-500">Nenhum registro em {ano}.</Card>;
  }

  const dados = retro.data;
  const pace = dados.categorias.treino.paceMedioSegPorKm;
  const classificacoes = [
    { nome: "DIFICIL", dias: dados.dificeis },
    { nome: "NORMAL", dias: dados.normais },
    { nome: "BOM", dias: dados.bons },
    { nome: "EXCELENTE", dias: dados.excelentes },
  ];

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <p className="text-sm font-medium">Retrospectiva {ano}</p>

        <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div>
            <Destaque rotulo="XP no ano" valor={String(dados.totais.xp)} />
            <p className={`text-xs ${corDaVariacao(dados.xp.percentual)}`}>
              {formatarVariacao(dados.xp.percentual)} vs {ano - 1}
            </p>
          </div>
          <div>
            <Destaque rotulo="Tempo" valor={formatarDuracao(dados.totais.minutos)} />
            <p className={`text-xs ${corDaVariacao(dados.minutos.percentual)}`}>
              {formatarVariacao(dados.minutos.percentual)} vs {ano - 1}
            </p>
          </div>
          <div>
            <Destaque rotulo="Dias ativos" valor={String(dados.totais.diasComPresenca)} />
            <p className={`text-xs ${corDaVariacao(dados.diasComPresenca.percentual)}`}>
              {formatarVariacao(dados.diasComPresenca.percentual)} vs {ano - 1}
            </p>
          </div>
          <Destaque
            rotulo="Maior sequencia"
            valor={`${dados.maiorSequencia} dias`}
            detalhe="recorde do ano"
          />
        </div>

        <div className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Destaque
            rotulo="Dias dificeis vencidos"
            valor={String(dados.totais.diasDificeisVencidos)}
            detalhe="entregou mesmo sem estar bem"
          />
          <Destaque rotulo="Registros" valor={String(dados.totais.registros)} />
          <Destaque
            rotulo="Indice medio"
            valor={dados.totais.indiceMedio.toFixed(1)}
            detalhe="media dos dias ativos"
          />
          <Destaque
            rotulo="Revisoes semanais"
            valor={String(dados.revisoesConcluidas)}
            detalhe="de 52 semanas"
          />
        </div>

        {dados.melhorDia && (
          <p className="mt-5 text-sm text-zinc-400">
            O melhor dia foi <span className="text-zinc-100">{dados.melhorDia.data}</span> — {dados.melhorDia.xp}{" "}
            XP, {ROTULO_CLASSIFICACAO[dados.melhorDia.classificacao].toLowerCase()}.
          </p>
        )}
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <p className="mb-3 text-sm font-medium">XP por mes</p>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart
              data={dados.porMes.map((mes) => ({
                ...mes,
                rotulo: MESES[Number(mes.periodo.slice(5, 7)) - 1],
              }))}
            >
              <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
              <XAxis dataKey="rotulo" tick={{ stroke: "#52525b", fontSize: 11 }} />
              <YAxis tick={{ stroke: "#52525b", fontSize: 11 }} />
              <Tooltip
                contentStyle={{
                  background: "#18181b",
                  border: "1px solid #3f3f46",
                  borderRadius: 6,
                  fontSize: 12,
                }}
              />
              <Bar dataKey="xp" name="XP" fill="#10b981" />
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card>
          <p className="mb-3 text-sm font-medium">Como foram os dias do ano</p>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={classificacoes}>
              <CartesianGrid stroke="#27272a" strokeDasharray="3 3" />
              <XAxis dataKey="nome" tick={{ stroke: "#52525b", fontSize: 11 }} />
              <YAxis tick={{ stroke: "#52525b", fontSize: 11 }} allowDecimals={false} />
              <Tooltip
                contentStyle={{
                  background: "#18181b",
                  border: "1px solid #3f3f46",
                  borderRadius: 6,
                  fontSize: 12,
                }}
              />
              <Bar dataKey="dias">
                {classificacoes.map((item) => (
                  <Cell key={item.nome} fill={COR_HEATMAP[item.nome as keyof typeof COR_HEATMAP]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <p className="mb-3 text-sm font-medium">O ano por categoria</p>
          <dl className="grid grid-cols-2 gap-y-2 text-sm">
            <dt className="text-zinc-500">Distancia</dt>
            <dd>{dados.categorias.treino.km} km</dd>
            <dt className="text-zinc-500">Pace medio</dt>
            <dd>{pace ? `${Math.floor(pace / 60)}:${String(pace % 60).padStart(2, "0")} /km` : "—"}</dd>
            <dt className="text-zinc-500">Paginas lidas</dt>
            <dd>{dados.categorias.leitura.paginas}</dd>
            <dt className="text-zinc-500">Livros concluidos</dt>
            <dd>{dados.categorias.leitura.livrosConcluidos}</dd>
            <dt className="text-zinc-500">Acoes concluidas</dt>
            <dd>{dados.gtd.acoesConcluidas}</dd>
            <dt className="text-zinc-500">Itens capturados</dt>
            <dd>{dados.gtd.capturados}</dd>
          </dl>

          {dados.categorias.temas.length > 0 && (
            <>
              <p className="mt-4 text-xs uppercase tracking-wide text-zinc-500">Mais estudado</p>
              <ul className="mt-1 flex flex-col gap-1 text-sm">
                {dados.categorias.temas.slice(0, 5).map((tema) => (
                  <li key={tema.tema} className="flex justify-between">
                    <span className="text-zinc-300">{tema.tema}</span>
                    <span className="text-zinc-500">{formatarDuracao(tema.minutos)}</span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </Card>

        <Card>
          <p className="mb-3 text-sm font-medium">Conquistas de {ano}</p>
          {dados.conquistas.length === 0 ? (
            <p className="text-sm text-zinc-500">Nenhuma conquista desbloqueada neste ano.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {dados.conquistas.map((conquista) => (
                <li key={conquista.codigo} className="flex items-center gap-2 text-sm">
                  <Trophy className="size-4 shrink-0 text-amber-400" />
                  <span className="text-zinc-200">{conquista.titulo}</span>
                  <span className="ml-auto text-xs text-zinc-500">{conquista.dataLocal}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
