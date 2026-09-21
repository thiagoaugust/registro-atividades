import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api, CATEGORIAS, type Categoria, type DadosRegistro, type RegistroDto } from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Select, Textarea } from "@/components/ui/input";
import { Campo, Card } from "@/components/ui/campo";

const TECNICAS_ESTUDO = ["LEITURA", "EXERCICIO", "FLASHCARD", "PROJETO_PRATICO"];

/** "5:30" -> 330 segundos por km. Vazio ou malformado volta undefined. */
function paceParaSegundos(texto: string): number | undefined {
  const partes = texto.split(":");
  if (partes.length !== 2) {
    return undefined;
  }
  const minutos = Number(partes[0]);
  const segundos = Number(partes[1]);
  if (!Number.isFinite(minutos) || !Number.isFinite(segundos)) {
    return undefined;
  }
  return minutos * 60 + segundos;
}

function segundosParaPace(segundos: unknown): string {
  if (typeof segundos !== "number") {
    return "";
  }
  return `${Math.floor(segundos / 60)}:${String(segundos % 60).padStart(2, "0")}`;
}

function numeroOuUndefined(texto: string): number | undefined {
  if (texto.trim() === "") {
    return undefined;
  }
  const valor = Number(texto);
  return Number.isFinite(valor) ? valor : undefined;
}

function textoOuUndefined(texto: string): string | undefined {
  return texto.trim() === "" ? undefined : texto.trim();
}

interface Props {
  data: string;
  editando?: RegistroDto | null;
  aoSalvar: (dados: DadosRegistro) => void;
  aoCancelar?: () => void;
  salvando?: boolean;
  erro?: string | null;
}

export function FormularioRegistro({ data, editando, aoSalvar, aoCancelar, salvando, erro }: Props) {
  const [categoria, setCategoria] = useState<Categoria>("TREINO");
  const [duracao, setDuracao] = useState("45");
  const [esforco, setEsforco] = useState(5);
  const [expandido, setExpandido] = useState(false);

  const [titulo, setTitulo] = useState("");
  const [satisfacao, setSatisfacao] = useState("");
  const [notas, setNotas] = useState("");
  const [vinculoId, setVinculoId] = useState("");

  // Detalhes por categoria, cada um como texto do input; a conversao acontece no envio.
  const [modalidade, setModalidade] = useState("");
  const [distancia, setDistancia] = useState("");
  const [pace, setPace] = useState("");
  const [fcMedia, setFcMedia] = useState("");
  const [tema, setTema] = useState("");
  const [fonte, setFonte] = useState("");
  const [tecnica, setTecnica] = useState("");
  const [foco, setFoco] = useState("");
  const [paginaFinal, setPaginaFinal] = useState("");
  const [progresso, setProgresso] = useState("");
  const [marco, setMarco] = useState("");
  const [statusApos, setStatusApos] = useState("");

  const livros = useQuery({ queryKey: ["livros"], queryFn: api.livros, enabled: categoria === "LEITURA" });
  const progressoLivros = useQuery({
    queryKey: ["progresso-livros"],
    queryFn: api.progressoLivros,
    enabled: categoria === "LEITURA",
  });
  const desafios = useQuery({ queryKey: ["desafios"], queryFn: api.desafios, enabled: categoria === "DESAFIO" });
  const projetos = useQuery({ queryKey: ["projetos"], queryFn: api.projetos, enabled: categoria === "PROJETO" });

  useEffect(() => {
    if (!editando) {
      return;
    }
    const d = editando.detalhes;
    setCategoria(editando.categoria);
    setDuracao(String(editando.duracaoMin));
    setEsforco(editando.esforco);
    setTitulo(editando.titulo ?? "");
    setSatisfacao(editando.satisfacao ? String(editando.satisfacao) : "");
    setNotas(editando.notas ?? "");
    setVinculoId(String(editando.livro?.id ?? editando.desafio?.id ?? editando.projeto?.id ?? ""));
    setModalidade(String(d.modalidade ?? ""));
    setDistancia(String(d.distanciaKm ?? ""));
    setPace(segundosParaPace(d.paceSegPorKm));
    setFcMedia(String(d.fcMedia ?? ""));
    setTema(String(d.tema ?? ""));
    setFonte(String(d.fonte ?? ""));
    setTecnica(String(d.tecnica ?? ""));
    setFoco(String(d.foco ?? ""));
    setPaginaFinal(String(d.paginaFinal ?? ""));
    setProgresso(String(d.valorProgresso ?? ""));
    setMarco(String(d.marco ?? ""));
    setStatusApos(String(d.statusApos ?? ""));
    setExpandido(true);
  }, [editando]);

  /** Trocar de categoria zera os detalhes: o backend recusa campo de outra categoria. */
  function trocarCategoria(nova: Categoria) {
    setCategoria(nova);
    setVinculoId("");
    setModalidade("");
    setDistancia("");
    setPace("");
    setFcMedia("");
    setTema("");
    setFonte("");
    setTecnica("");
    setFoco("");
    setPaginaFinal("");
    setProgresso("");
    setMarco("");
    setStatusApos("");
  }

  // Mostrar onde a leitura parou evita a pergunta "em que pagina eu estava?" na hora de registrar.
  const livroEscolhido = progressoLivros.data?.find((l) => String(l.livroId) === vinculoId);
  const dicaDaPagina =
    livroEscolhido && livroEscolhido.ultimaPagina > 0
      ? `voce parou na pagina ${livroEscolhido.ultimaPagina}${
          livroEscolhido.totalPaginas ? ` de ${livroEscolhido.totalPaginas}` : ""
        }`
      : undefined;

  function montarDetalhes(): Record<string, unknown> {
    const detalhes: Record<string, unknown> = {};
    const por = (chave: string, valor: unknown) => {
      if (valor !== undefined) {
        detalhes[chave] = valor;
      }
    };
    switch (categoria) {
      case "TREINO":
        por("modalidade", textoOuUndefined(modalidade));
        por("distanciaKm", numeroOuUndefined(distancia));
        por("paceSegPorKm", paceParaSegundos(pace));
        por("fcMedia", numeroOuUndefined(fcMedia));
        break;
      case "ESTUDO":
        por("tema", textoOuUndefined(tema));
        por("fonte", textoOuUndefined(fonte));
        por("tecnica", textoOuUndefined(tecnica));
        por("foco", numeroOuUndefined(foco));
        break;
      case "LEITURA":
        // So "parei na pagina X": o backend deriva o inicio a partir da ultima pagina do livro.
        por("paginaFinal", numeroOuUndefined(paginaFinal));
        break;
      case "DESAFIO":
        por("valorProgresso", numeroOuUndefined(progresso));
        break;
      case "PROJETO":
        por("marco", textoOuUndefined(marco));
        por("statusApos", textoOuUndefined(statusApos));
        break;
    }
    return detalhes;
  }

  function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    const vinculo = numeroOuUndefined(vinculoId) ?? null;
    aoSalvar({
      dataLocal: data,
      categoria,
      duracaoMin: Number(duracao),
      esforco,
      titulo: textoOuUndefined(titulo) ?? null,
      satisfacao: numeroOuUndefined(satisfacao) ?? null,
      notas: textoOuUndefined(notas) ?? null,
      livroId: categoria === "LEITURA" ? vinculo : null,
      desafioId: categoria === "DESAFIO" ? vinculo : null,
      projetoId: categoria === "PROJETO" ? vinculo : null,
      detalhes: montarDetalhes(),
    });
  }

  return (
    <Card>
      <form onSubmit={enviar} className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Campo rotulo="Categoria">
            <Select value={categoria} onChange={(e) => trocarCategoria(e.target.value as Categoria)}>
              {CATEGORIAS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </Campo>

          <Campo rotulo="Duracao (min)">
            <Input
              type="number"
              min={1}
              max={1440}
              required
              value={duracao}
              onChange={(e) => setDuracao(e.target.value)}
            />
          </Campo>

          <Campo rotulo={`Esforco ${esforco}`} className="col-span-2">
            <input
              type="range"
              min={1}
              max={10}
              value={esforco}
              onChange={(e) => setEsforco(Number(e.target.value))}
              className="h-9 w-full accent-emerald-500"
            />
          </Campo>
        </div>

        {expandido && (
          <div className="flex flex-col gap-3 border-t border-zinc-800 pt-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <Campo rotulo="Titulo">
                <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} placeholder="opcional" />
              </Campo>
              <Campo rotulo="Satisfacao (1-5)">
                <Select value={satisfacao} onChange={(e) => setSatisfacao(e.target.value)}>
                  <option value="">-</option>
                  {[1, 2, 3, 4, 5].map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </Select>
              </Campo>
            </div>

            {categoria === "TREINO" && (
              <div className="grid gap-3 sm:grid-cols-4">
                <Campo rotulo="Modalidade">
                  <Input value={modalidade} onChange={(e) => setModalidade(e.target.value)} placeholder="corrida" />
                </Campo>
                <Campo rotulo="Distancia (km)">
                  <Input type="number" step="0.01" value={distancia} onChange={(e) => setDistancia(e.target.value)} />
                </Campo>
                <Campo rotulo="Pace (min:seg)">
                  <Input value={pace} onChange={(e) => setPace(e.target.value)} placeholder="5:30" />
                </Campo>
                <Campo rotulo="FC media">
                  <Input type="number" value={fcMedia} onChange={(e) => setFcMedia(e.target.value)} />
                </Campo>
              </div>
            )}

            {categoria === "ESTUDO" && (
              <div className="grid gap-3 sm:grid-cols-4">
                <Campo rotulo="Tema">
                  <Input value={tema} onChange={(e) => setTema(e.target.value)} placeholder="Quarkus" />
                </Campo>
                <Campo rotulo="Fonte">
                  <Input value={fonte} onChange={(e) => setFonte(e.target.value)} placeholder="doc oficial" />
                </Campo>
                <Campo rotulo="Tecnica">
                  <Select value={tecnica} onChange={(e) => setTecnica(e.target.value)}>
                    <option value="">-</option>
                    {TECNICAS_ESTUDO.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </Select>
                </Campo>
                <Campo rotulo="Foco (1-5)">
                  <Input type="number" min={1} max={5} value={foco} onChange={(e) => setFoco(e.target.value)} />
                </Campo>
              </div>
            )}

            {categoria === "LEITURA" && (
              <div className="grid gap-3 sm:grid-cols-2">
                <Campo rotulo="Livro">
                  <Select value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                    <option value="">-</option>
                    {livros.data?.map((l) => (
                      <option key={l.id} value={l.id}>
                        {l.titulo}
                      </option>
                    ))}
                  </Select>
                </Campo>
                <Campo rotulo="Parei na pagina" dica={dicaDaPagina}>
                  <Input
                    type="number"
                    min={1}
                    value={paginaFinal}
                    onChange={(e) => setPaginaFinal(e.target.value)}
                  />
                </Campo>
              </div>
            )}

            {categoria === "DESAFIO" && (
              <div className="grid gap-3 sm:grid-cols-2">
                <Campo rotulo="Desafio">
                  <Select value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                    <option value="">-</option>
                    {desafios.data?.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.titulo} ({d.progresso}/{d.metaValor} {d.unidade})
                      </option>
                    ))}
                  </Select>
                </Campo>
                <Campo rotulo="Progresso registrado">
                  <Input type="number" step="0.01" value={progresso} onChange={(e) => setProgresso(e.target.value)} />
                </Campo>
              </div>
            )}

            {categoria === "PROJETO" && (
              <div className="grid gap-3 sm:grid-cols-3">
                <Campo rotulo="Projeto">
                  <Select value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                    <option value="">-</option>
                    {projetos.data?.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.titulo}
                      </option>
                    ))}
                  </Select>
                </Campo>
                <Campo rotulo="Marco">
                  <Input value={marco} onChange={(e) => setMarco(e.target.value)} />
                </Campo>
                <Campo rotulo="Status apos a sessao">
                  <Input value={statusApos} onChange={(e) => setStatusApos(e.target.value)} />
                </Campo>
              </div>
            )}

            <Campo rotulo="Notas">
              <Textarea rows={2} value={notas} onChange={(e) => setNotas(e.target.value)} />
            </Campo>
          </div>
        )}

        {erro && <p className="text-sm text-rose-400">{erro}</p>}

        <div className="flex items-center gap-2">
          <Button type="submit" disabled={salvando}>
            {editando ? "Salvar alteracoes" : "Registrar"}
          </Button>
          <Button type="button" variante="fantasma" onClick={() => setExpandido(!expandido)}>
            {expandido ? "Menos campos" : "Mais campos"}
          </Button>
          {editando && aoCancelar && (
            <Button type="button" variante="secundario" onClick={aoCancelar}>
              Cancelar
            </Button>
          )}
        </div>
      </form>
    </Card>
  );
}
