---
name: x-plan-task
description: "Generates a detailed implementation plan for an individual task with per-task TDD cycle mapping (TPP order), file impact analysis by architecture layer, security checklist by task type, and integration points. Reads the task definition from story Section 8 and produces a self-contained execution guide."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[STORY-ID] --task [TASK-ID] [--force]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Plan Task

## Purpose

Produces a detailed implementation plan for a single task extracted from a story's Section 8. The plan contains: objective, implementation guide with class/method/pattern, TDD cycles mapped in TPP order (degenerate -> constants -> conditionals -> iterations -> complex), affected files organized by architecture layer, security checklist adapted to the task type, dependencies, and definition of done. Each task plan is a self-contained execution guide that eliminates ad-hoc decisions during coding.

## Triggers

- `/x-plan-task STORY-ID --task TASK-ID` -- generate a task plan
- `/x-plan-task STORY-ID --task TASK-ID --force` -- regenerate even if plan exists

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `STORY-ID` | Yes | Story identifier (pattern: `story-XXXX-YYYY`). If not provided, prompt for it. |
| `--task` | Yes | Task identifier (pattern: `TASK-XXXX-YYYY-NNN`). Must exist in story Section 8. |
| `--force` | No | Regenerate plan even if a fresh one already exists. |

## Workflow

```
0. VALIDATE & PRE-CHECK  -> Parse arguments, resolve paths, staleness check
1. EXTRACT TASK           -> Read story Section 8, extract task definition
2. MAP TDD CYCLES         -> Generate TDD cycles in TPP order
3. ANALYZE FILES          -> Identify affected files by architecture layer
4. SECURITY CHECKLIST     -> Generate security items based on task type
5. WRITE PLAN             -> Assemble and write task-plan-XXXX-YYYY-NNN.md
```

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

### Phase 1 -- Extract Task Definition

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

### Phase 2 -- Map TDD Cycles (TPP Order)

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

### Phase 5 -- Write Plan

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

## Anti-Patterns

- Do NOT write implementation code -- only plan the approach
- Do NOT skip degenerate cases in TDD cycles (Cycle 1 is ALWAYS degenerate)
- Do NOT generate security items unrelated to the task type
- Do NOT list files outside the task scope
- Do NOT include cycles beyond what the task complexity requires (CRUD tasks need 3-4 cycles, not 8)
- Do NOT ignore the task's layer assignment -- files must align with the declared layer
- Do NOT generate cycles in non-TPP order (never start with complex cases)

## Error Handling

| Scenario | Action |
|----------|--------|
| No story ID provided | Prompt: `"Usage: /x-plan-task [STORY-ID] --task [TASK-ID] [--force]"` |
| No task ID provided | Abort: `"--task flag is required. Provide a TASK-XXXX-YYYY-NNN identifier."` |
| Story file not found | Abort: `"Story file not found at {path}"` |
| Section 8 not found | Abort: `"Section 8 (Tasks) not found in story {story-id}"` |
| Task ID not in Section 8 | Abort: `"Task {task-id} not found in story {story-id} Section 8"` |
| Plan exists and fresh | Return existing: `"Task plan already exists and is up-to-date"` |
| Plan exists with --force | Regenerate: `"Regenerating task plan (--force)"` |
| Epic directory not found | Abort: `"Epic directory not found for epic-XXXX"` |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-story-plan` | complementary | x-story-plan generates task breakdown; x-plan-task generates per-task execution plans |
| `x-dev-lifecycle` | called-by | Phase 2 (PRE_PLANNED mode) reads task plans to drive implementation |
| `x-dev-implement` | consumed-by | Task plans serve as implementation guides for the developer |
| `x-test-plan` | complementary | x-test-plan covers story-level tests; x-plan-task maps per-task TDD cycles |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| testing | `skills/testing/SKILL.md` | TDD patterns, TPP levels, test naming conventions |
| architecture | `skills/architecture/SKILL.md` | Layer definitions, package structure, dependency rules |
| security | `skills/security/SKILL.md` | OWASP Top 10, security checklist items |
| coding-standards | `skills/coding-standards/SKILL.md` | {{LANGUAGE}} conventions, naming, SOLID principles |
