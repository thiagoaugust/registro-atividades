import { useQuery } from "@tanstack/react-query";
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Trophy } from "lucide-react";
import { api } from "@/api";
import { Secao } from "@/components/ui/campo";
import { COR_HEATMAP, corDaVariacao, formatarVariacao } from "@/lib/analytics";
import { ROTULO_CLASSIFICACAO, formatarDuracao } from "@/lib/formato";
import { EIXO_GRAFICO, GRADE_GRAFICO, PALETA } from "@/lib/paleta";

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
      <p className="text-[0.8125rem] text-giz-fraco">{rotulo}</p>
      <p className="leitura text-2xl leading-none text-giz">{valor}</p>
      {detalhe && <p className="mt-1 text-xs text-giz-apagado">{detalhe}</p>}
    </div>
  );
}

/** O ano inteiro numa pagina: o que foi feito, quando rendeu, e o que ficou de recorde. */
export function Retrospectiva({ ano }: { ano: number }) {
  const retro = useQuery({ queryKey: ["retrospectiva", ano], queryFn: () => api.retrospectiva(ano) });

  if (retro.isLoading) {
    return <p className="text-sm text-giz-apagado">Montando a retrospectiva...</p>;
  }
  if (!retro.data || retro.data.totais.registros === 0) {
    return <p className="text-sm text-giz-apagado">Nenhum registro em {ano}.</p>;
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
      <section className="flex flex-col gap-3">
        <Secao>retrospectiva {ano}</Secao>

        <div className="grid grid-cols-2 gap-x-6 gap-y-5 sm:grid-cols-4">
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
          <p className="mt-5 text-sm text-giz-fraco">
            O melhor dia foi <span className="text-giz">{dados.melhorDia.data}</span> — {dados.melhorDia.xp}{" "}
            XP, {ROTULO_CLASSIFICACAO[dados.melhorDia.classificacao].toLowerCase()}.
          </p>
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="flex flex-col gap-3">
          <Secao>xp por mes</Secao>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart
              data={dados.porMes.map((mes) => ({
                ...mes,
                rotulo: MESES[Number(mes.periodo.slice(5, 7)) - 1],
              }))}
            >
              <CartesianGrid stroke={GRADE_GRAFICO} strokeDasharray="3 3" />
              <XAxis dataKey="rotulo" tick={EIXO_GRAFICO} />
              <YAxis tick={EIXO_GRAFICO} />
              <Tooltip
                contentStyle={{
                  background: PALETA.placaAlta,
                  border: `1px solid ${PALETA.riscoForte}`,
                  borderRadius: 6,
                  fontSize: 12,
                }}
              />
              <Bar dataKey="xp" name="XP" fill={PALETA.aferido} />
            </BarChart>
          </ResponsiveContainer>
        </section>

        <section className="flex flex-col gap-3">
          <Secao>como foram os dias do ano</Secao>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={classificacoes}>
              <CartesianGrid stroke={GRADE_GRAFICO} strokeDasharray="3 3" />
              <XAxis dataKey="nome" tick={EIXO_GRAFICO} />
              <YAxis tick={EIXO_GRAFICO} allowDecimals={false} />
              <Tooltip
                contentStyle={{
                  background: PALETA.placaAlta,
                  border: `1px solid ${PALETA.riscoForte}`,
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
        </section>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="flex flex-col gap-3">
          <Secao>o ano por categoria</Secao>
          <dl className="grid grid-cols-2 gap-y-2 text-sm">
            <dt className="text-giz-apagado">Distancia</dt>
            <dd>{dados.categorias.treino.km} km</dd>
            <dt className="text-giz-apagado">Pace medio</dt>
            <dd>{pace ? `${Math.floor(pace / 60)}:${String(pace % 60).padStart(2, "0")} /km` : "—"}</dd>
            <dt className="text-giz-apagado">Paginas lidas</dt>
            <dd>{dados.categorias.leitura.paginas}</dd>
            <dt className="text-giz-apagado">Livros concluidos</dt>
            <dd>{dados.categorias.leitura.livrosConcluidos}</dd>
            <dt className="text-giz-apagado">Acoes concluidas</dt>
            <dd>{dados.gtd.acoesConcluidas}</dd>
            <dt className="text-giz-apagado">Itens capturados</dt>
            <dd>{dados.gtd.capturados}</dd>
          </dl>

          {dados.categorias.temas.length > 0 && (
            <>
              <p className="mt-4 text-[0.8125rem] text-giz-apagado">Mais estudado</p>
              <ul className="mt-1 flex flex-col gap-1 text-sm">
                {dados.categorias.temas.slice(0, 5).map((tema) => (
                  <li key={tema.tema} className="flex justify-between">
                    <span className="text-giz">{tema.tema}</span>
                    <span className="text-giz-apagado">{formatarDuracao(tema.minutos)}</span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>

        <section className="flex flex-col gap-3">
          <Secao>conquistas de {ano}</Secao>
          {dados.conquistas.length === 0 ? (
            <p className="text-sm text-giz-apagado">Nenhuma conquista desbloqueada neste ano.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {dados.conquistas.map((conquista) => (
                <li key={conquista.codigo} className="flex items-center gap-2 text-sm">
                  <Trophy className="size-4 shrink-0 text-latao" />
                  <span className="text-giz">{conquista.titulo}</span>
                  <span className="ml-auto text-xs text-giz-apagado">{conquista.dataLocal}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
