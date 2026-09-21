import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, NaoAutenticado } from "@/api";
import { Login } from "@/pages/Login";
import { Principal } from "@/pages/Principal";

export default function App() {
  const queryClient = useQueryClient();
  const sessao = useQuery({ queryKey: ["sessao"], queryFn: api.sessao });

  if (sessao.isLoading) {
    return <div className="p-6 text-sm text-zinc-500">Carregando...</div>;
  }

  if (sessao.error instanceof NaoAutenticado || !sessao.data) {
    return <Login aoEntrar={() => queryClient.invalidateQueries()} />;
  }

  return (
    <Principal
      aoSair={async () => {
        await api.logout();
        queryClient.clear();
        queryClient.invalidateQueries();
      }}
    />
  );
}
