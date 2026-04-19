---
status: Accepted
date: 2026-04-16
deciders:
  - Eder Celeste Nunes Junior
story-ref: "story-0043-0001"
---

# ADR-0010: Interactive Gates Convention (Rule 20)

## Status

Accepted | 2026-04-16

## Context

Skills that orchestrate long-running operations pause execution at decision
points where a human must approve, reject, or fix something before the workflow
continues. Examples: `x-release` pauses before merging the release PR, waiting
for someone to confirm the draft looks correct; `x-story-implement` pauses after
creating a task PR under `--manual-task-approval`; `x-epic-implement` pauses
after creating a batch of story PRs under `--manual-batch-approval`;
`x-review-pr` terminates with "NO-GO" after exhausting auto-remediation retries.

An incident during the v3.6.0 release (PR #392) crystallised the problem. The
Phase 8 APPROVAL-GATE printed:

```
Skill pausada. Após merge da PR #392, rode:
  /x-release 3.6.0 --continue-after-merge
para retomar.
```

The operator had to memorise the exact resume command, had no in-band shortcut
to invoke `x-pr-fix` to correct the PR without leaving the context, and — when
`x-pr-fix` was called manually — had to re-enter the `x-release` flow with a
second resume command. Three invocations to complete what should be a one-option
interaction.

A survey of the existing codebase revealed five distinct gate implementations:

| Skill | Mechanism | Default |
| :--- | :--- | :--- |
| `x-release` Phase 8 | HALT text + `--interactive` opt-in | HALT |
| `x-story-implement` Phase 0.5 (contract) | HALT text + `--manual-contract-approval` opt-in | HALT |
| `x-story-implement` Phase 2.2.9 (task PR) | Auto-merge + `--manual-task-approval` opt-in | Auto |
| `x-epic-implement` batch gate | HALT text + `--manual-batch-approval` opt-in | HALT |
| `x-review-pr` retry-exhausted gate | Silent termination | None |

There is no ADR or rule establishing a common shape. Each skill invented its own
protocol, creating variability in operator experience, poor discoverability
(flags are buried in long skill docs), and no loop-back behaviour (correcting a
PR required the operator to leave the skill, run `x-pr-fix`, then restart or
resume manually). EPIC-0039 introduced a good prior art with its structured
approval-gate workflow for `x-release`; EPIC-0042 demonstrated the pattern of
making structured interaction the default rather than an opt-in. This ADR
formalises both insights as a project-wide convention.

## Decision

We adopt the **Interactive Gates Convention**, documented as Rule 20
(`20-interactive-gates.md`), applicable to all orchestrating skills that pause
for human approval.

### D1 — Fixed-option menu as the default

Every interactive gate MUST present **exactly 3 options** via `AskUserQuestion`:

| Slot | Option | Label | Action |
| :--- | :--- | :--- | :--- |
| 1 | PROCEED | `"Continue (Recommended)"` | Advance the workflow from current state |
| 2 | LOOP-BACK | `"Run x-pr-fix and retry"` (PR variant) or `"Regenerate and retry"` (no-PR variant) | Delegate to a fix/regenerate skill then re-present the same menu |
| 3 | ABORT | `"Cancel the operation"` | Terminate the skill with cleanup |

The menu is the **default** behaviour; no flag is required to activate it. The
only opt-out is `--non-interactive`, which produces the legacy HALT text plus
exit 0 (for CI/automation use).

### D2 — FIX-PR loop-back with guard-rail

When the operator selects slot 2, the skill invokes the fix/correction skill via
Rule 13 Pattern 1 INLINE-SKILL:

```
Skill(skill: "x-pr-fix", args: "<pr-number>")          # single-PR gates
Skill(skill: "x-pr-fix-epic", args: "--epic <id>")      # multi-PR gates
```

On return from the fix skill, the same menu is re-presented — the operator does
not need to restart or resume. To prevent infinite fix loops, the guard-rail
caps consecutive slot-2 invocations at **3**. When the 3rd fix attempt returns,
the gate auto-terminates with error code `GATE_FIX_LOOP_EXCEEDED` and emits a
textual orientation for manual recovery. No 4th option is added to the menu; the
total always remains exactly 3 options.

### D3 — Uniform state file schema

Every gate that persists state uses the following JSON shape:

```json
{
  "phase": "<skill-specific phase name>",
  "lastPhaseCompletedAt": "<ISO-8601 UTC>",
  "lastGateDecision": "<PROCEED|FIX_PR|ABORT|null>",
  "fixAttempts": [],
  "schemaVersion": "1.0"
}
```

State is written atomically via `.tmp` + rename. Validation failure emits
`GATE_SCHEMA_INVALID`. The state schema is the same across all skills; only
`phase` values differ.

### D4 — Deprecation of existing opt-in flags

| Old flag | Skill | New behaviour |
| :--- | :--- | :--- |
| `--interactive` | `x-release` | No-op with deprecation warning (menu is now default) |
| `--manual-contract-approval` | `x-story-implement` | No-op with deprecation warning |
| `--manual-task-approval` | `x-story-implement` | No-op with deprecation warning |
| `--manual-batch-approval` | `x-epic-implement` | No-op with deprecation warning |

Hard removal is deferred to a future epic after a deprecation period. Note:
`x-release --interactive` under `--dry-run` has a separate dry-run semantic
(aborting without `--dry-run`); that semantic is preserved unchanged in
story-0043-0002 and is not affected by this deprecation.

### D5 — Audit enforcement

Rule 20 defines a grep-based audit (analogous to Rule 13's audit) that CI must
run after all retrofits (story-0043-0006). The audit produces exit 1 when:

- A `HALT` or `paused` text block appears in a SKILL.md without a paired
  `AskUserQuestion` in the same file
- Any deprecated opt-in flag (`--interactive`, `--manual-task-approval`,
  `--manual-contract-approval`, `--manual-batch-approval`) appears in a
  code-path section (outside `## Triggers` and `## Examples`)

During the migration period (stories 0043-0001 to 0043-0005), the audit runs in
`--baseline` mode and reports matches without blocking CI. It switches to
blocking mode in story-0043-0006.

## Consequences

### Positive

- **Uniform operator experience.** The same 3-option menu appears in every
  gate. The operator learns one pattern, not four.
- **In-band fix loop.** The operator corrects a PR and reapproves without
  leaving the skill or memorising resume commands.
- **Auditability.** The grep audit detects regressions to HALT text or
  deprecated flags automatically.
- **CI-friendly opt-out.** `--non-interactive` replaces the patchwork of four
  opt-in flags with a single consistent escape hatch for automation.

### Negative

- **Migration work.** Four skills require retrofits (stories 0043-0002 through
  0043-0005). Each retrofit touches a SKILL.md and its golden file, and adjusts
  the `allowed-tools` frontmatter to include `AskUserQuestion`.
- **Breaking change for HALT consumers.** Scripts that parse the textual HALT
  message to extract the resume command will break. Mitigation: `--non-interactive`
  restores the textual output; no script needs to change immediately.

### Neutral

- Gate state files are skill-specific; there is no cross-skill state sharing.
  Each skill maintains its own `*-state.json`.
- The `x-review-pr` retry-exhausted gate (story-0043-0005) uses PROCEED with a
  contextual `description` ("Retry remediation (+2 loops)"), not a new slot name.
  This is not a new option; it is a skill-specific `description` on the fixed
  PROCEED slot — Rule 20 §Canonical Option Menu permits this.

## Alternatives Considered

### A1 — Per-skill gate design (rejected)

Allow each skill to design its own gate shape, documented only within that
skill's SKILL.md. This is the status quo. Rejected because operator cognitive
load does not decrease with each new skill added; there is no shared contract
to point to during reviews, and audit automation is impossible without a
canonical shape to lint against.

### A2 — Maintain HALT + opt-in flags (rejected)

Keep HALT as the default and improve discoverability of opt-in flags by
documenting them prominently. Rejected because the EPIC-0039 analysis showed
that opt-in flags for interactive features are underused: operators tolerate
the HALT workflow until they hit a breaking incident (see v3.6.0 context). A
behaviour that is opt-in will stay opt-in even when the default experience is
demonstrably worse. The right default is interactive; `--non-interactive` for
CI is the correct direction.

### A3 — 4th emergency option `FORCE-PROCEED` after 3 fixes (rejected)

Add a 4th option that appears only after the guard-rail is triggered, allowing
the operator to force-proceed past the failed fix state. Rejected because it
violates Rule 20 RULE-002 ("EXACTLY 3 options in any gate state") and introduces
uncertainty about the menu shape. When 3 fixes fail to converge, human triage
is required anyway; the correct response is to emit `GATE_FIX_LOOP_EXCEEDED`
with textual guidance for manual recovery via `--non-interactive` or direct
state-file editing, not to introduce a 4th escape option that bypasses the
fix discipline.

## References

- [Rule 13 — Skill Invocation Protocol](.claude/rules/13-skill-invocation-protocol.md)
- [Rule 20 — Interactive Gates](.claude/rules/20-interactive-gates.md)
- [EPIC-0039 — Release Orchestrator (prior art: approval-gate-workflow.md)](plans/epic-0039/)
- [EPIC-0042 — Merge-Train (default interactive style reference)](plans/epic-0042/)
- [story-0043-0001](plans/epic-0043/story-0043-0001.md)
