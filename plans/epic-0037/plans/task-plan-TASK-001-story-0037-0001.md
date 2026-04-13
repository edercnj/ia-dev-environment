# Task Plan — TASK-001

| Field | Value |
|-------|-------|
| Task ID | TASK-001 |
| Story ID | story-0037-0001 |
| Epic ID | 0037 |
| Source Agent | merged(Architect, QA, Security, PO) |
| Type | documentation |
| TDD Phase | GREEN |
| Layer | cross-cutting |
| Estimated Effort | S |
| Date | 2026-04-13 |

## Objective

Create `java/src/main/resources/targets/claude/rules/14-worktree-lifecycle.md` with 7 sections covering naming convention, protected branches, non-nesting invariant, lifecycle, creator-owns-removal matrix, when-to-use decision tree, and anti-patterns. Structural shape mirrors `13-skill-invocation-protocol.md`. Includes security-augmented criteria from SEC subagent.

## Implementation Guide

1. Read `targets/claude/rules/13-skill-invocation-protocol.md` for structural template (no frontmatter, H1 with `# Rule NN — {Title}`, `>` callouts, H2 sections).
2. Author 7 sections per story Section 3.1 spec, in exact order: Naming → Protegidas → Não-Aninhamento → Lifecycle → Ownership → Quando Usar → Anti-Patterns.
3. Section 1 (Naming): 6-row table (task / story / epic-fix / release / hotfix / custom) with 3 columns (Contexto, Padrão, Exemplo).
4. Section 5 (Ownership): matrix with ≥5 skill rows × 4 columns (Skill, Cria worktree?, Quem remove?, Quando?).
5. Section 7 (Anti-Patterns): explicitly list `Agent(isolation:"worktree")` deprecation, protected-branch checkout in worktree, worktree creation outside `.claude/worktrees/`, nested worktree, missing cleanup, orchestrated subagent removing parent worktree.
6. Add `.gitignore` requirement note for `.claude/worktrees/` (SEC-003).
7. Add cross-reference to Rule 06 path-normalization baseline (SEC).
8. Verify line width ≤120 chars per row (Rule 03).
9. Run grep for `TODO|FIXME|HACK` — must be zero.

## Definition of Done

- [ ] File at `java/src/main/resources/targets/claude/rules/14-worktree-lifecycle.md` (NOT in `.claude/`)
- [ ] H1 = `# Rule 14 — Worktree Lifecycle`
- [ ] 7 H2 sections in spec order
- [ ] Naming table: 6 rows × 3 cols, valid markdown alignment
- [ ] Ownership matrix: ≥5 skill rows × 4 cols
- [ ] Anti-Patterns lists 6 entries (incl. `Agent(isolation:"worktree")` deprecation)
- [ ] `.gitignore` guidance for `.claude/worktrees/` documented (SEC)
- [ ] Cross-reference to Rule 06 present (SEC)
- [ ] Anti-Patterns forbids worktree outside `.claude/worktrees/` and protected-branch checkout (SEC, A05)
- [ ] No absolute filesystem paths in document (`/Users/`, `/home/`, `C:\`) (SEC)
- [ ] Line width ≤120 chars
- [ ] Zero `TODO|FIXME|HACK` markers
- [ ] Structural shape matches `13-skill-invocation-protocol.md`

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-000 | DoR (slot 14 free, branch cut) |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Spec drift between Section 3.1 and authored content | Medium | Medium | Side-by-side review against story Section 3.1 before commit |
| Markdown table renders broken on GitHub | Low | Low | Preview locally before push |
