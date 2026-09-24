import { describe, expect, it } from "vitest";
import {
  formatarData,
  formatarVelocidade,
  placarDaEstante,
  progressoNaEstante,
  rotuloDificuldade,
  variacaoDeVelocidade,
} from "./leitura";
import type { ProgressoLeituraDto } from "@/api";

function livro(recente: number | null, media: number | null): ProgressoLeituraDto {
  return {
    livroId: 1,
    titulo: "Livro",
    autor: null,
    totalPaginas: 300,
    status: "LENDO",
    concluidoEm: null,
    capaUrl: null,
    dificuldade: 3,
    areaId: 1,
    area: "Tecnico",
    retroativo: false,
    diasLeitura: null,
    horasLeitura: null,
    ultimaPagina: 100,
    paginasLidas: 100,
    paginasRestantes: 200,
    percentualLido: 33.3,
    sessoes: 5,
    minutos: 200,
    esforcoMedio: 4,
    paginasPorHoraRecente: recente,
    paginasPorHoraMedia: media,
    ritmoDiario: 5,
    diasRestantes: 40,
    previsaoTermino: "2026-10-31",
    primeiraSessao: "2026-09-01",
    ultimaSessao: "2026-09-21",
  };
}

describe("formatarVelocidade", () => {
  it("mostra a unidade", () => {
    expect(formatarVelocidade(42.5)).toBe("42.5 pag/h");
  });

  it("sem medicao nao inventa numero", () => {
    expect(formatarVelocidade(null)).toBe("—");
  });
});

describe("variacaoDeVelocidade", () => {
  it("acusa quando a leitura acelerou", () => {
    expect(variacaoDeVelocidade(livro(40, 30))).toBe(33.3);
  });

  it("acusa quando travou", () => {
    expect(variacaoDeVelocidade(livro(20, 40))).toBe(-50);
  });

  it("diferenca pequena e ruido, nao tendencia", () => {
    expect(variacaoDeVelocidade(livro(41, 40))).toBeNull();
  });

  it("sem as duas velocidades nao ha comparacao", () => {
    expect(variacaoDeVelocidade(livro(null, 40))).toBeNull();
    expect(variacaoDeVelocidade(livro(40, null))).toBeNull();
    expect(variacaoDeVelocidade(livro(40, 0))).toBeNull();
  });
});

describe("rotuloDificuldade", () => {
  it("traduz o grau em palavras", () => {
    expect(rotuloDificuldade(1)).toBe("1 · leve");
    expect(rotuloDificuldade(5)).toBe("5 · denso");
  });

  it("grau desconhecido nao quebra a tela", () => {
    expect(rotuloDificuldade(9)).toBe("9");
  });
});

describe("formatarData", () => {
  it("vira o formato brasileiro", () => {
    expect(formatarData("2026-10-14")).toBe("14/10/2026");
  });
});

describe("progressoNaEstante", () => {
  it("em leitura usa o percentual lido", () => {
    expect(progressoNaEstante(livro(null, null))).toBe(33.3);
  });

  it("em leitura sem total de paginas nao tem barra", () => {
    expect(progressoNaEstante({ ...livro(null, null), percentualLido: null })).toBeNull();
  });

  it("concluido enche a barra mesmo sem total de paginas", () => {
    expect(
      progressoNaEstante({ ...livro(null, null), status: "CONCLUIDO", percentualLido: null }),
    ).toBe(100);
  });

  it("na fila nao tem barra", () => {
    expect(
      progressoNaEstante({ ...livro(null, null), status: "QUERO_LER", percentualLido: 0 }),
    ).toBeNull();
  });

  it("abandonado nao tem barra", () => {
    expect(progressoNaEstante({ ...livro(null, null), status: "ABANDONADO" })).toBeNull();
  });
});

describe("placarDaEstante", () => {
  const com = (status: ProgressoLeituraDto["status"], paginasLidas: number) => ({
    ...livro(null, null),
    status,
    paginasLidas,
  });

  it("estante vazia zera tudo", () => {
    expect(placarDaEstante([])).toEqual({ lidos: 0, paginasLidas: 0, lendo: 0, naFila: 0 });
  });

  it("conta livros por prateleira e soma as paginas de todos, abandonado inclusive", () => {
    const placar = placarDaEstante([
      com("CONCLUIDO", 300),
      com("CONCLUIDO", 200),
      com("LENDO", 80),
      com("ABANDONADO", 40),
      com("QUERO_LER", 0),
    ]);
    expect(placar).toEqual({ lidos: 2, paginasLidas: 620, lendo: 1, naFila: 1 });
  });
});
