# Especificação Técnica: Task-First Planning & Execution Architecture

**Autor:** Platform Team
**Data:** 2026-04-11
**Versão:** 0.1 (draft)
**Status:** Em Refinamento
**Épico:** EPIC-0038

---

## 1. Contexto

O gerador `ia-dev-env` adota um fluxo de planejamento e execução top-down:

```
Epic (x-epic-plan)
  └── Story (x-story-plan)
        └── Tasks (listadas como sub-seção de tasks-story-*.md)
              └── TDD (x-test-tdd, dentro de x-dev-story-implement)
```

A implementação atual trata **tasks como sub-artefato da story** — listadas em
`tasks-story-XXXX-YYYY.md` mas sem arquivo próprio, sem DoD próprio, sem
contratos I/O explícitos, sem grafo de dependências formal.

## 2. Problema

### 2.1 Sintomas observados no EPIC-0034 (Remoção de Targets Não-Claude)

O épico recém-fechado exibiu cinco sintomas diretamente atribuíveis ao modelo
top-down atual:

| # | Sintoma | Evidência no EPIC-0034 |
| :--- | :--- | :--- |
| 1 | Planning drift entre stories | Stories 3, 4 tinham task plans que contradiziam o estado pós-execução de stories anteriores. Subagents precisaram de "scope drift alert" explícito no prompt. |
| 2 | Naming confusion em tasks | Story 3 plan identificava `AgentsAssembler.java` como "writer de .agents/" — errado. Só descobrimos grepando no momento da execução. |
| 3 | TDD falso | Story 1 TASK-001 + TASK-002 tiveram que ser coalescidas porque TASK-001 sozinha deixa o build quebrado. A "atomicidade" era fantasia. |
| 4 | Coverage regression no nível errado | Story 4 tentou inlinar `PlatformContextBuilder.countActive` como refactor — derrubou branch coverage de 90.04 → 89.87, foi revertido. Decisão feita no nível da story, não da task. |
| 5 | Review incorreta por premissa errada | PR #274 review rejeitou corretamente um comment do Copilot sobre `.github/workflows/`. A premissa foi: "generator não escreve .github/". **Errada.** CiWorkflowAssembler e CdWorkflowAssembler ESCREVEM. Descoberto só no review da v3.0.0 PR #282. |

### 2.2 Análise da causa raiz

Em todos os cinco sintomas, a causa comum é **tasks não são first-class
citizens**:

- Sintomas 1–2: tasks não têm arquivo próprio, então atualizar uma task quando
  uma anterior muda exige editar dentro de um arquivo agregado → drift.
- Sintoma 3: tasks não declaram contratos I/O, então "posso testar TASK-001
  sem TASK-002" só é descoberto na execução.
- Sintoma 4: coverage é medido per-story, não per-task, então refactors
  pequenos "escondem" regressões incrementais.
- Sintoma 5: sem contratos de output explícitos (`CiWorkflowAssembler writes
  .github/workflows/ci.yml`), é possível assumir incorretamente o que uma
  classe faz.

### 2.3 Sintomas estruturais da arquitetura top-down

Independente do EPIC-0034, o modelo atual sofre de:

- **`x-task-plan` é órfã.** Existe na taxonomia de skills mas ninguém invoca.
  É código morto arquitetural.
- **Execução é top-down mas verificação é bottom-up.** A integrity gate do
  épico valida "build verde + coverage" — que é a soma das tasks. Cadeia de
  responsabilidade invertida.
- **Impossível paralelizar tasks independentes dentro da mesma story.**
  Mesmo que TASK-003 e TASK-005 toquem arquivos disjuntos, executam serial
  porque a story as vê como uma sequência linear.
- **Gates de refactor são ruins.** Hoje "refactor task" é opcional e
  implícito. Sem DoD per-task, refactors ficam para o final e rompem o ciclo
  Red-Green-Refactor.

## 3. Objetivo

Inverter o paradigma: **Task torna-se a unidade atômica, testável,
independentemente executável**. Stories agregam tasks em valor entregável.
Épicos agregam stories em iniciativas transversais.

### Fluxo alvo (planejamento)

```
Epic (x-epic-plan, agregador de stories + RULEs)
  └── Story (x-story-plan, agregador de tasks + UoW entregável)
        └── Task (x-task-plan, unidade testável)
              ├── task-TASK-NNN.md        (DoD, I/O contracts, deps)
              └── plan-task-TASK-NNN.md   (TDD cycle, test scenarios)
```

### Fluxo alvo (execução)

```
x-dev-epic-implement
  └── x-dev-story-implement (per story, in phase order)
        └── x-dev-task-implement (per task, in dependency order)
              └── TDD cycle (Red → Green → Refactor → atomic commit)
```

## 4. Escopo

### Incluído

- Novo schema `task-TASK-NNN.md` com DoD, contratos I/O, testabilidade, deps
- Novo `task-implementation-map-STORY-XXXX-YYYY.md` por story (dependency graph)
- Refactor de `x-task-plan` para ser callable skill (invocada por x-story-plan)
- Refactor de `x-story-plan` para invocar `x-task-plan` per task
- Novo skill `x-dev-task-implement` (substitui a lógica embedded no x-dev-story-implement)
- Refactor de `x-dev-story-implement` para orquestrar tasks via dependency map
- Simplificação de `x-dev-epic-implement` (só orquestra stories)
- Template `_TEMPLATE-TASK.md`
- Template `_TEMPLATE-TASK-IMPLEMENTATION-MAP.md`
- 5 novas RULEs (TF-01 a TF-05) sobre testabilidade, contratos I/O, topological execution, atomic commits, backward compat
- Migration path: `planning_schema_version` flag em execution-state.json (v1 legacy, v2 task-first)
- Atualização de CLAUDE.md, rules, docs
- Testes de integração do novo fluxo end-to-end
- Dogfood do próximo épico planejado após este shipar

### Excluído

- **Skill rename** (outro épico — pré-requisito, deve shipar ANTES deste)
- **Task = PR stacked** (fica como v3 futuro — este épico usa task = commit)
- **Migração retroativa** de epics 0025–0037 para o novo schema (backward-compat read-only)
- **Dashboard / observability de tasks** (follow-up futuro)

## 5. Arquitetura proposta

### 5.1 Task como artefato primário

Schema `plans/epic-XXXX/plans/task-TASK-NNN-story-XXXX-YYYY.md`:

```markdown
# Task: {título}

**ID:** TASK-XXXX-YYYY-NNN
**Story:** story-XXXX-YYYY
**Status:** Pendente | Em Andamento | Concluída | Bloqueada | Falha

## 1. Objetivo
{O que esta task entrega como unidade funcional. NÃO é valor de usuário —
é um "pedaço de código + teste" que pode ser commit atomic.}

## 2. Contratos I/O

### Inputs (pré-condições)
- {estado esperado antes da execução}
- {outras tasks que devem estar concluídas} (lista por TASK-ID)
- {dependências externas — ex: certa classe exists, certa enum value removed}

### Outputs (pós-condições)
- {artefatos produzidos: classe X criada, método Y alterado, teste Z passa}
- {side effects observáveis: build compila, teste específico verde}

### Testabilidade
- [ ] **Independentemente testável** (sem precisar de outras tasks não-concluídas)
- OU:
- [ ] **Requer mock de TASK-YYY** (declarar o mock explicitamente)
- OU:
- [ ] **Coalescível com TASK-ZZZ** (declarar justificação — mutualmente recursiva)

## 3. Definition of Done (per-task)

- [ ] Código implementado
- [ ] Teste automatizado cobre o output declarado
- [ ] `mvn compile` verde (build não quebra)
- [ ] Novo teste criado é Red → Green → Refactor (TDD honesto)
- [ ] Contratos I/O declarados foram respeitados (grep/assert verification)
- [ ] Commit atômico formatado com Conventional Commits

## 4. Dependências

| Depends on | Relação | Pode mockar? |
| :--- | :--- | :--- |
| TASK-XXX-YYY-NNN | inputs required | Sim — mock {classe} |
| TASK-XXX-YYY-MMM | outputs consumed | Não — coalesce |

## 5. Plano de implementação

(Gerado por x-task-plan; descreve o Red→Green→Refactor cycle)
```

### 5.2 Task-implementation-map por story

Schema `plans/epic-XXXX/plans/task-implementation-map-STORY-XXXX-YYYY.md`:

```markdown
# Task Implementation Map — story-XXXX-YYYY

## Dependency Graph (Mermaid)

\`\`\`mermaid
graph TD
    T001["TASK-XXXX-YYYY-001<br/>título"]
    T002["TASK-XXXX-YYYY-002<br/>título"]
    T003["TASK-XXXX-YYYY-003<br/>título"]

    T001 --> T003
    T002 --> T003
\`\`\`

## Execution Order (Topological Sort)

| Wave | Tasks (paralelizáveis) | Blocks |
| :--- | :--- | :--- |
| 1 | TASK-001, TASK-002 | — |
| 2 | TASK-003 | TASK-001, TASK-002 |

## Coalesced Groups

Tasks que devem commit juntas por serem mutuamente recursivas:

- (TASK-004, TASK-005): enum edit + CLI edit — não compilam uma sem a outra.

## Parallelism Analysis

- 2 tasks paralelas na wave 1 (estimativa: -30% wallclock vs sequential)
```

### 5.3 Fluxo de planejamento

```
x-epic-plan epic-0038
  └── (sequential, per story in implementation-map-XXXX.md)
       x-story-plan story-XXXX-YYYY
         ├── Phase 1: multi-agent consolidation (architect, qa, security, tl, po)
         ├── Phase 2: task breakdown into atomic units + I/O contracts
         ├── Phase 3: invoke x-task-plan per task (parallel subagents)
         │            └── produces plan-task-TASK-NNN.md
         ├── Phase 4: generate task-implementation-map via topological sort
         ├── Phase 5: DoR validation (story + all tasks READY)
         └── Output: tasks-story-*.md, task-TASK-NNN.md (N files),
                     plan-task-TASK-NNN.md (N files),
                     task-implementation-map-STORY-*.md
```

### 5.4 Fluxo de execução

```
x-dev-epic-implement
  └── (sequential per phase, per dependency-map)
       x-dev-story-implement story-XXXX-YYYY
         ├── Read task-implementation-map-STORY-*.md
         ├── For each wave (parallelizable batch):
         │     For each task in wave:
         │       x-dev-task-implement TASK-XXXX-YYYY-NNN
         │         ├── Read task-TASK-NNN.md + plan-task-TASK-NNN.md
         │         ├── TDD cycle (Red → Green → Refactor)
         │         ├── Atomic commit (Conventional Commits)
         │         └── Return task result (status, SHA, coverage delta)
         ├── Story verification (all tasks DONE, integration green)
         └── Story PR (aggregates task commits)
```

### 5.5 Task = commit (v1)

Decisão arquitetural: nesta primeira iteração, **tasks são commits atômicos
dentro da PR da story**, não PRs próprias. Rationale:

- Menos overhead de gestão (1 PR por story, não N)
- Reviewer ainda consegue ver task boundaries (commits são atômicos com
  Conventional Commits)
- Evita stacked-PR workflow complexity
- Upgrade para "task = PR" fica como v3 se volume justificar

## 6. Rules propostas

| RULE-ID | Título | Descrição |
| :--- | :--- | :--- |
| **RULE-TF-01** | Task Testability | Toda task DEVE ser independently testable OU declarar explicitamente que precisa mock OU que é coalescível com outra task. Sem justificação escrita, não pode commit. |
| **RULE-TF-02** | I/O Contracts Are Mandatory | Toda task DEVE declarar inputs (pré-condições) e outputs (pós-condições). Outputs são verificáveis via grep/assert/test. |
| **RULE-TF-03** | Topological Execution | A ordem de execução de tasks dentro de uma story DEVE respeitar o task-implementation-map. Paralelismo é opt-in baseado em análise de dependências. |
| **RULE-TF-04** | Task Commits Are Atomic | Cada task produz EXATAMENTE um commit. Coalesced groups produzem um commit com múltiplas tasks listadas no body. |
| **RULE-TF-05** | Backward Compatibility | Épicos com `planning_schema_version == "1.0"` (ou ausente) DEVEM continuar executando sem erro via legacy loader. Novos épicos iniciados APÓS este épico mergeado DEVEM usar v2. |

## 7. Interação com o skill rename epic

**Este épico depende do skill rename epic.** Ordem de execução:

1. Skill rename epic (outro épico a ser planejado) — renomeia todas as skills
   para o schema final
2. **Este épico (EPIC-0038)** — usa os nomes finais desde o início

Por que essa ordem:
- Skill rename é mecânico (find-replace + tests)
- Este refactor é arquitetural (novas skills, novo flow, migration path)
- Fazer rename primeiro reduz churn ao escrever este épico (não precisamos
  renomear `x-dev-task-implement` depois se a convenção mudar)

**Dependência explícita:** global DoR do EPIC-0038 inclui o check "skill
rename epic mergeado em develop". Se o rename epic decidir nomes diferentes
dos provisórios (`x-dev-task-implement`, `x-task-plan` refatorada), este
épico é re-baselined: spec, epic file e stories são atualizados ANTES do
start.

## 8. Migration path

### 8.1 Backward compatibility

- Épicos 0025–0037 (já planejados no formato atual) **não são migrados
  retroativamente**. Permanecem lendo `tasks-story-*.md` no formato v1.
- Novo flag `planning_schema_version` em `execution-state.json`:
  - `"1.0"` ou ausente → v1 flow (legacy, x-story-plan com tasks embedded)
  - `"2.0"` → v2 flow (task-first, x-story-plan invoca x-task-plan)
- Skills de execução detectam a versão e ramificam:
  ```
  if state.planning_schema_version == "2.0":
      dispatch_tasks_via_task_implementation_map()
  else:
      dispatch_story_as_single_unit()  # legacy
  ```

### 8.2 Primeiro épico usando v2

O primeiro épico a usar o novo schema será **o próximo épico planejado após
EPIC-0038 shipar** (dogfooding). EPIC-0038 permanece em v1 durante sua própria
execução (para evitar bootstrap problem — o `x-task-plan` callable e o
`x-dev-task-implement` só existem após story 0003 e 0005 shiparem).

### 8.3 Matriz de compatibilidade

| Épico | planning_schema_version | Flow | Skills usadas |
| :--- | :--- | :--- | :--- |
| epic-0025..0037 | "1.0" (ou ausente) | Legacy top-down | x-story-plan monolítica |
| epic-0038 (este) | "1.0" | Legacy top-down | x-story-plan monolítica |
| Próximo épico (pós-0038) | "2.0" | Task-first bottom-up | x-task-plan + x-dev-task-implement |

## 9. Riscos e mitigações

| Risco | Severidade | Mitigação |
| :--- | :--- | :--- |
| Refactor quebra x-dev-story-implement em execução de épicos em curso | Alta | planning_schema_version flag + backward-compat read |
| x-task-plan multiplica subagent dispatch (N tasks × custo) | Média | Paralelização dentro de story; batch size limit |
| Task = commit perde granularidade de review | Baixa | Conventional Commits + atomic boundaries suficientes para review |
| Escopo infla com cada skill tocada | Alta | Escopo congelado às 10 stories deste épico; outros refactors são follow-up |
| Dogfooding: primeiro épico pós-0038 expõe bugs tardios | Média | Integration test do fluxo completo (story 10) antes do primeiro dogfood |
| Skill rename epic decide nomes diferentes dos provisórios | Média | Re-baseline explícito no global DoR (check "rename mergeado") |

## 10. Definition of Done do épico

- [ ] Schema `task-TASK-NNN.md` implementado e documentado
- [ ] Schema `task-implementation-map-STORY-*.md` implementado
- [ ] `x-task-plan` refatorado como callable skill
- [ ] `x-story-plan` invoca `x-task-plan` per task
- [ ] `x-dev-task-implement` criada e testada
- [ ] `x-dev-story-implement` orquestra via task map
- [ ] `x-dev-epic-implement` simplificada (só phase order)
- [ ] Templates `_TEMPLATE-TASK.md` e `_TEMPLATE-TASK-IMPLEMENTATION-MAP.md` criados
- [ ] 5 novas RULEs (TF-01..05) escritas, revisadas e indexadas em CLAUDE.md
- [ ] `planning_schema_version` implementado com backward compat v1
- [ ] CLAUDE.md atualizada com o novo fluxo
- [ ] Testes de integração end-to-end do novo flow verdes
- [ ] Dogfood: próximo épico (após este shipar) usa v2 com sucesso
- [ ] Coverage ≥ 95% line / ≥ 90% branch mantida
- [ ] Grep sanity: zero referências a "task embedded in story"
