import type { Categoria, Classificacao, NivelDto, RegistroDto } from "@/api";

/** 95 -> "1h35"; 45 -> "45min". */
export function formatarDuracao(minutos: number): string {
  if (minutos < 60) {
    return `${minutos}min`;
  }
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return resto === 0 ? `${horas}h` : `${horas}h${String(resto).padStart(2, "0")}`;
}

export function somarMinutosPorCategoria(registros: RegistroDto[]): Record<string, number> {
  const totais: Record<string, number> = {};
  for (const r of registros) {
    totais[r.categoria] = (totais[r.categoria] ?? 0) + r.duracaoMin;
  }
  return totais;
}

export function esforcoMedio(registros: RegistroDto[]): number | null {
  if (registros.length === 0) {
    return null;
  }
  const soma = registros.reduce((total, r) => total + r.esforco, 0);
  return Math.round((soma / registros.length) * 10) / 10;
}

/** Rotulo curto para o dia: "2026-09-21" -> "seg, 21 de set". */
export function rotuloDoDia(iso: string): string {
  const [ano, mes, dia] = iso.split("-").map(Number);
  const data = new Date(ano, mes - 1, dia);
  return data
    .toLocaleDateString("pt-BR", { weekday: "short", day: "2-digit", month: "short" })
    .replace(".", "");
}

/**
 * Categoria e identidade, nao estado — e a palavra ja diz qual e. Cinco matizes brilhantes eram
 * decoracao: um chip neutro para todas, e a cor fica reservada para o que e estado de verdade.
 */
export const CORES_CATEGORIA: Record<Categoria, string> = {
  TREINO: "border-risco text-giz-fraco",
  ESTUDO: "border-risco text-giz-fraco",
  LEITURA: "border-risco text-giz-fraco",
  DESAFIO: "border-risco text-giz-fraco",
  PROJETO: "border-risco text-giz-fraco",
};

export const ROTULO_CLASSIFICACAO: Record<Classificacao, string> = {
  DIFICIL: "Dia dificil",
  NORMAL: "Dia normal",
  BOM: "Dia bom",
  EXCELENTE: "Dia excelente",
};

/** Aqui cor significa estado, e por isso existe. */
export const CORES_CLASSIFICACAO: Record<Classificacao, string> = {
  DIFICIL: "text-giz-fraco",
  NORMAL: "text-frio",
  BOM: "text-aferido",
  EXCELENTE: "text-latao",
};

/** Quanto do nivel atual ja foi percorrido, de 0 a 1. */
export function progressoDoNivel(nivel: NivelDto): number {
  const faixa = nivel.xpNesteNivel + nivel.xpParaOProximo;
  return faixa === 0 ? 0 : nivel.xpNesteNivel / faixa;
}

const ROTULOS_DETALHE: Record<string, string> = {
  modalidade: "modalidade",
  distanciaKm: "distancia",
  paceSegPorKm: "pace",
  fcMedia: "FC",
  tema: "tema",
  fonte: "fonte",
  tecnica: "tecnica",
  foco: "foco",
  paginaInicial: "da pagina",
  paginaFinal: "ate",
  valorProgresso: "progresso",
  marco: "marco",
  statusApos: "status",
  series: "series",
};

/** "tema: Quarkus · foco: 4" em vez de JSON com aspas na cara do usuario. */
export function formatarDetalhes(detalhes: Record<string, unknown>): string {
  return Object.entries(detalhes)
    .map(([chave, valor]) => {
      const rotulo = ROTULOS_DETALHE[chave] ?? chave;
      if (chave === "paceSegPorKm" && typeof valor === "number") {
        return `${rotulo} ${Math.floor(valor / 60)}:${String(valor % 60).padStart(2, "0")}/km`;
      }
      if (chave === "distanciaKm") {
        return `${rotulo} ${valor} km`;
      }
      if (chave === "series" && Array.isArray(valor)) {
        return `${valor.length} serie(s)`;
      }
      if (typeof valor === "object" && valor !== null) {
        return `${rotulo}: ${JSON.stringify(valor)}`;
      }
      return `${rotulo}: ${valor}`;
    })
    .join(" · ");
}

/** "Tarefas (uma por linha)": cada linha com texto vira uma tarefa; as em branco somem. */
export function linhasDeTarefa(texto: string): string[] {
  return texto
    .split(/\r?\n/)
    .map((linha) => linha.trim())
    .filter((linha) => linha !== "");
}
