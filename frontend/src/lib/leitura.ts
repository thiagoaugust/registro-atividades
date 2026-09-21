import type { ProgressoLeituraDto } from "@/api";

/** "40 pag/h"; traco quando ainda nao ha sessao com paginas para medir. */
export function formatarVelocidade(paginasPorHora: number | null): string {
  return paginasPorHora === null || paginasPorHora === undefined ? "—" : `${paginasPorHora} pag/h`;
}

/**
 * Quanto o ritmo recente esta acima (ou abaixo) da media do livro, em porcentagem. E o numero que
 * responde "a leitura esta fluindo melhor?" — sem as duas velocidades nao ha o que comparar.
 */
export function variacaoDeVelocidade(livro: ProgressoLeituraDto): number | null {
  const { paginasPorHoraRecente: recente, paginasPorHoraMedia: media } = livro;
  if (recente === null || media === null || media === 0) {
    return null;
  }
  const variacao = ((recente - media) / media) * 100;
  // Diferenca de menos de 5% e ruido de sessao, nao mudanca de ritmo.
  return Math.abs(variacao) < 5 ? null : Math.round(variacao * 10) / 10;
}

/** "2026-10-14" -> "14/10/2026". */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split("-");
  return `${dia}/${mes}/${ano}`;
}

/** 1 a 5 em palavras: um numero solto nao diz se 3 e leve ou denso. */
export function rotuloDificuldade(grau: number): string {
  return (
    { 1: "1 · leve", 2: "2 · tranquilo", 3: "3 · media", 4: "4 · puxado", 5: "5 · denso" }[grau] ??
    String(grau)
  );
}
