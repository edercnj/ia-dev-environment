# Task Breakdown — story-0005-0010: Parallel Execution with Worktrees

## Mode: Test-Driven (TDD)

## Tasks

### TASK-1: RED — Placeholder removal tests (UT-1, UT-2)
- Write tests asserting placeholder `[Placeholder.*story-0005-0010]` is absent
- Write test asserting Section 1.7 no longer lists story-0005-0010
- **Parallel**: yes (independent of other RED tasks)
- **TPP Level**: 1 (Degenerate)

### TASK-2: GREEN — Remove placeholder, update extension points
- Remove `[Placeholder: parallel worktree dispatch — story-0005-0010]` line from SKILL.md
- Remove story-0005-0010 from Section 1.7 extension points list
- Mirror changes to GitHub template
- **Depends On**: TASK-1
- **Parallel**: no

### TASK-3: RED — Scalar keyword tests (UT-3, UT-4, UT-5, UT-6, UT-7)
- Write tests for isolation/worktree keywords, SINGLE message, merge strategy, conflict resolution, cleanup
- **Parallel**: yes (with TASK-1 if interleaved)
- **TPP Level**: 2 (Scalar)

### TASK-4: GREEN — Add parallel dispatch section (1.4a)
- Write Section 1.4a: Parallel Worktree Dispatch
- Content: conditional `--parallel`, Agent tool with `isolation: "worktree"`, SINGLE message, branch naming
- **Depends On**: TASK-2, TASK-3
- **Parallel**: no

### TASK-5: GREEN — Add merge strategy section (1.4b)
- Write Section 1.4b: Merge Strategy
- Content: critical path ordering, sequential merge, checkpoint after merge
- **Depends On**: TASK-4
- **Parallel**: no

### TASK-6: GREEN — Add conflict resolution section (1.4c)
- Write Section 1.4c: Conflict Resolution Subagent
- Content: prompt template, success/failure paths, block propagation on failure
- **Depends On**: TASK-5
- **Parallel**: no

### TASK-7: GREEN — Add worktree cleanup section (1.4d)
- Write Section 1.4d: Worktree Cleanup
- Content: SUCCESS cleanup, FAILED preserve, auto-cleanup for no-change
- **Depends On**: TASK-6
- **Parallel**: no

### TASK-8: RED — Collection/composite tests (UT-8 through UT-15)
- Write tests for keyword collections, structural ordering, failure handling integration, checkpoint timing
- **Parallel**: yes (with TASK-4-7 implementation)
- **TPP Level**: 3-4 (Collection, Composite)

### TASK-9: REFACTOR — Mirror to GitHub template
- Copy condensed parallel execution content to GitHub template
- Maintain existing GitHub template style (more concise than Claude template)
- **Depends On**: TASK-7
- **Parallel**: no

### TASK-10: RED — Dual-copy consistency tests (UT-16)
- Write tests asserting GitHub template contains critical parallel terms
- **Depends On**: TASK-9
- **TPP Level**: 5 (Edge)

### TASK-11: GREEN — Regenerate golden files
- Run pipeline to regenerate golden files for all 8 profiles
- **Depends On**: TASK-9, TASK-10
- **Parallel**: no

### TASK-12: REFACTOR — Final cleanup
- Verify all tests pass
- Verify coverage thresholds
- Clean up any redundant content
- **Depends On**: TASK-11
- **Parallel**: no
