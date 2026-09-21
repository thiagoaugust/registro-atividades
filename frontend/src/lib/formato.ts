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

export const CORES_CATEGORIA: Record<Categoria, string> = {
  TREINO: "bg-emerald-500/15 text-emerald-300 border-emerald-500/30",
  ESTUDO: "bg-sky-500/15 text-sky-300 border-sky-500/30",
  LEITURA: "bg-violet-500/15 text-violet-300 border-violet-500/30",
  DESAFIO: "bg-amber-500/15 text-amber-300 border-amber-500/30",
  PROJETO: "bg-rose-500/15 text-rose-300 border-rose-500/30",
};

export const ROTULO_CLASSIFICACAO: Record<Classificacao, string> = {
  DIFICIL: "Dia dificil",
  NORMAL: "Dia normal",
  BOM: "Dia bom",
  EXCELENTE: "Dia excelente",
};

export const CORES_CLASSIFICACAO: Record<Classificacao, string> = {
  DIFICIL: "bg-zinc-500/15 text-zinc-300 border-zinc-500/30",
  NORMAL: "bg-sky-500/15 text-sky-300 border-sky-500/30",
  BOM: "bg-emerald-500/15 text-emerald-300 border-emerald-500/30",
  EXCELENTE: "bg-amber-500/15 text-amber-300 border-amber-500/30",
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
