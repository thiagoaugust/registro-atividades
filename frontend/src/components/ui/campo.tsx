import { useId, type ReactNode } from "react";
import { HelpCircle } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Explicacao do campo, no hover e tambem no foco por teclado — um tooltip so de :hover deixa de
 * fora quem navega por tab. O texto responde "para que serve", nao repete o rotulo.
 */
export function Ajuda({ texto }: { texto: string }) {
  const id = useId();

  return (
    <span className="group relative inline-flex align-middle">
      <button
        type="button"
        aria-label={`O que e este campo: ${texto}`}
        aria-describedby={id}
        // O botao vive dentro do <label>: sem o preventDefault, clicar no "?" abriria o select
        // ou focaria o input ao lado.
        onClick={(evento) => evento.preventDefault()}
        className="inline-flex cursor-help text-giz-apagado transition-colors hover:text-giz focus:text-giz focus:outline-none"
      >
        <HelpCircle className="size-3.5" />
      </button>

      <span
        id={id}
        role="tooltip"
        className="pointer-events-none absolute left-0 top-5 z-50 w-60 rounded border border-risco-forte bg-placa-alta p-2 text-xs font-normal leading-snug text-giz opacity-0 shadow-xl transition-opacity group-focus-within:opacity-100 group-hover:opacity-100"
      >
        {texto}
      </span>
    </span>
  );
}

/**
 * Rotulo em caixa de frase, no tamanho do texto. Caixa alta espacada em cima de cada campo era o
 * que mais fazia a tela parecer template: quarenta rotulos gritando o mesmo volume.
 */
export function Campo({
  rotulo,
  children,
  className,
  dica,
  ajuda,
}: {
  rotulo: string;
  children: ReactNode;
  className?: string;
  dica?: string;
  ajuda?: string;
}) {
  return (
    <label className={cn("flex flex-col gap-1", className)}>
      <span className="flex items-center gap-1.5 text-[0.8125rem] text-giz-fraco">
        {rotulo}
        {ajuda && <Ajuda texto={ajuda} />}
      </span>
      {children}
      {dica && <span className="text-xs text-giz-apagado">{dica}</span>}
    </label>
  );
}

/**
 * Regiao de conteudo. Sem borda por padrao: o que separa as regioes e o espaco e o fio do rotulo de
 * secao, nao mais uma caixa dentro da caixa. `elevada` existe para o que precisa se destacar de
 * verdade — na pratica, a leitura do dia.
 */
export function Card({
  children,
  className,
  elevada,
}: {
  children: ReactNode;
  className?: string;
  elevada?: boolean;
}) {
  return (
    <div
      className={cn(
        elevada ? "rounded border border-risco bg-placa p-4" : "border border-risco/60 p-4",
        "rounded",
        className,
      )}
    >
      {children}
    </div>
  );
}

/** Um fio que atravessa com o nome da secao em cima dele. */
export function Secao({ children, acao }: { children: ReactNode; acao?: ReactNode }) {
  return (
    <div className="flex items-center gap-3">
      <span className="text-[0.8125rem] text-giz-fraco">{children}</span>
      <span className="h-px flex-1 bg-risco" />
      {acao}
    </div>
  );
}
