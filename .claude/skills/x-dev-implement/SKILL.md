---
name: x-dev-implement
description: "Implements a feature/story using TDD (Red-Green-Refactor-Commit) workflow. Delegates preparation to a subagent that reads architecture, coding, and test plan KPs, then implements test-first with Double-Loop TDD, layer-by-layer with atomic commits and compile checks after each cycle."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[STORY-ID or feature-description]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Implement Story (Orchestrator)

## When to Use This vs `/x-dev-lifecycle`

| Scenario | Use |
|----------|-----|
| Quick implementation (single class, small fix) | This skill |
| Full story with multi-persona review | `/x-dev-lifecycle` |
| Coding without the review phases | This skill |
| Complete lifecycle: code → review → fix → PR | `/x-dev-lifecycle` |

## Execution Flow (Orchestrator Pattern)

```
1. PREPARE + UNDERSTAND  -> Subagent reads KPs + test plan, produces TDD implementation plan
2. TDD LOOP              -> For each scenario (TPP order): RED -> GREEN -> REFACTOR -> COMMIT -> compile check
3. VALIDATE              -> Coverage thresholds, all acceptance tests GREEN (inline)
```

## Step 1: Prepare + Understand (Subagent via Task)

Launch a **single** `general-purpose` subagent:

> You are a **Senior Developer** preparing an implementation plan for {{PROJECT_NAME}}.
>
> **Step 1 — Read the story/requirements:** `{STORY_PATH_OR_DESCRIPTION}`
> Extract: acceptance criteria, sub-tasks, test scenarios, dependencies.
>
> **Step 2 — Read test plan (MANDATORY):**
> - Look for test plan at `docs/plans/{STORY_ID}-tests.md` or `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md`
> - Extract: acceptance tests (AT-N), unit tests in TPP order (UT-N), integration tests (IT-N)
> - Identify the outer loop (acceptance tests that start RED)
> - Identify the inner loop order (unit tests in TPP sequence: degenerate first, complex last)
> - If NO test plan found: ABORT with message: `"ABORT: No test plan found. TDD is mandatory -- run /x-test-plan first. Implementation cannot proceed without a test plan (Rule 03)."`
>
> **Step 3 — Read project conventions:**
> - `skills/architecture/references/architecture-principles.md` — layer structure, dependency direction
> - `skills/coding-standards/references/coding-conventions.md` — {{LANGUAGE}} coding conventions
> - `skills/coding-standards/references/version-features.md` — {{LANGUAGE}} {{LANGUAGE_VERSION}} idioms
> - `skills/layer-templates/SKILL.md` — code templates per architecture layer (defines implementation order)
>
> **Step 4 — Review existing code** in the target packages to identify patterns to follow.
>
> **Step 5 — Produce implementation plan:**
> 1. Layer-by-layer implementation order (from layer-templates)
> 2. For each layer: classes to create/modify, package location, key patterns
> 3. TDD cycle mapping: which UT-N scenarios apply to which layer
> 4. Acceptance test identification: AT-N entries that validate the feature end-to-end
> 5. Key conventions to follow (naming, immutability, injection style)
> 6. Dependencies to verify before starting
>
> Also create feature branch if not already on one:
> ```bash
> git checkout main && git pull origin main
> git checkout -b feat/STORY-ID-short-description
> ```

## Step 2: TDD Loop — Red-Green-Refactor-Commit (Orchestrator — Inline)

Using the TDD implementation plan returned by the subagent, execute Red-Green-Refactor cycles.

**Layer order is preserved WITHIN each cycle.** When a UT-N test touches domain, that cycle implements domain first, then adapters. Dependencies point inward (domain has no outward dependencies).

### 2.0 Write Acceptance Test First (Double-Loop — Outer Loop)

Before any unit test cycle, write the acceptance test(s) from the test plan (AT-N entries):

1. Write the acceptance test (integration/API/E2E depending on scope)
2. Run it — it MUST be RED (failing)
3. This acceptance test stays RED throughout the inner loop
4. It turns GREEN only after all related unit test cycles complete

```bash
{{TEST_COMMAND}} -- [acceptance-test-file]
# Expected: FAIL (RED)
```

### 2.1 Inner Loop: Red-Green-Refactor per Unit Test (TPP Order)

For each UT-N in the test plan (strict TPP order: degenerate first, edge cases last):

#### RED — Write the Failing Test
1. Write the unit test for UT-N (test name follows `[method]_[scenario]_[expected]`)
2. Run it — it MUST fail
3. If the test passes without new code, the test is wrong or the scenario is already covered

```bash
{{TEST_COMMAND}} -- [test-file]
# Expected: FAIL (RED)
```

#### GREEN — Implement the Minimum
1. Write the MINIMUM production code to make UT-N pass
2. Respect layer order: domain -> ports -> adapters -> application -> inbound
3. Do NOT add code beyond what the test requires
4. Run the test — it MUST pass
5. Run ALL previous tests — they MUST still pass

```bash
{{TEST_COMMAND}}
# Expected: ALL PASS (GREEN)
```

#### REFACTOR — Improve Design
1. Look for: extract method (> 25 lines), duplicate code, unclear naming
2. Refactoring NEVER adds behavior — if behavior changes, write a new failing test first
3. Run all tests after refactoring — they MUST still pass

```bash
{{TEST_COMMAND}}
# Expected: ALL PASS (still GREEN)
```

#### COMMIT — Atomic Commit per TDD Cycle

After each Red-Green-Refactor cycle, commit immediately. Use the commit format decision table below to choose combined or fine-grained format.

**Commit Format Decision Table:**

| Scenario | Format | Rationale |
|----------|--------|-----------|
| Simple TDD cycle (<50 lines changed) | Combined: `feat(scope): implement [behavior] [TDD]` | Lower overhead, acceptable traceability |
| Complex TDD cycle (>=50 lines changed) | Fine-grained: `test(scope): [TDD:RED]` + `feat(scope): [TDD:GREEN]` + `refactor(scope): [TDD:REFACTOR]` | Verifiable test-first pattern in git log |
| Non-trivial refactoring | Separate: `refactor(scope): [TDD:REFACTOR]` | Proves no behavior was added during refactoring |
| Epic execution with TDD gate | Fine-grained (mandatory) | Required for automated integrity gate verification |

**Line threshold:** 50 lines refers to the total lines changed (test + production code) in one complete TDD cycle.

**Fine-grained ordering:** When using fine-grained format, commits MUST appear in this order per cycle:
1. `test(scope): ... [TDD:RED]` — the failing test commit
2. `feat(scope): ... [TDD:GREEN]` — the minimum implementation commit
3. `refactor(scope): ... [TDD:REFACTOR]` — the design improvement commit (omit if refactoring is a noop)

This ordering makes the test-first pattern visible in `git log --oneline`.

**Default (backward compatibility):** When no explicit criterion applies (e.g., stories created before this decision table), use the combined `[TDD]` format.

**Combined format example:**
```bash
git add [test-file-for-UT-N] [implementation-files-for-UT-N]
git commit -m "feat(scope): add [behavior] (UT-N) [TDD]" \
  -m "- RED: [test description]" \
  -m "- GREEN: [minimum implementation]" \
  -m "- REFACTOR: [what was improved, or 'noop']"
```

**Fine-grained format example:**
```bash
# RED
git add [test-file-for-UT-N]
git commit -m "test(scope): add [test description] (UT-N) [TDD:RED]"

# GREEN
git add [implementation-files-for-UT-N]
git commit -m "feat(scope): implement [behavior] (UT-N) [TDD:GREEN]"

# REFACTOR (only if non-trivial changes were made)
git add [refactored-files]
git commit -m "refactor(scope): [what was improved] (UT-N) [TDD:REFACTOR]"
```

For acceptance tests, commit when introduced (RED) and again when they turn GREEN (if updated):

```bash
# RED: introduce failing acceptance test
git add [acceptance-test-file]
git commit -m "test(scope): add acceptance test for [AT-N scenario] [TDD:RED]"

# Later, when the AT turns GREEN and you've updated it if needed:
git add [acceptance-test-file]
git commit -m "test(scope): update acceptance test for [AT-N scenario] [TDD:GREEN]"
```

**Commit ordering must reflect TDD progression:**
1. Acceptance test commit (RED) — first
2. Unit test + implementation commits (UT-1, UT-2, ...) — in TPP order
3. Final commit when AT turns GREEN (if AT content changed)

#### Compile Check
After each complete cycle (Red-Green-Refactor-Commit):

```bash
{{COMPILE_COMMAND}}
# Expected: zero errors, zero warnings
```

### 2.2 Cycle Completion

After all UT-N cycles for a given acceptance test (AT-N) are complete:
1. Run the acceptance test — it should now be GREEN
2. If still RED, identify missing unit test cycles and add them
3. Proceed to the next AT-N

### 2.3 Code Conventions (from subagent's plan)

- Named constants (never magic numbers/strings)
- Methods ≤ 25 lines, classes ≤ 250 lines
- Self-documenting code (comments only for "why")
- Never return null — use Optional/empty types
- Constructor/initializer injection
- Immutable DTOs, value objects, events

## Step 3: Validate (Orchestrator — Inline)

All TDD cycles are complete. Run final validation:

```bash
{{TEST_COMMAND}}
{{COVERAGE_COMMAND}}
```

**Definition of Done:**

| Criterion | Verification |
|-----------|-------------|
| All acceptance tests (AT-N) GREEN | Run full test suite |
| All unit tests (UT-N) GREEN | Run full test suite |
| Line coverage ≥ 95% | Coverage report |
| Branch coverage ≥ 90% | Coverage report |
| Code compiles cleanly | `{{COMPILE_COMMAND}}` with no warnings |
| All tests pass | `{{TEST_COMMAND}}` |
| Tests written BEFORE implementation | Verify test-first pattern in each cycle |
| Refactoring evaluated per cycle | Each cycle has explicit refactor step (even if noop) |
| Thread-safe (if applicable) | No mutable static state |
| Automated test validates primary AC | At least 1 test validates the story's primary acceptance criterion |
| Smoke test passes | `{{SMOKE_COMMAND}}` (if testing.smoke_tests == true) |

## Integration Notes

- **Prerequisite:** Run `/x-test-plan` first to generate the test plan with Double-Loop + TPP ordering
- For the full lifecycle with reviews, use `x-dev-lifecycle` instead
- Invokes patterns from `x-test-run` and `x-git-push` skills
- Works with any {{FRAMEWORK}} project following layered/hexagonal architecture
- The developer agent (typescript-developer) already includes TDD workflow rules (story-0003-0006)
