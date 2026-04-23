---
name: x-task-plan
description: "Generates a detailed per-task implementation plan (plan-task-TASK-XXXX-YYYY-NNN.md) with TDD cycles in TPP order, file impact analysis by architecture layer, security checklist by task type, and exit criteria. Two invocation modes: task-file-first (--task-file) consumes a standalone task-TASK-XXXX-YYYY-NNN.md contract (EPIC-0038); story-scoped (STORY-ID --task TASK-ID) reads the task from story Section 8 (legacy). Invocable standalone OR via x-story-plan (future)."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "--task-file <path> [--output-dir <dir>] [--no-commit] [--dry-run]  |  [STORY-ID] --task [TASK-ID] [--force] [--no-commit] [--dry-run]"
context-budget: heavy
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Plan Task

## Purpose

Produces a detailed implementation plan for a single task extracted from a story's Section 8. The plan contains: objective, implementation guide with class/method/pattern, TDD cycles mapped in TPP order (degenerate -> constants -> conditionals -> iterations -> complex), affected files organized by architecture layer, security checklist adapted to the task type, dependencies, and definition of done. Each task plan is a self-contained execution guide that eliminates ad-hoc decisions during coding.

## Triggers

- `/x-task-plan --task-file <path>` -- **task-file-first** (EPIC-0038): consume a standalone `task-TASK-XXXX-YYYY-NNN.md` contract, write the plan next to it.
- `/x-task-plan --task-file <path> --output-dir <dir>` -- override output directory.
- `/x-task-plan --task-file <path> --no-commit` -- **batch mode** (EPIC-0049 story-0049-0017): write the plan file but SKIP the individual planning-commit. Callers (e.g., `x-story-plan`) aggregate N plans and issue ONE batched commit.
- `/x-task-plan STORY-ID --task TASK-ID` -- **story-scoped (legacy)**: read task from story Section 8.
- `/x-task-plan STORY-ID --task TASK-ID --force` -- regenerate even if plan exists.
- `/x-task-plan STORY-ID --task TASK-ID --no-commit` -- **batch mode (story-scoped)**: write plan but skip commit.

> **Invocation modes.** Task-file-first is the canonical path post-EPIC-0038: an
> orchestrator (human or `x-story-plan` in the future) generates `task-TASK-NNN.md`
> files and pipes each one through this skill. Story-scoped mode is retained for
> backward compatibility with epics 0025-0037 that still declare tasks as sub-sections
> of the story file.

## Parameters

### Task-file-first mode (EPIC-0038)

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--task-file` | Yes | Path to a `task-TASK-XXXX-YYYY-NNN.md` file (schema: story-0038-0001). MUST pass `TaskFileParser` validation. |
| `--output-dir` | No (default: same dir as `--task-file`) | Directory to write `plan-task-TASK-XXXX-YYYY-NNN.md`. |
| `--force` | No | Regenerate plan even if a fresh one already exists. |
| `--no-commit` | No (default: `false`) | When `true`, skip the Planning Status Propagation commit (Phase 5.4). Plan file is still written to disk and status is still flipped `Pendente -> Planejada`, but the commit step is deferred to the caller. Used by `x-story-plan` to batch-commit N plans in a single commit (EPIC-0049 story-0049-0017). |
| `--dry-run` | No (default: `false`) | When `true`, plan file is written to disk but Steps P2 / P4 / P5 (branch-ensure / planning-commit / push) become no-ops (EPIC-0049 / RULE-007). |

### Story-scoped mode (legacy — epics 0025-0037)

| Parameter | Required | Description |
|-----------|----------|-------------|
| `STORY-ID` | Yes | Story identifier (pattern: `story-XXXX-YYYY`). |
| `--task` | Yes | Task identifier (pattern: `TASK-XXXX-YYYY-NNN`). Must exist in story Section 8. |
| `--force` | No | Regenerate plan even if a fresh one already exists. |
| `--no-commit` | No (default: `false`) | Same semantics as in task-file-first mode: write plan, skip commit; caller handles batched commit. |
| `--dry-run` | No (default: `false`) | Same semantics as in task-file-first mode: Steps P2 / P4 / P5 become no-ops. |

## Workflow

```
0. VALIDATE & PRE-CHECK  -> Parse arguments, resolve paths, staleness check
1. EXTRACT CONTRACTS      -> Read task source (task-file OR story Section 8)
2. MAP TDD CYCLES         -> Generate TDD cycles in TPP order
3. ANALYZE FILES          -> Identify affected files by architecture layer
3.5 COMPUTE FOOTPRINT     -> Emit structured File Footprint (write/read/regen)
4. SECURITY CHECKLIST     -> Generate security items based on task type
5. WRITE PLAN             -> Assemble and write plan-task-TASK-XXXX-YYYY-NNN.md
```

> **Phase 1 dispatch:** If `--task-file` is present, extract contracts from that file
> via TaskFileParser semantics (story-0038-0001 schema). Otherwise, fall back to the
> story-scoped reader that locates the task in `## 8. Tasks` of the story file.

### Step P1 — Detect Worktree Context (EPIC-0049 / RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-plan Phase-P1-Worktree-Detect`

Invoke `x-git-worktree` in detect-context mode to record whether the current checkout is already inside an epic worktree. Result is advisory only — `x-internal-epic-branch-ensure` (Step P2) makes the authoritative decision.

    Skill(skill: "x-git-worktree", args: "detect-context")

Continue on any detect-context failure (fail-open, RULE-006) — log a WARNING and proceed to Step P2.

When `--no-commit` is set (orchestrator mode, e.g., called by `x-story-plan`), skip this step — the parent owns branch lifecycle.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-plan Phase-P1-Worktree-Detect ok`

### Step P2 — Ensure `epic/<ID>` Branch (EPIC-0049 / RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-plan Phase-P2-Epic-Branch-Ensure`

Resolve the effective epic ID from the task source:

- **Task-file mode (`--task-file`):** extract `XXXX` from the task-file path (`plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md`) or from the task's `**Task ID:**` header.
- **Story-scoped mode:** extract `XXXX` from the `STORY-ID` argument (e.g., `story-0049-0001` → `0049`).

Invoke `x-internal-epic-branch-ensure` so the canonical `epic/<ID>` branch exists locally AND on origin (idempotent). The skill is a no-op when the current checkout is already on `epic/<ID>` or a worktree rooted at that branch.

    Skill(skill: "x-internal-epic-branch-ensure", args: "--epic-id <XXXX>")

On failure (non-zero exit), abort with `EPIC_BRANCH_ENSURE_FAILED` — a clean audit trail cannot be produced without the canonical branch.

When `--no-commit` or `--dry-run` is set, skip this step.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-plan Phase-P2-Epic-Branch-Ensure ok`

### Phase 0 -- Validate and Pre-Check

#### 0.1 Parse Arguments

Extract from story ID and task ID:
- Epic ID (XXXX) from `story-XXXX-YYYY`
- Story sequence (YYYY) from `story-XXXX-YYYY`
- Task number (NNN) from `TASK-XXXX-YYYY-NNN`

Validation rules:
- Story ID MUST match pattern `story-XXXX-YYYY` (4-digit groups)
- Task ID MUST match pattern `TASK-XXXX-YYYY-NNN` (3-digit task number)
- Epic ID from story MUST match epic ID in task ID
- Story sequence from story MUST match story sequence in task ID

If validation fails, abort with descriptive error message.

#### 0.2 Resolve Epic Directory

1. Extract epic ID from story ID (`story-XXXX-YYYY` -> `epic-XXXX`)
2. Resolve `EPIC_DIR` with a glob that supports both exact and suffix variants:
   - Exact match: `plans/epic-XXXX`
   - Suffix variant: `plans/epic-XXXX-*`
3. If exactly one directory matches, use that as `EPIC_DIR`
4. If both exist, prefer exact match `plans/epic-XXXX`
5. If no directory matches, abort: `"Epic directory not found for epic-XXXX"`

#### 0.3 Resolve Paths

| Path | Pattern | Example |
|------|---------|---------|
| Story file | `<EPIC_DIR>/story-XXXX-YYYY.md` | `plans/epic-0029/story-0029-0001.md` |
| Plan output | `<EPIC_DIR>/plans/task-plan-XXXX-YYYY-NNN.md` | `plans/epic-0029/plans/task-plan-0029-0001-001.md` |
| Output dir | `<EPIC_DIR>/plans/` | `plans/epic-0029/plans/` |

#### 0.4 Idempotency Check (Staleness)

Before generating, verify whether a valid plan already exists:

1. If the plan file does NOT exist, proceed to Phase 1.
2. If the plan file exists AND `--force` is set, log: `"Regenerating task plan (--force)"`. Proceed to Phase 1.
3. If the plan file exists AND `--force` is NOT set:
   - If `mtime(story file) <= mtime(plan file)` -- plan is **fresh**. Log: `"Task plan already exists and is up-to-date"`. Return existing plan. Stop.
   - If `mtime(story file) > mtime(plan file)` -- plan is **stale**. Log: `"Regenerating stale task plan for TASK-XXXX-YYYY-NNN"`. Proceed to Phase 1.

### Phase 1 -- Extract Contracts

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-plan Phase-1-Context-Gathering`

#### 1A. Task-file-first branch (EPIC-0038 — `--task-file` present)

1. Read the file at `<task-file>`. Abort with exit code 1 if missing.
2. Validate structure per story-0038-0001 schema (`plans/epic-0038/schemas/task-schema.md`):
   - `**ID:** TASK-XXXX-YYYY-NNN` present and matches filename.
   - `**Story:** story-XXXX-YYYY` present and well-formed.
   - `**Status:**` in the allowed enum (`Pendente | Em Andamento | Concluída | Bloqueada | Falha`).
   - `## 2. Contratos I/O` with subsections `### 2.1 Inputs`, `### 2.2 Outputs`, `### 2.3 Testabilidade` — all three present.
   - `### 2.3 Testabilidade` has exactly one checked declaration (INDEPENDENT / REQUIRES_MOCK / COALESCED).
   - `## 3. Definition of Done` present with ≥ 6 items (warn otherwise).
3. If validation fails, abort with exit code 1 and message `Task file invalid: {violations}`.
4. If testability is absent, abort with exit code 3: `Testability not declared (RULE-TF-01)`.
5. Project extracted fields into the internal TaskContract:
   - **Objective**: body of `## 1. Objetivo`.
   - **Inputs**: body of `### 2.1 Inputs`.
   - **Outputs**: body of `### 2.2 Outputs` (drives File Impact analysis in Phase 3).
   - **TestabilityKind**: the single checked option (drives Phase 2 cycle shape).
   - **TestabilityReferences**: TASK-IDs cited (REQUIRES_MOCK / COALESCED partners).
   - **DependsOn**: TASK-IDs from the first column of `## 4. Dependências`.

#### 1B. Story-scoped branch (legacy — no `--task-file`)

1. Read the story file at the resolved path.
2. Locate **Section 8** (heading `## 8.` or `## 8 `).
3. Find the task heading matching the task ID: `### TASK-XXXX-YYYY-NNN:` (case-insensitive match on the ID portion).
4. Extract the task definition including all fields:
   - **Title**: from the heading text after the task ID
   - **Layer**: Domain, Port, Adapter, Application, Config, Test, Doc
   - **Test Type**: Unit, Integration, API, Contract, E2E, Smoke, Verification
   - **Size**: S, M, L
   - **Dependencies**: list of TASK IDs or `--`
   - **Testability**: valid pattern from Section 8 table
   - **Files**: list of affected file paths
   - **Acceptance Criteria**: list of criteria
5. If the task ID is NOT found in Section 8, abort: `"Task TASK-XXXX-YYYY-NNN not found in story-XXXX-YYYY Section 8"`

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-plan Phase-1-Context-Gathering ok`

### Phase 2 -- Map TDD Cycles (TPP Order)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-plan Phase-2-Task-Breakdown`

Generate TDD cycles based on the task's layer, test type, and acceptance criteria. Cycles MUST follow strict **Transformation Priority Premise** order:

#### TPP Cycle Order

| Order | TPP Level | Transform | Description |
|-------|-----------|-----------|-------------|
| 1 | Degenerate | `{} -> nil` | Null/empty/zero inputs return default or error |
| 2 | Constant | `nil -> constant` | Single hardcoded return value |
| 3 | Scalar | `constant -> variable` | Parameterized return based on input |
| 4 | Conditional | `unconditional -> conditional` | Single if/else branching |
| 5 | Collection | `scalar -> collection` | Multiple items, iteration, map/filter |
| 6 | Complex | `collection -> complex` | Compound logic, nested conditions, state |

#### Per-Cycle Structure

Each TDD cycle MUST contain:

```markdown
#### Cycle N: [Description]

- **TPP Level**: [degenerate|constant|scalar|conditional|collection|complex]
- **Transform**: [TPP transformation applied]

**RED** (Write Failing Test):
- Test name: `[methodUnderTest]_[scenario]_[expectedBehavior]`
- Assertion: [primary assertion description]
- Run: `{{TEST_COMMAND}}`
- Expected: FAIL (test fails with specific error)

**GREEN** (Minimum Implementation):
- Implementation: [minimum code to make test pass]
- Run: `{{COMPILE_COMMAND}}` then `{{TEST_COMMAND}}`
- Expected: PASS (all tests green)

**REFACTOR** (Improve Design):
- Opportunities: [extract method, rename, eliminate duplication, or "None at this cycle"]
- Run: `{{TEST_COMMAND}}`
- Expected: PASS (no behavior change)

**Commit**: `feat(TASK-XXXX-YYYY-NNN): [description] [TDD:RED|GREEN|REFACTOR]`
```

#### Minimum Cycles

Every task plan MUST contain at least 3 TDD cycles. The first cycle MUST always be a degenerate case (TPP Level 1). For domain logic tasks, target 4-6 cycles. For simple config/doc tasks, 3 cycles suffice.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-plan Phase-2-Task-Breakdown ok`

### Phase 3 -- Analyze Affected Files by Layer

Organize all affected files by architecture layer following the {{ARCHITECTURE}} structure. For each file, indicate whether it is **new** (to be created) or **modified** (existing).

#### Layer Order (Implementation Sequence)

Files MUST be listed in implementation order (inner layers first):

| Order | Layer | Package Pattern | Description |
|-------|-------|----------------|-------------|
| 1 | Domain | `domain/model/`, `domain/engine/` | Entities, value objects, business logic |
| 2 | Port | `domain/port/` | Inbound and outbound interfaces |
| 3 | Adapter (outbound) | `adapter/outbound/` | DB repositories, external clients |
| 4 | Adapter (inbound) | `adapter/inbound/` | REST controllers, CLI handlers, DTOs, mappers |
| 5 | Application | `application/` | Use cases, orchestration |
| 6 | Config | `config/` | Framework configuration |
| 7 | Test | `test/` | Test classes (mirrors production structure) |

#### File Entry Format

| Field | Required | Description |
|-------|----------|-------------|
| Path | M | Relative file path |
| Action | M | `CREATE` or `MODIFY` |
| Layer | M | Architecture layer |
| Purpose | M | Brief description of what this file does/changes |

### Phase 4 -- Generate Security Checklist

Generate a security checklist based on the task type. The checklist adapts to the nature of the task:

#### Security Checklist by Task Type

| Task Type | Security Items |
|-----------|---------------|
| Endpoint / API | Input validation on all parameters; Output encoding (prevent XSS); Authentication check (endpoint protected); Authorization check (role/permission); Rate limiting consideration; CORS policy adherence |
| Persistence / DB | Parameterized queries only (no SQL concatenation); Sensitive data encryption at rest; Column-level access control; Audit logging for data mutations; No PII in log statements |
| Domain Logic | Business rule bypass prevention; State manipulation guards; Privilege escalation checks; Input boundary validation; Immutable value objects for sensitive data |
| Config | No hardcoded secrets or credentials; Secure defaults (fail-closed); Environment variable externalization; No default credentials; Configuration validation on startup |
| Integration | TLS validation (no trust-all); Certificate pinning where applicable; Timeout configuration (prevent hanging); Retry with backoff (prevent amplification); Circuit breaker for external calls |

For each applicable security item, generate a checklist entry with:
- [ ] Item description
- Severity: CRITICAL / HIGH / MEDIUM
- Reference: CWE identifier or OWASP category where applicable

### Phase 4.5 -- Compute File Footprint

Emit a structured machine-readable footprint so downstream tooling (e.g., `/x-parallel-eval`) can detect write-conflicts deterministically, without relying on prose parsing of "Affected Files".

#### Inference Rules

For each path in the task's `Files:` list (Section 8 of the story):

| Rule | Condition | Target sub-section |
|------|-----------|--------------------|
| R1 (default) | Any path declared on the task | `write:` |
| R2 (golden regen — pom) | Path ends in `pom.xml` | Add `regen:` entry for corresponding artifacts |
| R3 (golden regen — skill source) | Path matches `java/src/main/resources/targets/claude/**/SKILL.md` | Add `regen:` entry in `.claude/skills/**` at mirror path |
| R4 (golden regen — targets tree) | Path under `java/src/main/resources/targets/claude/` (non-SKILL.md) | Add matching `regen:` entry in `.claude/` or `src/test/resources/golden/` |
| R5 (reads) | Path listed in task's `Dependencies -> reads` section | `read:` |

Empty sub-sections MUST be omitted from the plan output. Paths within each sub-section MUST be sorted alphabetically for determinism (RULE-008).

#### Output Layout

Inject a `## File Footprint` section into the plan document, immediately BEFORE `## Definition of Done`:

```markdown
## File Footprint

### write:
- path/to/writeA
- path/to/writeB

### read:
- path/to/readA

### regen:
- path/to/regenA
```

#### Knowledge Pack Reference

The inference rules above are the working contract documented in the `parallelism-heuristics` knowledge pack (`skills/knowledge-packs/parallelism-heuristics/SKILL.md`). Read that KP when extending the rules (e.g., adding new regen patterns) to keep consumer tooling in sync.

### Phase 5 -- Write Plan

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-plan Phase-3-Validation`

#### 5.1 Ensure Output Directory

```bash
mkdir -p <EPIC_DIR>/plans/
```

#### 5.2 Assemble Plan Document

Write the plan to `<EPIC_DIR>/plans/task-plan-XXXX-YYYY-NNN.md` with the following structure:

```markdown
# Task Plan: TASK-XXXX-YYYY-NNN

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-XXXX-YYYY-NNN |
| Story ID | story-XXXX-YYYY |
| Epic ID | epic-XXXX |
| Layer | [extracted from Section 8] |
| Type | [extracted from Section 8: test type] |
| TDD Cycles | [count of cycles] |
| Estimated Effort | [S/M/L mapped to time] |
| Generated | [ISO-8601 date] |

## Objective

[Description of what this task accomplishes, extracted from Section 8 title and acceptance criteria]

## Implementation Guide

### Target Class/Method

[Specific class, method, and design pattern to implement, with examples in {{LANGUAGE}}]

### Design Pattern

[Pattern being applied: Strategy, Factory, Repository, etc.]

### Implementation Steps (Layer Order)

1. [Step 1 - innermost layer first]
2. [Step 2]
3. [Step N]

## TDD Cycles

[Generated cycles from Phase 2, in TPP order]

## Affected Files

| # | Path | Action | Layer | Purpose |
|---|------|--------|-------|---------|
| 1 | [path] | [CREATE/MODIFY] | [layer] | [purpose] |

## Security Checklist

[Generated checklist from Phase 4, adapted to task type]

## Dependencies

| Depends On | Reason |
|------------|--------|
| [TASK ID or cross-story reference] | [Why this dependency exists] |

## File Footprint

[Generated block from Phase 4.5 — sub-sections `write:`, `read:`, `regen:` with alphabetically-sorted paths. Empty sub-sections omitted.]

## Definition of Done

- [ ] All TDD cycles completed (RED -> GREEN -> REFACTOR)
- [ ] All tests passing: `{{TEST_COMMAND}}`
- [ ] Code compiles cleanly: `{{COMPILE_COMMAND}}`
- [ ] Security checklist items addressed
- [ ] No TODO/FIXME/HACK comments in task scope
- [ ] Acceptance criteria from Section 8 satisfied
- [extracted DoD criteria from task definition]
```

#### 5.3 Report

After writing, log: `"Task plan generated: task-plan-XXXX-YYYY-NNN.md (N TDD cycles, M affected files)"`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-plan Phase-3-Validation ok`

## Anti-Patterns

- Do NOT write implementation code -- only plan the approach
- Do NOT skip degenerate cases in TDD cycles (Cycle 1 is ALWAYS degenerate)
- Do NOT generate security items unrelated to the task type
- Do NOT list files outside the task scope
- Do NOT include cycles beyond what the task complexity requires (CRUD tasks need 3-4 cycles, not 8)
- Do NOT ignore the task's layer assignment -- files must align with the declared layer
- Do NOT generate cycles in non-TPP order (never start with complex cases)

## Error Handling

### Task-file-first mode (EPIC-0038)

| Scenario | Exit Code | Message |
|----------|-----------|---------|
| Task file missing or unreadable | 1 | `Task file invalid: file not found at {path}` |
| Task file schema violations (story-0038-0001) | 1 | `Task file invalid: {violations}` |
| Testability not declared (§2.3 empty / multiple checked) | 3 | `Testability not declared (RULE-TF-01). Declare Testability: Independent OR Requires Mock OR Coalesced` |
| Output dir not writable | 2 | `Output dir not writable: {path}` |
| Plan generated | 0 | `Plan written to {path}` |
| Plan exists and fresh (no `--force`) | 0 | `Task plan already exists and is up-to-date` |

### Story-scoped mode (legacy)

| Scenario | Action |
|----------|--------|
| No story ID provided | Prompt: `"Usage: /x-task-plan [STORY-ID] --task [TASK-ID] [--force]"` |
| No task ID provided | Abort: `"--task flag is required. Provide a TASK-XXXX-YYYY-NNN identifier."` |
| Story file not found | Abort: `"Story file not found at {path}"` |
| Section 8 not found | Abort: `"Section 8 (Tasks) not found in story {story-id}"` |
| Task ID not in Section 8 | Abort: `"Task {task-id} not found in story {story-id} Section 8"` |
| Plan exists and fresh | Return existing: `"Task plan already exists and is up-to-date"` |
| Plan exists with --force | Regenerate: `"Regenerating task plan (--force)"` |
| Epic directory not found | Abort: `"Epic directory not found for epic-XXXX"` |
| `x-internal-epic-branch-ensure` fails (Step P2) | Abort with `EPIC_BRANCH_ENSURE_FAILED`; canonical branch is required for versioning |
| `x-git-push` fails (Step P5) | WARN only; local commit preserved; operator re-runs manually |
| `--dry-run` set | Steps P2, P4 (Phase 5.4) and P5 become no-ops |
| `--no-commit` set | Steps P2, P4 (Phase 5.4) and P5 become no-ops — orchestrator owns branch + commit lifecycle |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-story-plan` | complementary | x-story-plan generates task breakdown; x-task-plan generates per-task execution plans |
| `x-story-implement` | called-by | Phase 2 (PRE_PLANNED mode) reads task plans to drive implementation |
| `x-task-implement` | consumed-by | Task plans serve as implementation guides for the developer |
| `x-test-plan` | complementary | x-test-plan covers story-level tests; x-task-plan maps per-task TDD cycles |
| `x-git-worktree` | calls (Step P1) | Detect-context (EPIC-0049 / RULE-001) |
| `x-internal-epic-branch-ensure` | calls (Step P2) | Ensure `epic/<ID>` exists locally + origin (EPIC-0049 / RULE-001) |
| `x-git-commit` | calls (Phase 5.4 / Step P4) | Commit plan + status flip in standalone mode |
| `x-git-push` | calls (Step P5) | Push canonical epic branch to origin (optional) |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| testing | `skills/testing/SKILL.md` | TDD patterns, TPP levels, test naming conventions |
| architecture | `skills/architecture/SKILL.md` | Layer definitions, package structure, dependency rules |
| security | `skills/security/SKILL.md` | OWASP Top 10, security checklist items |
| parallelism-heuristics | `skills/knowledge-packs/parallelism-heuristics/SKILL.md` | File Footprint semantics (write/read/regen sub-sections) consumed by Phase 4.5 |
| coding-standards | `skills/coding-standards/SKILL.md` | {{LANGUAGE}} conventions, naming, SOLID principles |

## Planning Status Propagation (Rule 22 / EPIC-0046)

> V2-gated: only runs when `SchemaVersionResolver.resolve(plans/epic-XXXX/execution-state.json) == V2`. v1 epics: skip silently (Rule 19).

After writing `plan-task-TASK-XXXX-YYYY-NNN.md`, propagate the lifecycle status of the source task artifact (`task-TASK-XXXX-YYYY-NNN.md` in v2, or the Section 8 task entry in v1) from `Pendente` to `Planejada` in the SAME commit as the plan artefact.

**Steps (end of Phase 3, BEFORE the final commit):**

1. Detect v2 via SchemaVersionResolver on the epic's `execution-state.json`. If v1: skip this entire block.
2. For the source task file `plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md`, read current status with the CLI:
   ```bash
   CURRENT=$(java -cp $CLAUDE_PROJECT_DIR/java/target/classes \
       dev.iadev.adapter.inbound.cli.StatusFieldParserCli \
       read plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md)
   ```
3. If `CURRENT == "Pendente"`, write `Planejada` atomically:
   ```bash
   java -cp $CLAUDE_PROJECT_DIR/java/target/classes \
       dev.iadev.adapter.inbound.cli.StatusFieldParserCli \
       write plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md Planejada
   ```
   If `CURRENT == "Planejada"`: idempotent re-run, skip the write.
4. Stage both the source task file and the plan artefact:
   ```bash
   git add plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md plans/epic-XXXX/plans/plan-task-TASK-XXXX-YYYY-NNN.md
   ```
5. **Commit gate (`--no-commit` aware):**
   - If `--no-commit=false` (default): commit via `x-git-commit` (Rule 13 Pattern 1 INLINE-SKILL):

         Skill(skill: "x-git-commit", args: "docs(task-TASK-XXXX-YYYY-NNN): add plan + update status to Planejada")

   - If `--no-commit=true` (EPIC-0049 batch mode): **SKIP the commit step**. The plan file and status write remain on disk (staged) but no commit is produced. Log: `"[no-commit] Plan written; commit deferred to caller"`. Return response with `commitSha: null`.

**Fail-loud:** non-zero CLI exit → abort skill with the same exit code (RULE-046-08).

### `--no-commit` Contract (story-0049-0017)

| Aspect | `--no-commit=false` (default) | `--no-commit=true` (batch) |
|--------|-------------------------------|----------------------------|
| Plan file written to disk | Yes | Yes |
| Status flipped `Pendente -> Planejada` | Yes | Yes |
| `git add` of plan + task file | Yes | Yes |
| `x-git-commit` invoked | Yes | **NO** (deferred to caller) |
| Response `commitSha` | non-null SHA | `null` |
| Re-invocation semantics | Idempotent (staleness check) | Idempotent; flipping the flag between runs alternates commit behavior |

**Caller contract (e.g., `x-story-plan`):** when invoking N tasks with `--no-commit=true`, the caller MUST aggregate all written paths and issue ONE consolidated `x-planning-commit` call covering every plan + status update — producing a single commit per story instead of N commits.

**Backward compat:** absence of `--no-commit` (or explicit `--no-commit=false`) preserves pre-EPIC-0049 behavior byte-for-byte.

### Step P4 — Planning Status Commit (alias of Phase 5.4)

The planning-commit step for `x-task-plan` is performed by **Phase 5.4 — Planning Status Propagation** (above). When `--no-commit=true` or `--dry-run=true`, that step becomes a no-op (logged as `"[no-commit] Plan written; commit deferred to caller"` or `"dry-run, skipping commit"` respectively). No additional P4 invocation is issued; this alias exists solely so the P1-P5 convention is readable end-to-end in the skill body (EPIC-0049 / RULE-007).

### Step P5 — Push to Origin (optional, EPIC-0049)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-plan Phase-P5-Push`

If `--dry-run` or `--no-commit` is set, log `"dry-run, skipping push"` / `"orchestrated mode, skipping push"` and skip.

Delegate the push to `x-git-push` so the canonical `epic/<ID>` branch is synchronized with origin:

    Skill(skill: "x-git-push", args: "--branch epic/<XXXX>")

On push failure (remote rejection, no connectivity), log a WARNING and continue — the local commit is preserved; the operator can re-run Step P5 or `git push` manually. Do NOT abort.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-plan Phase-P5-Push ok`
