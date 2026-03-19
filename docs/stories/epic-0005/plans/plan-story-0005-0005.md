# Implementation Plan -- story-0005-0005: Orchestrator Core Loop + Sequential Dispatcher

**Architecture Plan:** `architecture-story-0005-0005.md`
**Story:** `story-0005-0005.md`

---

## 1. Affected Layers and Components

| Layer | Impact | Details |
|-------|--------|---------|
| `resources/skills-templates/core/x-dev-epic-implement/` | **MODIFY** | Replace Phase 1 placeholder with core loop logic in `SKILL.md` |
| `resources/github-skills-templates/dev/` | **MODIFY** | Mirror Phase 1 content (abbreviated) in `x-dev-epic-implement.md` |
| `tests/node/content/` | **MODIFY** | Add content assertions for Phase 1 sections in `x-dev-epic-implement-content.test.ts` |
| `tests/golden/*/` (8 profiles) | **REGENERATE** | Golden files updated to match new SKILL.md template content |
| `src/checkpoint/` | **NO CHANGE** | Referenced by SKILL.md instructions at runtime; no code changes |
| `src/domain/implementation-map/` | **NO CHANGE** | Referenced by SKILL.md instructions at runtime; no code changes |

This story is a **template-only change**. The core loop is Markdown instructions consumed by an AI agent at runtime. No new TypeScript source files are created. No existing TypeScript source files are modified.

---

## 2. New Classes/Interfaces to Create

No new TypeScript classes or interfaces are created.

### 2.1 New Content Sections in SKILL.md Template

The following sections replace the Phase 1 placeholder in `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`:

| Section | Description |
|---------|-------------|
| **Phase 1 -- Execution Loop** | Top-level section replacing the placeholder |
| **1.1 Initialize Execution State** | Instructions for creating the checkpoint via `createCheckpoint()` |
| **1.2 Branch Management** | Instructions for creating or checking out the epic branch |
| **1.3 Core Loop Algorithm** | Phase-by-phase iteration with `getExecutableStories()` |
| **1.4 Subagent Dispatch (Sequential Mode)** | Prompt template for dispatching via Agent tool |
| **1.5 Result Validation (RULE-008)** | Inline validation of SubagentResult contract |
| **1.6 Checkpoint Update (RULE-002)** | Instructions for updating checkpoint after each story |
| **1.7 Extension Points** | Placeholders for integrity gate, retry, resume, parallel, consolidation, progress |

### 2.2 Abbreviated Mirror in GitHub Template

`resources/github-skills-templates/dev/x-dev-epic-implement.md` receives a condensed version of the same Phase 1 content, consistent with the abbreviated style already used in its Phase 0 section.

---

## 3. Existing Classes to Modify

### 3.1 Template Files

| File | Change | Reason |
|------|--------|--------|
| `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | Replace Phase 1 placeholder (lines 105-109) with ~100-150 lines of core loop instructions | Central deliverable of this story |
| `resources/github-skills-templates/dev/x-dev-epic-implement.md` | Replace Phase 1 placeholder (line 60-61) with ~30-50 lines of abbreviated core loop | Dual-copy consistency (RULE-001 in test) |

### 3.2 Test Files

| File | Change | Reason |
|------|--------|--------|
| `tests/node/content/x-dev-epic-implement-content.test.ts` | Modify `skillMd_phases1to3_arePlaceholders` test + add new Phase 1 content assertions | Phase 1 is no longer a placeholder; need to validate new content |

### 3.3 Golden Files (8 profiles)

| Profile | File | Change |
|---------|------|--------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-epic-implement/SKILL.md` | Regenerate |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-epic-implement/SKILL.md` | Regenerate |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-epic-implement/SKILL.md` | Regenerate |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-epic-implement/SKILL.md` | Regenerate |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-epic-implement/SKILL.md` | Regenerate |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-epic-implement/SKILL.md` | Regenerate |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-epic-implement/SKILL.md` | Regenerate |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-epic-implement/SKILL.md` | Regenerate |

Additionally, each profile's `.github/skills/x-dev-epic-implement/SKILL.md` golden file must be regenerated.

---

## 4. Dependency Direction Validation

This story does not introduce any new TypeScript module dependencies. The SKILL.md template **references** (via Markdown instructions) the following runtime APIs, but does not import them:

```
SKILL.md (Markdown)  --references-->  checkpoint/engine.ts (createCheckpoint, updateStoryStatus, readCheckpoint)
SKILL.md (Markdown)  --references-->  domain/implementation-map/index.ts (parseImplementationMap, getExecutableStories)
SKILL.md (Markdown)  --references-->  checkpoint/types.ts (SubagentResult, ExecutionState, StoryStatus)
SKILL.md (Markdown)  --references-->  domain/implementation-map/types.ts (ParsedMap, DagNode)
```

**Verification checklist:**
- [x] No new TypeScript imports introduced
- [x] No circular dependencies possible (template references only)
- [x] Content tests import only `vitest`, `node:fs`, `node:path` (unchanged)
- [x] Domain layer untouched
- [x] Checkpoint engine untouched

---

## 5. Integration Points

| Consumer | How It Integrates | When |
|----------|-------------------|------|
| story-0005-0006 (Integrity Gate) | Replaces `[Placeholder: integrity gate -- story-0005-0006]` in Phase 1 | After this story is complete |
| story-0005-0007 (Retry + Block Propagation) | Replaces `[Placeholder: retry -- story-0005-0007]` in Phase 1 | After this story is complete |
| story-0005-0008 (Resumability) | Replaces `[Placeholder: resume -- story-0005-0008]` in Phase 1 | After this story is complete |
| story-0005-0009 (Partial Execution) | Extends core loop filter logic in Phase 1 | After this story is complete |
| story-0005-0010 (Parallel Worktrees) | Replaces `[Placeholder: parallel -- story-0005-0010]` in dispatch section | After this story is complete |
| story-0005-0011 (Consolidation) | Replaces Phase 2 and Phase 3 placeholders | After this story is complete |
| story-0005-0013 (Progress Reporting) | Replaces `[Placeholder: progress reporting -- story-0005-0013]` | After this story is complete |
| story-0005-0014 (Error Handling) | Extends result validation logic in Phase 1 | After this story is complete |
| story-0005-0001 (Checkpoint Engine) | SKILL.md references `createCheckpoint()`, `updateStoryStatus()`, `readCheckpoint()` from `src/checkpoint/engine.ts` | Already complete (prerequisite) |
| story-0005-0004 (Implementation Map Parser) | SKILL.md references `parseImplementationMap()`, `getExecutableStories()` from `src/domain/implementation-map/index.ts` | Already complete (prerequisite) |

---

## 6. Database Changes

N/A -- This is a template change. No database or persistence logic is modified.

---

## 7. API Changes

N/A -- No HTTP/CLI/gRPC interface changes. The SKILL.md is consumed internally by the AI agent runtime.

---

## 8. Event Changes

N/A -- No events are produced or consumed.

---

## 9. Configuration Changes

N/A -- No environment variables, config files, or `settings.json` changes.

---

## 10. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| Existing Phase 1 content test (`skillMd_phases1to3_arePlaceholders`) breaks | Medium | Certain | The test currently asserts Phase 1 contains `placeholder\|story-0005\|TODO\|implemented in`. This regex will still match because Phase 1 references `story-0005-0006` through `story-0005-0013` in placeholder comments. However, the test must be refactored: split into Phase 1 content assertions (new) and Phase 2/3 placeholder assertions (retained). |
| Golden file regeneration misses a profile | Low | Medium | Run `npm run generate` across all 8 profiles. CI validates golden files via byte-for-byte comparison. Run `npx vitest tests/node/content/byte-for-byte.test.ts` locally before committing. |
| Downstream stories misread extension point placeholders | Low | Medium | Use consistent naming convention: `[Placeholder: {feature-name} -- story-XXXX-YYYY]`. Document the convention in SKILL.md itself. |
| SKILL.md Phase 1 section exceeds 300 lines (NFR target) | Low | Low | Architecture plan targets <300 lines. The core loop algorithm is inherently compact (~100-150 lines). Monitor during implementation. |
| SubagentResult type changes between story-0005-0001 and this story | Low | Low | The `SubagentResult` interface in `src/checkpoint/types.ts` is already finalized with fields: `status`, `commitSha?`, `findingsCount`, `summary`. Template references these exact field names. |
| Dual-copy consistency between Claude and GitHub templates drifts | Medium | Medium | Content test `bothContainTerm_%s_dualCopyConsistency` validates critical terms appear in both. Add new critical terms for Phase 1 content (e.g., `getExecutableStories`, `SubagentResult`, `IN_PROGRESS`). |

---

## 11. Implementation Order (TDD)

Implementation follows test-first (Red-Green-Refactor) with content tests driving template changes.

### Phase A: Update Content Tests (Red)

1. **Refactor test `skillMd_phases1to3_arePlaceholders`**: Split into two tests:
   - `skillMd_phase1_containsCoreLoopContent` -- asserts Phase 1 has execution loop keywords
   - `skillMd_phases2And3_arePlaceholders` -- retains placeholder assertion for Phase 2 and Phase 3

2. **Add Phase 1 content assertion tests** (all RED initially):

| Test Name | Assertion |
|-----------|-----------|
| `skillMd_phase1_containsCheckpointIntegration` | Phase 1 content contains `createCheckpoint`, `updateStoryStatus` |
| `skillMd_phase1_containsMapParserIntegration` | Phase 1 content contains `parseImplementationMap`, `getExecutableStories` |
| `skillMd_phase1_containsSubagentDispatch` | Phase 1 content contains `Agent tool`, `SubagentResult`, `x-dev-lifecycle` |
| `skillMd_phase1_containsResultValidation` | Phase 1 content contains `status`, `findingsCount`, `summary`, `commitSha` |
| `skillMd_phase1_containsBranchManagement` | Phase 1 content contains `feat/epic-`, `git checkout` |
| `skillMd_phase1_containsCriticalPathPriority` | Phase 1 content contains `critical path` or `criticalPath` |
| `skillMd_phase1_containsContextIsolation` | Phase 1 content contains `RULE-001` or `context isolation` or `clean context` |
| `skillMd_phase1_containsExtensionPlaceholders` | Phase 1 content contains placeholders for story-0005-0006, 0007, 0008, 0010, 0011, 0013 |

3. **Add dual-copy consistency tests** for new Phase 1 terms:

| Critical Term | Reason |
|---------------|--------|
| `getExecutableStories` | Core loop uses this function |
| `SubagentResult` | Contract validation term |
| `IN_PROGRESS` | Story status transition |
| `createCheckpoint` | Checkpoint integration |
| `RULE-008` or `result contract` | Contract validation rule reference |

### Phase B: Implement Phase 1 in SKILL.md Template (Green)

1. **Edit** `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`:
   - Replace the Phase 1 placeholder (lines 105-109) with the core loop content
   - Structure follows story section 3.1 (Core Loop Algorithm):
     a. Initialize execution state (checkpoint creation)
     b. Branch management (create or checkout)
     c. Phase-by-phase loop with `getExecutableStories()`
     d. Sequential dispatch per story via Agent tool
     e. Result validation (RULE-008)
     f. Checkpoint update (RULE-002)
     g. Extension point placeholders

2. **Edit** `resources/github-skills-templates/dev/x-dev-epic-implement.md`:
   - Replace Phase 1 placeholder with abbreviated version
   - Include same critical terms for dual-copy consistency

3. **Verify all Red tests now pass** (Green).

### Phase C: Regenerate Golden Files

1. Run `npm run generate` (or equivalent) for all 8 profiles.
2. Copy generated `.claude/skills/x-dev-epic-implement/SKILL.md` into each profile's golden directory.
3. Copy generated `.github/skills/x-dev-epic-implement/SKILL.md` into each profile's golden directory.
4. Run `npx vitest tests/node/content/byte-for-byte.test.ts` to verify all golden files match.

### Phase D: Refactor

1. Review Phase 1 content for clarity and conciseness.
2. Ensure line count is within 300-line NFR target for the Phase 1 section.
3. Verify all placeholder names follow the `[Placeholder: {name} -- story-XXXX-YYYY]` convention.
4. Confirm no runtime `{{PLACEHOLDER}}` tokens were accidentally introduced.

### Phase E: Final Verification

1. Run full content test suite: `npx vitest tests/node/content/x-dev-epic-implement-content.test.ts`.
2. Run golden file tests: `npx vitest tests/node/content/byte-for-byte.test.ts`.
3. Run TypeScript compilation: `npx tsc --noEmit` (should be clean -- no TS changes).
4. Verify coverage remains at >= 95% line, >= 90% branch.

---

## 12. Test File Structure

```
tests/
  node/
    content/
      x-dev-epic-implement-content.test.ts   # MODIFY: add Phase 1 content assertions
```

No new test files are created. All new assertions are added to the existing content test file.

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario (from story) | Test / Validation | Phase |
|-------------------------------|-------------------|-------|
| Single story execution | `skillMd_phase1_containsSubagentDispatch` + template review | B |
| Sequential two-phase execution with dependency | `skillMd_phase1_containsMapParserIntegration` (getExecutableStories) | A, B |
| Critical path prioritization | `skillMd_phase1_containsCriticalPathPriority` | A, B |
| Valid SubagentResult contract | `skillMd_phase1_containsResultValidation` (status, findingsCount, summary, commitSha) | A, B |
| Invalid SubagentResult contract | `skillMd_phase1_containsResultValidation` + template review of FAILED path | A, B |
| Branch creation | `skillMd_phase1_containsBranchManagement` (feat/epic-, git checkout) | A, B |
| Checkpoint updated after each story (RULE-002) | `skillMd_phase1_containsCheckpointIntegration` (createCheckpoint, updateStoryStatus) | A, B |
| Context isolation (RULE-001) | `skillMd_phase1_containsContextIsolation` (RULE-001 / clean context) | A, B |
| BLOCKED story not dispatched | `skillMd_phase1_containsMapParserIntegration` (getExecutableStories filters BLOCKED) | A, B |

---

## 14. Phase 1 Content Outline

The following is the target structure for the Phase 1 section in SKILL.md. This is the **specification** that drives implementation.

```markdown
## Phase 1 -- Execution Loop

### 1.1 Initialize Execution State
- Read IMPLEMENTATION-MAP.md content
- Call parseImplementationMap(content) to get ParsedMap
- Build story list with phases from ParsedMap
- Call createCheckpoint(epicDir, input) to create ExecutionState
  - input: { epicId, branch, stories: [{id, phase}], mode: {parallel: false, skipReview} }

### 1.2 Branch Management
- git checkout main && git pull origin main
- git checkout -b feat/epic-{epicId}-full-implementation
- If branch exists (resume): git checkout feat/epic-{epicId}-full-implementation

### 1.3 Core Loop Algorithm
- For each phase (0..totalPhases-1):
  - Call getExecutableStories(parsedMap, executionState) -> sorted by critical path priority
  - If no executable stories remain for this phase, advance to next phase
  - For each executable story:
    - Mark IN_PROGRESS: updateStoryStatus(epicDir, storyId, {status: IN_PROGRESS})
    - Dispatch subagent (see 1.4)
    - Validate result (see 1.5)
    - Update checkpoint (see 1.6)
  - [Placeholder: integrity gate -- story-0005-0006]
  - [Placeholder: progress reporting -- story-0005-0013]
- [Placeholder: consolidation -- story-0005-0011]

### 1.4 Subagent Dispatch (Sequential Mode)
- Use Agent tool to dispatch a general-purpose subagent
- Prompt includes: storyId, storyPath, branchName, currentPhase, skipReview, epicId
- Subagent executes x-dev-lifecycle logic with clean context (RULE-001)
- Subagent returns SubagentResult: {status, commitSha?, findingsCount, summary}
- [Placeholder: parallel dispatch -- story-0005-0010]

### 1.5 Result Validation (RULE-008)
- Check: result has `status` field (SUCCESS | FAILED | PARTIAL)
- Check: result has `findingsCount` (number)
- Check: result has `summary` (string)
- Check: if status === SUCCESS, `commitSha` must be present
- On invalid result: mark FAILED with summary "Invalid subagent result: missing {field} field"
- [Placeholder: retry on failure -- story-0005-0007]

### 1.6 Checkpoint Update (RULE-002)
- Call updateStoryStatus(epicDir, storyId, {status, commitSha, findingsCount, summary})
- Update metrics: increment storiesCompleted
- Checkpoint persisted to execution-state.json after EVERY story

### 1.7 Extension Points
- [Placeholder: integrity gate -- story-0005-0006]
- [Placeholder: retry + block propagation -- story-0005-0007]
- [Placeholder: resume from checkpoint -- story-0005-0008]
- [Placeholder: partial execution filter -- story-0005-0009]
- [Placeholder: parallel worktrees -- story-0005-0010]
- [Placeholder: consolidation + verification -- story-0005-0011]
- [Placeholder: progress reporting -- story-0005-0013]
```
