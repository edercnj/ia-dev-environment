# Schema: `plan-task-TASK-XXXX-YYYY-NNN.md`

> **Status:** Normativo (EPIC-0038 story-0038-0003)
> **Scope:** Output schema for the refactored `x-task-plan` skill. Plans a single task's
> TDD cycles (Red → Green → Refactor) in TPP order, derived from its `task-TASK-NNN.md`
> (story-0038-0001) I/O contracts.
> **Produced by:** `x-task-plan` skill (story-0038-0003, refactor of orphan skill)
> **Consumed by:** `x-task-implement` (story-0038-0005) and reviewers

---

## 1. Filename Convention

| Artifact | Pattern | Regex | Example |
| :--- | :--- | :--- | :--- |
| Plan (production) | `plan-task-TASK-XXXX-YYYY-NNN.md` | `^plan-task-TASK-\d{4}-\d{4}-\d{3}\.md$` | `plan-task-TASK-0038-0003-001.md` |
| Plan (documentation fixture) | `plan-task-TASK-XXXX-YYYY-EXAMPLE.md` | `^plan-task-TASK-\d{4}-\d{4}-(\d{3}\|EXAMPLE)\.md$` | `plan-task-TASK-0038-0003-EXAMPLE.md` |

> Production plans MUST use the numeric `NNN` suffix. Documentation fixtures under
> `plans/epic-XXXX/examples/` MAY use the literal `EXAMPLE` suffix to signal schema
> demonstration files. The skill consumes and the validator checks only the numeric pattern.

## 2. Required Sections (Order Significant)

| # | Section | Required | Format |
| :--- | :--- | :--- | :--- |
| 1 | `# Task Implementation Plan — TASK-XXXX-YYYY-NNN` | Yes | H1 markdown |
| 2 | `## 1. Resumo` | Yes | Objetivo + Testabilidade copied from task file |
| 3 | `## 2. Red-Green-Refactor Cycles (TPP Order)` | Yes | Ordered table |
| 4 | `## 3. File Impact Analysis` | Yes | Table `\| Cycle \| Layer \| Files (new/modified) \|` |
| 5 | `## 4. TPP Justification` | Yes | Free text ≤ 10 lines explaining chosen order |
| 6 | `## 5. Exit Criteria` | Yes | Checklist |

## 3. Section Details

### 3.1 Red-Green-Refactor Cycles (TPP Order)

Ordered list of TDD cycles following the Transformation Priority Premise:

| Order | TPP Level | Transform | Description |
|-------|-----------|-----------|-------------|
| 1 | Degenerate | `{} → nil` | Null/empty/zero inputs return default or error |
| 2 | Constant | `nil → constant` | Single hardcoded return value |
| 3 | Scalar | `constant → variable` | Parameterized return based on input |
| 4 | Conditional | `unconditional → conditional` | Single if/else branching |
| 5 | Collection | `scalar → collection` | Multiple items, iteration, map/filter |
| 6 | Complex | `collection → complex` | Compound logic, nested conditions, state |

Each cycle MUST have:
- **Red**: failing test name (`methodUnderTest_scenario_expectedBehavior`) + assertion summary
- **Green**: minimum code to make the test pass
- **Refactor**: explicit criterion (extract method / eliminate duplication / rename) OR `None at this cycle`

The FIRST cycle is always degenerate. Minimum 3 cycles per plan.

### 3.2 File Impact Analysis

Per-cycle enumeration of touched files, partitioned by architecture layer:

| Cycle | Layer | Files (new/modified) |
| :--- | :--- | :--- |
| 1 | Domain | `Foo.java` (new), `FooTest.java` (new) |
| 2 | Domain | `Foo.java` (modified) |

Layers in order: Domain → Port → Adapter → Application → Config → Test.

### 3.3 TPP Justification

Free-text paragraph (≤ 10 lines) explaining why this particular cycle order was chosen —
for example, why a COALESCED task's cycles share state with its partner, or why a
particular edge case is deferred to the final cycle.

### 3.4 Exit Criteria

Standard DoD checklist for the task:

- [ ] All cycles in section 2 show GREEN with a passing test
- [ ] Refactor step applied where noted
- [ ] `mvn clean verify` / build green
- [ ] Commits follow Conventional Commits with TDD tags `[TDD:RED|GREEN|REFACTOR]`
- [ ] Contracts I/O from task file respected (verified by grep/assert)
- [ ] Atomic commit per cycle (RULE-TF-04)

## 4. Exit Codes (skill invocation)

| Exit Code | Condition | Message |
| :--- | :--- | :--- |
| 0 | Plan generated | `Plan written to {path}` |
| 1 | `--task-file` missing or invalid | `Task file invalid: {violations}` |
| 2 | `--output-dir` not writable | `Output dir not writable: {path}` |
| 3 | Testability not declared (RULE-TF-01) | `Testability not declared (RULE-TF-01)` |

## 5. Cross-cutting Rules

| Rule | How the schema enforces |
| :--- | :--- |
| RULE-TF-01 (Task Testability) | Plan header copies §2.3 testability kind; exit code 3 if absent |
| RULE-TF-02 (I/O Contracts) | §3 File Impact derives from task file's §2.2 Outputs |
| RULE-TF-04 (Atomic Commits) | §5 Exit Criteria requires commit per cycle |
