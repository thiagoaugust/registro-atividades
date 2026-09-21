# CLAUDE.md

Sistema pessoal de **registro de atividades + GTD + gamificação**. Uso local, usuário único.

O plano vivo (modelo de dados, fórmulas, endpoints, fases) está em `docs/PLANO.md`.
Quando uma decisão mudar, atualize **os dois** arquivos no mesmo commit.

## Stack

| Camada | Escolha |
|---|---|
| Backend | Java 21, Quarkus 3.x LTS, Maven (wrapper) |
| Persistência | PostgreSQL 16, Hibernate ORM + Panache (**padrão repository**), Flyway |
| API | `quarkus-rest` + `quarkus-rest-jackson`, bloqueante/imperativa (sem Mutiny na v1) |
| Validação | `quarkus-hibernate-validator` |
| Docs/Saúde | `quarkus-smallrye-openapi` (Swagger UI em dev), `quarkus-smallrye-health` |
| Agendamento | `quarkus-scheduler` |
| Auth | `quarkus-elytron-security-properties-file` (form auth, usuário em config) |
| Frontend | React 19 + TypeScript + Vite, Tailwind + shadcn/ui, TanStack Query, Recharts (carregado sob demanda) |
| Testes | `@QuarkusTest` + RestAssured (API), JUnit 5 + AssertJ (domínio puro), Vitest (frontend) |

**JVM mode apenas.** Build nativo é opcional e está fora do critério de pronto.

## Comandos

```bash
# backend
./mvnw quarkus:dev          # Dev Services sobe o Postgres sozinho (Docker precisa estar rodando)
./mvnw verify               # unitários + @QuarkusTest
./mvnw test -Dtest=ClassificacaoDiaTest   # um teste só

# frontend (dentro de frontend/)
npm run dev
npm run test                # Vitest
npm run build

# tudo junto (cp .env.example .env antes da primeira vez)
docker compose up --build   # db + backend + frontend -> http://localhost:3000

# com ~120 dias de dados de exemplo (so semeia com o banco vazio)
QUARKUS_PROFILE=demo,prod docker compose up --build
./mvnw quarkus:dev -Dquarkus.profile=demo
docker compose --profile demo up   # sobe com o perfil demo (120 dias de dados)
```

## Estrutura

```
.
├── src/main/java/br/dev/registro/
│   ├── comum/              # Problem Details, relógio/fuso, tipos compartilhados
│   ├── atividades/         # registros, livros, cursos e areas de conhecimento
│   ├── checkin/
│   ├── gamificacao/
│   ├── gtd/                # inbox, esclarecimento, acoes, contextos, referencias
│   └── analytics/          # agregacoes por periodo, heatmap, correlacoes
├── src/main/resources/db/migration/   # Flyway: V001__*.sql
├── frontend/
│   ├── src/api.ts              # tipos + cliente REST (1 arquivo, sem geracao de codigo)
│   ├── src/components/ui/      # primitivos shadcn/ui escritos aqui, sem o CLI
│   ├── src/lib/formato.ts      # calculo exibido (coberto por Vitest)
│   └── src/pages/
└── docs/PLANO.md
```

No frontend, `<select>`, `<input type=range>` e afins nativos vem antes de qualquer biblioteca de
componente; Radix entra so se o nativo nao resolver. Sem CORS: o Vite faz proxy de `/api` em dev e o
nginx em producao, entao o cookie de sessao e sempre same-origin.

Cada módulo tem **`api/`** (resources REST + DTOs), **`domain/`** (entidades, serviços, regras puras) e
**`infra/`** (repositories Panache, queries nativas, integrações). `api` → `domain` → `infra`;
nunca o inverso, e módulos conversam via serviços de `domain`, nunca pelo repository do vizinho.

## Convenções

- **Domínio em português** (`RegistroAtividade`, `CheckinDiario`, `indiceProdutividade`). Termos
  técnicos em inglês onde já são padrão (`Repository`, `Resource`, `Dto`).
- **Regra de negócio fica no `domain`.** Resource só valida entrada, chama serviço e mapeia saída.
- **Injeção por construtor** em beans `@ApplicationScoped`. Sem `@Inject` em campo.
- **DTOs são `record`s**, com Bean Validation. Entidade JPA nunca cruza a fronteira REST — mas um
  record de domínio (`DadosRegistro`) pode ser o corpo da requisição; o que não atravessa é o estado
  gerenciado, não a imutabilidade.
- **Entidades JPA usam campos públicos** (acesso por campo, padrão em Quarkus/Panache). Getter e
  setter aqui seriam centenas de linhas sem leitor, e a entidade não sai pelo REST.
- **Consulta que alimenta DTO traz os vínculos com `join fetch`**: o DTO é montado depois do commit e
  um proxy lazy ali estoura.
- **Erros = RFC 7807.** Exceções de domínio herdam de `DominioException`; um `ExceptionMapper`
  central em `comum/` traduz para `application/problem+json`. Nada de `Response.status(...)` solto.
- **Migrations nunca são editadas depois de commitadas.** Sempre um `V00N__` novo.
- **Datas:** timestamps em `timestamptz` (UTC); o "dia" é `data_local` (`date`), derivado em
  `America/Sao_Paulo` por um `Relogio` injetável — nunca `LocalDate.now()` direto (impede testar).
- **Cálculo puro:** gamificação e classificação do dia são funções sem I/O (`CalculadoraXp`,
  `ClassificadorDia`, `StreakCalculator`), testadas sem Quarkus. Os parâmetros chegam como records
  (`ParametrosXp`, `ParametrosIndice`) que o `@ConfigMapping` monta — nenhum cálculo conhece Quarkus.
  Analytics agrega **no banco** (views/`date_trunc`); o código puro só classifica o resultado.
- **Módulos conversam por evento CDI**, não por chamada direta: quem escreve dispara
  `RegistroAlterado`/`DiaAlterado` e a gamificação observa. Síncrono e na mesma transação — um
  recálculo que falha desfaz a escrita que o disparou.
- **Antes de somar em query nativa, `em.flush()`.** O Hibernate não sincroniza a sessão sozinho antes
  de SQL nativo, e a soma veria o estado velho.
- **Commits:** Conventional Commits, **uma linha so**, sem corpo e sem linhas de atribuicao
  (`feat(gtd): inbox com captura rapida`). O porque da mudanca vai no codigo e no docs/PLANO.md.

## Notas Quarkus para quem vem de Spring

| Spring | Aqui |
|---|---|
| `@Service` / `@Component` | `@ApplicationScoped` (CDI). Sem component-scan configurável: tudo no jar é escaneado. |
| `@Repository` + Spring Data | `PanacheRepository<T>` — `find("campo", valor)`, `list(...)`, HQL simplificado. |
| `application-{perfil}.yml` | Um `application.properties` com prefixos `%dev.`, `%test.`, `%prod.`, `%demo.`. |
| Testcontainers manual | **Dev Services**: sem `quarkus.datasource.jdbc.url` no perfil, o Quarkus sobe o Postgres em container sozinho no `dev` e no `test`. |
| `@ConfigurationProperties` | `@ConfigMapping(prefix = "gamificacao")` em interface, records aninhados. |
| `@Transactional` do Spring | `jakarta.transaction.Transactional` — no serviço de domínio, não no resource. |
| `@Scheduled(cron=...)` | `@Scheduled(cron = "...")` do `quarkus-scheduler`, idem. |
| Jackson global | `quarkus.hibernate-orm.mapping.format.global=ignore` separa o mapper do JSONB do mapper do REST — sem isso o boot falha de propósito. |
| `spring.jpa.hibernate.ddl-auto=validate` | `quarkus.hibernate-orm.schema-management.strategy=validate`. Flyway cria, Hibernate confere. |

## Regras de trabalho

1. Uma fase por vez (ver `docs/PLANO.md`). Não começar a próxima sem confirmação explícita.
2. Fim de fase = `./mvnw verify` verde + `docker compose up` funcionando + commit + resumo curto.
3. Mudou decisão de design? Atualizar `docs/PLANO.md` **antes** do código que a implementa.
