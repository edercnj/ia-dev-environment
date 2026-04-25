---
name: x-epic-implement
model: sonnet
description: "Thin orchestrator (~460 lines — story-0049-0018 refactor) that drives an epic end-to-end via 6 delegated phases: Phase 0 (args via x-internal-args-normalize), Phase 1 (load+plan via x-internal-epic-build-plan), Phase 2 (epic branch via x-internal-epic-branch-ensure), Phase 3 (sequential-by-default story loop via x-story-implement), Phase 4 (integrity gate + report via x-internal-epic-integrity-gate + x-internal-report-write), Phase 5 (final PR epic/XXXX → develop via x-git-merge + x-pr-create). Defaults flipped by EPIC-0049: sequential execution (opt-in parallel via --parallel), auto-merge of story PRs into epic/XXXX (target changed from develop). Legacy EPIC-0042 behavior preserved under --legacy-flow (auto-detected via execution-state.json flowVersion=1). Zero inline git/gh/jq/mvn calls — orchestrator uses only Read/Glob + Skill."
user-invocable: true
allowed-tools: Read, Write, Glob, Skill, Agent, AskUserQuestion, TaskCreate, TaskUpdate
argument-hint: "[EPIC-ID] [--parallel] [--legacy-flow] [--phase N] [--story story-XXXX-YYYY] [--resume] [--dry-run] [--skip-review] [--auto-merge-strategy merge|squash|rebase] [--strict-overlap] [--non-interactive] [--skip-pr-comments] [--revert-on-failure] [--skip-smoke]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## Triggers

```
/x-epic-implement 0049                  — full run (sequential + auto-merge into epic/0049)
/x-epic-implement 0049 --parallel       — parallel story execution via worktrees
/x-epic-implement 0049 --legacy-flow    — EPIC-0042 behavior (stories → develop, no final PR)
/x-epic-implement 0049 --resume         — continue from execution-state.json
/x-epic-implement 0049 --story story-0049-0007  — single story in isolation
/x-epic-implement 0049 --dry-run        — generate execution plan only, no dispatch
```

## Parameters

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `EPIC-ID` | String (4-digit) | — | Positional, required. |
| `--parallel` | Boolean | `false` | Opt-in parallel via worktrees. Mutually exclusive with `--legacy-flow`. |
| `--legacy-flow` | Boolean | `false` | Force EPIC-0042: stories → develop; Phase 2 and Phase 5 become no-ops. Auto-set when `flowVersion=1`. |
| `--phase N` | Integer | — | Execute only phase N stories. Mutually exclusive with `--story`. |
| `--story ID` | String | — | Execute a single story. Mutually exclusive with `--phase`. |
| `--resume` | Boolean | `false` | Continue from checkpoint. Auto-detects `flowVersion`. |
| `--dry-run` | Boolean | `false` | Generate execution plan; exit after Phase 1. |
| `--skip-review` | Boolean | `false` | Propagated to `x-story-implement` — skips specialist/TL reviews. |
| `--auto-merge-strategy` | Enum | `merge` | Story-PR auto-merge strategy: `merge\|squash\|rebase`. |
| `--non-interactive` | Boolean | `false` | Skip all `AskUserQuestion` gates (CI / orchestrated calls). |
| `--skip-pr-comments` | Boolean | `false` | Skip Phase 4b post-gate PR-comment remediation pass. |
| `--revert-on-failure` | Boolean | `false` | On integrity-gate failure, revert last story merge instead of remediation agent. |
| `--skip-smoke` | Boolean | `false` | Bypass epic smoke gate (advisory; emergency only). |

Deprecated (still parsed, warn-once): `--sequential`, `--auto-merge`, `--interactive-merge`, `--manual-batch-approval`, `--single-pr`, `--task-tracking`, `--dry-run-only-comments`, `--auto-approve-pr` (propagated as-is).

## Output Contract

| Field | Description |
|-------|-------------|
| `epicId` | 4-digit zero-padded epic identifier |
| `epicBranch` | `epic/XXXX` (v2) or `develop` (legacy) |
| `flowVersion` | `"2"` (default) or `"1"` (legacy) |
| `phasesExecuted` | List of `{name, durationSec, status}` per phase |
| `storiesExecuted` | List of `{id, status, prNumber, prUrl}` per dispatched story |
| `finalPrUrl/Number` | Final PR `epic/XXXX → develop`; null when legacy |
| `integrityGatePassed` | Phase 4 gate `passed` value |
| `coverageLine/Branch` | Filtered coverage from integrity gate envelope |
| `reportsDir` | `plans/epic-XXXX/reports/` |

**Delegation Map (RULE-005 — zero inline shell invocations):**

| Concern | Skill | Phase |
|---------|-------|-------|
| Args parsing | `x-internal-args-normalize` | 0 |
| DAG + execution plan | `x-internal-epic-build-plan` | 1 |
| `epic/<ID>` branch | `x-internal-epic-branch-ensure` | 2 |
| Per-story TDD + PR | `x-story-implement` | 3 |
| Integrity gate + report | `x-internal-epic-integrity-gate` + `x-internal-report-write` | 4 |
| Develop sync + final PR | `x-git-merge` + `x-pr-create` | 5 |
| Status mutations | `x-internal-status-update` | all |
| Post-gate remediation | `x-pr-fix-epic` | 4b (optional) |

**Workflow:** Phase 0 (Args) → Phase 1 (Load & Plan) → Phase 2 (Branch Setup, skipped for legacy) → Phase 3 (Story Loop) → Phase 4 (Integrity Gate) → Phase 5 (Final PR, skipped for legacy).

## Phase 0 — Args

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-0-Args`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-epic-implement --phase Phase-0-Args")

Open phase tracker (close with `TaskUpdate(id: phase0TaskId, status: "completed")` after args normalization):

    TaskCreate(subject: "EPIC-XXXX › Phase 0 - Args", activeForm: "Normalizing args and detecting flow version")

Invoke args normalizer and resolve epicId, flowVersion, flags:

    Skill(skill: "x-internal-args-normalize", args: "--schema @references/args-schema.json --argv \"{raw argv}\"")

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-epic-implement --phase Phase-0-Args")

TaskUpdate(id: phase0TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-0-Args ok`

## Phase 1 — Load and Plan

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-1-Plan`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-epic-implement --phase Phase-1-Plan")

Open phase tracker (close with `TaskUpdate(id: phase1TaskId, status: "completed")` after plan build):

    TaskCreate(subject: "EPIC-XXXX › Phase 1 - Plan", activeForm: "Building epic execution plan")

Build DAG + execution plan:

    Skill(skill: "x-internal-epic-build-plan", args: "--epic-id <ID> --mode <sequential|parallel> --output plans/epic-XXXX/reports/epic-execution-plan-XXXX.md [--strict-overlap]")

Consume `{phases, criticalPath, planPath}`. If `--dry-run=true` → print plan path and stop.

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-epic-implement --phase Phase-1-Plan --expected-artifacts plans/epic-XXXX/reports/epic-execution-plan-XXXX.md")

TaskUpdate(id: phase1TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-1-Plan ok`

<!-- phase-no-gate: Phase 2 is skipped for legacy flow; gate lives inside the conditional block -->
## Phase 2 — Branch Setup (skipped for legacy)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-2-Branch`

Open phase tracker (close after branch ensure):

    TaskCreate(subject: "EPIC-XXXX › Phase 2 - Branch", activeForm: "Ensuring epic branch exists")

Ensure `epic/<ID>` branch exists:

    Skill(skill: "x-internal-epic-branch-ensure", args: "--epic-id <ID> --base develop --push true")

On non-zero exit → `BRANCH_ENSURE_FAILED`.

    TaskUpdate(id: phase2TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-2-Branch ok`

## Phase 3 — Story Loop

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-3-Execute`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-epic-implement --phase Phase-3-Stories")

Open phase tracker (close with `TaskUpdate(id: phase3TaskId, status: "completed")` after all stories complete):

    TaskCreate(subject: "EPIC-XXXX › Phase 3 - Stories", activeForm: "Executing story loop")

> 🔒 **EXECUTION INTEGRITY (Rule 24):** Each `x-story-implement` call below is a **MANDATORY TOOL CALL**.

**Per-story tracking (sequential, chained via `addBlockedBy`):**

    currentStoryId = TaskCreate(subject: "EPIC-XXXX › Phase 3 › story-XXXX-YYYY", activeForm: "Implementing story-XXXX-YYYY")
    [if previous story exists]: TaskUpdate(id: previousStoryId, addBlockedBy: [currentStoryId])

    Skill(skill: "x-story-implement", model: "sonnet", args: "<STORY-ID> --target-branch <epicBranch> --auto-merge <strategy> [--skip-review] [--non-interactive] [--auto-approve-pr]")

    TaskUpdate(id: currentStoryId, status: "completed")

Phase gate: all stories in phase N must be `status=SUCCESS` AND `prMergeStatus=MERGED` before phase N+1. Failed story → block-propagation → `STORY_FAILED` (unless `--revert-on-failure`). Resume: Phase 1 envelope `resumeProjection` provides reclassified story statuses.

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-epic-implement --phase Phase-3-Stories")

TaskUpdate(id: phase3TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-3-Execute ok`

## Phase 4 — Integrity Gate

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-4-Gate`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-epic-implement --phase Phase-4-Gate")

Open phase tracker (close with `TaskUpdate(id: phase4TaskId, status: "completed")` after report):

    TaskCreate(subject: "EPIC-XXXX › Phase 4 - Gate", activeForm: "Running integrity gate and report")

Run integrity gate:

    Skill(skill: "x-internal-epic-integrity-gate", args: "--epic-id <ID> --branch <epicBranch>")

On `passed=false`: remediation agent (or `--revert-on-failure` revert) + one retry → `INTEGRITY_GATE_FAILED`. Then write report:

    Skill(skill: "x-internal-report-write", args: "...")

Post-gate PR-comment remediation (unless `--skip-pr-comments`). **MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24):** Invoke the `x-pr-fix-epic` skill via the Skill tool whenever the gate would otherwise leave actionable Copilot comments unaddressed:

    Skill(skill: "x-pr-fix-epic", model: "sonnet", args: "<EPIC-ID>")

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-epic-implement --phase Phase-4-Gate")

TaskUpdate(id: phase4TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-4-Gate ok`

<!-- phase-no-gate: Phase 5 is skipped for legacy flow; gate lives inside the conditional block -->
## Phase 5 — Final PR (skipped for legacy)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-5-Final-PR`

Open phase tracker (close after PR creation):

    TaskCreate(subject: "EPIC-XXXX › Phase 5 - Final PR", activeForm: "Creating final PR epic/XXXX to develop")

Sync develop into epic branch:

    Skill(skill: "x-git-merge", model: "haiku", args: "--source develop --target epic/<ID> --strategy merge")

On conflict → `FINAL_PR_CONFLICTS`. Then create final PR:

    Skill(skill: "x-pr-create", model: "haiku", args: "--epic-id <ID> --head epic/<ID> --target-branch develop --auto-merge none --label epic-integration")

Interactive menu (`--non-interactive` skips): PROCEED / FIX-PR / ABORT.

    TaskUpdate(id: phase5TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-5-Final-PR ok`

## Error Envelope

| Exit | Code | Condition |
|------|------|-----------|
| 1 | `ARGS_INVALID` | Args normalizer exit 1 |
| 2 | `EPIC_DIR_MISSING` | `plans/epic-XXXX/` absent |
| 3 | `STORY_FAILED` | Story returned `status=FAILED` |
| 4 | `INTEGRITY_GATE_FAILED` | Phase 4 `passed=false` after recovery |
| 5 | `FINAL_PR_CONFLICTS` | Phase 5 develop-sync conflict |
| 6 | `BRANCH_ENSURE_FAILED` | Phase 2 non-zero exit |
| 7 | `PLAN_BUILD_FAILED` | Phase 1 non-zero (non-cyclic) |
| 8 | `CYCLIC_DEPENDENCY` | Phase 1 exit 3 |

## Full Protocol

> Per-phase detail (Phase 0 flow-version detection, Phase 3 retry/backoff/circuit-breaker, Phase 4 integrity-gate recovery algorithm + remediation-agent prompt, Phase 5 TTY-detection + gate menu), legacy-flow phase-by-phase diff (§6), resume workflow for v1/v2 (§7), `SubagentResult` error shape (§8), `--auto-approve-pr` propagation (§9), and `args-schema.json` reference (§1) in [`references/full-protocol.md`](references/full-protocol.md). Idempotency contract and integration notes also in references.
