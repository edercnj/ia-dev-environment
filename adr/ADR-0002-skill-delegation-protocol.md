---
status: Accepted
date: 2026-04-10
deciders:
  - Eder Celeste Nunes Junior
story-ref: "EPIC-0033"
---

# ADR-0002: Skill Delegation Protocol (Rule 13)

## Status

Accepted | 2026-04-10

## Context

The `skills/core/` templates in `java/src/main/resources/targets/claude/` define an orchestration chain where one skill delegates work to another: `x-dev-epic-implement` → `x-dev-story-implement` → `x-test-tdd` → `x-git-commit` → `x-code-format`/`x-code-lint`, plus `x-dev-story-implement` → `x-review` → 9 parallel specialist reviewers, plus Phase 1 planners (`x-dev-architecture-plan`, `x-test-plan`, `x-lib-task-decomposer`, `x-threat-model`) and Phase 3 finishers (`x-dev-arch-update`, `x-review-pr`).

Before EPIC-0033, this chain used three incompatible conventions for delegation, often mixed within the same file:

1. **Bare-slash text** — `Invoke /x-foo with args` or `/x-foo TASK-ID` inside a code block. The Claude runtime does NOT execute this as a tool call; the LLM has to guess whether to dispatch via the `Skill` tool or read-and-follow the referenced skill inline. This guessing caused silent delegation failures.
2. **Imperative prose without explicit tool call** — `Invoke skill x-test-plan` or `Launch general-purpose subagent: > prompt`. The LLM had to translate prose into an actual `Skill(...)` or `Agent(...)` tool call at runtime, with no guarantee of consistency.
3. **Missing `allowed-tools` entries** — `x-test-tdd` and `x-git-commit` lacked `Skill` in their `allowed-tools` frontmatter. Even if the body text had pointed to `Skill(...)`, the runtime would have rejected the call. Similarly, `x-dev-epic-implement` and `x-dev-story-implement` launched subagents via `Agent()` but lacked `Agent` in `allowed-tools`, relying on implicit availability.

A diagnostic audit during EPIC-0033 identified **13 logical delegation routes across 7 files (24 physical locations)** using the bare-slash anti-pattern, plus 4 additional routes using the imperative-prose variant, plus 1 orphan reference to a non-existent skill (`/x-test-contract-lint`). The audit also found that `x-test-tdd` relied on an **implicit "invoked via Skill tool" detection** to decide whether to emit compact-vs-full output — a check the skill cannot reliably perform from inside its own execution context.

The `x-review` Phase 2 dispatch block listed 9 specialist skills as bullet points (`/x-review-qa {STORY_ID}`, `/x-review-perf {STORY_ID}`, etc.), which the LLM was supposed to interpret as "emit 9 `Skill(...)` calls as siblings in one message for true parallelism". In practice this intent was lost — the bullets read as a sequential list, and the LLM would sometimes execute them one at a time, serializing the review.

The absence of `TaskCreate`/`TaskUpdate` in any orchestrator made the entire multi-minute execution invisible in the Claude Code task list: users saw an empty panel for 30+ minutes and had to read raw log output to know what was happening.

## Decision

Establish **Rule 13 — Skill Invocation Protocol** (`.claude/rules/13-skill-invocation-protocol.md`) as the canonical reference for all skill-to-skill delegation. Rule 13 defines three permitted patterns and forbids the bare-slash anti-pattern in delegation contexts.

### Permitted patterns

**1. INLINE-SKILL (preferred for direct delegation)**

Use when the caller wants a synchronous call to another skill and will act on its return value directly.

```markdown
Invoke the `x-foo` skill via the Skill tool:

    Skill(skill: "x-foo", args: "--flag value --other thing")
```

**Requirement:** the caller's `allowed-tools` MUST include `Skill`.

**2. SUBAGENT-GENERAL (isolated general-purpose subagent)**

Use when the caller needs context isolation — parallel work, avoiding context-window pollution, or multiple independent workers in one message. The subagent may (a) invoke another skill via `Skill(...)` from its own context, (b) produce an artifact without calling any skill, or (c) perform complex analysis and return a report. All three use the same call shape:

```markdown
Agent(
  subagent_type: "general-purpose",
  description: "<short 3-7 word summary>",
  prompt: "<multi-line prompt with FIRST ACTION TaskCreate + task body + LAST ACTION TaskUpdate>"
)
```

**Requirement:** the caller's `allowed-tools` MUST include `Agent`.

**3. SUBAGENT-RESEARCH (no Skill call, pure exploration)**

Use when the caller needs investigation that does NOT require invoking another skill. The subagent reads, greps, and reports back in natural language:

```markdown
Agent(
  subagent_type: "Explore",
  description: "Find all references to pattern X",
  prompt: "Search the codebase for {pattern} and report file:line matches. Do not modify anything."
)
```

### Forbidden pattern

The bare-slash form `Invoke /x-foo`, `/x-foo TASK-ID`, or bullet lists of slash commands in delegation contexts is **forbidden**. The exception is user-facing `## Triggers` and `## Examples` sections, where `/x-foo` is what the user literally types in chat.

### Parallelism + tracking batching

To preserve SINGLE-message parallelism while tracking per-planner/per-specialist progress, use the Batch A / Batch B pattern (documented in `x-dev-story-implement` "Phases 1B-1F Parallel Planning" and `x-review` Phase 2):

1. **Batch A — first assistant message:** emit all `TaskCreate(...)` calls + all `Skill(...)` or `Agent(...)` invocations as SIBLING tool calls in the same message. The runtime dispatches siblings in parallel. Record returned task IDs in an in-memory map indexed by planner/specialist name.
2. **Wait for all tool calls to return** (runtime handles this).
3. **Batch B — second assistant message:** emit all `TaskUpdate(...)` calls as siblings, closing each tracked task.

### Observability via `TaskCreate`/`TaskUpdate`

Level 2 visibility: `x-dev-epic-implement` creates one task per story dispatch; `x-dev-story-implement` creates one task per phase (0, 1, 2, 3) + one task per task-execution inside Phase 2.

Level 3 visibility: `x-review` creates one task per active specialist (9 possible: qa, perf, db, obs, devops, data-modeling, security, api, events); `x-dev-story-implement` Phase 1 creates one task per planner (1A architecture, 1B impl plan, 1B test plan, 1C task decomposer, 1D event schema, 1E security + fallback, 1F compliance).

Orchestrator-managed planners (invoked via `Skill(...)`): the parent orchestrator wraps the call with `TaskCreate` before + `TaskUpdate` after. Subagent-managed planners (launched via `Agent(...)`): the subagent prompt receives `FIRST ACTION` (emit `TaskCreate`) and `LAST ACTION` (emit `TaskUpdate`) instructions so it tracks its own lifecycle from inside.

Skipped planners (pre-check marked "Reuse", or a conditional activation evaluates false) do **NOT** emit `TaskCreate` — the task list only shows work that is actually being performed.

### Explicit `--orchestrated` flag in `x-test-tdd`

`x-test-tdd` previously decided compact-vs-full output format via an implicit "invoked via Skill tool by another skill" detection. This detection is fragile: a skill cannot reliably determine how it was invoked from inside its own execution context. Replaced with an explicit `--orchestrated` flag passed by the parent orchestrator:

```
if args.contains("--orchestrated"):
    emit_format = "compact"
else:
    emit_format = "full"
```

`x-dev-story-implement` Phase 2 step 2.2.5 passes `--orchestrated`; direct user invocations (`/x-test-tdd TASK-ID`) omit the flag.

### `TaskUpdate` convention

`TaskUpdate` always uses `status: "completed"` for UI visibility. The authoritative success/fail verdict stays in `execution-state.json` (per CR-04 of EPIC-0033). Failed/blocked terminal states are signaled by prefixing the task description with `"(FAILED) "` or `"(BLOCKED) "` via a preceding `TaskUpdate` before marking completed.

## Consequences

### Positive

- **Deterministic delegation.** Every skill-to-skill call is now an explicit `Skill(...)` or `Agent(...)` tool call, not prose. The LLM no longer guesses how to dispatch. Audit grep of `core/**/SKILL.md` shows 0 bare-slash matches in delegation contexts (down from 13 logical routes / 24 physical locations pre-EPIC-0033).
- **Runtime observability.** Users see ~13 + N tasks per story execution in the Claude Code task list (N = tasks in Phase 2 inner loop). `x-review` run directly surfaces up to 9 per-specialist tasks. Multi-minute executions are no longer black boxes.
- **Preserved parallelism.** Batch A / Batch B pattern keeps Phase 1B-1F (5 planners) and `x-review` Phase 2 (up to 9 specialists) executing as siblings in single messages, matching the original parallelism intent. Per-planner/per-specialist tracking is additive, not a serialization.
- **Single source of truth for delegation syntax.** Rule 13 is referenced 30+ times across `core/` skills. New skills that need to delegate have a canonical pattern to follow.
- **Deterministic mode selection in `x-test-tdd`.** The explicit `--orchestrated` flag replaces the fragile implicit detection, making the compact-vs-full decision testable and predictable.
- **`allowed-tools` are now explicit about `Skill`, `Agent`, `TaskCreate`, and `TaskUpdate`.** No more silent reliance on implicit runtime availability.

### Negative

- **Skill templates are more verbose.** Each delegation now includes the `Skill(...)` / `Agent(...)` call, the surrounding prose, and (where applicable) `TaskCreate`/`TaskUpdate` scaffolding. Rule 13 references add boilerplate.
- **Two-file sync burden.** Several Phase 1 and Phase 3 instructions exist in both `x-dev-story-implement/SKILL.md` and its `references/*.md` includes. Changes to delegation syntax in one file must be mirrored in the other. Mitigated by auditing during EPIC-0033 (Story 0001 T-005h/i sync edits, Story 0003 drift fix for Step 1E).
- **Rule 13, not Rule 10.** Conditional rule slots 10 (`10-anti-patterns`), 11 (`11-security-pci`), and 12 (`12-security-anti-patterns`) were already in use by per-profile conditional rules, so the new rule took slot 13. The `CLAUDE.md` rules index carries a note explaining the gap.
- **Discipline required when adding new skills.** A contributor writing a new skill MUST read Rule 13 before adding any delegation. The grep audit (`grep -rnE "Invoke\\s+\`?/?x-[a-z-]+" core/ --include=SKILL.md --include="*.md"`) should be added to CI to catch regressions.

### Neutral

- **`x-review` now has `TaskCreate`/`TaskUpdate` in `allowed-tools`.** It didn't need them before (it only dispatched Skills); adding them was necessary for per-specialist Level 3 tracking.
- **`Agent` added to `allowed-tools` of both orchestrators.** They were already using `Agent()` implicitly before EPIC-0033; the addition makes the contract explicit and aligns with Rule 13 Pattern 2 requirement. No runtime behavior change — the Claude Code runtime appears to allow `Agent` even without the explicit declaration.
- **Level 4 tracking (per-TDD-cycle inside `x-test-tdd`) was deliberately deferred.** Each TDD cycle (RED/GREEN/REFACTOR) produces a commit but no task entry. A future epic can add per-cycle `TaskCreate` inside `x-test-tdd` if cycle-level visibility becomes important.
- **`targets/github-copilot/` and `targets/codex/` were out of scope for Rule 13 enforcement.** GitHub Copilot uses bare-slash syntax natively (equivalent to `## Triggers`), and Codex does not host `.md` skills in the same structure. PR #260 (EPIC-0034) subsequently removed these targets altogether; Rule 13 now applies only to `targets/claude/`.

## Related ADRs

- **ADR-0001 — Hexagonal Architecture Migration** — Rule 13 operates at the template layer (`skills/core/**`) and is orthogonal to the Java hexagonal layers. The skill templates are generator inputs, not runtime business logic.

## Story Reference

- **EPIC-0033** — Fix Skill Delegation Chain and Subagent Observability
  - **STORY-0033-0001** — Add `Skill` to `allowed-tools` and standardize 13 delegation routes (PR #257)
  - **STORY-0033-0002** — `TaskCreate`/`TaskUpdate` Level 2 visibility (PR #258)
  - **STORY-0033-0003** — Planning subagent visibility + explicit `--orchestrated` flag (PR #259)
  - **Audit concerns resolution** — `Agent` in `allowed-tools`, explicit `Agent(...)` calls for Phase 1 planners, per-specialist tracking in `x-review` (PR #261)
  - **STORY-0033-0004** — Consolidate `x-dev-lifecycle` → `x-dev-story-implement` — **VOID** (done by EPIC-0032)
