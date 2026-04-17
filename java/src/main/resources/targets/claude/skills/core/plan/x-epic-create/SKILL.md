---
name: x-epic-create
description: "Generate an Epic document from a system specification file with cross-cutting business rules, global quality definitions (DoR/DoD), a complete story index with dependency declarations, and optional Jira integration."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion
argument-hint: "<SPEC_FILE> [--epic-id XXXX] [--jira <PROJECT_KEY>] [--no-jira]"
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

## Prerequisites

Read the following files before starting:

**Template (output structure):**
- `.claude/templates/_TEMPLATE-EPIC.md` — The exact structure to follow

**Decomposition philosophy (how to identify stories and rules):**
- `.claude/skills/x-epic-decompose/references/decomposition-guide.md`

If any template file is missing, stop and tell the user. The templates define the output structure
and must be read fresh from disk every time (never hardcode the structure).

## Workflow

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

## Error Handling

| Scenario | Action |
|----------|--------|
| Template file missing | Abort with message: "Template _TEMPLATE-EPIC.md not found" |
| Spec file missing or unparseable | Abort with message: "Specification file not found or invalid format" |
| Circular dependency in story graph | Warn, list the cycle, suggest merging conflicting stories |
| Story count too low (< 8 for complex spec) | Warn: "Possible over-bundling — review decomposition" |
| Jira MCP unavailable | Skip Jira integration silently, replace `<CHAVE-JIRA>` with `—` |
| Jira issue creation fails | Warn user, replace `<CHAVE-JIRA>` with failure message, continue |

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
| story-planning | reads | Reads decomposition guide for layer identification |

## Knowledge Pack References

| Knowledge Pack | Usage |
|----------------|-------|
| story-planning | Decomposition philosophy, layer identification, dependency validation |
