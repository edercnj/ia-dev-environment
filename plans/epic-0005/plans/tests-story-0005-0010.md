# Test Plan — story-0005-0010: Parallel Execution with Worktrees

## Test Strategy

This story modifies SKILL.md templates (content only, no runtime code). Tests validate
that the generated content contains the required parallel execution sections, keywords,
structural ordering, and dual-copy consistency.

## Acceptance Tests

### AT-1: SKILL.md contains parallel worktree dispatch section
- **Type**: Content assertion (integration)
- **Validates**: Placeholder replaced with substantive parallel execution content
- **Pass criteria**: No `[Placeholder.*story-0005-0010]` remains; parallel dispatch section exists with ≥20 lines

### AT-2: GitHub template mirrors parallel execution terms
- **Type**: Dual-copy consistency
- **Validates**: Critical parallel-execution terms exist in both templates
- **Pass criteria**: All critical terms present in both files

## Unit Tests (TPP Order)

### UT-1: Parallel dispatch placeholder removed (Level 1 — Degenerate)
- **Assert**: SKILL.md does NOT contain `[Placeholder: parallel worktree dispatch — story-0005-0010]`
- **Depends On**: None
- **Parallel**: yes

### UT-2: Extension points list updated (Level 1 — Degenerate)
- **Assert**: Section 1.7 Extension Points does NOT list story-0005-0010
- **Depends On**: None
- **Parallel**: yes

### UT-3: Parallel dispatch section exists with isolation keyword (Level 2 — Scalar)
- **Assert**: Content contains `isolation` and `worktree` in parallel dispatch context
- **Depends On**: UT-1
- **Parallel**: no

### UT-4: SINGLE message dispatch keyword present (Level 2 — Scalar)
- **Assert**: Content contains `SINGLE message` in parallel dispatch context
- **Depends On**: UT-1
- **Parallel**: yes (with UT-3)

### UT-5: Merge strategy section contains critical path priority (Level 2 — Scalar)
- **Assert**: Content contains merge strategy with `critical path` ordering reference
- **Depends On**: UT-1
- **Parallel**: yes

### UT-6: Conflict resolution section exists (Level 2 — Scalar)
- **Assert**: Content contains conflict resolution subagent prompt template
- **Depends On**: UT-1
- **Parallel**: yes

### UT-7: Worktree cleanup section exists (Level 2 — Scalar)
- **Assert**: Content contains worktree cleanup rules (SUCCESS → clean, FAILED → preserve)
- **Depends On**: UT-1
- **Parallel**: yes

### UT-8: Parallel dispatch references required keywords (Level 3 — Collection)
- **Assert**: Parallel sections contain: `Agent`, `isolation`, `worktree`, `SINGLE message`, `getExecutableStories`, `SubagentResult`
- **Depends On**: UT-3
- **Parallel**: no

### UT-9: Merge strategy references rule markers (Level 3 — Collection)
- **Assert**: Merge sections contain: `RULE-002`, `RULE-007`, `updateStoryStatus`, `checkpoint`
- **Depends On**: UT-5
- **Parallel**: no

### UT-10: Fallback to sequential when --parallel not active (Level 3 — Collection)
- **Assert**: Content documents that without `--parallel`, sequential dispatch (1.4) is used unchanged
- **Depends On**: UT-3
- **Parallel**: yes

### UT-11: Conflict resolution contains both success and failure paths (Level 3 — Collection)
- **Assert**: Conflict section mentions both SUCCESS resolution and FAILED (irresolvable)
- **Depends On**: UT-6
- **Parallel**: no

### UT-12: Parallel sections have minimum subsection count (Level 4 — Composite)
- **Assert**: At least 4 subsections (1.4a, 1.4b, 1.4c, 1.4d or equivalent headings)
- **Depends On**: UT-8
- **Parallel**: no

### UT-13: Parallel sections in logical order (Level 4 — Composite)
- **Assert**: dispatch appears before merge, merge before conflict, conflict before cleanup
- **Depends On**: UT-12
- **Parallel**: no

### UT-14: Integration with failure handling documented (Level 4 — Composite)
- **Assert**: Parallel sections reference story-0005-0007 or failure handling for FAILED stories
- **Depends On**: UT-11
- **Parallel**: no

### UT-15: Checkpoint timing documented — after merge not after subagent (Level 4 — Composite)
- **Assert**: Content explicitly states checkpoint update happens after each merge
- **Depends On**: UT-9
- **Parallel**: no

### UT-16: GitHub template dual-copy consistency for new terms (Level 5 — Edge)
- **Assert**: GitHub template contains all critical parallel-execution terms from Claude template
- **Depends On**: UT-8
- **Parallel**: no

## Integration Tests

### IT-1: Golden files regeneration (after all UTs pass)
- **Type**: Byte-for-byte golden file comparison
- **Validates**: Generated output for all 8 profiles matches updated golden files
- **Depends On**: All UTs
- **Parallel**: yes (per profile)
