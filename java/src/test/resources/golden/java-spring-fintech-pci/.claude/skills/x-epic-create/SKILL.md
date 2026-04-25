---
name: x-epic-create
description: "Generate an Epic document from a system specification file with cross-cutting business rules, global quality definitions (DoR/DoD), a complete story index with dependency declarations, and optional Jira integration."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion, Skill
argument-hint: "<SPEC_FILE> [--epic-id XXXX] [--jira <PROJECT_KEY>] [--no-jira] [--dry-run]"
context-budget: medium
---

## Output Policy

- **Language**: Portuguese (pt-BR) for all content. English for technical terms (cache, timeout, handler, endpoint) and code identifiers.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Epic Generator

## Purpose

Read a system specification document and generate an Epic file — the top-level artifact that defines scope, cross-cutting rules, quality criteria, and story index for a development effort. The Epic is the single source of truth for a decomposition: it captures rules spanning multiple stories, defines quality gates, and provides the complete story index with dependency relationships.

## Triggers

- `/x-epic-create <spec_file>` — generate an epic from the specification
- User asks to create an epic, generate an epic from a spec, or decompose a specification into an epic
- User mentions extracting cross-cutting rules, defining quality gates, or building a story backlog from a technical document

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `<SPEC_FILE>` | Path | Yes | — | Path to the system specification file |
| `--epic-id` | String | No | auto | Epic number (auto-increments from existing epics in `plans/`) |
| `--jira` | String | No | — | Jira project key (e.g., PROJ). When provided, skip AskUserQuestion and create in Jira directly (EPIC-0042). |
| `--no-jira` | Boolean | No | false | Skip Jira integration entirely, no prompting (EPIC-0042). |
| `--dry-run` | Boolean | No | false | When true, artifacts are written to disk but Steps P4 / P5 (planning-commit / push) become no-ops with a `"dry-run, skipping commit"` warning (EPIC-0049 / RULE-007). |

## Prerequisites

Read the following files before starting:

**Template (output structure):**
- `.claude/templates/_TEMPLATE-EPIC.md` — The exact structure to follow (RA9 v2: 9 sections)

**RA9 planning contract (source of truth for 9-section model):**
- `.claude/skills/planning-standards-kp/SKILL.md` — Defines all 9 RA9 sections, rule anchors, Packages granularity, and Decision Rationale micro-template. Read before generating Sections 2 and 8.

**Decomposition philosophy (how to identify stories and rules):**
- `.claude/skills/x-epic-decompose/references/decomposition-guide.md`

If any template file is missing, stop and tell the user. The templates define the output structure
and must be read fresh from disk every time (never hardcode the structure).

> **RA9 guidance (EPIC-0056):** When generating the epic, explicitly fill:
> - **Section 2 (Packages Hexagonal):** Identify all packages touched across the 5 hexagonal layers from the spec. Mark untouched layers with `—`. Declare dependency direction.
> - **Section 8 (Decision Rationale):** Extract at least 1 architectural decision from the spec using the 4-line micro-template (`**Decisão:** / **Motivo:** / **Alternativa descartada:** / **Consequência:**`).

## Workflow

### Step P1 — Detect Worktree Context (EPIC-0049 / RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-create Phase-P1-Worktree-Detect`

Invoke `x-git-worktree` in detect-context mode to record whether the current checkout is already inside an epic worktree (`STORY_OWNS_WORKTREE=false`, `EPIC_WORKTREE_DETECTED=true|false`). Result is advisory only — `x-internal-epic-branch-ensure` (Step P2) makes the authoritative decision.

    Skill(skill: "x-git-worktree", args: "detect-context")

Continue on any detect-context failure (fail-open, RULE-006) — log a WARNING and proceed to Step P2.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-create Phase-P1-Worktree-Detect ok`

### Step P2 — Ensure `epic/<ID>` Branch (EPIC-0049 / RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-create Phase-P2-Epic-Branch-Ensure`

Resolve the effective `--epic-id`:

- If `--epic-id` was provided, use it verbatim (4-digit regex validated by Step 5).
- Otherwise, scan `plans/` for existing `epic-XXXX` folders and pick the next available number (default `0001` if none). Set this as the effective epic ID for both P2 and P4.

Invoke `x-internal-epic-branch-ensure` so the canonical `epic/<ID>` branch exists locally AND on origin (idempotent). The skill is a no-op when the current checkout is already on `epic/<ID>` or a worktree rooted at that branch.

    Skill(skill: "x-internal-epic-branch-ensure", args: "--epic-id <XXXX>")

On failure (non-zero exit), abort with `EPIC_BRANCH_ENSURE_FAILED` — a clean audit trail cannot be produced without the canonical branch.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-create Phase-P2-Epic-Branch-Ensure ok`

### Step 1 — Read the Input Spec

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-create Phase-1-Spec-Analysis`

Read the entire system specification file provided by the user. This file follows the `_TEMPLATE.md`
format with sections like Overview, Business Rules, Platform Specs, Data Contracts, Journeys,
Sync Journeys, Dependencies, and Interfaces.

Understand the full scope before starting extraction.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-create Phase-1-Spec-Analysis ok`

### Step 2 — Extract Cross-Cutting Business Rules

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-create Phase-2-Rules-Extraction`

Scan the spec for business rules that apply to more than one journey or operation. These become
the Epic's Rules table with unique IDs (RULE-001, RULE-002, ...).

**What qualifies as a cross-cutting rule:**
- A decision logic that affects multiple journeys (e.g., "cents-based approval applies to all transaction types")
- A platform-wide constraint (e.g., "idempotency key required on all mutations")
- A behavioral policy (e.g., "timeout handling: N seconds sleep before response")
- A validation that gates multiple operations (e.g., "entity must be active for any operation")

**What stays in individual stories:**
- Rules that apply to only one journey (e.g., "bulk import only allowed via async endpoint")
- Implementation details specific to one handler

Each rule gets a description detailed enough that a developer can implement it without going
back to the spec. Use `<br>` for line breaks within table cells. Include priority/precedence
when rules can conflict.

**TDD cross-cutting rules:**
When the spec describes a system that follows TDD practices, extract these as cross-cutting rules:
- **Red-Green-Refactor**: Mandatory cycle for all production code — write failing test, make it pass, refactor
- **Atomic TDD Commits**: Each Red-Green-Refactor cycle produces atomic commits with Conventional Commits format
- **Gherkin Completeness**: Every acceptance criterion must have corresponding Gherkin scenarios

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-create Phase-2-Rules-Extraction ok`

### Step 3 — Identify Stories

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-create Phase-3-Story-Index`

Read the decomposition guide (`x-epic-decompose/references/decomposition-guide.md`) for
the layer-by-layer approach. In summary:

1. **Foundation (Layer 0):** Infrastructure stories — servers, schemas, base APIs, protocol adapters
2. **Core Domain (Layer 1):** The central operation that establishes architectural patterns
3. **Extensions (Layer 2):** Additional operations reusing core patterns
4. **Compositions (Layer 3):** Stories combining multiple extension capabilities
5. **Cross-Cutting (Layer 4):** Testing, observability, security, tech debt

For each story, determine:
- **Title**: Concise, action-oriented (e.g., "Infraestrutura Socket e Echo Test (1804)")
- **Blocked By**: Which stories must be done first (structural, data, or pattern dependencies)
- **Blocks**: Which stories depend on this one

Validate the dependency graph: no circular dependencies, every extension depends on the core,
compositions depend on their constituent extensions.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-create Phase-3-Story-Index ok`

### Step 4 — Define Quality Criteria

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-create Phase-4-Epic-Generation`

**Global Definition of Ready (DoR):**
Extract from the spec's quality requirements, or derive sensible defaults:
- Technical specifications validated
- Dependencies resolved
- Data contracts reviewed

**Global Definition of Done (DoD):**
Extract from the spec, or derive from the tech stack:
- Coverage targets (line, branch)
- Required test types (unit, integration, E2E)
- Documentation requirements
- Performance SLOs
- Persistence/data integrity criteria
- TDD Compliance: commits show test-first pattern (test precedes implementation in git log), explicit refactoring after green, tests are incremental (simple to complex via Transformation Priority Premise)
- Double-Loop TDD: acceptance tests derived from Gherkin scenarios (outer loop), unit tests guided by Transformation Priority Premise (inner loop)

### Step 5 — Generate the Epic File

Write the Epic following the `_TEMPLATE-EPIC.md` structure exactly:

1. **Header**: Title, author, date, version, status
2. **Section 1 — Visao Geral**: Scope derived from the spec's Overview section
3. **Section 2 — Anexos e Referencias**: Links to the input spec and related documents
4. **Section 3 — Definicoes de Qualidade Globais**: DoR and DoD from Step 4
5. **Section 4 — Regras de Negocio Transversais**: Rules table from Step 2
6. **Section 5 — Indice de Historias**: Story index from Step 3, with links, dependencies, and **Entrega de Valor** column (measurable business value per story)

**Directory and file naming** (mandatory — see SD-09 in decomposition guide):
1. Determine the epic number: scan `plans/` for existing `epic-XXXX` folders and use the next available number (default `0001` if none exist). Ask the user if unsure.
2. Create the directory `plans/epic-XXXX/`
3. Save the Epic file as `plans/epic-XXXX/epic-XXXX.md`
4. Story IDs in the index use composite format: `story-XXXX-YYYY` (where XXXX = epic number, YYYY = story sequence)
5. Story links in the index point to `./story-XXXX-YYYY.md` (relative to the epic folder)

### Step 6 — Optional Jira Integration

After generating the Epic file content but before the final save, optionally create the
Epic in Jira.

#### 6.1 — Check MCP Availability

Verify that the Jira MCP tool (`mcp__atlassian__createJiraIssue`) is available.
If not available, skip this entire step silently and proceed to Step 7.

#### 6.2 — Check Context and Flags (EPIC-0042)

**Roteamento por flag (EPIC-0042):**

- Se `--no-jira` estiver presente: pular a integracao com Jira completamente. Substituir
  `<CHAVE-JIRA>` por `—` e prosseguir para o Step 7. Log:
  `"Jira integration skipped (--no-jira, EPIC-0042)"`

- Se `--jira <PROJECT_KEY>` estiver presente: usar a chave de projeto fornecida
  diretamente. Pular AskUserQuestion. Descobrir o `cloudId` chamando
  `mcp__atlassian__getAccessibleAtlassianResources`. Usar o `id` do primeiro site
  disponivel como `cloudId`. Se a chamada falhar ou nao retornar sites, alertar o
  usuario e pular para o Step 7 (substituir `<CHAVE-JIRA>` por `—`). Caso contrario,
  prosseguir para 6.4. Log:
  `"Jira integration via --jira flag: project {PROJECT_KEY} (EPIC-0042)"`

- Se esta skill foi invocada pelo orquestrador (`x-epic-decompose`) e um `jiraContext`
  ja foi fornecido, usar esse contexto diretamente (pular o prompt ao usuario — ja foi
  perguntado na Phase A.5). Se `jiraContext.enabled == true`, prosseguir para 6.4. Se
  `false`, pular.

- Se invocada standalone (sem `jiraContext`, sem `--jira`, sem `--no-jira`), prosseguir
  para 6.3 para o prompt interativo compativel com a versao anterior.

#### 6.3 — Ask the User (standalone invocation only, no flags)

Use the `AskUserQuestion` tool:

```
question: "Deseja criar este epico no Jira?"
header: "Jira"
options:
  - label: "Sim, criar no Jira"
    description: "Criar o epico como issue no Jira via MCP e preencher a Chave Jira no markdown"
  - label: "Nao, apenas markdown"
    description: "Gerar apenas o arquivo markdown sem integracao com Jira"
multiSelect: false
```

If "Sim":
1. Ask for the Jira project key:
   ```
   question: "Qual a chave do projeto Jira? (ex: PROJ, MYAPP, TEAM)"
   header: "Projeto"
   ```
2. Discover the `cloudId` by calling `mcp__atlassian__getAccessibleAtlassianResources`.
   Use the first available site's `id` as the `cloudId`. If the call fails or returns
   no sites, warn the user and skip to Step 7 (replace `<CHAVE-JIRA>` with `—`).

If "Nao": replace `<CHAVE-JIRA>` with `—` and proceed to Step 7.

#### 6.4 — Create Epic in Jira

Call `mcp__atlassian__createJiraIssue` to create an Epic issue:
- `cloudId`: the discovered `cloudId` (or `jiraContext.cloudId`)
- `projectKey`: the user-provided project key (or `jiraContext.projectKey`)
- `issueTypeName`: "Epic"
- `summary`: the Epic title from the generated header
- `description`: the "Visao Geral" section text
- `contentFormat`: "markdown"
- `additional_fields`: `{ "labels": [{ "name": "generated-by-ia-dev-env" }, { "name": "epic-XXXX" }] }`

Where `epic-XXXX` is the local epic ID (e.g., `epic-0012`) for bidirectional ID sync.

Capture the returned Jira issue key (e.g., "PROJ-123").

#### 6.5 — Update Markdown

Replace `<CHAVE-JIRA>` in the generated Epic markdown with the actual Jira key.
Report: "Epico criado no Jira: PROJ-123"

#### 6.6 — Jira Error Handling

If the Jira MCP tool call fails:
1. Warn the user with the error message
2. Replace `<CHAVE-JIRA>` with `EPIC-XXXX (Jira: falha na criacao)`
3. Continue to Step 7 — NEVER block Epic file generation due to Jira failures

### Step 7 — Save and Report

Save the file to `plans/epic-XXXX/epic-XXXX.md`.
Report: number of rules extracted, number of stories identified, dependency structure summary.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-create Phase-4-Epic-Generation ok`

### Step P4 — Commit Planning Artifacts (EPIC-0049 / RULE-007)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-create Phase-P4-Planning-Commit`

If `--dry-run` is set, log `"dry-run, skipping commit"` and skip this step entirely.

Otherwise, delegate the commit to `x-planning-commit` so the newly written `plans/epic-XXXX/epic-XXXX.md` is versioned on the canonical `epic/<ID>` branch without triggering the code pre-commit chain (format / lint / compile):

    Skill(skill: "x-planning-commit",
          args: "--scope chore --epic-id <XXXX> --paths plans/epic-<XXXX>/epic-<XXXX>.md --subject \"init epic specification\"")

Idempotency: re-executing the skill with identical inputs produces `commitSha=null` (silent no-op, RULE-007 `--dry-run`-like semantics on diff vazio). The contract is enforced by `x-planning-commit` itself — this step does not perform any additional diff check.

On `COMMIT_FAILED` (exit 4 from `x-planning-commit`), abort the workflow with the same error code so operators receive a single, unambiguous signal.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-create Phase-P4-Planning-Commit ok`

### Step P5 — Push to Origin (optional, EPIC-0049)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-create Phase-P5-Push`

If `--dry-run` is set, log `"dry-run, skipping push"` and skip.

When `x-internal-epic-branch-ensure` (Step P2) already pushed the branch to origin, the P4 commit is not yet on origin. Delegate the push to `x-git-push`:

    Skill(skill: "x-git-push", args: "--branch epic/<XXXX>")

On push failure (remote rejection, no connectivity), log a WARNING and continue — the local commit is preserved; the operator can re-run Step P5 or `git push` manually. Do NOT abort.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-create Phase-P5-Push ok`

## Error Handling

| Scenario | Action |
|----------|--------|
| Template file missing | Abort with message: "Template _TEMPLATE-EPIC.md not found" |
| Spec file missing or unparseable | Abort with message: "Specification file not found or invalid format" |
| Circular dependency in story graph | Warn, list the cycle, suggest merging conflicting stories |
| Story count too low (< 8 for complex spec) | Warn: "Possible over-bundling — review decomposition" |
| Jira MCP unavailable | Skip Jira integration silently, replace `<CHAVE-JIRA>` with `—` |
| Jira issue creation fails | Warn user, replace `<CHAVE-JIRA>` with failure message, continue |
| `x-internal-epic-branch-ensure` fails (Step P2) | Abort with `EPIC_BRANCH_ENSURE_FAILED`; canonical branch is required for versioning |
| `x-planning-commit` exit 4 (Step P4) | Abort with `COMMIT_FAILED`; file has already been written but not versioned |
| `x-planning-commit` exit 0 + `noOp=true` (Step P4) | Silent no-op — re-execution idempotency confirmed; continue to Step P5 |
| `x-git-push` fails (Step P5) | WARN only; local commit preserved; operator re-runs manually |
| `--dry-run` set | Steps P4 and P5 become no-ops with log line `"dry-run, skipping commit"` / `"dry-run, skipping push"` |

## Common Mistakes

- **Missing rules**: If a validation appears in 3+ journeys, it is cross-cutting — extract it
- **Rules too vague**: "Validar entidade" is useless. "Entidade deve estar ativa (status=ACTIVE) no cache L1/L2 -> DB. Se inativa, retornar 400 ENTITY_INACTIVE" is useful
- **Dependency gaps**: If Story B uses a table created by Story A, declare the dependency
- **Circular dependencies**: If A blocks B and B blocks A, they are probably one story
- **Story count too low**: A spec with 8 journeys typically generates 12-20 stories (infrastructure + journeys + cross-cutting). Fewer than 8 stories suggests over-bundling

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-epic-decompose | called-by | Orchestrator invokes x-epic-create in Phase B |
| x-story-create | calls | Story Creator reads the generated Epic file |
| x-epic-map | calls | Implementation Map reads the Epic's story index |
| x-jira-create-epic | calls | Creates Jira Epic from the generated file |
| x-git-worktree | calls (Step P1) | Detect-context (EPIC-0049 / RULE-001) |
| x-internal-epic-branch-ensure | calls (Step P2) | Ensure `epic/<ID>` exists locally + origin (EPIC-0049 / RULE-001) |
| x-planning-commit | calls (Step P4) | Batch-commit epic file without code pre-commit chain (EPIC-0049 / RULE-007) |
| x-git-push | calls (Step P5) | Push canonical epic branch to origin (optional) |
| story-planning | reads | Reads decomposition guide for layer identification |

## Knowledge Pack References

| Knowledge Pack | Usage |
|----------------|-------|
| story-planning | Decomposition philosophy, layer identification, dependency validation |

## Planning Status Propagation (Rule 22 / EPIC-0046)

> V2-gated: only runs when the newly-created epic declares `planningSchemaVersion: "2.0"` in its front matter / execution-state seed. For v1 epics (or epics with no version declaration): skip silently (Rule 19).

`x-epic-create` produces a fresh `plans/epic-XXXX/epic-XXXX.md` from a spec. The epic's initial lifecycle status is `Em Refinamento` (per Rule 22 — an epic starts in refinement, not `Pendente`). There is NO transition at creation time; the skill simply writes `**Status:** Em Refinamento` in the generated artifact.

**Steps (while materialising the epic file template):**

1. Ensure the epic template contains the literal line:

   ```markdown
   **Status:** Em Refinamento
   ```

   immediately after the `**ID:**` and `**Chave Jira:**` metadata block.

2. After writing the epic file, validate the Status line is parseable by the CLI (sanity check, NOT a transition):
   ```bash
   java -cp $CLAUDE_PROJECT_DIR/java/target/classes \
       dev.iadev.adapter.inbound.cli.StatusFieldParserCli \
       read plans/epic-XXXX/epic-XXXX.md
   ```
   Exit code 0 required. Exit 20 → abort skill (epic template is malformed).

3. No separate commit is needed — the Status line is part of the initial epic file, committed with the rest of the epic creation.

**Fail-loud:** validation read failure (exit 20) aborts the skill (RULE-046-08). Subsequent transitions out of `Em Refinamento` are owned by `x-epic-decompose` (to `Pendente` when decomposition completes) and `x-story-implement`/`x-epic-implement` (to `Em Andamento`, `Concluída`, etc.).
