# Architecture Plan — story-0005-0010: Parallel Execution with Worktrees

## Scope: Simplified (New Feature, No Contract/Infra Change)

## 1. Change Summary

Add parallel worktree dispatch capability to the epic orchestrator SKILL.md template.
When `--parallel` flag is active, executable stories in the same phase are dispatched
concurrently via `Agent` tool with `isolation: "worktree"`. After all subagents complete,
worktree branches are merged sequentially (critical path first) with conflict resolution.

## 2. Affected Files

| File | Action | Description |
|------|--------|-------------|
| `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | **Modify** | Replace `[Placeholder: parallel worktree dispatch — story-0005-0010]` with full parallel execution sections |
| `resources/github-skills-templates/dev/x-dev-epic-implement.md` | **Modify** | Mirror parallel execution content to GitHub template |
| `tests/node/content/x-dev-epic-implement-content.test.ts` | **Modify** | Add content tests for parallel execution sections |
| `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md` | **Regenerate** | Golden files updated via pipeline |
| `tests/golden/{profile}/.github/skills/x-dev-epic-implement/SKILL.md` | **Regenerate** | Golden files updated via pipeline |

## 3. SKILL.md Content Architecture

### 3.1 New Section: 1.4a Parallel Worktree Dispatch

Replaces the placeholder `[Placeholder: parallel worktree dispatch — story-0005-0010]` in Section 1.4.
Must be positioned as an alternative to the existing sequential dispatch (Section 1.4).

**Content structure:**
- Conditional activation: `--parallel` flag check
- Worktree dispatch: All executable stories in SINGLE message via `Agent` with `isolation: "worktree"`
- Branch naming: `feat/epic-{epicId}-{storyId}` per worktree
- Await all subagents before proceeding to merge

### 3.2 New Section: 1.4b Merge Strategy

After all parallel subagents complete:
1. Sort SUCCESS stories by critical path priority (RULE-007)
2. Sequential merge: worktree branch → epic branch
3. Conflict detection and resolution subagent dispatch
4. Checkpoint update after EACH merge (RULE-002)
5. FAILED stories: skip merge, delegate to failure handling (story-0005-0007)

### 3.3 New Section: 1.4c Conflict Resolution

- Subagent prompt template for merge conflict resolution
- Input: main branch, worktree branch, conflict file list
- Output: resolved merge commit or FAILED status
- On irresolvable conflict: mark story FAILED, trigger block propagation

### 3.4 New Section: 1.4d Worktree Cleanup

- SUCCESS stories: worktree cleaned after merge
- FAILED stories: worktree preserved for diagnostics
- Agent tool auto-cleanup for no-change worktrees

## 4. Integration Points

| Component | Integration |
|-----------|-------------|
| **Core Loop (story-0005-0005)** | Parallel dispatch is alternative path in Phase 1.3 loop |
| **Failure Handling (story-0005-0007)** | FAILED stories from parallel dispatch feed into retry/block propagation |
| **Checkpoint Engine** | `updateStoryStatus()` called after each merge (not after subagent completion) |
| **getExecutableStories()** | Used to determine which stories can run in parallel within a phase |
| **RULE-001 (Context Isolation)** | Each worktree subagent gets clean context — same as sequential |
| **RULE-007 (Critical Path Priority)** | Merge order prioritizes critical path stories |

## 5. Fallback Behavior

When `--parallel` is NOT active (default), the existing sequential dispatch (Section 1.4)
remains unchanged. The new parallel sections are conditional and do not modify the
sequential path.

## 6. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Merge conflicts between parallel stories | Medium | Conflict resolution subagent + fallback to FAILED |
| Resource consumption (multiple worktrees) | Low | Documentation warning; optional flag |
| Checkpoint timing (after merge vs after subagent) | Medium | Explicit rule: checkpoint after EACH merge |
| GitHub template drift from Claude template | Low | Dual-copy consistency tests already exist |

## 7. Test Strategy

- **Content tests**: Verify parallel execution keywords, section structure, merge strategy content
- **Dual-copy tests**: Verify GitHub template mirrors Claude template for new terms
- **Golden file tests**: Regenerate all 8 profiles after SKILL.md changes
- **No runtime code changes**: This story modifies only SKILL.md templates
