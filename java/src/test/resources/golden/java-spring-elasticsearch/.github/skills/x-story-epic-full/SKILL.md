---
name: x-story-epic-full
description: >
  Complete decomposition of a system specification into an Epic, individual Story files, and an
  Implementation Map with dependency graph and phased execution plan. This is the orchestrator
  skill that guides the full workflow: spec analysis, rule extraction, story identification,
  and implementation planning. Use this skill whenever the user asks to decompose a spec into
  stories and epic, break down a system document into implementable work items, generate a
  complete project backlog from a specification, create epic stories and implementation plan
  from a technical document, or any variation of "read this spec and create everything".
  Also trigger when the user wants the full decomposition pipeline — epic + stories + map —
  in a single pass, or mentions planning the complete implementation of a system from its
  specification. Prefer this skill over the individual x-story-epic, x-story-create, or
  x-story-map skills when the user wants all three deliverables.
---

# Complete Spec-to-Stories Decomposition

This skill orchestrates the full decomposition of a system specification into three
deliverables: an **Epic**, individual **Stories**, and an **Implementation Map**. It
coordinates the work of three focused skills, each handling one deliverable.

## The Three Deliverables

1. **Epic** — Scope, cross-cutting rules, quality gates, story index
2. **Stories** — One file per story with data contracts, Gherkin, diagrams, sub-tasks
3. **Implementation Map** — Phases, critical path, dependency graph, strategic analysis

## Prerequisites

Read these files before starting:

**Templates (output structure) — read all three:**
- `resources/templates/_TEMPLATE-EPIC.md`
- `resources/templates/_TEMPLATE-STORY.md`
- `resources/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`

**Decomposition philosophy:**
- `.github/skills/x-story-epic-full/SKILL.md`

If any template is missing, stop and tell the user.

## Decomposition Philosophy

Before generating anything, read the decomposition guide. It explains the
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

## Complete Workflow

### Phase A: Analysis

1. Ask the user which spec file to process (or use the one they provided)
2. Read the entire spec file
3. Read the decomposition guide
4. Analyze the spec:
   - Identify cross-cutting rules (rules that span multiple journeys)
   - Identify stories by layer (foundation -> core -> extensions -> compositions -> cross-cutting)
   - Map dependencies between stories
   - Compute phases from the dependency DAG
   - Identify the critical path

### Phase A.5: Jira Integration Decision

Before generating any artifacts, determine if Jira integration is desired.

#### A.5.1: Check MCP Availability

Verify that the Jira MCP tool (`mcp__atlassian__createJiraIssue`) is available.
If the tool is NOT available, set `jiraContext = { enabled: false }` and skip to
Phase B silently — do not warn the user.

#### A.5.2: Ask the User

Present the user with three options via a text prompt in chat:

```
Deseja criar o épico e as histórias no Jira?

1. Sim, criar tudo no Jira — Criar o épico e TODAS as histórias como issues no Jira automaticamente via MCP
2. Apenas o épico no Jira — Criar somente o épico no Jira. As histórias serão apenas markdown
3. Não, apenas markdown — Gerar apenas os arquivos markdown sem integração com Jira

Responda com o número da opção (1, 2 ou 3):
```

Wait for the user's response in the next chat turn.

#### A.5.3: Build jiraContext

Based on user selection:

- **Option 1 — "Sim, criar tudo no Jira"**:
  1. Ask for the Jira project key via text prompt:
     ```
     Qual a chave do projeto Jira? (ex: PROJ, MYAPP, TEAM)
     ```
  2. Discover the `cloudId` by calling `mcp__atlassian__getAccessibleAtlassianResources`.
     Use the first available site's `id` as the `cloudId`. If the call fails or returns
     no sites, warn the user and set `jiraContext = { enabled: false }`.
  3. Set `jiraContext = { enabled: true, cascadeToStories: true, projectKey: "<key>", cloudId: "<cloudId>" }`

- **Option 2 — "Apenas o épico no Jira"**:
  1. Ask for the Jira project key (same prompt as above)
  2. Discover the `cloudId` (same as above)
  3. Set `jiraContext = { enabled: true, cascadeToStories: false, projectKey: "<key>", cloudId: "<cloudId>" }`

- **Option 3 — "Não, apenas markdown"**:
  1. Set `jiraContext = { enabled: false }`

The `jiraContext` is passed to all subsequent phases and used to control Jira issue creation.

### Phase B: Generate the Epic

Follow the instructions in `.github/skills/x-story-epic/SKILL.md`:

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
   - `additional_fields`: `{ "labels": [{ "name": "generated-by-ia-dev-env" }] }`
2. Capture the returned Jira issue key (e.g., "PROJ-123")
3. Update `jiraContext.epicIssueKey` with the returned key
4. Replace `<CHAVE-JIRA>` in the generated Epic markdown with the actual Jira key
5. If creation fails: warn the user, set `<CHAVE-JIRA>` to `EPIC-XXXX (Jira: falha na criação)`,
   leave `jiraContext.epicIssueKey` absent (do NOT set it to an empty string or invalid value),
   and continue. In Phase C, stories will be created without a `parent` link when
   `jiraContext.epicIssueKey` is absent, maintaining non-blocking behavior

If `jiraContext.enabled == false`: replace `<CHAVE-JIRA>` with `—` in the Epic markdown.

### Phase C: Generate the Stories

Follow the instructions in `.github/skills/x-story-create/SKILL.md`:

For each story in the Epic's index:
- Dependencies (Blocked By / Blocks) — must be symmetric
- Applicable rules (reference Epic rules by ID)
- User story description + technical context
- Data contracts (precise: field names, types, formats, derivation rules)
- Mermaid sequence diagrams (real component names from the spec)
- Gherkin acceptance criteria with mandatory categories (degenerate case, happy path, error paths, boundary values) ordered by Transformation Priority Premise (simplest degenerate → complex edge cases)
- Sub-tasks tagged `[Dev]`, `[Test]`, `[Doc]`

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
   - `additional_fields`: `{ "labels": [{ "name": "generated-by-ia-dev-env" }] }`
2. Replace `<CHAVE-JIRA>` in the story markdown with the returned Jira key
3. If creation fails for a story: warn, set `<CHAVE-JIRA>` to `—`, continue with remaining stories

If `jiraContext.cascadeToStories == false` or `jiraContext.enabled == false`:
replace `<CHAVE-JIRA>` with `—` in all story markdowns.

No additional user prompting is needed — the cascade decision was already made in Phase A.5.

### Phase D: Generate the Implementation Map

Follow the instructions in `.github/skills/x-story-map/SKILL.md`:

- Dependency matrix (all stories, validated for consistency)
- Phase diagram (ASCII box-drawing)
- Critical path analysis
- Mermaid dependency graph (color-coded by phase)
- Phase summary and detail tables
- Strategic observations (bottleneck, leaves, parallelism, convergences, validation milestone)

Generate `plans/epic-XXXX/implementation-map-XXXX.md` following `_TEMPLATE-IMPLEMENTATION-MAP.md`.

If Jira keys are available (from Phase C), include them in the dependency matrix's
`Chave Jira` column.

### Phase D.5: Jira Dependency Linking (if applicable)

If `jiraContext.enabled == true` and `jiraContext.cascadeToStories == true`, and stories
have Jira keys:

1. For each story, read its "Blocked By" list
2. For each blocker that has a Jira key, call `mcp__atlassian__createIssueLink`:
   - `cloudId`: `jiraContext.cloudId`
   - `type`: "Blocks"
   - `inwardIssue`: the blocker's Jira key (the issue that blocks)
   - `outwardIssue`: the current story's Jira key (the issue that is blocked)
3. Report: "N dependency links criados no Jira"

If linking fails for some stories, log warnings but do not fail the pipeline.
This step is best-effort — Jira links are a convenience, not a hard requirement.

### Phase E: Save and Report

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

## Language Rules

- All generated content must be in **Brazilian Portuguese (pt-BR)**
- Technical terms in English stay in English (cache, timeout, handler, endpoint, etc.)
- Code identifiers, field names, enum values stay in English
- Gherkin in Portuguese: `Cenario`, `DADO`, `QUANDO`, `ENTÃO`, `E`, `MAS`
- IDs: RULE-NNN (English format), story-XXXX-YYYY (composite), epic-XXXX (kebab-case)

## Quality Checklist

Before delivering, verify:

- [ ] Every rule in the Epic is referenced by at least one story
- [ ] Every story references at least one rule (except infrastructure stories)
- [ ] Dependencies are symmetric (A blocks B <-> B blocked by A)
- [ ] No circular dependencies
- [ ] Phase computation is correct (stories only enter phase when ALL deps are in earlier phases)
- [ ] Critical path is the actual longest chain (not just the deepest phase)
- [ ] Data contracts match the spec exactly (field names, types, formats)
- [ ] Each story has at least 4 Gherkin scenarios covering all mandatory categories (degenerate, happy path, error paths, boundary values), ordered by TPP (degenerate → edge cases)
- [ ] Epic DoD includes TDD Compliance and Double-Loop TDD
- [ ] Boundary values use triplet pattern (at-min, at-max, past-max)
- [ ] Implementation map observations are specific, not generic
- [ ] All files follow their respective templates exactly

## Detailed References

For in-depth guidance, see:
- `.github/skills/x-story-epic-full/SKILL.md`
- `.github/skills/x-story-epic-full/SKILL.md`
