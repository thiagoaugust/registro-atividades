import { describe, expect, it } from "vitest";
import {
  esforcoMedio,
  formatarDetalhes,
  formatarDuracao,
  progressoDoNivel,
  somarMinutosPorCategoria,
} from "./formato";
import type { RegistroDto } from "@/api";

function registro(categoria: RegistroDto["categoria"], duracaoMin: number, esforco: number): RegistroDto {
  return {
    id: 1,
    dataLocal: "2026-09-21",
    inicioEm: null,
    duracaoMin,
    categoria,
    titulo: null,
    esforco,
    satisfacao: null,
    notas: null,
    projeto: null,
    desafio: null,
    livro: null,
    detalhes: {},
    criadoEm: "",
    atualizadoEm: "",
  };
}

describe("formatarDuracao", () => {
  it("mostra minutos abaixo de uma hora", () => {
    expect(formatarDuracao(45)).toBe("45min");
    expect(formatarDuracao(59)).toBe("59min");
  });

  it("mostra horas cheias sem minutos", () => {
    expect(formatarDuracao(60)).toBe("1h");
    expect(formatarDuracao(120)).toBe("2h");
  });

  it("preenche os minutos com zero a esquerda", () => {
    expect(formatarDuracao(65)).toBe("1h05");
    expect(formatarDuracao(95)).toBe("1h35");
  });
});

describe("somarMinutosPorCategoria", () => {
  it("agrupa e soma", () => {
    const totais = somarMinutosPorCategoria([
      registro("TREINO", 30, 5),
      registro("TREINO", 45, 7),
      registro("ESTUDO", 60, 4),
    ]);
    expect(totais).toEqual({ TREINO: 75, ESTUDO: 60 });
  });

  it("dia vazio nao tem categoria", () => {
    expect(somarMinutosPorCategoria([])).toEqual({});
  });
});

describe("esforcoMedio", () => {
  it("arredonda para uma casa", () => {
    expect(esforcoMedio([registro("TREINO", 10, 7), registro("ESTUDO", 10, 4)])).toBe(5.5);
    expect(esforcoMedio([registro("TREINO", 10, 8), registro("ESTUDO", 10, 5), registro("LEITURA", 10, 5)]))
      .toBe(6);
  });

  it("sem registros nao inventa media", () => {
    expect(esforcoMedio([])).toBeNull();
  });
});

describe("progressoDoNivel", () => {
  it("mede quanto do nivel atual ja foi percorrido", () => {
    expect(progressoDoNivel({ nivel: 2, xp: 150, xpNesteNivel: 50, xpParaOProximo: 150 })).toBeCloseTo(0.25);
    expect(progressoDoNivel({ nivel: 5, xp: 800, xpNesteNivel: 0, xpParaOProximo: 319 })).toBe(0);
  });

  it("nivel sem faixa conhecida nao divide por zero", () => {
    expect(progressoDoNivel({ nivel: 1, xp: 0, xpNesteNivel: 0, xpParaOProximo: 0 })).toBe(0);
  });
});

describe("formatarDetalhes", () => {
  it("troca as chaves cruas por rotulos legiveis", () => {
    expect(formatarDetalhes({ tema: "Quarkus", foco: 4 })).toBe("tema: Quarkus · foco: 4");
  });

  it("formata pace e distancia com a unidade", () => {
    expect(formatarDetalhes({ distanciaKm: 8.2, paceSegPorKm: 330 })).toBe(
      "distancia 8.2 km · pace 5:30/km",
    );
  });

  it("resume series em vez de despejar o array", () => {
    expect(formatarDetalhes({ series: [{ exercicio: "supino" }, { exercicio: "remada" }] })).toBe(
      "2 serie(s)",
    );
  });

  it("registro sem detalhes nao vira string suja", () => {
    expect(formatarDetalhes({})).toBe("");
  });
});
