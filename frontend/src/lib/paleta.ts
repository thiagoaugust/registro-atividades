/**
 * A paleta em valores, para o que nao pode usar classe do Tailwind — Recharts recebe cor em prop, e
 * o heatmap pinta celula por estilo inline. Os mesmos tokens de `index.css`, num lugar so.
 *
 * Duas regras: cor significa estado, e nao existe vermelho (o sistema nao pune).
 */
export const PALETA = {
  breu: "#10151B",
  placa: "#171E26",
  placaAlta: "#1E2731",
  risco: "#2A333D",
  riscoForte: "#3B4753",
  giz: "#C8D2DC",
  gizFraco: "#7C8894",
  gizApagado: "#5A646F",
  aferido: "#6FCF97",
  latao: "#D9A94C",
  frio: "#6BA8D6",
} as const;

/**
 * Series de categoria nos graficos. Categoria e identidade, nao estado: em vez de cinco matizes
 * brilhantes, cinco valores tirados da propria paleta — distinguiveis sem virar arco-iris.
 */
export const COR_SERIE: Record<string, string> = {
  TREINO: PALETA.aferido,
  ESTUDO: PALETA.frio,
  LEITURA: PALETA.giz,
  DESAFIO: PALETA.latao,
  PROJETO: "#4C5A68",
};

/** Eixos, grade e tooltip dos graficos, no tom dos fios da interface. */
export const EIXO_GRAFICO = { stroke: PALETA.gizApagado, fontSize: 11 };
export const GRADE_GRAFICO = PALETA.risco;
export const TOOLTIP_GRAFICO = {
  contentStyle: {
    background: PALETA.placaAlta,
    border: `1px solid ${PALETA.riscoForte}`,
    borderRadius: 4,
    fontSize: 12,
    color: PALETA.giz,
  },
};
