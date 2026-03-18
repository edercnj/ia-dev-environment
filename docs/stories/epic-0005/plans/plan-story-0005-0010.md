# Implementation Plan — story-0005-0010: Parallel Execution with Worktrees

## 1. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Template | `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | Add parallel execution sections |
| Template | `resources/github-skills-templates/dev/x-dev-epic-implement.md` | Mirror parallel content |
| Test | `tests/node/content/x-dev-epic-implement-content.test.ts` | Add parallel content tests |
| Golden | `tests/golden/{8 profiles}/` | Regenerate via pipeline |

## 2. New Content Sections to Create

### In SKILL.md (Section 1.4 area):

1. **Section 1.4a — Parallel Worktree Dispatch**
   - Replace `[Placeholder: parallel worktree dispatch — story-0005-0010]`
   - Add conditional: `if mode.parallel === true`
   - Dispatch ALL executable stories in SINGLE message
   - Agent tool with `isolation: "worktree"`, `subagent_type: "general-purpose"`
   - Branch per worktree: `feat/epic-{epicId}-{storyId}`
   - Same subagent prompt as sequential (1.4) but with worktree isolation
   - Await all subagents before merge

2. **Section 1.4b — Merge Strategy**
   - Sort completed stories by critical path priority (RULE-007)
   - For each SUCCESS story (in order): `git merge` worktree branch
   - On conflict: dispatch conflict resolution subagent
   - On merge OK: `updateStoryStatus(epicDir, storyId, { status: "SUCCESS", ... })`
   - FAILED stories: skip merge, invoke failure handling (retry/block from story-0005-0007)

3. **Section 1.4c — Conflict Resolution Subagent**
   - Prompt template for conflict resolution
   - Receives: epic branch, worktree branch, conflict file list
   - Analyzes diff from both sides
   - Resolves preserving intent of both stories
   - Returns: SUCCESS (with merge commit) or FAILED
   - On FAILED: mark story FAILED, trigger block propagation

4. **Section 1.4d — Worktree Cleanup**
   - SUCCESS + merged: cleanup worktree
   - FAILED: preserve for diagnostics
   - Agent tool auto-cleanup for no-change worktrees

### In GitHub template:
   - Condensed version of same sections (following existing GitHub template pattern)

## 3. Existing Content to Modify

| Location | Current | Change |
|----------|---------|--------|
| SKILL.md §1.4 | `[Placeholder: parallel worktree dispatch — story-0005-0010]` | Replace with `See Section 1.4a-1.4d for parallel mode` |
| SKILL.md §1.7 | Lists placeholder for story-0005-0010 | Remove from extension points list |
| GitHub template §1.7 | Lists placeholder for story-0005-0010 | Remove from extension points list |

## 4. Dependency Direction Validation

- No new source code dependencies introduced
- SKILL.md references existing types: `getExecutableStories()`, `updateStoryStatus()`, `SubagentResult`
- References `Agent` tool with `isolation: "worktree"` (Claude Code platform feature)
- No domain layer changes

## 5. Integration Points

- **story-0005-0005 (Core Loop)**: Parallel dispatch is alternative execution path within Phase 1.3 loop
- **story-0005-0007 (Failure Handling)**: FAILED parallel stories feed into retry + block propagation
- **RULE-001**: Context isolation maintained — each worktree subagent has clean context
- **RULE-002**: Checkpoint update timing changes — after merge, not after subagent completion
- **RULE-007**: Critical path priority determines merge order

## 6. Database Changes

None.

## 7. API Changes

None.

## 8. Event Changes

None.

## 9. Configuration Changes

None. The `--parallel` flag is already defined in the SKILL.md frontmatter `argument-hint`.

## 10. Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Content tests may need to handle new section numbering | Low | Medium | Use flexible regex patterns |
| Golden files need regeneration for 8 profiles | Low | High | Run pipeline after template changes |
| GitHub template must stay consistent with Claude template | Medium | Low | Existing dual-copy tests + new terms |

## 11. Implementation Order

1. Write content tests for parallel execution sections (RED)
2. Add parallel execution content to SKILL.md template (GREEN)
3. Mirror to GitHub template (GREEN)
4. Remove placeholder from extension points (REFACTOR)
5. Regenerate golden files
6. Verify all tests pass
