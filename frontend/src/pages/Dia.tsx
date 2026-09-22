import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { api, ErroApi, type DadosCheckin, type DadosRegistro, type RegistroDto } from "@/api";
import { Button } from "@/components/ui/button";
import { Secao } from "@/components/ui/campo";
import { FormularioRegistro } from "@/components/FormularioRegistro";
import { CheckinCard } from "@/components/CheckinCard";
import { LeituraDoDia } from "@/components/LeituraDoDia";
import { EmAndamento, type Atalho } from "@/components/EmAndamento";
import { esforcoMedio, formatarDetalhes, formatarDuracao } from "@/lib/formato";

export function hojeLocal(): string {
  return new Date().toLocaleDateString("sv-SE", { timeZone: "America/Sao_Paulo" });
}

function somarDias(iso: string, dias: number): string {
  const [ano, mes, dia] = iso.split("-").map(Number);
  const data = new Date(Date.UTC(ano, mes - 1, dia + dias));
  return data.toISOString().slice(0, 10);
}

/**
 * Uma linha da folha do dia. Colunas alinhadas com algarismos tabulares, em vez da fileirinha
 * "58min · esforco 9 · satisfacao 4": o dia inteiro passa a ser lido de cima a baixo, e duas
 * sessoes de duracao parecida ficam visivelmente parecidas.
 */
function LinhaRegistro({
  registro,
  aoEditar,
  aoExcluir,
  excluindo,
}: {
  registro: RegistroDto;
  aoEditar: () => void;
  aoExcluir: () => void;
  excluindo?: boolean;
}) {
  const titulo =
    registro.titulo ?? registro.livro?.titulo ?? registro.projeto?.titulo ?? registro.curso?.titulo ?? "sem titulo";
  const detalhes = formatarDetalhes(registro.detalhes);

  return (
    <div className="group grid grid-cols-[5.5rem_1fr_auto] items-baseline gap-x-3 border-b border-risco/50 py-2 last:border-0">
      <span className="text-xs lowercase text-giz-apagado">{registro.categoria}</span>

      <div className="min-w-0">
        <p className="truncate text-sm text-giz">{titulo}</p>
        {(detalhes || registro.notas) && (
          <p className="truncate text-xs text-giz-apagado">{detalhes || registro.notas}</p>
        )}
      </div>

      <div className="flex items-baseline gap-3">
        <span className="medida w-14 text-right text-sm text-giz">
          {formatarDuracao(registro.duracaoMin)}
        </span>
        <span className="medida w-12 text-right text-xs text-giz-fraco" title="Esforco declarado">
          esf {registro.esforco}
        </span>
        {/* As acoes aparecem no hover e no foco: presentes sempre poluem a coluna de medidas. */}
        <span className="flex gap-0.5 opacity-0 transition-opacity focus-within:opacity-100 group-hover:opacity-100">
          <Button variante="fantasma" tamanho="icone" aria-label={`Editar ${titulo}`} onClick={aoEditar}>
            <Pencil className="size-3.5" />
          </Button>
          <Button
            variante="perigo"
            tamanho="icone"
            aria-label={`Excluir ${titulo}`}
            onClick={aoExcluir}
            disabled={excluindo}
          >
            <Trash2 className="size-3.5" />
          </Button>
        </span>
      </div>
    </div>
  );
}

export function PainelDia() {
  const [data, setData] = useState(hojeLocal());
  const [editando, setEditando] = useState<RegistroDto | null>(null);
  // `linha` diz em qual item o formulario esta aberto; `chave` sobe a cada clique, para o
  // formulario reagir mesmo quando o item escolhido e o mesmo de antes.
  const [atalho, setAtalho] = useState<(Atalho & { chave: number; linha: string }) | null>(null);
  const [avulso, setAvulso] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const dia = useQuery({ queryKey: ["dia", data], queryFn: () => api.dia(data) });
  const faixa = useQuery({ queryKey: ["faixa", data], queryFn: () => api.faixa(data) });

  // Qualquer escrita mexe em XP, nivel, streak e conquistas: o jeito honesto e invalidar tudo.
  const invalidar = () => queryClient.invalidateQueries();

  const salvar = useMutation({
    mutationFn: (dados: DadosRegistro) =>
      editando ? api.atualizarRegistro(editando.id, dados) : api.criarRegistro(dados),
    onSuccess: () => {
      setEditando(null);
      // Fecha o formulario aberto na linha: a sessao foi salva, e deixa-lo aberto com os valores
      // digitados convida a um segundo clique que criaria registro duplicado.
      setAtalho(null);
      setAvulso(false);
      setErro(null);
      invalidar();
    },
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para salvar."),
  });

  const excluir = useMutation({ mutationFn: api.excluirRegistro, onSuccess: invalidar });

  const salvarCheckin = useMutation({
    mutationFn: (dados: DadosCheckin) => api.salvarCheckin(data, dados),
    onSuccess: invalidar,
  });

  const fecharDia = useMutation({
    mutationFn: (dados: { dificuldadeFinal?: number | null; atrapalhou?: string | null }) =>
      api.fecharDia(data, dados),
    onSuccess: invalidar,
  });

  const registros = dia.data?.registros ?? [];
  const medio = esforcoMedio(registros);

  // Um elemento so, montado ou dentro da linha do item escolhido ou no fim da tela. Trocar de
  // lugar remonta e zera os campos — que e justamente o que se quer ao mudar de alvo.
  const formulario = (
    <FormularioRegistro
      data={data}
      editando={editando}
      atalho={atalho}
      aoSalvar={(dados) => salvar.mutate(dados)}
      aoCancelar={() => {
        setEditando(null);
        setAtalho(null);
        setAvulso(false);
        setErro(null);
      }}
      salvando={salvar.isPending}
      erro={erro}
    />
  );

  const formularioSolto = editando !== null || avulso;

  return (
    <div className="flex flex-col gap-6">
      <LeituraDoDia
        data={data}
        hoje={hojeLocal()}
        resumo={dia.data?.resumo ?? null}
        faixa={faixa.data}
        totalMinutos={dia.data?.totalMinutos ?? 0}
        aoNavegar={(dias) => setData(somarDias(data, dias))}
        aoIrParaHoje={() => setData(hojeLocal())}
      />

      <EmAndamento
        aberta={atalho?.linha ?? null}
        formulario={formulario}
        aoRegistrar={(escolha, linha) => {
          setEditando(null);
          setAvulso(false);
          setErro(null);
          setAtalho({ ...escolha, linha, chave: Date.now() });
        }}
        aoFechar={() => {
          setAtalho(null);
          setErro(null);
        }}
      />

      <CheckinCard
        data={data}
        checkin={dia.data?.checkin ?? null}
        aoSalvar={(dados) => salvarCheckin.mutate(dados)}
        aoFechar={(dados) => fecharDia.mutate(dados)}
        salvando={salvarCheckin.isPending || fecharDia.isPending}
      />

      <div className="flex flex-col gap-2">
        <Secao
          acao={
            !formularioSolto && (
              <Button variante="fantasma" tamanho="sm" onClick={() => setAvulso(true)}>
                <Plus className="size-3.5" />
                Registrar
              </Button>
            )
          }
        >
          o dia
          {registros.length > 0 && (
            <span className="medida ml-2 text-giz-apagado">
              {formatarDuracao(dia.data?.totalMinutos ?? 0)}
              {medio !== null && ` · esforco medio ${medio}`}
            </span>
          )}
        </Secao>

        {formularioSolto && formulario}

        {dia.isLoading && <p className="text-sm text-giz-fraco">Carregando...</p>}
        {!dia.isLoading && registros.length === 0 && !formularioSolto && (
          <p className="py-2 text-sm text-giz-apagado">
            Nenhum registro neste dia. Use os atalhos acima, ou Registrar para algo avulso.
          </p>
        )}

        {registros.map((registro) => (
          <LinhaRegistro
            key={registro.id}
            registro={registro}
            aoEditar={() => {
              setAtalho(null);
              setAvulso(false);
              setEditando(registro);
            }}
            aoExcluir={() => excluir.mutate(registro.id)}
            excluindo={excluir.isPending}
          />
        ))}
      </div>
    </div>
  );
}
