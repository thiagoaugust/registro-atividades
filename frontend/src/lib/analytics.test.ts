import { describe, expect, it } from "vitest";
import { forcaDaCorrelacao, formatarVariacao, montarGradeHeatmap } from "./analytics";
import type { DiaHeatmap } from "@/api";

function dia(data: string, xp = 100): DiaHeatmap {
  return {
    data,
    xp,
    minutos: 60,
    indice: 50,
    classificacao: "NORMAL",
    diaDificilVencido: false,
    descanso: false,
  };
}

describe("montarGradeHeatmap", () => {
  it("cobre o ano inteiro em semanas de sete dias", () => {
    const semanas = montarGradeHeatmap(2025, []);

    expect(semanas.every((semana) => semana.length === 7)).toBe(true);
    // 365 dias de 2025 mais o preenchimento das bordas cabem em 53 colunas
    expect(semanas.length).toBe(53);
  });

  it("alinha o primeiro dia no dia da semana certo", () => {
    // 01/01/2025 foi quarta-feira: as duas primeiras celulas (seg e ter) ficam vazias
    const semanas = montarGradeHeatmap(2025, [dia("2025-01-01")]);

    expect(semanas[0][0]).toBeNull();
    expect(semanas[0][1]).toBeNull();
    expect(semanas[0][2]?.data).toBe("2025-01-01");
  });

  it("ano que comeca na segunda nao tem celula vazia na frente", () => {
    // 01/01/2024 foi segunda-feira
    const semanas = montarGradeHeatmap(2024, [dia("2024-01-01")]);
    expect(semanas[0][0]?.data).toBe("2024-01-01");
  });

  it("dia sem dados vira celula nula", () => {
    const semanas = montarGradeHeatmap(2025, [dia("2025-01-02")]);
    const todos = semanas.flat().filter(Boolean);
    expect(todos).toHaveLength(1);
    expect(todos[0]?.data).toBe("2025-01-02");
  });

  it("bissexto tem os 366 dias", () => {
    const semanas = montarGradeHeatmap(2024, []);
    expect(semanas.flat().length % 7).toBe(0);
  });
});

describe("formatarVariacao", () => {
  it("mostra o sinal e a virgula decimal", () => {
    expect(formatarVariacao(12.5)).toBe("+12,5%");
    expect(formatarVariacao(-8)).toBe("-8%");
    expect(formatarVariacao(0)).toBe("0%");
  });

  it("sem base de comparacao nao inventa numero", () => {
    expect(formatarVariacao(null)).toBe("—");
  });
});

describe("forcaDaCorrelacao", () => {
  it("traduz o coeficiente em palavras", () => {
    expect(forcaDaCorrelacao(0.85)).toBe("forte positiva");
    expect(forcaDaCorrelacao(-0.5)).toBe("moderada negativa");
    expect(forcaDaCorrelacao(0.3)).toBe("fraca positiva");
    expect(forcaDaCorrelacao(0.05)).toBe("sem relacao aparente");
  });

  it("sem coeficiente diz que faltam dados", () => {
    expect(forcaDaCorrelacao(null)).toBe("sem dados");
  });
});
