---
name: x-status-reconcile
description: "Reconciles execution-state.json (telemetry) against the **Status:** field of Epic / Story markdown artifacts. Default mode (diagnose) is read-only and prints a divergence table. Opt-in --apply rewrites the markdowns atomically via StatusFieldParser and commits via x-git-commit. Respects Rule 19 (legacy v1 epics skip silently) and Rule 22 (markdown is SoT; state.json is telemetry). Use for manual recovery of legacy epics whose markdown status drifted from execution checkpoints."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, AskUserQuestion
argument-hint: "--epic XXXX | --story story-XXXX-YYYY [--apply] [--non-interactive] [--dry-run]"
category: ops
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Status Reconciler

## Purpose

Reconcile drift between `plans/epic-XXXX/execution-state.json`
(telemetry, per RULE-046-07) and the canonical `**Status:**`
field of Epic / Story markdown artifacts (source of truth, per
Rule 22 — Lifecycle Integrity).

Typical use case: legacy epic EPIC-0024 has 16 stories with
`status: SUCCESS` in `execution-state.json` but every story
markdown still says `**Status:** Pendente`. Running
`/x-status-reconcile --epic 0024 --apply` rewrites the 16
markdowns (plus the epic-level rollup) and emits a single
audit commit.

## Triggers

- `/x-status-reconcile --epic 0024` — diagnose mode (read-only)
- `/x-status-reconcile --epic 0024 --apply` — apply + commit
- `/x-status-reconcile --epic 0024 --apply --non-interactive` — CI
- `/x-status-reconcile --story story-0024-0005 --apply` — narrow scope
- `/x-status-reconcile --epic 0024 --apply --dry-run` — force diagnose even with --apply

## Parameters

| Parameter | Required | Description |
| :--- | :--- | :--- |
| `--epic XXXX` | one of (`--epic`, `--story`) | Full epic scope |
| `--story story-XXXX-YYYY` | one of (`--epic`, `--story`) | Single-story scope |
| `--apply` | no | Rewrites markdowns + commits; default is diagnose |
| `--non-interactive` | no | Skips the PROCEED/FIX/ABORT gate (assumes PROCEED) |
| `--dry-run` | no | Forces diagnose output even when `--apply` is present |

## Exit Codes

| Code | Name | Meaning |
| :--- | :--- | :--- |
| 0 | SUCCESS | No divergence OR diagnose mode |
| 10 | APPLIED | `--apply` succeeded (commit created) |
| 20 | STATUS_SYNC_FAILED | Markdown write failure |
| 30 | STATE_FILE_INVALID | `execution-state.json` missing or malformed |
| 40 | STATUS_TRANSITION_INVALID | Suspicious transition (e.g., CONCLUIDA→PENDENTE) |
| 50 | USER_ABORTED | Operator chose ABORT at gate |
| 2 | USAGE_ERROR | Invalid / conflicting CLI arguments (picocli) |

## Workflow

### Phase 0 — Parse args + locate epic dir

1. Extract epic id from `--epic XXXX` or parse story id from `--story`.
2. Resolve `plans/epic-XXXX/execution-state.json`.
3. If the epic directory itself is missing → exit 30; absence of `execution-state.json` is treated as legacy v1 (Phase 1) — exit 0 silently (Rule 19).

### Phase 1 — Rule 19 check

Delegate to `LifecycleReconciler.isLegacyV1(epicDir)`. When
true, print `"legacy epic; skipping per Rule 19"` and exit 0
without any side effect.

### Phase 2 — Diagnose

Call `LifecycleReconciler.diff(epicDir)` and render a table:

```
Divergence report for epic 0024:
  story-0024-0001.md: Pendente → Concluída
  story-0024-0002.md: Pendente → Concluída
  ...
  epic-0024.md:       Em Andamento → Concluída

Total divergences: N
```

When `--apply` is NOT present (or `--dry-run` forces diagnose),
print the final JSON with `"mode": "diagnose"` and exit 0.

### Phase 3 — Gate (only when `--apply` and NOT `--non-interactive`)

Present the canonical 3-option menu (reuse Rule 20 / EPIC-0043
when available):

```
AskUserQuestion(
  question: "N divergences detected for epic XXXX. Apply?",
  options: [
    { header: "Proceed", label: "Continue (Recommended)",
      description: "Apply all markdown rewrites and commit." },
    { header: "Fix",     label: "Regenerate diagnose",
      description: "Re-run diagnose with verbose detail; menu reappears." },
    { header: "Abort",   label: "Cancel",
      description: "Exit 50 — no markdown is touched." }
  ]
)
```

- PROCEED → Phase 4
- FIX → re-render diagnose with each Divergence's file path; menu reappears (max 3 iterations, RULE-20 guard-rail)
- ABORT → exit 50

### Phase 4 — Apply + commit

1. `LifecycleReconciler.apply(divergences)` — validates every
   transition against `LifecycleTransitionMatrix` BEFORE any
   write (atomicity). A forbidden transition → exit 40.
2. Delegate the commit to `x-git-commit` via Rule 13
   INLINE-SKILL:

   ```
   Skill(skill: "x-git-commit",
         args: "--type chore --scope epic-XXXX --subject 'reconcile lifecycle status backfill'")
   ```

3. On success, print the final JSON with `"mode": "apply"`,
   `"commitSha": "<sha>"`, and exit 10.

## Final JSON contract (last line of stdout)

```json
{
  "status": "APPLIED",
  "epicId": "0024",
  "divergences": [
    {"artifact": "story-0024-0001.md",
     "from": "Pendente", "to": "Concluída"}
  ],
  "divergenceCount": 33,
  "commitSha": "abc1234",
  "mode": "apply"
}
```

Consumer tests parse the final line as JSON; everything
preceding is human-readable diagnostics.

## Invariants

- NEVER writes `execution-state.json` (RULE-046-07).
- NEVER modifies files outside `plans/epic-XXXX/` (path
  canonicalization; no traversal).
- Every markdown write goes through `StatusFieldParser`
  (atomic temp-file + rename, per Rule 22).
- Legacy v1 epics are NEVER upgraded implicitly (Rule 19).
- Commit is created iff `--apply` AND divergences exist AND
  gate = PROCEED AND apply() succeeds.

## Error Handling

| Condition | Action |
| :--- | :--- |
| `--epic` and `--story` both missing | exit 2 with usage |
| `execution-state.json` missing or malformed | exit 30 |
| Legacy v1 epic | exit 0 with "skipping" message |
| Forbidden transition | exit 40; no markdown rewritten |
| Markdown write failure | exit 20; partial writes possible (rare — rename is atomic); operator inspects `git status` |
| Gate → ABORT | exit 50; working tree unchanged |

## Related

- Rule 22 — Lifecycle Integrity (SoT contract).
- Rule 19 — Backward Compatibility (v1 skip).
- Rule 20 — Interactive Gates Convention (PROCEED / FIX / ABORT).
- Rule 13 — Skill Invocation Protocol (INLINE-SKILL for `x-git-commit`).
- Helpers: `StatusFieldParser`, `LifecycleTransitionMatrix`, `LifecycleReconciler` (story-0046-0001 + 0046-0006).
