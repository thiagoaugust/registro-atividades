import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Dumbbell, ExternalLink, GraduationCap, History, Plus, Trash2 } from "lucide-react";
import { api, ErroApi, type ProgressoCursoDto } from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Select } from "@/components/ui/input";
import { Campo, Card, Secao } from "@/components/ui/campo";
import { formatarDuracao } from "@/lib/formato";
import { formatarData } from "@/lib/leitura";

function Numero({ rotulo, valor, detalhe }: { rotulo: string; valor: string; detalhe?: string }) {
  return (
    <div>
      <p className="text-[0.8125rem] text-giz-fraco">{rotulo}</p>
      <p className="medida text-sm text-giz">{valor}</p>
      {detalhe && <p className="text-xs text-giz-apagado">{detalhe}</p>}
    </div>
  );
}

/**
 * A barra mostra duas coisas de uma vez: quanto do curso foi feito e quanto desse tempo foi pratica
 * deliberada. Sao as duas perguntas que interessam num curso em andamento.
 */
function CartaoCurso({
  curso,
  aoExcluir,
  aoMudarStatus,
}: {
  curso: ProgressoCursoDto;
  aoExcluir: (id: number) => void;
  aoMudarStatus: (curso: ProgressoCursoDto, status: string) => void;
}) {
  return (
    <div className="flex flex-col gap-3 border-b border-risco/60 pb-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="flex items-center gap-2 text-sm font-medium">
            {curso.titulo}
            {curso.url && (
              <a href={curso.url} target="_blank" rel="noreferrer" className="text-frio">
                <ExternalLink className="size-3.5" />
              </a>
            )}
          </p>
          <p className="text-xs text-giz-apagado">
            {curso.instituicao ?? "sem instituicao"}
            {curso.cargaHoraria ? ` · ${curso.cargaHoraria}h de carga` : " · carga nao informada"}
          </p>
          <div className="mt-1 flex flex-wrap items-center gap-1.5">
            {curso.area && (
              <span className="rounded-full border border-frio/30 bg-frio/15 px-2 py-0.5 text-xs text-frio">
                {curso.area}
              </span>
            )}
            {curso.retroativo && (
              <span
                className="flex items-center gap-1 rounded-full border border-risco-forte px-2 py-0.5 text-xs text-giz-apagado"
                title="Cadastrado como curso antigo; nao entra nos graficos de evolucao"
              >
                <History className="size-3" />
                retroativo
              </span>
            )}
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-1">
          <Select
            value={curso.status}
            onChange={(e) => aoMudarStatus(curso, e.target.value)}
            className="h-8 w-32 text-xs"
            aria-label={`Status de ${curso.titulo}`}
          >
            <option value="CURSANDO">cursando</option>
            <option value="CONCLUIDO">concluido</option>
            <option value="ABANDONADO">abandonado</option>
          </Select>
          <Button
            variante="perigo"
            tamanho="icone"
            aria-label={`Excluir ${curso.titulo}`}
            onClick={() => aoExcluir(curso.cursoId)}
          >
            <Trash2 className="size-4" />
          </Button>
        </div>
      </div>

      {curso.percentualConcluido !== null && (
        <div>
          <div className="flex items-baseline justify-between text-xs">
            <span className="text-giz-fraco">
              {formatarDuracao(curso.minutos)} de {curso.cargaHoraria}h
            </span>
            <span className="text-giz-fraco">{curso.percentualConcluido}%</span>
          </div>
          <div className="mt-1 h-0.5 overflow-hidden bg-risco">
            <div
              className={`h-full rounded-full ${
                curso.status === "CONCLUIDO" ? "bg-frio" : "bg-aferido"
              }`}
              style={{ width: `${curso.percentualConcluido}%` }}
            />
          </div>
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Numero
          rotulo="Dedicado"
          valor={formatarDuracao(curso.minutos)}
          detalhe={curso.retroativo ? "informado no cadastro" : `${curso.sessoes} sessao(oes)`}
        />
        <Numero
          rotulo="Pratica deliberada"
          valor={curso.percentualPratica !== null ? `${curso.percentualPratica}%` : "—"}
          detalhe={
            curso.percentualPratica !== null ? formatarDuracao(curso.minutosPratica) : "sem sessao"
          }
        />
        <Numero
          rotulo="Ritmo"
          valor={curso.horasPorSemana !== null ? `${curso.horasPorSemana} h/sem` : "—"}
          detalhe={curso.retroativo ? "no periodo informado" : "ultimas 4 semanas"}
        />
        <Numero
          rotulo={curso.retroativo || curso.status === "CONCLUIDO" ? "Concluido" : "Previsao"}
          valor={
            curso.status === "CONCLUIDO" || curso.retroativo
              ? curso.concluidoEm
                ? formatarData(curso.concluidoEm)
                : "—"
              : curso.previsaoTermino
                ? formatarData(curso.previsaoTermino)
                : "—"
          }
          detalhe={
            !curso.retroativo && curso.diasRestantes !== null
              ? `em ${curso.diasRestantes} dias`
              : undefined
          }
        />
      </div>

      {curso.percentualPratica !== null && curso.minutos > 0 && (
        <div>
          <div className="h-0.5 overflow-hidden bg-risco">
            <div
              className="h-full bg-latao"
              style={{ width: `${curso.percentualPratica}%` }}
            />
          </div>
          <p className="mt-1 flex items-center gap-1 text-xs text-giz-apagado">
            <Dumbbell className="size-3" />
            {curso.percentualPratica >= 50
              ? "a maior parte do tempo foi praticando"
              : "a maior parte do tempo foi consumindo conteudo"}
          </p>
        </div>
      )}
    </div>
  );
}

function OndeVaiOTempo() {
  const estudo = useQuery({ queryKey: ["estudo-por-area"], queryFn: () => api.estudoPorArea() });

  const areas = (estudo.data?.porArea ?? []).filter((a) => a.minutosCurso + a.minutosLivro > 0);
  const temas = (estudo.data?.porTema ?? []).filter((t) => t.minutos > 0).slice(0, 8);
  const pratica = estudo.data?.pratica;

  if (areas.length === 0 && temas.length === 0) {
    return null;
  }
  const maiorArea = Math.max(1, ...areas.map((a) => a.minutosCurso + a.minutosLivro));
  const maiorTema = Math.max(1, ...temas.map((t) => t.minutos));

  return (
    <div className="grid gap-x-8 gap-y-6 lg:grid-cols-2">
      <section className="flex flex-col gap-2">
        <Secao>Onde vai o tempo</Secao>
        <p className="text-xs text-giz-apagado">por area, somando curso e livro</p>
        <ul className="flex flex-col gap-2">
          {areas.map((area) => (
            <li key={area.area}>
              <div className="flex items-baseline justify-between text-sm">
                <span className="text-giz">{area.area}</span>
                <span className="text-xs text-giz-apagado">
                  {formatarDuracao(area.minutosCurso + area.minutosLivro)}
                </span>
              </div>
              {/* duas cores na mesma barra: o quanto veio de curso e o quanto veio de livro */}
              <div className="mt-1 flex h-0.5 overflow-hidden bg-risco">
                <div
                  className="h-full bg-frio"
                  style={{ width: `${(area.minutosCurso / maiorArea) * 100}%` }}
                />
                <div
                  className="h-full bg-giz-apagado"
                  style={{ width: `${(area.minutosLivro / maiorArea) * 100}%` }}
                />
              </div>
              <p className="mt-0.5 text-xs text-giz-apagado">
                {area.cursos > 0 && `${area.cursos} curso(s)`}
                {area.cursos > 0 && area.livros > 0 && " · "}
                {area.livros > 0 && `${area.livros} livro(s)`}
              </p>
            </li>
          ))}
        </ul>
        <p className="mt-3 flex gap-3 text-xs text-giz-apagado">
          <span className="flex items-center gap-1">
            <span className="size-2 rounded-full bg-frio" /> curso
          </span>
          <span className="flex items-center gap-1">
            <span className="size-2 rounded-full bg-giz-apagado" /> livro
          </span>
        </p>
      </section>

      <section className="flex flex-col gap-2">
        <Secao>Assuntos dos ultimos 30 dias</Secao>
        <p className="text-xs text-giz-apagado">o tema que voce escreve em cada sessao</p>

        {pratica?.percentual !== null && pratica !== undefined && (
          <div className="border-l-2 border-latao pl-3">
            <p className="text-xs text-giz-fraco">
              {pratica.percentual}% do seu estudo foi pratica deliberada
            </p>
            <p className="text-xs text-giz-apagado">
              {formatarDuracao(pratica.minutosPratica)} de {formatarDuracao(pratica.minutosEstudo)}
            </p>
          </div>
        )}

        <ul className="flex flex-col gap-2">
          {temas.map((tema) => (
            <li key={tema.tema}>
              <div className="flex items-baseline justify-between text-sm">
                <span className="truncate text-giz">{tema.tema}</span>
                <span className="shrink-0 text-xs text-giz-apagado">{formatarDuracao(tema.minutos)}</span>
              </div>
              <div className="mt-1 flex h-0.5 overflow-hidden bg-risco">
                <div
                  className="h-full bg-latao"
                  style={{ width: `${(tema.minutosPratica / maiorTema) * 100}%` }}
                  title="pratica deliberada"
                />
                <div
                  className="h-full bg-frio"
                  style={{
                    width: `${((tema.minutos - tema.minutosPratica) / maiorTema) * 100}%`,
                  }}
                  title="consumo"
                />
              </div>
            </li>
          ))}
        </ul>
        <p className="mt-3 flex gap-3 text-xs text-giz-apagado">
          <span className="flex items-center gap-1">
            <span className="size-2 rounded-full bg-latao" /> pratica
          </span>
          <span className="flex items-center gap-1">
            <span className="size-2 rounded-full bg-frio" /> consumo
          </span>
        </p>
      </section>
    </div>
  );
}

export function Cursos() {
  const [aberto, setAberto] = useState(false);
  const [retroativo, setRetroativo] = useState(false);
  const [titulo, setTitulo] = useState("");
  const [instituicao, setInstituicao] = useState("");
  const [url, setUrl] = useState("");
  const [cargaHoraria, setCargaHoraria] = useState("");
  const [areaId, setAreaId] = useState("");
  const [horasRetroativas, setHorasRetroativas] = useState("");
  const [diasRetroativos, setDiasRetroativos] = useState("");
  const [concluidoEm, setConcluidoEm] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const cursos = useQuery({ queryKey: ["progresso-cursos"], queryFn: api.progressoCursos });
  const areas = useQuery({ queryKey: ["areas"], queryFn: api.areas });

  const invalidar = () => queryClient.invalidateQueries();

  function limpar() {
    setTitulo("");
    setInstituicao("");
    setUrl("");
    setCargaHoraria("");
    setAreaId("");
    setHorasRetroativas("");
    setDiasRetroativos("");
    setConcluidoEm("");
    setAberto(false);
    setErro(null);
  }

  const criar = useMutation({
    mutationFn: () =>
      api.criarCurso({
        titulo: titulo.trim(),
        instituicao: instituicao.trim() || null,
        url: url.trim() || null,
        cargaHoraria: cargaHoraria === "" ? null : Number(cargaHoraria),
        areaId: areaId === "" ? null : Number(areaId),
        horasRetroativas: retroativo && horasRetroativas !== "" ? Number(horasRetroativas) : null,
        diasRetroativos: retroativo && diasRetroativos !== "" ? Number(diasRetroativos) : null,
        concluidoEm: retroativo && concluidoEm !== "" ? concluidoEm : null,
      }),
    onSuccess: () => {
      limpar();
      invalidar();
    },
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para salvar o curso."),
  });

  const atualizar = useMutation({
    mutationFn: ({ curso, status }: { curso: ProgressoCursoDto; status: string }) =>
      api.atualizarCurso(curso.cursoId, {
        titulo: curso.titulo,
        instituicao: curso.instituicao,
        url: curso.url,
        cargaHoraria: curso.cargaHoraria,
        areaId: curso.areaId,
        horasRetroativas: curso.retroativo ? curso.minutos / 60 : null,
        diasRetroativos: curso.retroativo && curso.horasPorSemana
          ? Math.round((curso.minutos / 60 / curso.horasPorSemana) * 7)
          : null,
        concluidoEm: curso.concluidoEm,
        status: status as "CURSANDO" | "CONCLUIDO" | "ABANDONADO",
      }),
    onSuccess: invalidar,
  });

  const excluir = useMutation({
    mutationFn: api.excluirCurso,
    onSuccess: invalidar,
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para excluir."),
  });

  const cursando = cursos.data?.filter((c) => c.status === "CURSANDO") ?? [];
  const outros = cursos.data?.filter((c) => c.status !== "CURSANDO") ?? [];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <p className="flex items-center gap-2 text-sm font-medium">
          <GraduationCap className="size-4 text-frio" />
          Cursos
        </p>
        <Button tamanho="sm" onClick={() => setAberto(!aberto)}>
          <Plus className="size-4" />
          Novo curso
        </Button>
      </div>

      {aberto && (
        <Card>
          <div className="mb-3 flex gap-1">
            <Button
              variante={retroativo ? "fantasma" : "secundario"}
              tamanho="sm"
              onClick={() => setRetroativo(false)}
            >
              Vou fazer
            </Button>
            <Button
              variante={retroativo ? "secundario" : "fantasma"}
              tamanho="sm"
              onClick={() => setRetroativo(true)}
            >
              Ja fiz (retroativo)
            </Button>
          </div>

          <form
            className="grid gap-3 sm:grid-cols-4"
            onSubmit={(evento) => {
              evento.preventDefault();
              criar.mutate();
            }}
          >
            <Campo rotulo="Titulo" className="sm:col-span-2">
              <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} autoFocus required />
            </Campo>
            <Campo rotulo="Instituicao">
              <Input
                value={instituicao}
                onChange={(e) => setInstituicao(e.target.value)}
                placeholder="Alura, Udemy..."
              />
            </Campo>
            <Campo
              rotulo="Carga horaria"
              dica="da o percentual e a previsao"
              ajuda="As horas que o curso anuncia. O progresso e o tempo que voce dedicou dividido por ela; sem carga, o curso so acumula tempo."
            >
              <Input
                type="number"
                step="0.5"
                min={0.5}
                value={cargaHoraria}
                onChange={(e) => setCargaHoraria(e.target.value)}
              />
            </Campo>
            <Campo
              rotulo="Area"
              ajuda="O assunto do curso. Compartilhada com os livros, para que o painel Estudo responda quanto tempo foi para cada area no total."
            >
              <Select value={areaId} onChange={(e) => setAreaId(e.target.value)}>
                <option value="">-</option>
                {areas.data?.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.nome}
                  </option>
                ))}
              </Select>
            </Campo>
            <Campo rotulo="Link" className="sm:col-span-3">
              <Input value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://" />
            </Campo>

            {retroativo && (
              <>
                <Campo
                  rotulo="Horas que levou"
                  dica="obrigatorio no retroativo"
                  ajuda="Quanto tempo o curso tomou. E o que faz ele contar nas horas por area mesmo sem sessoes registradas."
                >
                  <Input
                    type="number"
                    step="0.5"
                    min={0.5}
                    value={horasRetroativas}
                    onChange={(e) => setHorasRetroativas(e.target.value)}
                    required
                  />
                </Campo>
                <Campo
                  rotulo="Em quantos dias"
                  dica="opcional; da o ritmo"
                  ajuda="Em quantos dias corridos voce fez o curso. Da o ritmo em horas por semana daquele periodo."
                >
                  <Input
                    type="number"
                    min={1}
                    value={diasRetroativos}
                    onChange={(e) => setDiasRetroativos(e.target.value)}
                  />
                </Campo>
                <Campo rotulo="Terminei em" className="sm:col-span-2">
                  <input
                    type="date"
                    value={concluidoEm}
                    onChange={(e) => setConcluidoEm(e.target.value)}
                    className="h-9 w-full rounded-md border border-risco bg-placa px-2 text-sm"
                  />
                </Campo>
              </>
            )}

            <div className="flex items-center gap-2 sm:col-span-4">
              <Button type="submit" disabled={criar.isPending || titulo.trim() === ""}>
                {retroativo ? "Cadastrar curso antigo" : "Cadastrar"}
              </Button>
              <Button type="button" variante="fantasma" onClick={limpar}>
                Cancelar
              </Button>
            </div>
          </form>
        </Card>
      )}

      {erro && <p className="text-sm text-giz-fraco">{erro}</p>}

      <OndeVaiOTempo />

      {cursos.isLoading && <p className="text-sm text-giz-apagado">Carregando...</p>}
      {!cursos.isLoading && (cursos.data?.length ?? 0) === 0 && (
        <p className="py-2 text-sm text-giz-apagado">
          Nenhum curso cadastrado. Cadastre um para registrar a dedicacao diaria.
        </p>
      )}

      {cursando.map((curso) => (
        <CartaoCurso
          key={curso.cursoId}
          curso={curso}
          aoExcluir={(id) => excluir.mutate(id)}
          aoMudarStatus={(c, status) => atualizar.mutate({ curso: c, status })}
        />
      ))}

      {outros.length > 0 && (
        <>
          <p className="mt-2 text-[0.8125rem] text-giz-apagado">Fora de andamento</p>
          {outros.map((curso) => (
            <CartaoCurso
              key={curso.cursoId}
              curso={curso}
              aoExcluir={(id) => excluir.mutate(id)}
              aoMudarStatus={(c, status) => atualizar.mutate({ curso: c, status })}
            />
          ))}
        </>
      )}
    </div>
  );
}
