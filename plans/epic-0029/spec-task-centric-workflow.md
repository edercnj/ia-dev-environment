# Especificacao: Task-Centric Workflow Overhaul

## Visao Geral

O skill `/x-dev-epic-implement` e `/x-dev-lifecycle` atualmente operam com PRs por **historia** — cada story gera um unico branch e PR. Tasks dentro de uma historia sao checkboxes informais (Section 8 do template de story) sem IDs, sem rastreamento de estado, e sem entrega independente. Isso causa PRs grandes, dificeis de revisar, e impede aprovacao humana granular.

## Problema

1. **PRs grandes demais**: Uma story com 5-8 sub-tarefas gera um PR com centenas de linhas de mudanca, dificultando revisao humana
2. **Sem rastreabilidade por task**: Tasks nao tem IDs formais, nao aparecem em commits, e nao podem ser individualmente rastreadas
3. **Sem gate de aprovacao granular**: O humano so pode aprovar/rejeitar a story inteira, nao tasks individuais
4. **Tasks nao-testaveis**: Tasks como "criar interface Port" ou "criar DTO" sozinhas nao sao verificaveis
5. **Skills atomicas faltantes**: Nao existem skills dedicadas para lint, format, TDD, commit, worktree, documentacao
6. **Reviews monoliticos**: O `x-review` tem 8 especialistas inline em um unico SKILL.md enorme, dificultando manutencao
7. **Git Flow nao integrado**: Branch naming e merge strategy nao seguem Git Flow rigorosamente

## Objetivo

Refatorar o workflow de desenvolvimento para que a **task** seja a unidade de entrega (1 task = 1 branch = 1 PR). Cada PR requer aprovacao humana antes de prosseguir. Criar skills atomicas para cada etapa do workflow (lint, format, commit, TDD, PR, docs, worktree). Extrair reviews individuais em skills separadas. Adicionar flag `--auto-approve-pr` para modo de velocidade com branch-mae.

## Componentes do Sistema

### 1. _TEMPLATE-STORY.md (Template de Historia)

Template que define o formato de historias geradas. Localizado em `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md`.

**Secao relevante:**
- Section 8 (Sub-tarefas): atualmente checkboxes informais sem IDs, sem testabilidade

### 2. _TEMPLATE-IMPLEMENTATION-MAP.md (Template de Mapa de Implementacao)

Template para grafos de dependencia entre historias. Localizado em `java/src/main/resources/shared/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`.

**Secao relevante:**
- Dependencias sao apenas no nivel de story, nao de task

### 3. _TEMPLATE-EXECUTION-STATE.json (Template de Estado de Execucao)

Template para checkpoint de execucao. Localizado em `java/src/main/resources/shared/templates/`.

**Secao relevante:**
- Rastreamento apenas no nivel de story (status, prUrl, prNumber)
- Sem campos para tasks individuais

### 4. x-dev-lifecycle/SKILL.md (Orquestrador de Story)

Arquivo (~742 linhas) que implementa o ciclo de vida de cada story em 9 fases. Localizado em `java/src/main/resources/targets/claude/skills/core/x-dev-lifecycle/SKILL.md`.

**Fases relevantes:**
- Phase 0: Branch & Setup
- Phase 2: TDD Implementation (monolitico, implementa toda a story em um unico branch)
- Phase 6: Commit & PR (1 PR por story)

### 5. x-dev-epic-implement/SKILL.md (Orquestrador de Epico)

Arquivo (~1500 linhas) que orquestra multi-story execution. Localizado em `java/src/main/resources/targets/claude/skills/core/x-dev-epic-implement/SKILL.md`.

**Secoes relevantes:**
- Phase 1: Execution Loop (dispatch por story, nao por task)
- execution-state.json: estado por story, nao por task
- Merge modes: --auto-merge, --no-merge, --interactive-merge

### 6. x-review/SKILL.md (Orquestrador de Review)

Arquivo que contem 8 especialistas inline com checklists completos. Localizado em `java/src/main/resources/targets/claude/skills/core/x-review/SKILL.md`.

**Problema:** Checklists de cada especialista vivem dentro do mesmo arquivo, dificultando manutencao e reutilizacao independente.

### 7. x-git-push/SKILL.md (Operacoes Git)

Skill para branch creation, commits e PR. Localizado em `java/src/main/resources/targets/claude/skills/core/x-git-push/SKILL.md`.

**Problema:** Branch naming e commit conventions nao incluem task IDs.

### 8. x-story-create/SKILL.md (Criacao de Historias)

Skill para gerar historias a partir de epicos. Localizado em `java/src/main/resources/targets/claude/skills/core/x-story-create/SKILL.md`.

**Problema:** Section 8 gera tasks informais sem testabilidade enforced.

### 9. x-story-map/SKILL.md (Mapa de Implementacao)

Skill para gerar o grafo de dependencias. Localizado em `java/src/main/resources/targets/claude/skills/core/x-story-map/SKILL.md`.

**Problema:** Dependencias apenas no nivel de story, nao de task.

### 10. Checkpoint Java Classes

- `StoryEntry.java`: Record para estado por story
- `CheckpointEngine.java`: Engine de checkpoint
- `ResumeHandler.java`: Handler de resume/recovery
- `CheckpointValidation.java`: Validacao de checkpoint
- `SkillsSelection.java`: Selecao condicional de skills
- `SkillGroupRegistry.java`: Registro de grupos de skills

Localizados em `java/src/main/java/dev/iadev/checkpoint/` e `java/src/main/java/dev/iadev/application/assembler/`.

## Mudancas Necessarias

### MUD-01: Formal Task Definition no Template de Story

**O que:** Substituir Section 8 (Sub-tarefas) no `_TEMPLATE-STORY.md` por formato formal com IDs, testabilidade, camadas, dependencias, branch naming, e criterios de testabilidade.

**Formato do Task ID:** `TASK-XXXX-YYYY-NNN` (epic-story-sequencial)

**Anti-patterns proibidos:**
- Interface sem implementacao
- DTO sem uso
- Teste sem codigo (exceto Layer 4)
- Config sem teste de verificacao

**Padroes validos (toda task DEVE ser um destes):**
- Domain model + unit tests
- Port + adapter + integration test
- Use case + acceptance test
- Endpoint + API test
- Migration + smoke test
- Config + verification test

**Sizing:** 3-6 tasks por story, cada 50-150 LOC (ideal M).

### MUD-02: Task Status Model e Execution State

**O que:** Criar `TaskEntry.java` record e `TaskStatus.java` enum. Atualizar `StoryEntry.java` com campo `tasks: Map<String, TaskEntry>` e `parentBranch: String`. Atualizar `execution-state.json` com task-level tracking.

**TaskStatus:** PENDING, IN_PROGRESS, PR_CREATED, PR_APPROVED, PR_MERGED, DONE, BLOCKED, FAILED, SKIPPED.

**Resume:** Task-level reclassification (IN_PROGRESS → PENDING, verificar PRs via `gh pr view`).

### MUD-03: Skill x-format (Code Formatting)

**O que:** Criar skill que detecta linguagem via `{{LANGUAGE}}` e roda formatter apropriado.

**Suporte:** Java (spotless/google-java-format), TypeScript (prettier), Python (ruff format/black), Go (gofmt), Rust (rustfmt), Kotlin (ktfmt).

**Flags:** `--check` (dry-run), `--changed-only`.

### MUD-04: Skill x-lint (Code Linting)

**O que:** Criar skill que detecta linguagem e roda linter apropriado.

**Suporte:** Java (checkstyle/spotbugs/pmd), TypeScript (eslint), Python (ruff/pylint), Go (golangci-lint), Rust (clippy), Kotlin (ktlint/detekt).

**Flags:** `--fix`, `--changed-only`.

### MUD-05: Skill x-commit (Conventional Commit)

**O que:** Criar skill para commits padronizados com task ID no scope e pre-commit chain.

**Formato:** `<type>(<TASK-XXXX-YYYY-NNN>): <subject> [TDD:TAG]`

**Pre-commit chain:** format → lint → compile → commit.

**TDD tags:** `[TDD]`, `[TDD:RED]`, `[TDD:GREEN]`, `[TDD:REFACTOR]`.

### MUD-06: Skill x-worktree (Git Worktree Management)

**O que:** Criar skill para gerenciar worktrees para execucao paralela de tasks/stories.

**Operacoes:** create, list, remove, cleanup.

**Naming:** `.claude/worktrees/task-XXXX-YYYY-NNN/` ou `.claude/worktrees/story-XXXX-YYYY/`.

### MUD-07: Skill x-plan-task (Task Planning)

**O que:** Criar skill para planejar uma task individual dentro de uma story.

**Input:** Story ID + `--task TASK-NNN`.

**Output:** `plans/epic-XXXX/plans/task-plan-XXXX-YYYY-NNN.md` com TDD cycle mapping, affected files, layer order, security checklist.

### MUD-08: Skill x-tdd (TDD Execution)

**O que:** Criar skill para executar ciclos Red-Green-Refactor por task.

**Ciclos TPP:** degenerate → constants → conditionals → iterations → complex.

**Cada ciclo:** RED (test falha) → GREEN (minimo para passar) → REFACTOR (sem mudar comportamento) → commit via x-commit.

### MUD-09: Skill x-pr-create (Task PR Creation)

**O que:** Criar skill para PRs padronizados com task ID.

**Title:** `feat(TASK-XXXX-YYYY-NNN): description` (≤ 70 chars).

**Auto-labels:** `task`, `story-XXXX-YYYY`, `epic-XXXX`.

**Target:** `develop` (default) ou parent branch (auto-approve mode).

### MUD-10: Skill x-docs (Documentation)

**O que:** Criar skill para criar/atualizar documentacao baseada em mudancas de codigo.

**Tipos:** api, readme, adr, changelog.

**Delegacao:** x-changelog, x-dev-adr-automation, x-dev-arch-update.

### MUD-11: Individual Review Skills Extraction

**O que:** Extrair cada especialista do x-review para sua propria skill.

**Core (sempre incluidos):**
- x-review-qa: QA (18 items, /36)
- x-review-perf: Performance (13 items, /26)

**Conditional (feature-gated):**
- x-review-db: Database (20 items, /40) — quando database != none
- x-review-obs: Observability (9 items, /18) — quando observability configurado
- x-review-devops: DevOps (10 items, /20) — quando container/orchestrator != none
- x-review-data-modeling: Data Modeling (10 items, /20) — quando database != none AND architecture hexagonal/ddd/cqrs

### MUD-12: x-review Orchestrator Refactor

**O que:** Simplificar x-review para delegar a skills individuais em vez de conter checklists inline.

**Phase 2:** Invocar `/x-review-{specialist}` via Skill tool em paralelo (single message).

### MUD-13: x-story-create — Testable Tasks e Value Delivery

**O que:** Atualizar geracao de Section 8 com tasks formais e testabilidade enforced.

**Quality gate:** adicionar dimensao "Task testability" (20%) e "Task independence" (10%).

### MUD-14: x-story-map — Task-Level Dependency Graph

**O que:** Adicionar Section 8 ao Implementation Map com dependencias de tasks.

**Conteudo:** Cross-story task dependencies, computed merge order, Mermaid task dependency graph.

### MUD-15: x-dev-lifecycle — Task-Centric Workflow

**O que:** Reescrever Phase 2 como Task Execution Loop com PRs por task e gates de aprovacao.

**Novo Phase 2:**
1. Para cada task na ordem de dependencias
2. Criar branch: feat/task-XXXX-YYYY-NNN-desc
3. Implementar via x-tdd
4. Push + criar PR via x-pr-create
5. APPROVAL GATE: AskUserQuestion (APPROVE/REJECT/PAUSE)
6. Atualizar execution-state.json

**Flag `--auto-approve-pr`:**
- Cria parent branch: feat/story-XXXX-YYYY-desc
- Task PRs auto-merged na parent branch
- Parent branch NUNCA auto-merged para develop/main

### MUD-16: x-dev-epic-implement — Auto-Approve e Task Tracking

**O que:** Propagar --auto-approve-pr, task-level state tracking, batch approval prompts.

**Batch approval:** Quando multiplas stories produzem task PRs em paralelo, consolidar em prompt unico.

### MUD-17: x-git-push — Task Branch Naming

**O que:** Atualizar branch naming e commit conventions para modelo task-centric.

**Branch:** `feat/task-XXXX-YYYY-NNN-short-desc` (≤ 60 chars).

**Commit scope:** `feat(TASK-XXXX-YYYY-NNN): subject`.

### MUD-18: Golden Files e Integration Tests

**O que:** Regenerar golden files para 8 perfis. Adicionar testes para novo formato de tasks, task-level execution state, novas skills, conditional review skills.

## Regras de Negocio

### RULE-001: Task como Unidade de Entrega
Cada task gera seu proprio branch e PR. Tasks sao independentemente testaveis e revisaveis.

### RULE-002: Testabilidade Obrigatoria
Toda task DEVE ser testavel — deve seguir um dos padroes validos e incluir criterios de testabilidade explicitos.

### RULE-003: Approval Gate Humano
Cada task PR requer aprovacao humana antes de prosseguir (exceto em modo --auto-approve-pr).

### RULE-004: Auto-Approve com Branch-Mae
Flag --auto-approve-pr cria branch-mae (story ou epic level). Task PRs sao auto-merged na branch-mae. A branch-mae NUNCA eh auto-merged para develop ou main.

### RULE-005: Git Flow Compliance
Feature branches criadas a partir de develop. PRs para develop (modo manual) ou parent branch (modo auto-approve). Nunca commit direto em main ou develop.

### RULE-006: Task ID Format
IDs seguem `TASK-XXXX-YYYY-NNN` onde XXXX=epic, YYYY=story, NNN=sequencial (001-999).

### RULE-007: Pre-Commit Chain
Commits passam por format → lint → compile antes de serem criados.

### RULE-008: TDD Strict
Ciclos Red-Green-Refactor sao obrigatorios. RED que passa = ciclo invalido. GREEN com gold-plating = violacao.

### RULE-009: Review Modular
Cada especialista tem sua propria skill. O orquestrador apenas delega e consolida.

### RULE-010: Backward Compatibility
Tasks field no execution-state.json eh opcional — epicos antigos continuam funcionando.

### RULE-011: Sizing Constraints
Minimo 3 tasks por story, maximo 8. Cada task 50-150 LOC (ideal).

### RULE-012: Cross-Story Task Dependencies
Dependencias entre tasks de stories diferentes DEVEM ser refletidas em dependencias story-level.

### RULE-013: Batch Approval
Quando multiplas stories produzem task PRs simultaneamente, o usuario recebe prompt consolidado.

### RULE-014: Resume por Task
Resume verifica status de PRs de cada task via `gh pr view`. IN_PROGRESS → PENDING.

### RULE-015: Value Delivery
Cada story DEVE ter Secao 3.5 (Entrega de Valor) com valor de negocio, nao tarefa tecnica.

### RULE-016: Conventional Commits com Task ID
Formato: `<type>(<TASK-XXXX-YYYY-NNN>): <subject> [TDD:TAG]`.

### RULE-017: Documentation Sync
Documentacao deve ser atualizada em sincronia com mudancas de codigo.

### RULE-018: Worktree Lifecycle
Worktrees sao criados para tasks paralelas e limpos apos merge ou apos 7 dias sem atividade.

## Fluxo Principal

```
1. x-story-epic-full gera epic + stories (com tasks formais) + implementation map (com task deps)
2. x-dev-epic-implement:
   a. Parse arguments, validar prerequisites
   b. Pre-flight analysis (advisory)
   c. Para cada fase do implementation map:
      i.   getExecutableStories() — verifica dependencias
      ii.  Dispatch x-dev-lifecycle per story
      iii. x-dev-lifecycle executa tasks sequencialmente:
           - Para cada task: branch → x-tdd → x-pr-create → APPROVAL GATE
           - Se --auto-approve-pr: auto-merge task PR na parent branch
           - Se nao: pausa e espera aprovacao humana
      iv.  Story DONE quando todas tasks DONE
      v.   Integrity gate apos fase completa
   d. Report final com tabela de PRs
   e. Verificacao final
```

## Restricoes

- Todos os artefatos gerados devem ser em portugues brasileiro (pt-BR)
- Termos tecnicos em ingles permanecem em ingles
- IDs seguem o padrao: epic-XXXX, story-XXXX-YYYY, TASK-XXXX-YYYY-NNN, RULE-NNN
- As mudancas sao em SKILL.md files (skills), templates (_TEMPLATE-*.md), e Java source (checkpoint/)
- O projeto eh `ia-dev-environment` — uma meta-ferramenta CLI Java 21 (picocli 4.7) que gera configuracoes de dev environment
- Skills usam template variables: {{LANGUAGE}}, {{ARCHITECTURE}}, {{BUILD_TOOL}}, {{FRAMEWORK}}, {{COMPILE_COMMAND}}, {{TEST_COMMAND}}, {{COVERAGE_COMMAND}}, etc.
- Skills core vivem em `java/src/main/resources/targets/claude/skills/core/`
- Skills conditional vivem em `java/src/main/resources/targets/claude/skills/conditional/`
- Knowledge packs vivem em `java/src/main/resources/targets/claude/skills/knowledge-packs/`
