---
name: x-epic-decompose
description: "Complete decomposition of a system specification into an Epic, individual Story files, and an Implementation Map with dependency graph and phased execution plan. Orchestrates spec analysis, rule extraction, story identification, and implementation planning."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion
argument-hint: "[SPEC-FILE-PATH] [--jira <PROJECT_KEY>] [--no-jira]"
context-budget: medium
---

## Output Policy

- **Language**: Portuguese (pt-BR) for all content. English for technical terms (cache, timeout, handler, endpoint) and code identifiers.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Spec-to-Stories Decomposition (Orchestrator)

## Purpose

Orchestrate the full decomposition of a system specification into three deliverables: an **Epic**, individual **Stories**, and an **Implementation Map**. Coordinate the work of three focused skills, each handling one deliverable.

## When to Use

- Decompose a spec into stories and epic
- Break down a system document into implementable work items
- Generate a complete project backlog from a specification
- Create epic, stories, and implementation plan from a technical document
- Full decomposition pipeline (epic + stories + map) in a single pass
- Prefer this skill over the individual `x-epic-create`, `x-story-create`, or `x-epic-map` skills when all three deliverables are needed

## Prerequisites

Read these files before starting:

**Templates (output structure) — read all three:**
- `.claude/templates/_TEMPLATE-EPIC.md`
- `.claude/templates/_TEMPLATE-STORY.md`
- `.claude/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`

**Decomposition philosophy:**
- `references/decomposition-guide.md` (bundled with this skill)

If any template is missing, stop and tell the user.

## Workflow Overview

```
1. ANALYSIS      -> Read spec, identify rules, stories, dependencies, phases (inline)
1.5. JIRA        -> Determine Jira integration mode (conditional, inline)
2. EPIC          -> Generate Epic file with rules, story index, DoR/DoD (inline)
3. STORIES       -> Generate one story file per story with contracts, Gherkin, sub-tasks (inline)
4. MAP           -> Generate Implementation Map with dependency graph, phases, critical path (inline)
4.5. JIRA LINKS  -> Create Jira dependency links (conditional, inline)
5. REPORT        -> Save all files, validate quality, report summary (inline)
```

## Decomposition Philosophy

Before generating anything, read `references/decomposition-guide.md`. It explains the
layer-by-layer approach that drives the entire decomposition:

- **Layer 0 (Foundation)**: Infrastructure — servers, schemas, APIs, protocol adapters
- **Layer 1 (Core Domain)**: The central operation establishing architectural patterns
- **Layer 2 (Extensions)**: Additional operations reusing core patterns
- **Layer 3 (Compositions)**: Stories combining multiple extension capabilities
- **Layer 4 (Cross-Cutting)**: Testing, observability, security, tech debt

The guide also covers:
- How to identify story boundaries (1 endpoint = 1 story, 1 protocol flow = 1 story)
- Dependency mapping (structural, data, pattern dependencies)
- Phase computation (DAG-based, parallel within phases)
- Sizing heuristics (too big, too small, just right)

## Phase 1 — Analysis

1. Ask the user which spec file to process (or use the one they provided)
2. Read the entire spec file
3. Read `references/decomposition-guide.md`
4. Analyze the spec:
   - Identify cross-cutting rules (rules that span multiple journeys)
   - Identify stories by layer (foundation → core → extensions → compositions → cross-cutting)
   - Map dependencies between stories
   - Compute phases from the dependency DAG
   - Identify the critical path

## Phase 1.5 — Jira Integration Decision

Before generating any artifacts, determine if Jira integration is desired.

### 1.5.1 — Check MCP Availability

Verify that the Jira MCP tool (`mcp__atlassian__createJiraIssue`) is available.
If the tool is NOT available, set `jiraContext = { enabled: false }` and skip to
Phase 2 silently — do not warn the user.

### 1.5.1b — Roteamento por Flag (EPIC-0042)

Antes de prompt ao usuario, verificar as flags de Jira:

- Se `--no-jira` estiver presente: definir `jiraContext = { enabled: false }` e pular
  para a Phase 2. Log: `"Jira integration skipped (--no-jira, EPIC-0042)"`

- Se `--jira <PROJECT_KEY>` estiver presente: usar a chave de projeto fornecida
  diretamente. Pular AskUserQuestion (Step 1.5.2). Descobrir o `cloudId` chamando
  `mcp__atlassian__getAccessibleAtlassianResources`. Usar o `id` do primeiro site
  disponivel como `cloudId`. Se a chamada falhar ou nao retornar sites, alertar o
  usuario e definir `jiraContext = { enabled: false }`. Caso contrario, definir
  `jiraContext = { enabled: true, cascadeToStories: true, projectKey: "<PROJECT_KEY>", cloudId: "<cloudId>" }`.
  Log: `"Jira integration via --jira flag: project {PROJECT_KEY}, cascade to stories (EPIC-0042)"`
  Pular para a Phase 2.

- Se nenhuma flag estiver presente: prosseguir para o Step 1.5.2 (prompt interativo
  compativel com a versao anterior).

### 1.5.2 — Ask the User (no flags provided)

Use the `AskUserQuestion` tool:

```
question: "Deseja criar o épico e as histórias no Jira?"
header: "Jira"
options:
  - label: "Sim, criar tudo no Jira"
    description: "Criar o épico e TODAS as histórias como issues no Jira automaticamente via MCP"
  - label: "Apenas o épico no Jira"
    description: "Criar somente o épico no Jira. As histórias serão apenas markdown"
  - label: "Não, apenas markdown"
    description: "Gerar apenas os arquivos markdown sem integração com Jira"
multiSelect: false
```

### 1.5.3 — Build jiraContext

Based on user selection:

- **"Sim, criar tudo no Jira"**:
  1. Ask for the Jira project key using AskUserQuestion:
     ```
     question: "Qual a chave do projeto Jira? (ex: PROJ, MYAPP, TEAM)"
     header: "Projeto"
     ```
  2. Discover the `cloudId` by calling `mcp__atlassian__getAccessibleAtlassianResources`.
     Use the first available site's `id` as the `cloudId`. If the call fails or returns
     no sites, warn the user and set `jiraContext = { enabled: false }`.
  3. Set `jiraContext = { enabled: true, cascadeToStories: true, projectKey: "<key>", cloudId: "<cloudId>" }`

- **"Apenas o épico no Jira"**:
  1. Ask for the Jira project key (same as above)
  2. Discover the `cloudId` (same as above)
  3. Set `jiraContext = { enabled: true, cascadeToStories: false, projectKey: "<key>", cloudId: "<cloudId>" }`

- **"Não, apenas markdown"**:
  1. Set `jiraContext = { enabled: false }`

The `jiraContext` is passed to all subsequent phases and used to control Jira issue creation.

## Phase 2 — Generate the Epic

Follow the instructions in `.claude/skills/x-epic-create/SKILL.md`:

- Determine the epic number (scan `plans/` for existing `epic-XXXX` folders, use next available; default `0001`)
- Create directory `plans/epic-XXXX/`
- Extract rules → RULE-001..N table
- Build story index with titles and dependencies (using `story-XXXX-YYYY` IDs)
- Define DoR/DoD from spec quality requirements — the DoD must include TDD Compliance (test-first commits, explicit refactoring after green, incremental tests via TPP) and Double-Loop TDD (acceptance tests from Gherkin as outer loop, unit tests as inner loop)
- Generate `plans/epic-XXXX/epic-XXXX.md` following `_TEMPLATE-EPIC.md`

**Jira Integration (if `jiraContext.enabled == true`):**

After generating the Epic markdown file:
1. Call `mcp__atlassian__createJiraIssue` to create an Epic issue:
   - `cloudId`: `jiraContext.cloudId`
   - `projectKey`: `jiraContext.projectKey`
   - `issueTypeName`: "Epic"
   - `summary`: The Epic title (from the generated header)
   - `description`: The "Visão Geral" section text
   - `contentFormat`: "markdown"
   - `additional_fields`: `{ "labels": [{ "name": "generated-by-ia-dev-env" }, { "name": "epic-XXXX" }] }` (where `epic-XXXX` is the local ID for bidirectional sync)
2. Capture the returned Jira issue key (e.g., "PROJ-123")
3. Update `jiraContext.epicIssueKey` with the returned key
4. Replace `<CHAVE-JIRA>` in the generated Epic markdown with the actual Jira key
5. If creation fails: warn the user, set `<CHAVE-JIRA>` to `EPIC-XXXX (Jira: falha na criação)`,
   leave `jiraContext.epicIssueKey` absent (do NOT set it to an empty string or invalid value),
   and continue. In Phase 3, stories will be created without a `parent` link when
   `jiraContext.epicIssueKey` is absent, maintaining non-blocking behavior

If `jiraContext.enabled == false`: replace `<CHAVE-JIRA>` with `—` in the Epic markdown.

## Phase 3 — Generate the Stories

Follow the instructions in `.claude/skills/x-story-create/SKILL.md`:

For each story in the Epic's index:
- Dependencies (Blocked By / Blocks) — must be symmetric
- Applicable rules (reference Epic rules by ID)
- User story description + technical context
- **Entrega de Valor (Section 3.5)** — measurable business value from the business perspective
  (NOT technical tasks — see x-story-create Section 3.5 rules for FORBIDDEN/REQUIRED patterns)
- Data contracts (precise: field names, types, formats, derivation rules)
- Mermaid sequence diagrams (real component names from the spec)
- Gherkin acceptance criteria with mandatory categories (degenerate case, happy path, error paths, boundary values) ordered by Transformation Priority Premise (simplest degenerate → complex edge cases)
- Sub-tasks tagged `[Dev]`, `[Test]`, `[Doc]` — MUST include at least one `[Test] Smoke/E2E` sub-task

Generate files as `plans/epic-XXXX/story-XXXX-YYYY.md` following `_TEMPLATE-STORY.md`.

**Jira Integration (if `jiraContext.cascadeToStories == true`):**

Pass `jiraContext` to the story generation logic. For each generated story:
1. Call `mcp__atlassian__createJiraIssue` to create a Story issue:
   - `cloudId`: `jiraContext.cloudId`
   - `projectKey`: `jiraContext.projectKey`
   - `issueTypeName`: "Story"
   - `summary`: The story title
   - `description`: The user story text from Section 3 (the "Como **Persona**..." paragraph)
   - `contentFormat`: "markdown"
   - `parent`: `jiraContext.epicIssueKey` (links the story to the parent epic) — include only if `jiraContext.epicIssueKey` is present; omit entirely when absent
   - `additional_fields`: `{ "labels": [{ "name": "generated-by-ia-dev-env" }, { "name": "story-XXXX-YYYY" }] }` (where `story-XXXX-YYYY` is the local story ID for bidirectional sync)
2. Replace `<CHAVE-JIRA>` in the story markdown with the returned Jira key
3. If creation fails for a story: warn, set `<CHAVE-JIRA>` to `—`, continue with remaining stories

If `jiraContext.cascadeToStories == false` or `jiraContext.enabled == false`:
replace `<CHAVE-JIRA>` with `—` in all story markdowns.

No additional `AskUserQuestion` is needed — the cascade decision was already made in Phase 1.5.

## Phase 4 — Generate the Implementation Map

Follow the instructions in `.claude/skills/x-epic-map/SKILL.md`:

- Dependency matrix (all stories, validated for consistency)
- Phase diagram (ASCII box-drawing)
- Critical path analysis
- Mermaid dependency graph (color-coded by phase)
- Phase summary and detail tables
- Strategic observations (bottleneck, leaves, parallelism, convergences, validation milestone)

Generate `plans/epic-XXXX/implementation-map-XXXX.md` following `_TEMPLATE-IMPLEMENTATION-MAP.md`.

If Jira keys are available (from Phase 3), include them in the dependency matrix's
`Chave Jira` column.

## Phase 4.5 — Jira Dependency Linking (if applicable)

If `jiraContext.enabled == true` and `jiraContext.cascadeToStories == true`, and all
stories have Jira keys:

1. For each story, read its "Blocked By" list
2. For each blocker that has a Jira key, call `mcp__atlassian__createIssueLink`:
   - `cloudId`: `jiraContext.cloudId`
   - `type`: "Blocks"
   - `inwardIssue`: the blocker's Jira key (the issue that blocks)
   - `outwardIssue`: the current story's Jira key (the issue that is blocked)
3. Report: "N dependency links criados no Jira"

If linking fails for some stories, log warnings but do not fail the pipeline.
This step is best-effort — Jira links are a convenience, not a hard requirement.

## Phase 5 — Save and Report

All files are saved inside `plans/epic-XXXX/` (the epic's dedicated folder).

Report summary:
- Total rules extracted
- Total stories generated
- Total phases computed
- Critical path length (phases and stories)
- Maximum parallelism (largest phase)
- Main bottleneck (story blocking the most others)

If Jira integration was active, also report:
- Jira integration: Enabled (project: `<PROJECT_KEY>`)
- Epic created in Jira: `<JIRA-KEY>` (or "falha" if failed)
- Stories created in Jira: N of M (successful/total)
- Dependency links created: K
- Failures: list any failed items

### Quality Validation (before completing Phase 5)

Before reporting, validate that all generated artifacts meet these quality gates:

- [ ] Every story has a "Entrega de Valor" section (Section 3.5) with measurable business value
- [ ] Every story has at least one smoke/automated test sub-task `[Test] Smoke/E2E` in Section 8
- [ ] Every story DoD Local includes automated test requirement
- [ ] Stories are decomposed around business value, not technical layers (SD-10 compliance)
- [ ] Story template includes `**Status:** Pendente` field in the header
- [ ] "Entrega de Valor" column is populated in the Epic Index (Section 5)
- [ ] Epic labels in Jira include local ID (e.g., `epic-XXXX`) for bidirectional sync
- [ ] Story labels in Jira include local ID (e.g., `story-XXXX-YYYY`) for bidirectional sync

If any story fails validation, fix it before saving. Do not skip validation.

## Language Rules

- All generated content must be in **Brazilian Portuguese (pt-BR)**
- Technical terms in English stay in English (cache, timeout, handler, endpoint, etc.)
- Code identifiers, field names, enum values stay in English
- Gherkin in Portuguese: `Cenario`, `DADO`, `QUANDO`, `ENTÃO`, `E`, `MAS`
- IDs: RULE-NNN (English format), story-XXXX-YYYY (composite), epic-XXXX (kebab-case)

## Error Handling

| Scenario | Action |
|----------|--------|
| Spec file not found or empty | Abort with message: `Spec file not found or is empty. Provide a valid path.` |
| Template file missing (`_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, or `_TEMPLATE-IMPLEMENTATION-MAP.md`) | Stop and tell the user which template is missing |
| Jira MCP tool unavailable | Set `jiraContext = { enabled: false }`, skip Jira integration silently |
| Jira Epic creation fails | Warn the user, set `<CHAVE-JIRA>` to `EPIC-XXXX (Jira: falha na criacao)`, continue |
| Jira Story creation fails for a story | Warn, set `<CHAVE-JIRA>` to `—` for that story, continue with remaining stories |
| Circular dependency detected in story graph | Abort with message listing the cycle and affected stories |
| Story fails quality validation (Phase 5) | Fix the story before saving — do not skip validation |

## Template Fallback

This skill requires all three templates (`_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md`). Unlike other orchestrators, there is no graceful fallback — missing templates halt execution because the output format is strictly defined.

## Quality Checklist

Before delivering, verify:

- [ ] Every rule in the Epic is referenced by at least one story
- [ ] Every story references at least one rule (except infrastructure stories)
- [ ] Dependencies are symmetric (A blocks B ↔ B blocked by A)
- [ ] No circular dependencies
- [ ] Phase computation is correct (stories only enter phase when ALL deps are in earlier phases)
- [ ] Critical path is the actual longest chain (not just the deepest phase)
- [ ] Data contracts match the spec exactly (field names, types, formats)
- [ ] Each story has at least 4 Gherkin scenarios covering all mandatory categories (degenerate, happy path, error paths, boundary values), ordered by TPP (degenerate → edge cases)
- [ ] Epic DoD includes TDD Compliance and Double-Loop TDD
- [ ] Boundary values use triplet pattern (at-min, at-max, past-max)
- [ ] Implementation map observations are specific, not generic
- [ ] All files follow their respective templates exactly

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-epic-create` | Delegates to | Phase 2 follows `x-epic-create/SKILL.md` instructions for Epic generation |
| `x-story-create` | Delegates to | Phase 3 follows `x-story-create/SKILL.md` instructions for Story generation |
| `x-epic-map` | Delegates to | Phase 4 follows `x-epic-map/SKILL.md` instructions for Implementation Map |
| `x-epic-implement` | Followed by | Generated artifacts are consumed by epic implementation |
| `x-task-implement` | Followed by | Individual stories can be implemented via `/x-task-implement` |
| `_TEMPLATE-EPIC.md` | Reads | Output format for Epic file |
| `_TEMPLATE-STORY.md` | Reads | Output format for Story files |
| `_TEMPLATE-IMPLEMENTATION-MAP.md` | Reads | Output format for Implementation Map |
| `references/decomposition-guide.md` | Reads | Decomposition philosophy and layer-by-layer approach |
| `mcp__atlassian__createJiraIssue` | Calls (conditional) | Jira Epic and Story creation when `jiraContext.enabled` |
| `mcp__atlassian__createIssueLink` | Calls (conditional) | Jira dependency linking in Phase 4.5 |
