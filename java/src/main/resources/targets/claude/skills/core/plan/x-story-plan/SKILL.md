---
name: x-story-plan
description: "Multi-agent story planning: launches 5 specialized agents (Architect, QA, Security, Tech Lead, Product Owner) in parallel to produce a consolidated task breakdown, individual task plans, planning report, and DoR validation. Schema-aware: v1 (legacy) runs the original 6-phase flow; v2 (task-first, EPIC-0038) adds Phases 4a-4c that emit task-TASK-NNN.md + plan-task-TASK-NNN.md per task and a task-implementation-map-STORY-*.md, wiring every task through x-task-plan in parallel."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[STORY-ID] [--force] [--skip-dor]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Multi-Agent Story Planning (Orchestrator)

## Purpose

Orchestrate multi-agent story planning by launching 5 specialized agents in parallel to analyze a story from different perspectives (architecture, testing, security, quality, product). Each agent produces TASK_PROPOSAL entries. The orchestrator then consolidates proposals using deterministic merge rules, generates planning artifacts, and validates Definition of Ready (DoR).

## When to Use

- `/x-story-plan STORY-ID` -- plan a story with 5 parallel agents
- `/x-story-plan STORY-ID --force` -- regenerate even if artifacts are fresh
- `/x-story-plan STORY-ID --skip-dor` -- skip DoR validation phase

## CRITICAL EXECUTION RULE

**6 phases (0-5). ALL mandatory (unless `--skip-dor` skips Phase 5). NEVER stop before the final phase.**

After each phase 0-4: `>>> Phase N/5 completed. Proceeding to Phase N+1...`
After Phase 5: `>>> Phase 5/5 completed. Story planning complete.`

## Workflow Overview

```
Phase 0: INPUT RESOLUTION       -> Parse argument, resolve paths, staleness check (inline)
Phase 0b: SCHEMA VERSION DETECT -> Read execution-state.json; select v1 or v2 flow (inline)
Phase 1: CONTEXT GATHERING      -> Read story, epic, implementation map (inline)
Phase 2: PARALLEL PLANNING      -> Launch 5 subagents in SINGLE message (parallel)
Phase 3: CONSOLIDATION          -> Merge TASK_PROPOSAL entries with deterministic rules (inline)
Phase 4: ARTIFACT GENERATION    -> Write tasks-story-*.md + planning report (inline)
Phase 4a: TASK BREAKDOWN (v2)   -> Emit task-TASK-NNN.md per atomic task with I/O contracts
Phase 4b: PARALLEL TASK PLANS (v2) -> Invoke x-task-plan per task in parallel (batch=4)
Phase 4c: TASK MAP (v2)         -> Generate task-implementation-map-STORY-*.md (topological sort)
Phase 5: DOR VALIDATION         -> Run 12 checks + (v2 only) per-task READY checks
```

> **v1 vs v2 gating.** Phases 4a-4c execute ONLY when Phase 0b resolves to
> `planningSchemaVersion == "2.0"`. For v1 (legacy, including EPIC-0038 itself during
> its own execution per the bootstrap rule in spec §8.2), the workflow ends after
> Phase 4 and Phase 5 runs the legacy 12-check DoR without per-task extensions.

## Input Parsing

### Positional Argument (Required)

| Argument | Format | Required | Description |
|----------|--------|----------|-------------|
| `STORY-ID` | `story-XXXX-YYYY` | **Mandatory** | Story identifier (epic XXXX, story sequence YYYY) |

If missing, abort: `ERROR: Story ID is required. Usage: /x-story-plan [STORY-ID] [--force] [--skip-dor]`

### Optional Flags

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `--force` | boolean | `false` | Regenerate all artifacts even if fresh |
| `--skip-dor` | boolean | `false` | Skip Phase 5 (DoR validation) |

---

## Phase 0 -- Input Resolution (Orchestrator -- Inline)

### 0.1 Parse Story Argument

Extract epic ID (XXXX) and story sequence (YYYY) from the story ID argument.

- Input: `story-0028-0002` -> epic ID = `0028`, story sequence = `0002`
- If the argument does not match `story-XXXX-YYYY` format, abort with format error.

### 0.2 Resolve Epic Directory

Resolve the actual epic directory before computing any other paths:

1. Extract the epic ID from the story ID (`story-XXXX-YYYY` -> `epic-XXXX`)
2. Resolve `EPIC_DIR` with a glob that supports both the exact directory and suffix variants:
   - Exact match: `plans/epic-XXXX`
   - Suffix variant: `plans/epic-XXXX-*`
3. If exactly one directory matches, use that as `EPIC_DIR`
4. If both exact and suffix matches exist, prefer the exact match `plans/epic-XXXX`
5. If no directory matches, stop and report that the epic directory could not be resolved

### 0.3 Resolve Paths

Compute all required paths relative to `<EPIC_DIR>`:

| Path | Pattern | Example |
|------|---------|---------|
| Story file | `<EPIC_DIR>/story-XXXX-YYYY.md` | `plans/epic-0028/story-0028-0002.md` |
| Epic file | `<EPIC_DIR>/epic-XXXX.md` | `plans/epic-0028/epic-0028.md` |
| Implementation map | Resolve first existing match in `<EPIC_DIR>/` from: `IMPLEMENTATION-MAP.md`, `implementation-map-XXXX.md` | `plans/epic-0028/IMPLEMENTATION-MAP.md` or `plans/epic-0028/implementation-map-0028.md` |
| Output directory | `<EPIC_DIR>/plans/` | `plans/epic-0028/plans/` |
| Tasks file | `<EPIC_DIR>/plans/tasks-story-XXXX-YYYY.md` | `plans/epic-0028/plans/tasks-story-0028-0002.md` |
| Planning report | `<EPIC_DIR>/plans/planning-report-story-XXXX-YYYY.md` | `plans/epic-0028/plans/planning-report-story-0028-0002.md` |
| DoR checklist | `<EPIC_DIR>/plans/dor-story-XXXX-YYYY.md` | `plans/epic-0028/plans/dor-story-0028-0002.md` |

For the implementation map, check both supported naming conventions above and use the first file that exists. If neither exists, continue without implementation map context (logged as warning in Phase 1).

Ensure output directory exists: `mkdir -p <EPIC_DIR>/plans`

### 0.4 Staleness Check (RULE-002 -- Idempotency via Staleness Check)

Unless `--force` is passed, check if planning artifacts already exist and are fresh:

1. Check if the tasks file exists at `<EPIC_DIR>/plans/tasks-story-XXXX-YYYY.md`
2. If it does NOT exist, proceed to Phase 1 (first generation).
3. If it exists, compare modification times:

| Condition | Action | Log Message |
|-----------|--------|-------------|
| Tasks file does not exist | Generate new | `"Generating story plan for {story-id}"` |
| `mtime(story file) > mtime(tasks file)` | Regenerate (stale) | `"Regenerating stale story plan for {story-id}"` |
| `mtime(story file) <= mtime(tasks file)` | Reuse existing | `"Reusing existing story plan from {date}"` |
| `--force` flag provided | Regenerate always | `"Force-regenerating story plan for {story-id}"` |

If reusing: read existing artifacts and skip directly to Phase 5 (DoR validation only). **Do NOT invoke any subagent.**

### 0.5 Verify Story File Exists

```bash
test -f plans/epic-XXXX/story-XXXX-YYYY.md && echo "FOUND" || echo "NOT_FOUND"
```

If NOT_FOUND, abort: `ERROR: Story file not found at plans/epic-XXXX/story-XXXX-YYYY.md`

---

## Phase 1 -- Context Gathering (Orchestrator -- Inline)

Read all source materials needed for agent prompts.

### 1.1 Read Story File

Read the story file and extract:

- **Title and description**
- **Acceptance criteria** (Gherkin scenarios)
- **Data contracts** (request/response types, fields, validation rules)
- **Dependencies** (predecessor stories)
- **Sub-tasks** (Section 8 if present)
- **Technical description** (implementation hints)
- **Non-functional requirements**

### 1.2 Read Epic File (if exists)

Read the epic file for cross-cutting context:

- **Business rules** table
- **Quality definitions** (DoR/DoD)
- **Story index** with dependency declarations
- **Architecture constraints**

If epic file does not exist, log warning and continue: `"WARNING: Epic file not found, planning without epic context"`

### 1.3 Read Implementation Map (if exists)

Read the implementation map for:

- **Phase assignments** (which phase this story belongs to)
- **Dependency graph** (stories that depend on this one)
- **Critical path** information

If implementation map does not exist, log warning and continue: `"WARNING: Implementation map not found, planning without dependency context"`

### 1.4 Read Existing Plans (if any)

Check for existing architecture and test plans that may have been generated by other skills:

| Plan | Path | Action if found |
|------|------|-----------------|
| Architecture plan | `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` | Include in agent context |
| Test plan | `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | Include in agent context |
| Implementation plan | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | Include in agent context |
| Security assessment | `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md` | Include in agent context |

These are optional context -- agents produce their own proposals regardless.

---

## Phase 2 -- Parallel Planning (Subagents via Task Tool -- SINGLE Message)

**CRITICAL: ALL 5 subagents MUST be launched in a SINGLE message for true parallelism.**

Each subagent reads relevant knowledge packs, analyzes the story, and produces TASK_PROPOSAL entries in the standardized format.

### Template Detection (before dispatching subagents)

Before launching subagents, check if planning templates exist:

```bash
test -f .claude/templates/_TEMPLATE-TASK-BREAKDOWN.md && echo "TB_AVAILABLE" || echo "TB_MISSING"
test -f .claude/templates/_TEMPLATE-TASK-PLAN.md && echo "TP_AVAILABLE" || echo "TP_MISSING"
test -f .claude/templates/_TEMPLATE-STORY-PLANNING-REPORT.md && echo "SPR_AVAILABLE" || echo "SPR_MISSING"
test -f .claude/templates/_TEMPLATE-DOR-CHECKLIST.md && echo "DOR_AVAILABLE" || echo "DOR_MISSING"
```

Log any missing templates with warning: `"WARNING: Template {name} not found, using inline format"`

### TASK_PROPOSAL Format

Each subagent MUST return proposals in this format (one per task):

```
TASK_PROPOSAL:
  source: {AGENT_NAME}
  id: {AGENT_PREFIX}-NNN
  type: {architecture|implementation|test|security|quality-gate|validation}
  description: {what this task accomplishes}
  layer: {domain|application|adapter.inbound|adapter.outbound|config|cross-cutting}
  components: [{list of affected components}]
  tdd_phase: {RED|GREEN|REFACTOR|VERIFY|N/A}
  tpp_level: {nil|constant|scalar|collection|conditional|iteration|N/A}
  dod_criteria: [{list of Definition of Done items}]
  dependencies: [{list of task IDs this depends on, or empty}]
  estimated_effort: {XS|S|M|L|XL}
END_PROPOSAL
```

### Subagent 1: Architect (model hint: opus)

Launch `general-purpose` subagent:

> You are a **Senior Architect** planning the implementation of a story for {{PROJECT_NAME}}.
>
> **Step 1 -- Read Knowledge Packs:**
> - Read `skills/architecture/references/architecture-principles.md` -- layer structure, dependency direction, package conventions
> - Read `skills/layer-templates/SKILL.md` -- code templates per architecture layer
> - Read `skills/coding-standards/references/coding-conventions.md` -- {{LANGUAGE}} conventions
> - Read any relevant ADRs in `adr/` directory
>
> **Step 2 -- Read Story Context:**
> {STORY_CONTENT}
>
> {EPIC_CONTEXT_IF_AVAILABLE}
>
> {EXISTING_ARCHITECTURE_PLAN_IF_AVAILABLE}
>
> **Step 3 -- Produce Architecture Plan:**
> Analyze the story and produce:
> 1. Affected layers and components (domain, application, adapter.inbound, adapter.outbound, config)
> 2. New classes/interfaces to create (with package locations)
> 3. Existing classes to modify
> 4. Dependency direction validation (inner layers NEVER depend on outer)
> 5. Integration points with other components
> 6. Class diagram (Mermaid classDiagram)
> 7. Implementation order (domain first, adapters last)
>
> **Step 4 -- Produce Implementation Plan:**
> For each component identified, describe:
> 1. Method signatures per new class
> 2. Constructor dependencies (injection points)
> 3. Port interfaces needed
> 4. Database/API/event changes (if applicable)
> 5. Configuration changes
>
> **Step 5 -- Generate TASK_PROPOSAL Entries:**
> Create TASK_PROPOSAL entries with prefix `ARCH-NNN`:
> - One task per component/class to create or modify
> - Order by layer (domain first, adapters last)
> - Set `tdd_phase` to `GREEN` for implementation tasks
> - Set `layer` to the appropriate architecture layer
> - Include specific DoD criteria per task
>
> **Output:** Return the architecture plan summary followed by all TASK_PROPOSAL entries in the standardized format.

### Subagent 2: QA Engineer (model hint: opus)

Launch `general-purpose` subagent:

> You are a **QA Engineer** planning tests for a story in {{PROJECT_NAME}}.
>
> **Step 1 -- Read Knowledge Packs:**
> - Read `skills/testing/references/testing-philosophy.md` -- 8 test categories, fixture patterns, data uniqueness
> - Read `skills/testing/references/testing-conventions.md` -- {{LANGUAGE}}-specific test frameworks, naming conventions
> - Read `skills/architecture/references/architecture-principles.md` -- layer boundaries for unit vs integration scope
>
> **Step 2 -- Read Story Context:**
> {STORY_CONTENT}
>
> {EXISTING_TEST_PLAN_IF_AVAILABLE}
>
> **Step 3 -- Plan Double-Loop TDD Tests:**
> Design test scenarios following Transformation Priority Premise (TPP) order:
>
> **Outer Loop -- Acceptance Tests (AT-N):**
> For each Gherkin scenario in the story, generate an acceptance test entry.
> Acceptance tests stay RED until all related unit tests complete.
>
> **Inner Loop -- Unit Tests (UT-N, TPP Order):**
> Generate unit test scenarios in strict TPP order:
> - TPP Level 1 (nil): Degenerate cases -- null, empty, zero
> - TPP Level 2 (constant): Single valid input, direct output
> - TPP Level 3 (scalar): Simple conditions, single if/else
> - TPP Level 4 (collection): Iterations, map/filter/reduce
> - TPP Level 5 (conditional): Multiple branches, compound boolean
> - TPP Level 6 (iteration): Edge cases, boundary values
>
> **Integration Tests (IT-N):**
> Cross-component tests positioned after related UTs.
>
> **Step 4 -- Generate TASK_PROPOSAL Entries:**
> Create TASK_PROPOSAL entries with prefix `QA-NNN`:
> - Each test scenario becomes a RED task (write test) + GREEN task (make it pass)
> - Order by TPP level (degenerate first, edge cases last)
> - Set `tdd_phase` to `RED` for test-writing tasks, `GREEN` for implementation tasks
> - Set `tpp_level` to the appropriate TPP level
> - Pair every RED task with its GREEN counterpart
> - Include specific DoD criteria (test naming convention, assertion specificity, coverage target)
>
> **Output:** Return the test plan summary followed by all TASK_PROPOSAL entries in the standardized format.

### Subagent 3: Security Engineer (adaptive model)

Launch `general-purpose` subagent:

> You are a **Security Engineer** assessing security impact for a story in {{PROJECT_NAME}}.
>
> **Step 1 -- Read Knowledge Packs:**
> - Read `skills/security/references/application-security.md` -- OWASP Top 10, security headers, dependency security
> - Read `skills/security/references/security-principles.md` -- data classification, input validation, secure error handling
> - If compliance is enabled, read the project's active compliance reference under `skills/compliance/references/` (e.g., `gdpr.md`, `lgpd.md`, `pci-dss.md`)
>
> **Step 2 -- Read Story Context:**
> {STORY_CONTENT}
>
> {EXISTING_SECURITY_ASSESSMENT_IF_AVAILABLE}
>
> **Step 3 -- Produce Security Assessment:**
> Analyze the story for security concerns:
> 1. OWASP Top 10 mapping (which categories apply)
> 2. Input validation requirements (all external inputs)
> 3. Authentication/authorization checks needed
> 4. Data protection requirements (PII, sensitive data)
> 5. Secrets management (any new credentials/keys)
> 6. Error handling (no internal details exposed)
> 7. Dependency security (new libraries to vet)
>
> **Step 4 -- Generate TASK_PROPOSAL Entries:**
> Create TASK_PROPOSAL entries with prefix `SEC-NNN`:
> - One task per security control to implement or verify
> - Set `type` to `security`
> - Set `tdd_phase` to `RED` for security test tasks, `GREEN` for security implementation tasks
> - Include specific DoD criteria referencing OWASP categories
> - Mark dependencies on implementation tasks they augment
>
> **Output:** Return the security assessment summary followed by all TASK_PROPOSAL entries in the standardized format.

### Subagent 4: Tech Lead (adaptive model)

Launch `general-purpose` subagent:

> You are a **Tech Lead** defining quality gates for a story in {{PROJECT_NAME}}.
>
> **Step 1 -- Read Knowledge Packs:**
> - Read `skills/coding-standards/references/coding-conventions.md` -- Clean Code rules, SOLID
> - Read `skills/architecture/references/architecture-principles.md` -- layer boundaries, dependency rules
> - Read `skills/testing/references/testing-philosophy.md` -- TDD workflow, coverage thresholds
>
> **Step 2 -- Read Story Context:**
> {STORY_CONTENT}
>
> {EXISTING_IMPLEMENTATION_PLAN_IF_AVAILABLE}
>
> **Step 3 -- Define Quality Gates:**
> Identify quality gate tasks:
> 1. Architecture compliance checks (dependency direction, layer violations)
> 2. Code quality gates (method length <= 25 lines, class length <= 250 lines)
> 3. Test quality gates (coverage >= 95% line, >= 90% branch)
> 4. Cross-file consistency checks (uniform patterns within module)
> 5. TDD compliance verification (test-first in git history)
> 6. Refactoring verification (explicit refactoring after green)
> 7. Documentation completeness (API docs, changelog)
>
> **Step 4 -- Resolve Implementation Approach:**
> When multiple approaches exist for implementing a component:
> - Choose the approach that best aligns with existing codebase patterns
> - Prefer simpler solutions unless complexity is justified by requirements
> - Document the decision rationale in the task DoD
>
> **Step 5 -- Generate TASK_PROPOSAL Entries:**
> Create TASK_PROPOSAL entries with prefix `TL-NNN`:
> - One task per quality gate checkpoint
> - Set `type` to `quality-gate`
> - Set `tdd_phase` to `VERIFY`
> - Include specific DoD criteria with measurable thresholds
> - Mark dependencies on the tasks they verify
>
> **Output:** Return the quality gate summary followed by all TASK_PROPOSAL entries in the standardized format.

### Subagent 5: Product Owner (model hint: sonnet)

Launch `general-purpose` subagent:

> You are a **Product Owner** validating story completeness for {{PROJECT_NAME}}.
>
> **Step 1 -- Read Story Context:**
> {STORY_CONTENT}
>
> {EPIC_CONTEXT_IF_AVAILABLE}
>
> **Step 2 -- Validate Acceptance Criteria:**
> Review the story's acceptance criteria for:
> 1. Completeness -- all user-visible behaviors covered
> 2. Testability -- each criterion maps to a verifiable test
> 3. Clarity -- no ambiguous terms or undefined behaviors
> 4. Edge cases -- error scenarios, boundary conditions
> 5. User experience -- success and failure paths
>
> **Step 3 -- Amend Acceptance Criteria:**
> If gaps are found:
> - Add missing Gherkin scenarios
> - Clarify ambiguous criteria
> - Add error handling scenarios
> - Add boundary condition scenarios
>
> **Step 4 -- Generate TASK_PROPOSAL Entries:**
> Create TASK_PROPOSAL entries with prefix `PO-NNN`:
> - One task per acceptance criteria validation
> - Set `type` to `validation`
> - Set `tdd_phase` to `VERIFY`
> - Include DoD criteria that reference specific Gherkin scenarios
> - PO tasks typically depend on implementation tasks
>
> **Output:** Return the acceptance criteria review followed by all TASK_PROPOSAL entries in the standardized format.

---

## Phase 3 -- Consolidation (Orchestrator -- Inline)

Merge TASK_PROPOSAL entries from all 5 agents using deterministic rules.

### 3.1 Collect All Proposals

Parse output from each subagent and extract all TASK_PROPOSAL entries. Group by source agent:

| Agent | Prefix | Expected Proposal Types |
|-------|--------|------------------------|
| Architect | ARCH-NNN | architecture, implementation |
| QA Engineer | QA-NNN | test (RED/GREEN pairs) |
| Security Engineer | SEC-NNN | security |
| Tech Lead | TL-NNN | quality-gate |
| Product Owner | PO-NNN | validation |

### 3.2 Apply Consolidation Rules

Process proposals in this order:

#### Rule 1: MERGE -- Union of DoD Criteria

When two or more tasks from different agents target the same component AND the same layer:

- Merge into a single task
- Union all DoD criteria from both tasks
- Keep the more specific description
- Retain dependencies from both tasks
- Source becomes `merged({agent1},{agent2})`

Example: ARCH-003 (create UserValidator) + SEC-002 (input validation for UserValidator) -> merged task with combined DoD.

#### Rule 2: AUGMENT -- Security Criteria Injection

For every implementation task (from Architect) that touches a security-sensitive component:

- Components handling user input, authentication, authorization, file I/O, network calls, or database queries
- Add the relevant SEC-NNN DoD criteria to the implementation task
- Do NOT remove the original SEC task -- retain it as a security verification task for the final plan

Representation of retained SEC tasks after augmentation:

- Keep `type: security`
- Set `tdd_phase: VERIFY`
- Do NOT represent the retained SEC task as `RED` or `GREEN` in the final consolidated task list
- The retained SEC task verifies the augmented implementation task and MUST depend on that implementation task
- Order retained SEC verification tasks after the implementation task they verify

Security-sensitive detection keywords: `input`, `auth`, `password`, `token`, `file`, `path`, `query`, `sql`, `http`, `request`, `session`, `cookie`, `encrypt`, `decrypt`, `hash`, `secret`.

#### Rule 3: PAIR -- RED before GREEN

For every QA-NNN GREEN task (implementation to make test pass):

- Ensure a corresponding QA-NNN RED task (write the failing test) exists
- The RED task MUST appear before the GREEN task in the final ordering
- If a GREEN task has no RED counterpart, create a synthetic RED task
- Set dependency: GREEN depends on RED

#### Rule 4: Tech Lead Wins Conflicts

When the Tech Lead (TL) and Architect (ARCH) propose conflicting implementation approaches for the same component:

- The Tech Lead's approach takes precedence
- The Architect's approach is recorded as "considered alternative" in the task notes
- Rationale: Tech Lead has final authority on implementation standards

#### Rule 5: PO Amends Acceptance Criteria

When the Product Owner identifies missing or unclear acceptance criteria:

- PO amendments are applied to the story's acceptance criteria section
- PO validation tasks (PO-NNN) are added AFTER implementation tasks
- PO tasks verify that the amended criteria are met

### 3.3 Assign Final Task IDs

After consolidation, assign sequential TASK-NNN IDs:

1. Order tasks by execution phase:
   - RED test tasks first (TPP Level 1 through 6)
   - GREEN implementation tasks (paired with their RED tasks)
   - REFACTOR tasks
   - Security verification tasks
   - Quality gate tasks
   - Validation tasks
2. Within each phase, order by layer (domain -> application -> adapter -> config)
3. Within each layer, order by TPP level (nil -> constant -> scalar -> collection -> conditional -> iteration)
4. Assign TASK-001, TASK-002, ... sequentially

### 3.4 Build Dependency Graph

Construct the task dependency DAG:

1. Preserve all explicit dependencies from agent proposals
2. Add implicit dependencies:
   - RED -> GREEN (same test scenario)
   - GREEN -> REFACTOR (same component)
   - Domain layer tasks before application layer tasks
   - Application layer tasks before adapter layer tasks
3. Validate: no circular dependencies (abort with error if detected)
4. Generate Mermaid dependency graph

---

## Phase 4 -- Artifact Generation (Orchestrator -- Inline)

Write all output files to `plans/epic-XXXX/plans/` using flat naming convention (RULE-004).

### 4.1 Generate Tasks Breakdown File

**Output:** `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`

Use `_TEMPLATE-TASK-BREAKDOWN.md` if available (RULE-007), otherwise use inline format (RULE-012).

The tasks table MUST include Agent and DoD columns (extended from standard template):

```markdown
# Task Breakdown -- story-XXXX-YYYY

## Header

| Field | Value |
|-------|-------|
| Story ID | story-XXXX-YYYY |
| Epic ID | XXXX |
| Date | {generation date} |
| Author | x-story-plan (multi-agent) |
| Template Version | 1.0.0 |

## Summary

| Metric | Value |
|--------|-------|
| Total Tasks | {count} |
| Parallelizable Tasks | {count} |
| Estimated Effort | {total} |
| Mode | multi-agent |
| Agents Participating | Architect, QA, Security, Tech Lead, PO |

## Dependency Graph

```mermaid
graph TD
    {DEPENDENCY_GRAPH}
```

## Tasks Table

| Task ID | Source Agent | Type | TDD Phase | TPP Level | Layer | Components | Parallel | Depends On | Estimated Effort | DoD |
|---------|-------------|------|-----------|-----------|-------|-----------|----------|-----------|-----------------|-----|
| TASK-001 | {source} | {type} | {phase} | {level} | {layer} | {components} | {yes/no} | {deps} | {effort} | {dod} |
...

## Escalation Notes

| Task ID | Reason | Recommended Action |
|---------|--------|--------------------|
| {task} | {reason} | {action} |
```

### 4.2 Generate Individual Task Plan Files

**Output:** One file per task at `plans/epic-XXXX/plans/task-plan-TASK-NNN-story-XXXX-YYYY.md`

Use `_TEMPLATE-TASK-PLAN.md` if available (RULE-007), otherwise use inline format (RULE-012).

```markdown
# Task Plan -- TASK-NNN

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-NNN |
| Story ID | story-XXXX-YYYY |
| Epic ID | XXXX |
| Source Agent | {agent who proposed this task} |
| Type | {type} |
| TDD Phase | {RED/GREEN/REFACTOR/VERIFY/N/A} |
| Layer | {layer} |
| Estimated Effort | {effort} |
| Date | {generation date} |

## Objective

{detailed description of what this task accomplishes}

## Implementation Guide

{step-by-step implementation instructions from the proposing agent}

## Definition of Done

{consolidated DoD criteria, one per line with checkbox}

## Dependencies

| Depends On | Reason |
|-----------|--------|
| {task-id} | {why this dependency exists} |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| {risk} | {prob} | {impact} | {mitigation} |
```

### 4.3 Generate Planning Report

**Output:** `plans/epic-XXXX/plans/planning-report-story-XXXX-YYYY.md`

Use `_TEMPLATE-STORY-PLANNING-REPORT.md` if available (RULE-007), otherwise use inline format (RULE-012).

```markdown
# Story Planning Report -- story-XXXX-YYYY

## Header

| Field | Value |
|-------|-------|
| Story ID | story-XXXX-YYYY |
| Epic ID | XXXX |
| Date | {generation date} |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

{high-level summary of the planning outcome}

## Architecture Assessment

{summary from Architect agent: layers affected, new components, dependency validation}

## Test Strategy Summary

{summary from QA agent: acceptance test count, unit test count, TPP coverage, estimated coverage %}

## Security Assessment Summary

{summary from Security agent: OWASP categories applicable, controls needed, risk level}

## Implementation Approach

{summary from Tech Lead: chosen approach, quality gates, compliance with coding standards}

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | {count} |
| Architecture tasks | {count from ARCH} |
| Test tasks | {count from QA} |
| Security tasks | {count from SEC} |
| Quality gate tasks | {count from TL} |
| Validation tasks | {count from PO} |
| Merged tasks | {count of tasks merged during consolidation} |
| Augmented tasks | {count of tasks with injected security criteria} |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| {risk} | {agent} | {severity} | {likelihood} | {mitigation} |

## DoR Status

{READY or NOT_READY with summary of validation results -- filled in Phase 5}
```

### 4.4 Generate DoR Checklist

**Output:** `plans/epic-XXXX/plans/dor-story-XXXX-YYYY.md`

Use `_TEMPLATE-DOR-CHECKLIST.md` if available (RULE-007), otherwise use inline format (RULE-012).

This file is written in Phase 5 after validation completes.

### 4.5 Update Story File Section 8

Read the story file and update Section 8 (Sub-Tasks) with the consolidated task list:

```markdown
## 8. Sub-Tasks

### 8.1 Detailed Tasks (generated by x-story-plan)

| # | Task ID | Description | Type | TDD Phase | Layer | Depends On | Effort |
|---|---------|-------------|------|-----------|-------|-----------|--------|
| 1 | TASK-001 | {description} | {type} | {phase} | {layer} | {deps} | {effort} |
...

> Generated by `/x-story-plan` on {date}. See `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` for full breakdown.
```

If Section 8 already exists with sub-tasks, preserve existing content and add Section 8.1 below it. Do NOT remove manually written sub-tasks.

---

## Phase 5 -- DoR Validation (Orchestrator -- Inline)

Run validation checks against the generated artifacts to determine if the story meets the Definition of Ready.

### 5.1 Mandatory Checks (10)

| # | Check | How to Validate | Pass Criteria |
|---|-------|-----------------|---------------|
| 1 | Architecture plan exists | Check Architecture Assessment section in `planning-report-story-XXXX-YYYY.md`; standalone `architecture-story-XXXX-YYYY.md` may be used as fallback evidence | Architecture content exists with layer analysis |
| 2 | Test plan with AT-N + UT-N in TPP order | Check Test Strategy Summary section in `planning-report-story-XXXX-YYYY.md`; standalone `tests-story-XXXX-YYYY.md` may be used as fallback evidence | AT-N entries exist AND UT-N entries follow TPP level ordering (1 through 6) |
| 3 | Security assessment | Check Security Assessment Summary section in `planning-report-story-XXXX-YYYY.md`; standalone `security-story-XXXX-YYYY.md` may be used as fallback evidence | Assessment exists with OWASP mapping |
| 4 | Minimum 4 tasks | Count TASK-NNN entries in tasks file | `total_tasks >= 4` |
| 5 | Each task has >= 1 DoD criterion | Scan tasks table DoD column | No task has empty DoD |
| 6 | No circular task dependencies | Validate dependency DAG | DAG is acyclic |
| 7 | Story Gherkin has >= 4 scenarios | Count `Scenario:` lines in story acceptance criteria | `scenario_count >= 4` |
| 8 | Data contracts defined | Check story data contracts section | Request/response types with typed fields exist |
| 9 | Implementation plan exists | Check Implementation Approach section in `planning-report-story-XXXX-YYYY.md`; standalone `plan-story-XXXX-YYYY.md` may be used as fallback evidence | Implementation content exists with component list |
| 10 | Planning report exists | Check `planning-report-story-XXXX-YYYY.md` | File exists with all sections populated, including architecture, test plan, security, and implementation sections required by checks #1/#2/#3/#9 |

### 5.2 Conditional Checks (2)

| # | Check | Condition | How to Validate | Pass Criteria |
|---|-------|-----------|-----------------|---------------|
| 11 | Compliance assessment | Compliance required (compliance field != "none" in project config) | Check for compliance assessment in Security agent output | Assessment exists with regulatory mapping |
| 12 | Contract tests | Contract testing enabled (contract_tests == true in project config) | Check for contract test scenarios in QA agent output | At least 1 contract test scenario exists |

### 5.3 Emit Verdict

Calculate results:

```
mandatory_passed = count of checks 1-10 that pass
conditional_applicable = count of checks 11-12 that are applicable
conditional_passed = count of applicable conditional checks that pass
total_checks = mandatory_passed + conditional_applicable
total_passed = mandatory_passed + conditional_passed
```

**Verdict determination:**

| Condition | Verdict |
|-----------|---------|
| All mandatory pass AND all applicable conditional pass | `READY` |
| Any mandatory check fails | `NOT_READY` |
| Mandatory all pass but conditional fails | `NOT_READY` |

Write the DoR checklist file:

```markdown
# Definition of Ready Checklist -- story-XXXX-YYYY

## Header

| Field | Value |
|-------|-------|
| Story ID | story-XXXX-YYYY |
| Epic ID | XXXX |
| Date | {date} |
| Verdict | {READY / NOT_READY} |

## Mandatory Checks

- [x/] 1. Architecture plan exists -- {status}
- [x/] 2. Test plan with TPP ordering -- {status}
- [x/] 3. Security assessment -- {status}
- [x/] 4. Minimum 4 tasks -- {count} tasks
- [x/] 5. Each task has DoD -- {status}
- [x/] 6. No circular dependencies -- {status}
- [x/] 7. Gherkin >= 4 scenarios -- {count} scenarios
- [x/] 8. Data contracts defined -- {status}
- [x/] 9. Implementation plan exists -- {status}
- [x/] 10. Planning report exists -- {status}

## Conditional Checks

- [x/ /N/A] 11. Compliance assessment -- {status or N/A}
- [x/ /N/A] 12. Contract tests -- {status or N/A}

## Summary

Passed: {total_passed}/{total_checks}
Verdict: **{READY / NOT_READY}**

## Blockers (if NOT_READY)

{list of failed checks with remediation guidance}
```

Update the planning report's DoR Status section with the verdict.

**Final output message:**

```
>>> Phase 5/5 completed. Story planning complete.

Story: story-XXXX-YYYY
Verdict: {READY / NOT_READY}
Tasks: {total_tasks} ({parallelizable} parallelizable)
Agents: Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner
Artifacts:
  - plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md
  - plans/epic-XXXX/plans/task-plan-TASK-NNN-story-XXXX-YYYY.md (x{count})
  - plans/epic-XXXX/plans/planning-report-story-XXXX-YYYY.md
  - plans/epic-XXXX/plans/dor-story-XXXX-YYYY.md
  - story-XXXX-YYYY.md (Section 8.1 updated)
```

---

## Error Handling

| Scenario | Action |
|----------|--------|
| No story ID provided | Abort with usage message |
| Story file not found | Abort with file-not-found message |
| Epic file not found | Log warning, continue without epic context |
| Implementation map not found | Log warning, continue without dependency context |
| Subagent returns no proposals | Log warning for that agent, continue with other agents' proposals |
| All subagents return no proposals | Abort: `"ERROR: No task proposals generated by any agent"` |
| Circular dependency detected | Abort: `"ERROR: Circular dependency detected between {tasks}. Manual resolution required."` |
| Existing plan is fresh (RULE-002) | Reuse, skip to Phase 5 |
| Template not found (RULE-012) | Log warning, use inline format, continue |
| Less than 4 tasks after consolidation | DoR verdict is NOT_READY (check #4 fails) |
| Fewer than 4 Gherkin scenarios | DoR verdict is NOT_READY (check #7 fails) |

## Template Fallback (RULE-012)

When any template file is **not available** (projects predating template adoption):

1. Log warning: `"WARNING: Template {name} not found, using inline format"`
2. Generate the artifact using the inline format defined in this skill
3. Execution continues normally -- no interruption, no error
4. The inline format produces the same conceptual sections

This ensures backward compatibility with projects that have not yet adopted template-based generation.

## Anti-Patterns

- Do NOT generate implementation code -- only plan tasks
- Do NOT skip any agent (all 5 must run for comprehensive coverage)
- Do NOT ignore security agent proposals (they augment other tasks)
- Do NOT reorder RED before GREEN pairs (test-first is mandatory)
- Do NOT assign the same TASK-NNN ID to multiple tasks
- Do NOT create circular dependencies in the task graph
- Do NOT overwrite manually written sub-tasks in Section 8 (append Section 8.1 instead)
- Do NOT reference `.claude/agents/` files in subagent prompts (RULE-005: prompts are embedded)

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| architecture | `skills/architecture/references/architecture-principles.md` | Layer structure, dependency direction |
| layer-templates | `skills/layer-templates/SKILL.md` | Code templates per layer |
| coding-standards | `skills/coding-standards/references/coding-conventions.md` | {{LANGUAGE}} conventions, Clean Code |
| testing | `skills/testing/references/testing-philosophy.md` | TDD workflow, 8 test categories |
| testing | `skills/testing/references/testing-conventions.md` | {{LANGUAGE}}-specific test patterns |
| security | `skills/security/SKILL.md` + references | OWASP, input validation, secrets |
| compliance | `skills/compliance/SKILL.md` | Regulatory frameworks (conditional) |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-story-implement` | called-by | Can invoke x-story-plan in Phase 1 for multi-agent planning |
| `x-test-plan` | complementary | QA agent produces similar output; x-story-plan adds multi-agent perspective |
| `x-arch-plan` | complementary | Architect agent produces similar output with cross-agent consolidation |
| `x-task-implement` | downstream | Consumes task breakdown as implementation roadmap |
| `x-story-create` | upstream | Story files are the input to this skill |
| `x-epic-map` | upstream | Implementation map provides dependency context |

- Pre-check (RULE-002) prevents redundant regeneration when story has not changed
- Template reference (RULE-007) ensures consistent output format when templates are available
- Embedded prompts (RULE-005) keep all agent instructions in this SKILL.md
- Flat file naming (RULE-004) ensures all artifacts are in `plans/epic-XXXX/plans/`
- Staleness uses mtime comparison for idempotent re-execution

---

## v2 Extensions (EPIC-0038 — Task-First Planning)

This appendix documents the schema-aware behavior introduced by story-0038-0004. It
layers three new phases (4a, 4b, 4c) on top of the legacy v1 flow documented above,
plus a per-task DoR extension in Phase 5. v1 behavior is **unchanged**.

### Phase 0b — Schema Version Detection

After Phase 0 (input resolution) and before Phase 1 (context gathering):

1. Read `plans/epic-XXXX/execution-state.json` if present.
2. Resolve `planningSchemaVersion` via the shared resolver delivered by
   story-0038-0008 (`SchemaVersionResolver`):
   - Present and equal to `"2.0"` -> v2 flow (Phases 4a-4c execute; Phase 5 adds per-task DoR).
   - Absent / `"1.0"` / malformed / unknown value -> v1 flow (legacy; skip 4a-4c).
3. Emit a single-line log reporting the resolved version: `schema: v2` or `schema: v1`.
4. Hard fail only when `execution-state.json` is present but not parseable JSON.

> **Bootstrap note.** EPIC-0038 itself is executed in v1 per spec §8.2. The v2 flow
> only activates for epics/stories that explicitly declare `planningSchemaVersion: "2.0"`
> in their execution-state.json — the first dogfood happens in story-0038-0010.

### Phase 4a — Task Breakdown with I/O Contracts (v2 only)

After Phase 4 writes the consolidated `tasks-story-XXXX-YYYY.md`:

1. For each task declared in Section 8 of the consolidated output, emit a standalone
   `plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md` following the schema from
   story-0038-0001 (`plans/epic-0038/schemas/task-schema.md`).
2. Required sections per task file: header (ID + Story + Status), `## 1. Objetivo`,
   `## 2. Contratos I/O` (Inputs, Outputs, Testabilidade), `## 3. Definition of Done`,
   `## 4. Dependências`, `## 5. Plano de implementação` (placeholder — filled by 4b).
3. Validation (reject the task and abort if any fails):
   - RULE-TF-01 Testability: §2.3 has exactly one checked declaration
     (INDEPENDENT / REQUIRES_MOCK / COALESCED).
   - RULE-TF-02 Outputs: §2.2 is non-empty and lists at least one grep/assert/test
     verifiable output.
4. Until story-0038-0009 ships `_TEMPLATE-TASK.md`, the skill uses an inline template
   identical in shape to the schema. Once the template lands, switch to it via the
   standard `.claude/templates/` path.

### Phase 4b — Per-Task Plan via x-task-plan (v2 only)

For each `task-TASK-XXXX-YYYY-NNN.md` produced by 4a:

1. Invoke `x-task-plan --task-file plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md`
   via the Skill tool.
2. Parallelism: fire invocations in batches of up to 4 concurrent subagents (single
   assistant message with sibling Skill calls, per Rule 13 — INLINE-SKILL pattern).
3. Each invocation writes `plan-task-TASK-XXXX-YYYY-NNN.md` next to the task file.
4. A non-zero exit from any invocation aborts the story planning; collect every
   failed task-id and emit a single consolidated error report.

### Phase 4c — Task-Implementation-Map Generation (v2 only)

Once all per-task plans exist:

1. Invoke the `task-map-gen` CLI (story-0038-0002) with
   `--story story-XXXX-YYYY --plans-dir plans/epic-XXXX/plans/`.
2. The CLI reads each `task-TASK-NNN.md`, runs TopologicalSorter + MarkdownWriter,
   and writes `task-implementation-map-STORY-XXXX-YYYY.md`.
3. Propagate CLI exit code verbatim. On non-zero, abort with the stderr diagnostic
   (the map CLI's error messages already contain TASK-IDs per story-0038-0002 §7).

### Phase 5 — DoR (v2 extensions)

The legacy 12-check story-level DoR runs unchanged. When the resolved schema is
`"2.0"`, append:

- **Task READY checks:** each task-TASK-NNN.md must be schema-valid per the TF-SCHEMA
  rules (from story-0038-0001 §4) with zero ERROR-level violations.
- **Plan presence:** every task must have a corresponding plan-task-TASK-NNN.md
  produced in 4b.
- **Map integrity:** task-implementation-map-STORY-*.md exists and topological sort
  succeeded (presence alone is sufficient — CLI-level cycle detection already ran).

Aggregated verdict:

- **READY**: story-level DoR PASS AND all task READY checks PASS.
- **NOT_READY**: any check fails — enumerate failures per task.

### v2 Artifacts Summary

In addition to the legacy outputs (tasks-story-*.md + planning report + consolidated
agent file), a v2 run produces:

- `task-TASK-XXXX-YYYY-NNN.md` × N  (one per task)
- `plan-task-TASK-XXXX-YYYY-NNN.md` × N  (one per task)
- `task-implementation-map-STORY-XXXX-YYYY.md` × 1
