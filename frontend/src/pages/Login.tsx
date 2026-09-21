import { useState } from "react";
import { api } from "@/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Campo, Card } from "@/components/ui/campo";

export function Login({ aoEntrar }: { aoEntrar: () => void }) {
  const [usuario, setUsuario] = useState("admin");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function entrar(evento: React.FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setErro(null);
    try {
      await api.login(usuario, senha);
      aoEntrar();
    } catch {
      setErro("Usuario ou senha invalidos.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="flex min-h-dvh items-center justify-center p-6">
      <Card className="w-full max-w-sm">
        <h1 className="text-lg font-semibold">Registro de Atividades</h1>
        <p className="mt-1 text-sm text-zinc-400">Entre para registrar o dia.</p>

        <form className="mt-6 flex flex-col gap-4" onSubmit={entrar}>
          <Campo rotulo="Usuario">
            <Input value={usuario} onChange={(e) => setUsuario(e.target.value)} autoComplete="username" />
          </Campo>
          <Campo rotulo="Senha">
            <Input
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              autoComplete="current-password"
              autoFocus
            />
          </Campo>
          {erro && <p className="text-sm text-rose-400">{erro}</p>}
          <Button type="submit" disabled={enviando || senha.length === 0}>
            {enviando ? "Entrando..." : "Entrar"}
          </Button>
        </form>
      </Card>
    </div>
  );
}
