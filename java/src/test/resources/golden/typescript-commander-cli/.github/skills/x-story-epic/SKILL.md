---
name: x-story-epic
description: >
  Generate an Epic document from a system specification file. This skill reads a technical spec
  and produces an Epic file with cross-cutting business rules, global quality definitions
  (DoR/DoD), and a complete story index with dependency declarations. Use this skill whenever
  the user asks to create an epic, generate an epic from a spec, extract business rules from
  a system document, decompose a specification into an epic, build a story index, or any
  variation of "read this spec and create an epic". Also trigger when the user mentions
  extracting cross-cutting rules, defining quality gates for a project, or building a story
  backlog from a technical document — even if they don't use the word "epic" explicitly.
---

# Create Epic from System Specification

This skill reads a system specification document and generates an Epic file — the top-level
artifact that defines the scope, cross-cutting rules, quality criteria, and story index for
a development effort.

## Why This Matters

The Epic is the single source of truth for a decomposition. It captures rules that span multiple
stories (so they don't get duplicated or contradicted), defines quality gates that every story
must meet, and provides the complete story index with dependency relationships. Getting the Epic
right makes story generation and implementation planning straightforward.

## Prerequisites

Read the following files before starting:

**Template (output structure):**
- `resources/templates/_TEMPLATE-EPIC.md` — The exact structure to follow

**Decomposition philosophy (how to identify stories and rules):**
- `.github/skills/x-story-epic-full/SKILL.md`

If any template file is missing, stop and tell the user. The templates define the output structure
and must be read fresh from disk every time (never hardcode the structure).

## Workflow

### Step 1: Read the Input Spec

Read the entire system specification file provided by the user. This file follows the `_TEMPLATE.md`
format with sections like Overview, Business Rules, Platform Specs, Data Contracts, Journeys,
Sync Journeys, Dependencies, and Interfaces.

Understand the full scope before starting extraction.

### Step 2: Extract Cross-Cutting Business Rules

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

### Step 3: Identify Stories

Read the decomposition guide (`.github/skills/x-story-epic-full/SKILL.md`) for
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

### Step 4: Define Quality Criteria

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

### Step 5: Generate the Epic File

Write the Epic following the `_TEMPLATE-EPIC.md` structure exactly:

1. **Header**: Title, author, date, version, status
2. **Section 1 — Overview**: Scope derived from the spec's Overview section
3. **Section 2 — Attachments and References**: Links to the input spec and related documents
4. **Section 3 — Global Quality Definitions**: DoR and DoD from Step 4
5. **Section 4 — Cross-Cutting Business Rules**: Rules table from Step 2
6. **Section 5 — Story Index**: Story index from Step 3, with links and dependencies

**Directory and file naming** (mandatory — see SD-09 in decomposition guide):
1. Determine the epic number: scan `plans/` for existing `epic-XXXX` folders and use the next available number (default `0001` if none exist). Ask the user if unsure.
2. Create the directory `plans/epic-XXXX/`
3. Save the Epic file as `plans/epic-XXXX/epic-XXXX.md`
4. Story IDs in the index use composite format: `story-XXXX-YYYY` (where XXXX = epic number, YYYY = story sequence)
5. Story links in the index point to `./story-XXXX-YYYY.md` (relative to the epic folder)

### Step 5.5: Optional Jira Integration

After generating the Epic file content but before the final save, optionally create the
Epic in Jira.

#### 5.5.1: Check MCP Availability

Verify that the Jira MCP tool (`mcp__atlassian__createJiraIssue`) is available.
If not available, skip this entire step silently and proceed to Step 6.

#### 5.5.2: Check Context

If this skill was invoked by the orchestrator (`x-story-epic-full`) and a `jiraContext`
was already provided, use that context directly (skip the user prompt — it was already
asked in Phase A.5). If `jiraContext.enabled == true`, proceed to 5.5.4. If `false`, skip.

If invoked standalone (no `jiraContext`), proceed to 5.5.3.

#### 5.5.3: Ask the User (standalone invocation only)

Use the `AskUserQuestion` tool:

```
question: "Deseja criar este épico no Jira?"
header: "Jira"
options:
  - label: "Sim, criar no Jira"
    description: "Criar o épico como issue no Jira via MCP e preencher a Chave Jira no markdown"
  - label: "Não, apenas markdown"
    description: "Gerar apenas o arquivo markdown sem integração com Jira"
multiSelect: false
```

If "Sim":
1. Ask for the Jira project key:
   ```
   Qual a chave do projeto Jira? (ex: PROJ, MYAPP, TEAM)
   ```
2. Discover the `cloudId` by calling `mcp__atlassian__getAccessibleAtlassianResources`.
   Use the first available site's `id` as the `cloudId`. If the call fails or returns
   no sites, warn the user and skip to Step 6 (replace `<CHAVE-JIRA>` with `—`).

If "Não": replace `<CHAVE-JIRA>` with `—` and proceed to Step 6.

#### 5.5.4: Create Epic in Jira

Call `mcp__atlassian__createJiraIssue` to create an Epic issue:
- `cloudId`: the discovered `cloudId` (or `jiraContext.cloudId`)
- `projectKey`: the user-provided project key (or `jiraContext.projectKey`)
- `issueTypeName`: "Epic"
- `summary`: the Epic title from the generated header
- `description`: the "Visão Geral" section text
- `contentFormat`: "markdown"
- `additional_fields`: `{ "labels": [{ "name": "generated-by-ia-dev-env" }] }`

Capture the returned Jira issue key (e.g., "PROJ-123").

#### 5.5.5: Update Markdown

Replace `<CHAVE-JIRA>` in the generated Epic markdown with the actual Jira key.
Report: "Épico criado no Jira: PROJ-123"

#### 5.5.6: Error Handling

If the Jira MCP tool call fails:
1. Warn the user with the error message
2. Replace `<CHAVE-JIRA>` with `EPIC-XXXX (Jira: falha na criação)`
3. Continue to Step 6 — NEVER block Epic file generation due to Jira failures

### Step 6: Save and Report

Save the file to `plans/epic-XXXX/epic-XXXX.md`.
Report: number of rules extracted, number of stories identified, dependency structure summary.

## Language Rules

- All generated content must be in **Brazilian Portuguese (pt-BR)**
- Technical terms that are industry-standard in English stay in English (cache, timeout, circuit breaker, handler, endpoint, state machine)
- Code identifiers, field names, enum values stay in English
- Rule IDs use the format RULE-NNN (English)
- Story IDs use composite format: `story-XXXX-YYYY` (epic number + story sequence)
- Epic IDs use kebab-case format: `epic-XXXX`

## Common Mistakes

- **Missing rules**: If a validation appears in 3+ journeys, it's cross-cutting — extract it
- **Rules too vague**: "Validate entity" is useless. "Entity must be active (status=ACTIVE) in L1/L2 cache → DB. If inactive, return 400 ENTITY_INACTIVE" is useful
- **Dependency gaps**: If Story B uses a table created by Story A, declare the dependency
- **Circular dependencies**: If A blocks B and B blocks A, they're probably one story
- **Story count too low**: A spec with 8 journeys typically generates 12-20 stories (infrastructure + journeys + cross-cutting). If you have fewer than 8 stories, you're probably bundling too much

## Detailed References

For in-depth guidance, see:
- `.github/skills/x-story-epic/SKILL.md`
- `.github/skills/x-story-epic-full/SKILL.md`
