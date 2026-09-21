import { cva, type VariantProps } from "class-variance-authority";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

const estilos = cva(
  "inline-flex items-center justify-center gap-2 rounded-md text-sm font-medium transition-colors disabled:pointer-events-none disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/60",
  {
    variants: {
      variante: {
        primario: "bg-emerald-600 text-white hover:bg-emerald-500",
        secundario: "bg-zinc-800 text-zinc-100 hover:bg-zinc-700",
        fantasma: "text-zinc-300 hover:bg-zinc-800 hover:text-zinc-100",
        perigo: "bg-transparent text-rose-300 hover:bg-rose-500/10",
      },
      tamanho: {
        md: "h-9 px-4",
        sm: "h-8 px-3 text-xs",
        icone: "h-9 w-9",
      },
    },
    defaultVariants: { variante: "primario", tamanho: "md" },
  },
);

type Props = ComponentProps<"button"> & VariantProps<typeof estilos>;

export function Button({ className, variante, tamanho, ...props }: Props) {
  return <button className={cn(estilos({ variante, tamanho }), className)} {...props} />;
}
