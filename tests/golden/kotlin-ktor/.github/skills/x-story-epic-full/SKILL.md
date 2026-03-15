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

### Phase B: Generate the Epic

Follow the instructions in `.github/skills/x-story-epic/SKILL.md`:

- Determine the epic number (scan `docs/stories/` for existing `epic-XXXX` folders, use next available; default `0001`)
- Create directory `docs/stories/epic-XXXX/`
- Extract rules → RULE-001..N table
- Build story index with titles and dependencies (using `story-XXXX-YYYY` IDs)
- Define DoR/DoD from spec quality requirements
- Generate `docs/stories/epic-XXXX/epic-XXXX.md` following `_TEMPLATE-EPIC.md`

### Phase C: Generate the Stories

Follow the instructions in `.github/skills/x-story-create/SKILL.md`:

For each story in the Epic's index:
- Dependencies (Blocked By / Blocks) — must be symmetric
- Applicable rules (reference Epic rules by ID)
- User story description + technical context
- Data contracts (precise: field names, types, formats, derivation rules)
- Mermaid sequence diagrams (real component names from the spec)
- Gherkin acceptance criteria (happy path + errors + edge cases)
- Sub-tasks tagged `[Dev]`, `[Test]`, `[Doc]`

Generate files as `docs/stories/epic-XXXX/story-XXXX-YYYY.md` following `_TEMPLATE-STORY.md`.

### Phase D: Generate the Implementation Map

Follow the instructions in `.github/skills/x-story-map/SKILL.md`:

- Dependency matrix (all stories, validated for consistency)
- Phase diagram (ASCII box-drawing)
- Critical path analysis
- Mermaid dependency graph (color-coded by phase)
- Phase summary and detail tables
- Strategic observations (bottleneck, leaves, parallelism, convergences, validation milestone)

Generate `docs/stories/epic-XXXX/implementation-map-XXXX.md` following `_TEMPLATE-IMPLEMENTATION-MAP.md`.

### Phase E: Save and Report

All files are saved inside `docs/stories/epic-XXXX/` (the epic's dedicated folder).

Report summary:
- Total rules extracted
- Total stories generated
- Total phases computed
- Critical path length (phases and stories)
- Maximum parallelism (largest phase)
- Main bottleneck (story blocking the most others)

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
- [ ] Each story has at least 4 Gherkin scenarios (happy + 2 errors + 1 edge case)
- [ ] Implementation map observations are specific, not generic
- [ ] All files follow their respective templates exactly

## Detailed References

For in-depth guidance, see:
- `.github/skills/x-story-epic-full/SKILL.md`
- `.github/skills/x-story-epic-full/SKILL.md`
