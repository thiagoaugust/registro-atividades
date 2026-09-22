import type { Classificacao, DiaHeatmap } from "@/api";
import { PALETA } from "@/lib/paleta";

/** Cor de fundo de cada celula do heatmap. Dia sem dados fica no cinza mais apagado. */
export const COR_HEATMAP: Record<Classificacao | "VAZIO", string> = {
  VAZIO: PALETA.risco,
  // Dia dificil e neutro, nao alarme: o sistema nao pune um dia em que voce apareceu.
  DIFICIL: PALETA.gizApagado,
  NORMAL: PALETA.frio,
  BOM: PALETA.aferido,
  EXCELENTE: PALETA.latao,
};

/**
 * Monta a grade do heatmap: uma coluna por semana ISO (segunda a domingo), do primeiro ao ultimo dia
 * do ano. Os dias antes da primeira segunda e depois do ultimo domingo entram como celulas vazias,
 * senao as linhas sairiam desalinhadas dos dias da semana.
 */
export function montarGradeHeatmap(ano: number, dias: DiaHeatmap[]): (DiaHeatmap | null)[][] {
  const porData = new Map(dias.map((dia) => [dia.data, dia]));

  const primeiro = new Date(Date.UTC(ano, 0, 1));
  const ultimo = new Date(Date.UTC(ano, 11, 31));

  // getUTCDay: 0 = domingo. Queremos a semana comecando na segunda, como o date_trunc do Postgres.
  const deslocamentoInicial = (primeiro.getUTCDay() + 6) % 7;

  const semanas: (DiaHeatmap | null)[][] = [];
  let semana: (DiaHeatmap | null)[] = Array(deslocamentoInicial).fill(null);

  for (let data = primeiro; data <= ultimo; data = new Date(data.getTime() + 86400000)) {
    const iso = data.toISOString().slice(0, 10);
    semana.push(porData.get(iso) ?? null);
    if (semana.length === 7) {
      semanas.push(semana);
      semana = [];
    }
  }
  if (semana.length > 0) {
    semanas.push([...semana, ...Array(7 - semana.length).fill(null)]);
  }
  return semanas;
}

/** "+12,5%" / "-8%" / "—" quando nao havia base de comparacao. */
export function formatarVariacao(percentual: number | null): string {
  if (percentual === null || percentual === undefined) {
    return "—";
  }
  const sinal = percentual > 0 ? "+" : "";
  return `${sinal}${percentual.toString().replace(".", ",")}%`;
}

export function corDaVariacao(percentual: number | null): string {
  if (percentual === null || percentual === undefined || percentual === 0) {
    return "text-giz-apagado";
  }
  // Queda nao e erro — fica em cinza, nao em vermelho.
  return percentual > 0 ? "text-aferido" : "text-giz-fraco";
}

/** Coeficiente de Pearson em palavras — um numero solto nao diz nada a quem olha o painel. */
export function forcaDaCorrelacao(coeficiente: number | null): string {
  if (coeficiente === null || coeficiente === undefined) {
    return "sem dados";
  }
  const forca = Math.abs(coeficiente);
  const sentido = coeficiente > 0 ? "positiva" : "negativa";
  if (forca < 0.2) {
    return "sem relacao aparente";
  }
  if (forca < 0.4) {
    return `fraca ${sentido}`;
  }
  if (forca < 0.6) {
    return `moderada ${sentido}`;
  }
  return `forte ${sentido}`;
}

export const DIAS_SEMANA = ["seg", "ter", "qua", "qui", "sex", "sab", "dom"];
