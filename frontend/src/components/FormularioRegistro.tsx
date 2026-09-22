import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api, CATEGORIAS, type Categoria, type DadosRegistro, type RegistroDto } from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Select, Textarea } from "@/components/ui/input";
import { Campo, Card } from "@/components/ui/campo";

const TECNICAS_ESTUDO = ["LEITURA", "EXERCICIO", "FLASHCARD", "PROJETO_PRATICO"];

const LOCAIS = ["CASA", "TRABALHO", "TRANSPORTE_PUBLICO", "RUA", "OUTRO"] as const;

const ROTULO_LOCAL: Record<(typeof LOCAIS)[number], string> = {
  CASA: "casa",
  TRABALHO: "trabalho",
  TRANSPORTE_PUBLICO: "transporte publico",
  RUA: "rua",
  OUTRO: "outro",
};

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

/**
 * Onde a sessao aconteceu. Existe por causa de uma bonificacao: ler ou estudar no transporte
 * publico e tempo resgatado de um deslocamento, e vale 30% mais XP.
 */
function CampoLocal({
  valor,
  aoMudar,
  className,
}: {
  valor: string;
  aoMudar: (valor: string) => void;
  className?: string;
}) {
  return (
    <Campo
      rotulo="Onde"
      className={className}
      dica={valor === "TRANSPORTE_PUBLICO" ? "+30% de XP: tempo aproveitado" : undefined}
      ajuda="Onde voce estava. Transporte publico rende 30% mais XP e conta para o trofeu do tempo aproveitado: e tempo que estava perdido no deslocamento e custa mais atencao para virar estudo."
    >
      <Select value={valor} onChange={(e) => aoMudar(e.target.value)}>
        <option value="">-</option>
        {LOCAIS.map((l) => (
          <option key={l} value={l}>
            {ROTULO_LOCAL[l]}
          </option>
        ))}
      </Select>
    </Campo>
  );
}

interface Props {
  data: string;
  editando?: RegistroDto | null;
  /**
   * Pre-selecao vinda de "em andamento". A `chave` sobe a cada clique para o efeito rodar de novo
   * mesmo quando o item escolhido e o mesmo de antes.
   */
  atalho?: { categoria: Categoria; vinculoId?: string; cursoId?: string; chave: number } | null;
  aoSalvar: (dados: DadosRegistro) => void;
  aoCancelar?: () => void;
  salvando?: boolean;
  erro?: string | null;
}

export function FormularioRegistro({
  data,
  editando,
  atalho,
  aoSalvar,
  aoCancelar,
  salvando,
  erro,
}: Props) {
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
  const [minutosPratica, setMinutosPratica] = useState("");
  const [local, setLocal] = useState("");
  const [cursoId, setCursoId] = useState("");
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
  const cursos = useQuery({ queryKey: ["cursos"], queryFn: api.cursos, enabled: categoria === "ESTUDO" });
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
    setMinutosPratica(String(d.minutosPratica ?? ""));
    setLocal(String(d.local ?? ""));
    setCursoId(String(editando.curso?.id ?? ""));
    setPaginaFinal(String(d.paginaFinal ?? ""));
    setProgresso(String(d.valorProgresso ?? ""));
    setMarco(String(d.marco ?? ""));
    setStatusApos(String(d.statusApos ?? ""));
    setExpandido(true);
  }, [editando]);

  // O atalho de "em andamento" abre o formulario ja apontado para o item: a categoria certa, o
  // vinculo escolhido e a secao de detalhes aberta, onde estao a pagina e o local.
  useEffect(() => {
    if (!atalho) {
      return;
    }
    trocarCategoria(atalho.categoria);
    setVinculoId(atalho.vinculoId ?? "");
    setCursoId(atalho.cursoId ?? "");
    setExpandido(true);
  }, [atalho?.chave]);

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
    setMinutosPratica("");
    setLocal("");
    setCursoId("");
    setPaginaFinal("");
    setProgresso("");
    setMarco("");
    setStatusApos("");
  }

  /**
   * So livros em leitura: registrar sessao num livro ja concluido ou abandonado nao faz sentido. A
   * excecao e o livro que o registro em edicao ja aponta — tirar ele da lista apagaria o vinculo de
   * um registro antigo so por o livro ter sido terminado depois.
   */
  const livrosSelecionaveis = (livros.data ?? []).filter(
    (l) => l.status === "LENDO" || String(l.id) === vinculoId,
  );

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
        por("minutosPratica", numeroOuUndefined(minutosPratica));
        por("local", textoOuUndefined(local));
        break;
      case "LEITURA":
        // So "parei na pagina X": o backend deriva o inicio a partir da ultima pagina do livro.
        por("paginaFinal", numeroOuUndefined(paginaFinal));
        por("local", textoOuUndefined(local));
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
      cursoId: categoria === "ESTUDO" && cursoId !== "" ? Number(cursoId) : null,
      desafioId: categoria === "DESAFIO" ? vinculo : null,
      projetoId: categoria === "PROJETO" ? vinculo : null,
      detalhes: montarDetalhes(),
    });
  }

  return (
    <Card>
      <form onSubmit={enviar} className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Campo
            rotulo="Categoria"
            ajuda="O tipo da atividade. Cada categoria tem um peso diferente no XP e um teto diario proprio, para que 4h da mesma coisa nao valham mais que um dia variado."
          >
            <Select value={categoria} onChange={(e) => trocarCategoria(e.target.value as Categoria)}>
              {CATEGORIAS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </Campo>

          <Campo
            rotulo="Duracao (min)"
            ajuda="Quanto tempo a sessao levou. E a base do XP: o tempo e multiplicado pelo esforco e pelo peso da categoria."
          >
            <Input
              type="number"
              min={1}
              max={1440}
              required
              value={duracao}
              onChange={(e) => setDuracao(e.target.value)}
            />
          </Campo>

          <Campo
            rotulo={`Esforco ${esforco}`}
            className="col-span-2"
            ajuda="O quanto custou, de 1 a 10. Multiplica o XP: uma hora puxada rende mais que uma hora leve. E a diferenca entre tempo gasto e esforco de verdade."
          >
            <input
              type="range"
              min={1}
              max={10}
              value={esforco}
              onChange={(e) => setEsforco(Number(e.target.value))}
              className="h-9 w-full accent-aferido"
            />
          </Campo>
        </div>

        {expandido && (
          <div className="flex flex-col gap-3 border-t border-risco pt-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <Campo
                rotulo="Titulo"
                ajuda="Como voce quer reconhecer essa sessao na lista do dia. Se ficar vazio, o nome do livro ou do projeto aparece no lugar."
              >
                <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} placeholder="opcional" />
              </Campo>
              <Campo
                rotulo="Satisfacao (1-5)"
                ajuda="O quanto o RESULTADO te agradou, independente do esforco. Serve para cruzar depois: as sessoes que mais rendem XP sao as mesmas que te deixam satisfeito?"
              >
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
                <Campo
                  rotulo="Modalidade"
                  ajuda="Corrida, musculacao, calistenia. Agrupa os treinos nas metricas por modalidade."
                >
                  <Input value={modalidade} onChange={(e) => setModalidade(e.target.value)} placeholder="corrida" />
                </Campo>
                <Campo
                  rotulo="Distancia (km)"
                  ajuda="Alimenta o total de quilometros e o pace medio. Se o treino estiver vinculado a um desafio de distancia, tambem soma no progresso dele."
                >
                  <Input type="number" step="0.01" value={distancia} onChange={(e) => setDistancia(e.target.value)} />
                </Campo>
                <Campo
                  rotulo="Pace (min:seg)"
                  ajuda="Ritmo por quilometro, como 5:30. Opcional: sem ele, o pace medio e calculado pelo tempo dividido pela distancia."
                >
                  <Input value={pace} onChange={(e) => setPace(e.target.value)} placeholder="5:30" />
                </Campo>
                <Campo
                  rotulo="FC media"
                  ajuda="Frequencia cardiaca media do treino, se o relogio mediu."
                >
                  <Input type="number" value={fcMedia} onChange={(e) => setFcMedia(e.target.value)} />
                </Campo>
              </div>
            )}

            {categoria === "ESTUDO" && (
              <div className="grid gap-3 sm:grid-cols-4">
                <Campo
                  rotulo="Curso"
                  className="sm:col-span-2"
                  ajuda="Vincular a sessao a um curso faz as horas contarem na carga horaria dele e na previsao de termino. Estudo avulso pode ficar sem curso."
                >
                  <Select value={cursoId} onChange={(e) => setCursoId(e.target.value)}>
                    <option value="">nenhum (estudo avulso)</option>
                    {cursos.data
                      ?.filter((c) => c.status === "CURSANDO")
                      .map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.titulo}
                        </option>
                      ))}
                  </Select>
                </Campo>
                <Campo
                  rotulo="Tema"
                  ajuda="O assunto desta sessao, em texto livre. E o detalhe fino do painel Estudo: mostra em que voce gastou as horas dentro de uma area."
                >
                  <Input value={tema} onChange={(e) => setTema(e.target.value)} placeholder="Quarkus" />
                </Campo>
                <Campo
                  rotulo="Fonte"
                  ajuda="De onde veio o conteudo: doc oficial, video, livro, aula."
                >
                  <Input value={fonte} onChange={(e) => setFonte(e.target.value)} placeholder="doc oficial" />
                </Campo>
                <Campo
                  rotulo="Tecnica"
                  ajuda="Como voce estudou. Ler e diferente de fazer exercicio, e registrar isso mostra se voce estuda variando de metodo."
                >
                  <Select value={tecnica} onChange={(e) => setTecnica(e.target.value)}>
                    <option value="">-</option>
                    {TECNICAS_ESTUDO.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </Select>
                </Campo>
                <Campo
                  rotulo="Foco (1-5)"
                  ajuda="O quanto voce conseguiu se concentrar. Diferente de esforco: da para se esforcar muito num dia disperso."
                >
                  <Input type="number" min={1} max={5} value={foco} onChange={(e) => setFoco(e.target.value)} />
                </Campo>
                <CampoLocal valor={local} aoMudar={setLocal} />
                <Campo
                  rotulo="Minutos de pratica"
                  className="sm:col-span-2"
                  dica="quanto da sessao foi exercicio, e nao consumo"
                  ajuda="Pratica deliberada e o tempo em que voce produziu algo ou errou e corrigiu, e nao apenas assistiu. Numa sessao de 60 min podem ter sido 20. E a medida que separa estudar de consumir conteudo."
                >
                  <Input
                    type="number"
                    min={0}
                    max={Number(duracao) || undefined}
                    value={minutosPratica}
                    onChange={(e) => setMinutosPratica(e.target.value)}
                  />
                </Campo>
              </div>
            )}

            {categoria === "LEITURA" && (
              <div className="grid gap-3 sm:grid-cols-2">
                <Campo
                  rotulo="Livro"
                  ajuda="So aparecem os livros com status lendo. Para registrar um livro novo, cadastre antes na aba Livros."
                >
                  <Select value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                    <option value="">-</option>
                    {livrosSelecionaveis.map((l) => (
                      <option key={l.id} value={l.id}>
                        {l.titulo}
                      </option>
                    ))}
                  </Select>
                </Campo>
                <Campo
                  rotulo="Parei na pagina"
                  dica={dicaDaPagina}
                  ajuda="A pagina onde voce parou agora. O sistema calcula sozinho quantas paginas voce leu desde a ultima sessao deste livro."
                >
                  <Input
                    type="number"
                    min={1}
                    value={paginaFinal}
                    onChange={(e) => setPaginaFinal(e.target.value)}
                  />
                </Campo>
                <CampoLocal valor={local} aoMudar={setLocal} className="sm:col-span-2" />
              </div>
            )}

            {categoria === "DESAFIO" && (
              <div className="grid gap-3 sm:grid-cols-2">
                <Campo
                  rotulo="Desafio"
                  ajuda="A meta com prazo que esta sessao faz avancar. O progresso do desafio e a soma dos registros vinculados a ele."
                >
                  <Select value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                    <option value="">-</option>
                    {desafios.data?.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.titulo} ({d.progresso}/{d.metaValor} {d.unidade})
                      </option>
                    ))}
                  </Select>
                </Campo>
                <Campo
                  rotulo="Progresso registrado"
                  ajuda="Quanto voce avancou na unidade do desafio (km, paginas, repeticoes). Soma no total automaticamente."
                >
                  <Input type="number" step="0.01" value={progresso} onChange={(e) => setProgresso(e.target.value)} />
                </Campo>
              </div>
            )}

            {categoria === "PROJETO" && (
              <div className="grid gap-3 sm:grid-cols-3">
                <Campo
                  rotulo="Projeto"
                  ajuda="O projeto que esta sessao faz andar. E o mesmo projeto do GTD: nao existem duas listas."
                >
                  <Select value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                    <option value="">-</option>
                    {projetos.data?.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.titulo}
                      </option>
                    ))}
                  </Select>
                </Campo>
                <Campo
                  rotulo="Marco"
                  ajuda="A entrega concreta desta sessao: o que ficou pronto."
                >
                  <Input value={marco} onChange={(e) => setMarco(e.target.value)} />
                </Campo>
                <Campo
                  rotulo="Status apos a sessao"
                  ajuda="Onde o projeto ficou quando voce parou, para voce saber por onde retomar."
                >
                  <Input value={statusApos} onChange={(e) => setStatusApos(e.target.value)} />
                </Campo>
              </div>
            )}

            <Campo
              rotulo="Notas"
              ajuda="O que voce vai querer lembrar quando reler esse dia: o que funcionou, o que atrapalhou, onde parou."
            >
              <Textarea rows={2} value={notas} onChange={(e) => setNotas(e.target.value)} />
            </Campo>
          </div>
        )}

        {erro && <p className="text-sm text-giz-fraco">{erro}</p>}

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
