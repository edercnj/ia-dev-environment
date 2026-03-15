# Implementation Plan -- STORY-0003-0008: x-lib-task-decomposer -- Test-Driven Task Decomposition

## 1. Affected Layers and Components

This story modifies **template content only** -- no TypeScript source code changes are required. The assembler already copies lib skills correctly (verified in `skills-assembler.test.ts` line 199-205 and `github-skills-assembler.test.ts` line 413-486). The change is purely to the Markdown template content of `x-lib-task-decomposer`.

### Template Files (Source of Truth -- RULE-002)

| File | Action | Description |
|------|--------|-------------|
| `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md` | **MODIFY** | Primary source -- .claude and .agents targets |
| `resources/github-skills-templates/lib/x-lib-task-decomposer.md` | **MODIFY** | GitHub copy -- .github target |

### Golden Files (8 profiles x 3 targets = 24 files)

Each profile has the task-decomposer golden file in three locations:

| Target | Path Pattern |
|--------|-------------|
| `.claude` | `tests/golden/{profile}/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| `.github` | `tests/golden/{profile}/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| `.agents` | `tests/golden/{profile}/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |

**Profiles:** go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs

### Downstream Template References (NO changes needed, but must remain compatible)

| File | Reference | Impact |
|------|-----------|--------|
| `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Phase 1C invokes `x-lib-task-decomposer`, Phase 2 consumes `tasks-story-XXXX-YYYY.md` with "Implement groups G1-G7" | **Phase 2 references G1-G7 explicitly** -- backward compat required |
| `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Same references as above for GitHub copy | Same impact |

### Test Files (NO source changes needed)

| File | Relevance |
|------|-----------|
| `tests/node/assembler/skills-assembler.test.ts` | Validates lib skill copying -- already works for task-decomposer |
| `tests/node/assembler/github-skills-assembler.test.ts` | Validates GitHub lib skill copying -- already works |
| `tests/node/integration/byte-for-byte.test.ts` | Golden file parity test -- will validate new content automatically |

## 2. New Content to Add

### 2.1 Test-Driven Decomposition Logic (New Primary Mode)

Add a new section **before** the existing Layer Task Catalog that introduces the test-driven decomposition approach. This becomes the **primary** decomposition mode when a test plan exists.

**New Sections to Add:**

#### A. Updated Purpose

```
Decomposes an implementation plan into granular tasks. When a test plan exists (from x-test-plan),
derives tasks from test scenarios using TDD structure (RED/GREEN/REFACTOR). Falls back to the
Layer Task Catalog (G1-G7) when no test plan is available.
```

#### B. New Input: Test Plan (Optional)

```
3. `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` -- Test plan (from x-test-plan) [OPTIONAL]
```

#### C. New STEP 1.5: Detect Test Plan

Between existing STEP 1 (Read Story Context) and STEP 2 (Identify Affected Layers), insert:

```
### STEP 1.5 -- Detect Decomposition Mode

Check if test plan exists at `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md`:
- **If exists:** Use TEST-DRIVEN MODE (Step 2A). Derive tasks from test scenarios.
- **If absent:** Use LAYER-BASED MODE (Step 2B-5). Fall back to Layer Task Catalog (G1-G7).
  Emit warning: "No test plan found. Falling back to layer-based decomposition (G1-G7)."
```

#### D. New STEP 2A: Test-Driven Decomposition

```
### STEP 2A -- Test-Driven Decomposition (Primary Mode)

For each test scenario in the test plan (ordered by TPP level):

1. **Map scenario to task**: One task per UT/AT scenario
2. **Identify layer components**: Which layers does this scenario touch?
3. **Determine dependencies**: Which tasks must complete before this one?
4. **Assess parallelism**: Can this task run concurrently with others?
5. **Assign tier**: Based on the highest-complexity layer touched

#### Task Structure (TDD)

Each generated task MUST contain:

| Field | Required | Description |
|-------|----------|-------------|
| Test Scenario | M | Reference to UT-N or AT-N from test plan |
| TPP Level | M | 1-7 from TPP ordering |
| RED | M | What the test asserts (expected behavior) |
| GREEN | M | Minimum implementation to make test pass |
| REFACTOR | O | Refactoring opportunities after green |
| Layer Components | M | Which layers/classes are touched |
| Parallel | M | yes/no -- can run concurrently with other tasks |
| Depends On | M | List of prerequisite TASK-N references |
| Tier | M | Junior/Mid/Senior based on complexity |
| Budget | M | S/M/L context budget |

#### Parallelism Detection

A task is parallelizable when ALL of:
1. Its test scenario operates on different layers than concurrent tasks
2. No shared state with concurrent tasks
3. No dependency on output of concurrent tasks

#### Ordering Rules

1. Unit test tasks ordered by TPP level (degenerate first, complex last)
2. Within same TPP level, inner layers before outer layers
3. Acceptance test tasks ALWAYS come after ALL related unit test tasks
4. Tasks with dependencies come after their prerequisites

#### Task Type Classification

| Scenario Type | Task Marker | Dependency Rule |
|--------------|-------------|-----------------|
| UT (Unit Test) | `[UT]` | Depends on earlier TPP-level tasks that create shared components |
| AT (Acceptance Test) | `[AT]` | Depends on ALL related UT tasks |
| IT (Integration Test) | `[IT]` | Depends on UT tasks for involved components |
```

#### E. New Output Format Section: TDD Tasks

```
## TDD Task Output Format

When using test-driven mode, generate tasks in this format:

### TASK-N: [UT-X/AT-X] scenario-name
- **TPP Level:** N
- **Type:** UT | AT | IT
- **Tier:** Junior | Mid | Senior
- **Budget:** S | M | L
- **Parallel:** yes | no
- **Depends On:** TASK-N, TASK-M (or "none")

**RED:** [What the test asserts -- expected behavior]

**GREEN:** [Minimum implementation steps]
- Layer: component to create/modify
- Layer: component to create/modify

**REFACTOR:** [Optional refactoring opportunities]

**Layer Components:**
- domain.model: EntityName
- domain.port: PortName
- adapter.outbound: RepositoryName
```

### 2.2 Updated YAML Frontmatter (description field)

Update the `description` field in the YAML frontmatter to reflect the dual-mode capability:

**Claude/Agents copy:**
```yaml
description: "Decomposes an implementation plan into tasks. Primary mode: derives tasks from test scenarios (x-test-plan output) using TDD structure (RED/GREEN/REFACTOR). Fallback mode: uses Layer Task Catalog (G1-G7) when no test plan exists."
```

**GitHub copy:**
```yaml
description: >
  Decomposes an implementation plan into tasks. Primary mode: derives tasks
  from test scenarios (x-test-plan output) using TDD structure (RED/GREEN/REFACTOR).
  Fallback mode: uses Layer Task Catalog (G1-G7) when no test plan exists.
  Reference: `.github/skills/lib/x-lib-task-decomposer/SKILL.md`
```

### 2.3 Updated Title

Change from:
```
# Skill: Task Decomposer (Layer-Based)
```
To:
```
# Skill: Task Decomposer (Test-Driven + Layer Fallback)
```

## 3. Existing Content to Modify

### 3.1 Preserve Layer Task Catalog as Fallback (RULE-003)

The entire existing Layer Task Catalog section (G1-G7) and Layer Dependency Graph MUST be **preserved** but repositioned under a clearly labeled fallback section:

```
## Fallback: Layer Task Catalog (G1-G7)

> **When to use:** Only when no test plan exists at
> `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md`.
> Prefer test-driven decomposition when test plan is available.

[... entire existing catalog preserved verbatim ...]
```

### 3.2 Procedure Steps -- Reorganized

The existing STEP 2 through STEP 5 become the "Layer-Based Mode" path:

| Current Step | New Location | Change |
|-------------|-------------|--------|
| STEP 0 (Read Architecture) | STEP 0 (unchanged) | No change |
| STEP 1 (Read Story) | STEP 1 (unchanged) | Add test plan to inputs |
| -- | **STEP 1.5 (NEW)** | Mode detection |
| STEP 2 (Identify Layers) | STEP 2B (Layer mode) | Relabeled, scoped to fallback |
| STEP 3 (Apply Catalog) | STEP 3B (Layer mode) | Relabeled, scoped to fallback |
| STEP 4 (Tier Decision) | STEP 4B (Layer mode) | Relabeled, scoped to fallback |
| STEP 5 (Generate Output) | STEP 5 (unified) | Accepts output from either mode |
| -- | **STEP 2A (NEW)** | Test-driven decomposition (primary) |

### 3.3 Context Budget Sizes -- Retained

No changes. The existing budget table applies to both modes.

### 3.4 Review Tier Assignment -- Retained

No changes. Tier assignment rules apply regardless of decomposition mode.

### 3.5 Escalation Rules -- Retained

No changes.

### 3.6 Integration Notes -- Updated

Add reference to test plan consumption:

```
- Invoked by `x-dev-lifecycle` during Phase 1C
- Consumes test plan from `x-test-plan` (Phase 1B output) when available
- Output consumed by Phase 2 (group-based or TDD-based implementation)
- Works with any layered architecture -- layer names derived from project rules
- When test plan present: generates TDD tasks with RED/GREEN/REFACTOR structure
- When test plan absent: generates layer-based tasks using G1-G7 catalog (backward compatible)
```

## 4. Dependency Direction Validation

### x-test-plan Output Format (Input to Task Decomposer)

The x-test-plan produces `tests-story-XXXX-YYYY.md` with this structure:

```
# Test Plan -- STORY-ID: [Title]
## Summary
## Test Class 1: [ClassNameTest]
### Happy Path
| # | Method | Test Name | Description |
### Error Path
| # | Exception | Test Name | Trigger |
### Boundary
| # | Boundary | Test Name | Values Tested |
### Parametrized
| # | Matrix | Test Name | Source | Rows |
## Coverage Estimation
```

**Compatibility assessment:** The task decomposer needs to parse test scenarios from this output. The current x-test-plan format organizes by test class and category (Happy/Error/Boundary/Parametrized), NOT by TPP level. Once story-0003-0007 is implemented, the x-test-plan will add TPP ordering.

**Action required:** The task decomposer must handle BOTH formats:
1. **Post story-0003-0007 format**: Scenarios ordered by TPP with explicit UT-N/AT-N markers and TPP level annotations
2. **Pre story-0003-0007 format**: Scenarios organized by category -- task decomposer should still be able to derive tasks (using category as grouping heuristic), or fall back to G1-G7

This means the test plan detection in STEP 1.5 should check for TPP markers, not just file existence:
- File exists AND has TPP markers --> Test-driven mode
- File exists but NO TPP markers --> Layer-based mode with warning
- File absent --> Layer-based mode with warning

## 5. Integration Points

### 5.1 x-dev-lifecycle Phase 1C (Invocation)

**Current:** Phase 1C invokes `x-lib-task-decomposer` which receives only the architect's plan.

**After:** Phase 1C invokes `x-lib-task-decomposer` which ALSO reads the test plan from Phase 1B. Since Phase 1B (test plan) and Phase 1C (task decomposer) are launched in parallel in the current lifecycle, there is a **sequencing concern**.

**Resolution:** The task decomposer already handles the "no test plan" case via fallback. In practice:
- If 1B and 1C run in parallel: task decomposer falls back to G1-G7 (test plan not yet written)
- If 1C runs after 1B: task decomposer uses test-driven mode

**No changes to x-dev-lifecycle are required for this story.** The lifecycle may be updated in story-0003-0014 to sequence 1B before 1C. For now, the fallback mechanism in the task decomposer handles the race condition gracefully.

### 5.2 x-dev-lifecycle Phase 2 (Consumption)

**Current:** Phase 2 developer subagent reads `tasks-story-XXXX-YYYY.md` and implements "groups G1-G7."

**After:** The output file path remains `tasks-story-XXXX-YYYY.md`. The content may be in either format:
- TDD tasks (TASK-1, TASK-2, ... with RED/GREEN/REFACTOR)
- Layer tasks (G1-G7 groups)

**Phase 2 compatibility:** The developer subagent reads the task file as instructions. Both formats are human-readable Markdown that the developer subagent can follow. The explicit "Implement groups G1-G7" instruction in Phase 2 will work because:
- In fallback mode: G1-G7 groups are present
- In TDD mode: tasks are still grouped by dependency order, and the developer can follow TASK-N sequence

**Story-0003-0012** will later update x-dev-implement and x-dev-lifecycle Phase 2 to explicitly understand TDD task format.

### 5.3 x-dev-implement (Standalone)

x-dev-implement does NOT reference `x-lib-task-decomposer` directly (confirmed by grep). No changes needed.

## 6. RULE-001: Dual Copy Consistency

### Copy Mapping

| Source (resources/) | Target | Differences |
|---------------------|--------|-------------|
| `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md` | `.claude/skills/lib/x-lib-task-decomposer/SKILL.md` | Identical content (source of truth) |
| `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md` | `.agents/skills/lib/x-lib-task-decomposer/SKILL.md` | Identical content (source of truth) |
| `resources/github-skills-templates/lib/x-lib-task-decomposer.md` | `.github/skills/lib/x-lib-task-decomposer/SKILL.md` | Different frontmatter format, different path references |

### Key Differences Between Copies

| Aspect | Claude/Agents Copy | GitHub Copy |
|--------|-------------------|-------------|
| YAML frontmatter | `allowed-tools`, `argument-hint` fields | `description` with `Reference:` line |
| Architecture path | `skills/architecture/references/architecture-principles.md` | `.github/skills/architecture/SKILL.md` |
| Layer templates path | `skills/layer-templates/SKILL.md` | `.github/skills/layer-templates/SKILL.md` |
| Test plan path | `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | Same (shared docs/) |
| Integration note | Full detail with escalation, budgets, review tiers | Abbreviated with `Reference:` pointer |
| Content sections | Full (all sections including Context Budget, Review Tier, Escalation) | Abbreviated (catalog, graph, integration notes only) |

### Implementation Order

1. Modify `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md` (full version)
2. Derive `resources/github-skills-templates/lib/x-lib-task-decomposer.md` (abbreviated version with GitHub path references)
3. Regenerate golden files

## 7. Golden File Update Strategy

### 7.1 Scope

24 golden files must be updated (8 profiles x 3 targets):

| Profile | `.claude` | `.github` | `.agents` |
|---------|-----------|-----------|-----------|
| go-gin | update | update | update |
| java-quarkus | update | update | update |
| java-spring | update | update | update |
| kotlin-ktor | update | update | update |
| python-click-cli | update | update | update |
| python-fastapi | update | update | update |
| rust-axum | update | update | update |
| typescript-nestjs | update | update | update |

### 7.2 Content Per Target

| Target | Content Source | Notes |
|--------|--------------|-------|
| `.claude` golden | Copy of `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md` | Verbatim after template engine processing |
| `.agents` golden | Same as `.claude` | Identical content |
| `.github` golden | Copy of `resources/github-skills-templates/lib/x-lib-task-decomposer.md` | Different frontmatter, abbreviated |

### 7.3 Update Procedure

1. Modify the 2 source templates
2. Run the pipeline for each profile: `npx ts-node src/cli.ts generate --config resources/config-templates/setup-config.{profile}.yaml --output tests/golden/{profile}/`
3. OR: manually copy the processed output to golden dirs (since task-decomposer has no template variables like `{{LANGUAGE}}`)
4. Verify with `npm test` -- the byte-for-byte test will catch any mismatches

### 7.4 Template Variable Check

The current task-decomposer template contains **no** `{{PLACEHOLDER}}` variables. This means the content is identical across all 8 profiles for `.claude` and `.agents` targets. For `.github`, the content is also profile-independent. This simplifies the update: same content copied to all 24 golden files.

**Verification:** Confirmed by reading both template files -- no `{{...}}` placeholders found. The template engine will pass content through unchanged.

## 8. Backward Compatibility (RULE-003)

### 8.1 G1-G7 Preserved as Fallback

The Layer Task Catalog (G1-G7) is NOT removed. It is:
- Repositioned under a "Fallback" heading
- Activated when no test plan with TPP markers is detected
- Fully functional with identical behavior to current version

### 8.2 Output File Path Unchanged

`docs/stories/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` -- no path change.

### 8.3 x-dev-lifecycle Compatibility

Phase 2 references "Implement groups G1-G7" -- this continues to work in fallback mode. In TDD mode, the developer subagent receives TDD tasks which are self-explanatory. No x-dev-lifecycle changes required.

### 8.4 Existing Consumers

| Consumer | Impact | Mitigation |
|----------|--------|------------|
| x-dev-lifecycle Phase 1C | Invocation unchanged | No change needed |
| x-dev-lifecycle Phase 2 | Reads task file | Both formats are valid Markdown instructions |
| x-dev-implement | Does not use task decomposer | No impact |
| x-review | Does not read task file | No impact |

### 8.5 Three-Level Fallback Strategy

1. **Test plan with TPP markers present** --> Test-driven mode (new)
2. **Test plan present but no TPP markers** --> Layer-based mode with warning (graceful degradation)
3. **No test plan file** --> Layer-based mode with warning (current behavior preserved)

## 9. Risk Assessment

### 9.1 Low Risk

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden file mismatch | High (expected) | Low | Run pipeline, update 24 golden files, byte-for-byte test catches errors |
| Dual copy drift | Medium | Medium | Update both copies in same commit, review side-by-side |
| Template variable regression | Low | Low | No template variables in this skill -- content is static |

### 9.2 Medium Risk

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Phase 1B/1C race condition | High | Medium | Fallback mechanism handles it; documented for story-0003-0014 |
| Phase 2 confused by TDD format | Medium | Medium | TDD tasks are self-documenting; story-0003-0012 will update Phase 2 |
| Test plan format mismatch | Medium | Medium | Three-level fallback strategy; TPP marker detection, not just file existence |

### 9.3 High Risk

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| None identified | -- | -- | -- |

### 9.4 Implementation Complexity

**Estimated effort: Small-Medium.** This is a template content change (2 Markdown files + 24 golden files). No TypeScript source code, no assembler logic, no new tests beyond golden file updates. The byte-for-byte integration test automatically validates all 24 golden files.

## 10. Implementation Order

| Step | Action | Files |
|------|--------|-------|
| 1 | Modify Claude/Agents template | `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md` |
| 2 | Modify GitHub template | `resources/github-skills-templates/lib/x-lib-task-decomposer.md` |
| 3 | Update `.claude` golden files (8 profiles) | `tests/golden/{profile}/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| 4 | Update `.agents` golden files (8 profiles) | `tests/golden/{profile}/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |
| 5 | Update `.github` golden files (8 profiles) | `tests/golden/{profile}/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| 6 | Run full test suite | `npm test` -- 1,384+ tests, byte-for-byte parity validates all 24 golden files |
| 7 | Verify coverage unchanged | Coverage should remain at 99.6% lines, 97.84% branches |

## 11. Downstream Story Impact

| Story | Dependency | What This Story Enables |
|-------|-----------|------------------------|
| story-0003-0012 (x-dev-implement) | Blocked by this story | Can consume TDD task format for Red-Green-Refactor implementation |
| story-0003-0014 (x-dev-lifecycle) | Blocked by this story | Can sequence Phase 1B before 1C to ensure test plan availability |
