import { useQuery } from "@tanstack/react-query";
import { api } from "@/api";
import { Card } from "@/components/ui/campo";
import { COR_HEATMAP, DIAS_SEMANA, montarGradeHeatmap } from "@/lib/analytics";
import { ROTULO_CLASSIFICACAO } from "@/lib/formato";
import { formatarDuracao } from "@/lib/formato";

const MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];

/**
 * Heatmap anual estilo GitHub, em CSS grid. Uma biblioteca de calendario aqui seria mais codigo do
 * que as celulas que ela desenharia.
 */
export function Heatmap({ ano }: { ano: number }) {
  const dias = useQuery({ queryKey: ["heatmap", ano], queryFn: () => api.heatmap(ano) });
  const semanas = montarGradeHeatmap(ano, dias.data ?? []);

  return (
    <Card className="overflow-x-auto">
      <div className="flex items-baseline justify-between">
        <p className="text-sm font-medium">Ano de {ano}</p>
        <div className="flex items-center gap-2 text-xs text-zinc-500">
          <span>menos</span>
          {(["DIFICIL", "NORMAL", "BOM", "EXCELENTE"] as const).map((c) => (
            <span
              key={c}
              className="size-3 rounded-sm"
              style={{ backgroundColor: COR_HEATMAP[c] }}
              title={ROTULO_CLASSIFICACAO[c]}
            />
          ))}
          <span>mais</span>
        </div>
      </div>

      <div className="mt-3 flex gap-1">
        <div className="flex shrink-0 flex-col gap-[3px] pt-[18px] text-[10px] text-zinc-600">
          {DIAS_SEMANA.map((dia, i) => (
            <span key={dia} className="h-[11px] leading-[11px]">
              {i % 2 === 1 ? dia : ""}
            </span>
          ))}
        </div>

        <div>
          <div className="mb-1 flex gap-[3px] text-[10px] text-zinc-600">
            {semanas.map((semana, i) => {
              const primeiro = semana.find(Boolean);
              const mes = primeiro ? Number(primeiro.data.slice(5, 7)) - 1 : null;
              const mesAnterior = i === 0 ? null : semanas[i - 1].find(Boolean);
              const mostra =
                mes !== null &&
                (i === 0 || !mesAnterior || Number(mesAnterior.data.slice(5, 7)) - 1 !== mes);
              return (
                <span key={i} className="w-[11px] shrink-0 overflow-visible whitespace-nowrap">
                  {mostra ? MESES[mes] : ""}
                </span>
              );
            })}
          </div>

          <div className="flex gap-[3px]">
            {semanas.map((semana, i) => (
              <div key={i} className="flex flex-col gap-[3px]">
                {semana.map((dia, j) => (
                  <span
                    key={j}
                    className="size-[11px] rounded-sm"
                    style={{
                      backgroundColor: dia ? COR_HEATMAP[dia.classificacao] : COR_HEATMAP.VAZIO,
                      outline: dia?.diaDificilVencido ? "1px solid #fbbf24" : undefined,
                      opacity: dia?.descanso ? 0.45 : 1,
                    }}
                    title={
                      dia
                        ? `${dia.data} · ${ROTULO_CLASSIFICACAO[dia.classificacao]} · ${dia.xp} XP · ${formatarDuracao(dia.minutos)}${
                            dia.diaDificilVencido ? " · dia dificil vencido" : ""
                          }${dia.descanso ? " · descanso" : ""}`
                        : undefined
                    }
                  />
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>

      <p className="mt-3 text-xs text-zinc-600">
        Contorno amarelo marca os dias dificeis vencidos; celulas apagadas sao descanso planejado.
      </p>
    </Card>
  );
}
