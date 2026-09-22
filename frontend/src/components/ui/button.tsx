import { cva, type VariantProps } from "class-variance-authority";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

const estilos = cva(
  "inline-flex items-center justify-center gap-1.5 rounded text-sm font-medium transition-colors disabled:pointer-events-none disabled:opacity-40 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-aferido",
  {
    variants: {
      variante: {
        primario: "bg-aferido text-breu hover:brightness-110",
        secundario: "border border-risco bg-placa-alta text-giz hover:border-risco-forte",
        fantasma: "text-giz-fraco hover:bg-placa hover:text-giz",
        // Excluir e destrutivo, mas nao e erro: fica discreto ate o hover, e nunca vermelho forte.
        perigo: "text-giz-apagado hover:bg-placa hover:text-latao",
      },
      tamanho: {
        md: "h-9 px-4",
        sm: "h-7 px-2.5 text-[0.8125rem]",
        icone: "h-8 w-8",
      },
    },
    defaultVariants: { variante: "primario", tamanho: "md" },
  },
);

type Props = ComponentProps<"button"> & VariantProps<typeof estilos>;

export function Button({ className, variante, tamanho, ...props }: Props) {
  return <button className={cn(estilos({ variante, tamanho }), className)} {...props} />;
}
