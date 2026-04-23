# Rule 21 — Epic Branch Model

> **Related:** Rule 09 (Branching Model — Git Flow), Rule 22 (Skill Visibility), Rule 19 (Backward Compatibility).
> **Introduced by:** EPIC-0049 (Refatoração do Fluxo de Épico — Sequencial Default, Branch Única e Orquestradores Thin).

## Purpose

Every epic lives on a single long-lived integration branch `epic/XXXX` created at the epic's birth (planning **or** execution — whichever happens first). This branch receives:

1. Planning artifact commits (produced by `x-epic-create`, `x-epic-decompose`, `x-epic-map`, `x-epic-orchestrate`, `x-story-create`, `x-story-plan`, `x-task-plan` via `x-planning-commit`).
2. Story-level PR merges (auto-merged by GitHub native auto-merge — see RULE-002 of EPIC-0049).

The **final** PR `epic/XXXX → develop` is a **manual gate**: a human reviewer inspects the aggregated change-set before promoting it to the integration branch. There is no automatic promotion.

## Branch Naming

| Pattern | Example | Notes |
| :--- | :--- | :--- |
| `epic/{epic-id}` | `epic/0049` | `epic-id` is a 4-digit zero-padded integer matching the epic markdown filename. |

- Branch names MUST be lowercase with hyphens (no underscores, no camelCase).
- The `epic/` prefix is reserved — no other branch type may use it.
- One and only one branch per epic ID — idempotent creation (see `x-internal-epic-branch-ensure`).

## Lifecycle

```
develop ──●────────────────────────────────●──────────→
          │                                ↑
          │ (birth)                        │ manual PR gate
          ↓                                │
epic/XXXX ●──●──●──●──●──●──●──●──●──●──●──●
             ↑  ↑  ↑  ↑  ↑  ↑  ↑  ↑  ↑
             │  │  story PRs auto-merge in
             │  │
             planning artifact commits
```

| Phase | Operation | Actor |
| :--- | :--- | :--- |
| 1. Birth | `epic/XXXX` created from `develop` | `x-internal-epic-branch-ensure` (first entry-point) |
| 2. Planning | Planning skills commit artifacts via `x-planning-commit` | 7 planning skills (Rule 07 of EPIC-0049) |
| 3. Execution | Story PRs auto-merge into `epic/XXXX` | `x-pr-create --target-branch epic/XXXX --auto-merge` |
| 4. Gate | Manual PR `epic/XXXX → develop` | Human reviewer |
| 5. Retirement | Branch deleted after merge | `x-git-cleanup-branches` (post-merge) |

## Invariants

1. **Single source of truth.** `x-internal-epic-branch-ensure` is the only skill that decides "does `epic/XXXX` exist? create it." All other epic-scoped skills call it.
2. **Protection from cleanup.** `x-git-cleanup-branches` MUST exclude `epic/*` branches from its destructive sweep; they are cleaned only after the manual PR gate is merged.
3. **Worktree base.** In `--parallel` mode, story worktrees (`.claude/worktrees/story-XXXX-YYYY/`) use `epic/XXXX` as their base branch, **not** `develop`. See Rule 14 (Worktree Lifecycle).
4. **Auto-merge target.** Story PRs created under EPIC-0049 flow target `epic/XXXX`, never `develop`. Propagated OO-style via `--target-branch` (RULE-009 of EPIC-0049).
5. **Legacy escape hatch.** `--legacy-flow` forces all target branches back to `develop` — disables Rule 21. Used during the backward-compatibility window (Rule 19) and for epics created before EPIC-0049 merged.

## Anti-Patterns

- **Direct commit to `epic/XXXX`** — always via PR or `x-planning-commit` (the latter is the only direct-commit path, restricted to `plans/**` paths).
- **Multiple epic branches for one epic ID** — `epic/0049`, `epic/0049-refactor`, etc. are forbidden. One ID = one branch.
- **Merging `epic/XXXX` into `main`** — epic branches merge into `develop` only; the release flow (`release/*`) promotes `develop → main`.
- **Force-pushing `epic/XXXX`** — prohibited once any story PR has been merged in (rewriting history invalidates the merge record).
- **Skipping the manual gate** — automating `epic/XXXX → develop` defeats the purpose of Rule 21.

## Backward Compatibility

- Epics created before EPIC-0049 merged (flowVersion `"1"` or absent) use legacy flow — story PRs target `develop` directly, no `epic/XXXX` branch exists.
- `--legacy-flow` on a new epic forces legacy mode (warning emitted — see Rule 19 fallback matrix).
- Deprecation window: 2 releases after EPIC-0049 merge. After the window, `flowVersion != "2"` without `--legacy-flow` fails fast.

## Audit

CI script `scripts/audit-epic-branches.sh` (or equivalent) scans `gh pr list --base develop --head 'epic/*'` and verifies:

- Each open `epic/*` PR has `flowVersion: "2"` in its `execution-state.json` (when present).
- No `epic/*` branch has been force-pushed after its first merge commit.
- `x-git-cleanup-branches` configuration excludes `epic/*` from protected-branch bypass.

Any violation fails the CI build with `EPIC_BRANCH_VIOLATION`.
