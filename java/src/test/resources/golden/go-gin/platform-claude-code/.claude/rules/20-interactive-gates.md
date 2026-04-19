# Rule 20 — Interactive Gates Convention

> **Related:** Rule 13 (Skill Invocation Protocol — the `FIX-PR` handler MUST
> use Rule 13 Pattern 1 INLINE-SKILL). ADR-0010 (decision record for this
> convention). Applies to all orchestrating skills that pause for human
> approval; exceptions listed in §Scope.

## Rule

Every interactive gate in an orchestrating skill MUST present the operator with
**exactly 3 options** via `AskUserQuestion`. The menu is the **default**
behaviour — it fires whenever execution reaches a decision point, without any
flag. The only opt-out is `--non-interactive`, which produces legacy HALT text
plus exit 0 (for CI/automation use).

The `FIX-PR` option (slot 2) invokes a correction skill via Rule 13 Pattern 1
INLINE-SKILL and reapresents the same menu on return — the operator never needs
to restart or resume manually. Consecutive slot-2 invocations are capped at 3;
exceeding the cap auto-terminates the gate with `GATE_FIX_LOOP_EXCEEDED`.

## Scope

**In scope — skills with interactive gates:**

| Skill | Gate |
| :--- | :--- |
| `x-release` | Phase 8 APPROVAL-GATE (PR merge approval) |
| `x-story-implement` | Phase 0.5 contract gate; Phase 2.2.9 task-PR gate |
| `x-epic-implement` | Batch-gate after each story wave |
| `x-review-pr` | Retry-exhausted gate after auto-remediation fails |

**Out of scope — skills without gates:**

`x-code-format`, `x-code-lint`, `x-test-run`, `x-test-tdd`, `x-git-commit`,
`x-git-push`, `x-git-worktree`, `x-pr-create`, `x-task-implement`,
`x-story-plan`, `x-epic-map`, and any skill operating exclusively in batch
mode with no human decision point.

## Canonical Option Menu

Every gate presents **exactly** these 3 slots. Slot labels are invariant.
Only the `description` MAY be contextualised per skill (see §Scope table and
the note on `x-review-pr` below).

| Slot | Canonical Option | `header` (≤12 chars) | `label` (1–5 words) | `description` guidance |
| :--- | :--- | :--- | :--- | :--- |
| 1 — PROCEED | PROCEED | `"Proceed"` | `"Continue (Recommended)"` | Describe the concrete action that "proceed" means in the current skill context. Example for `x-release`: `"Merge the release PR and cut the tag."` Example for `x-review-pr` retry-exhausted: `"Re-dispatch auto-remediation (+2 loops)."` |
| 2 — LOOP-BACK (PR variant) | FIX-PR | `"Fix PR"` | `"Run x-pr-fix and retry"` | `"Invokes x-pr-fix on the current PR; reapresents this menu on return."` |
| 2 — LOOP-BACK (no-PR variant) | REJECT | `"Reject"` | `"Regenerate and retry"` | `"Returns to the previous step with feedback to regenerate the artifact; reapresents this menu on return. Use only when the gate has no PR (e.g., contract gate in x-story-implement Phase 0.5)."` |
| 3 — ABORT | ABORT | `"Abort"` | `"Cancel the operation"` | `"Terminates the skill with cleanup. For release and epic gates, confirm twice before aborting."` |

> **Note on `x-review-pr` retry-exhausted gate:** slot 1 PROCEED here means
> "re-dispatch auto-remediation"; the label remains `"Continue (Recommended)"`.
> This is NOT a new option name — it is the invariant PROCEED slot with a
> skill-specific `description`. Rule 20 RULE-002 forbids new slot names.

### AskUserQuestion call shape

```markdown
AskUserQuestion(
  question: "<context-specific question>",
  options: [
    {
      header: "Proceed",
      label: "Continue (Recommended)",
      description: "<skill-specific action description>"
    },
    {
      header: "Fix PR",
      label: "Run x-pr-fix and retry",
      description: "Invokes x-pr-fix on the current PR; reapresents this menu on return."
    },
    {
      header: "Abort",
      label: "Cancel the operation",
      description: "Terminates the skill with cleanup."
    }
  ]
)
```

The `allowed-tools` frontmatter of each skill that uses a gate MUST include
`AskUserQuestion` and `Skill` (for the INLINE-SKILL handler in slot 2).

## State File Schema

Every gate that persists state uses this canonical JSON shape. The schema is
versioned at `"1.0"`. Writes are atomic: write to `<path>.tmp`, then rename.

```json
{
  "phase": "<skill-specific phase machine value>",
  "lastPhaseCompletedAt": "<ISO-8601 UTC timestamp>",
  "lastGateDecision": "<PROCEED|FIX_PR|ABORT|null>",
  "fixAttempts": [],
  "schemaVersion": "1.0"
}
```

| Field | Type | Required | Notes |
| :--- | :--- | :--- | :--- |
| `phase` | `String` | Yes | Skill-specific phase machine value, e.g. `"APPROVAL_PENDING"` |
| `lastPhaseCompletedAt` | `String` (ISO-8601 UTC) | Yes | Set on every write |
| `lastGateDecision` | `String \| null` | Yes | Always present; `null` before first interaction; one of `PROCEED`, `FIX_PR`, `ABORT` after |
| `fixAttempts` | `Array<FixAttempt>` | Yes | Always present; empty before first fix; ≤ 3 items |
| `schemaVersion` | `String` | Yes | Literal `"1.0"` |

**`FixAttempt` sub-object:**

| Field | Type | Required | Notes |
| :--- | :--- | :--- | :--- |
| `at` | `String` (ISO-8601 UTC) | Yes | Timestamp of the fix attempt |
| `delegateSkill` | `String` | Yes | `"x-pr-fix"` or `"x-pr-fix-epic"` |
| `prNumber` | `Integer` | Yes (PR gates) | PR number passed to the fix skill |
| `outcome` | `String` | Yes | One of `applied`, `no_comments`, `compile_regression`, `aborted` |

**Validation failure** emits `GATE_SCHEMA_INVALID` with the path and the
missing/malformed field name.

## Default Behavior

The menu always fires when execution reaches a gate. No flag is required.

```
GATE REACHED: <gate name>

<context summary>

How would you like to proceed?
  1. Continue (Recommended) — <skill-specific action>
  2. Run x-pr-fix and retry — Invokes x-pr-fix on current PR; reapresents menu on return
  3. Cancel the operation   — Terminates skill with cleanup
```

`--non-interactive` flag: when present, the gate skips `AskUserQuestion`,
prints the legacy HALT text and exit 0. This preserves backward compatibility
for CI pipelines and automation scripts that expect the old textual output.

## FIX-PR Loop-Back and Guard-Rail

When slot 2 (FIX-PR or REJECT) is selected:

1. Record the attempt in `fixAttempts[]` with a timestamp, the delegate skill
   name, PR number (if applicable), and a pending `outcome`.
2. Invoke the fix/correction skill via Rule 13 Pattern 1 INLINE-SKILL:

   ```markdown
   Skill(skill: "x-pr-fix", args: "<pr-number>")
   ```

   or for multi-PR gates:

   ```markdown
   Skill(skill: "x-pr-fix-epic", args: "--epic <id>")
   ```

3. On return, update `outcome` in the last `FixAttempt` entry.
4. Reapresent the full gate menu (do NOT advance to the next phase).

**Guard-rail:** if `fixAttempts.size()` is already 3 when slot 2 is selected,
skip the `Skill(...)` invocation and emit:

```
GATE_FIX_LOOP_EXCEEDED: 3 consecutive fix attempts did not resolve the gate.
Gate terminated automatically. To recover:
  1. Inspect the PR manually and apply a direct fix.
  2. Resume the skill with --non-interactive to skip this gate.
  3. Or edit the state file directly to reset fixAttempts to [] and
     set lastGateDecision to PROCEED.
```

The gate terminates immediately after this message. No 4th option is shown;
the total option count remains exactly 3 for all previous presentations.

## Deprecation of Opt-In Flags

The following flags are deprecated as of EPIC-0043. They emit a one-time
deprecation warning and are **no-ops** — the default interactive menu fires
regardless of their presence. Hard removal is deferred to a future epic.

| Deprecated flag | Skill | Warning text |
| :--- | :--- | :--- |
| `--interactive` | `x-release` | `"[DEPRECATED] --interactive is no longer needed; the gate menu is now the default. Use --non-interactive to suppress it."` |
| `--manual-contract-approval` | `x-story-implement` | `"[DEPRECATED] --manual-contract-approval is no longer needed; the gate menu is now the default."` |
| `--manual-task-approval` | `x-story-implement` | `"[DEPRECATED] --manual-task-approval is no longer needed; the gate menu is now the default."` |
| `--manual-batch-approval` | `x-epic-implement` | `"[DEPRECATED] --manual-batch-approval is no longer needed; the gate menu is now the default."` |

> **Exception — `x-release --interactive` dry-run semantic:** when `--interactive`
> is paired with `--dry-run`, it retains its original dry-run meaning (interactive
> dry-run mode that aborts without `--dry-run`). This interaction is explicitly
> handled in story-0043-0002 and is NOT deprecated by this rule.

## Forbidden

- Using HALT text (unstructured `"Skill paused. Run /x-foo to resume."` messages)
  as the default gate mechanism in any in-scope skill.
- Presenting fewer or more than 3 options at any gate in any state.
- Adding a 4th option (`FORCE-PROCEED`, `SKIP`, etc.) to handle edge cases; the
  guard-rail and `--non-interactive` are the sanctioned escape hatches.
- Invoking `x-pr-fix` or `x-pr-fix-epic` via bare-slash (`/x-pr-fix`) inside a
  gate handler — use Rule 13 Pattern 1 INLINE-SKILL only.
- Omitting `AskUserQuestion` or `Skill` from the `allowed-tools` frontmatter of
  any skill that implements a gate.
- Persisting gate state without the 5 mandatory fields (`phase`,
  `lastPhaseCompletedAt`, `lastGateDecision`, `fixAttempts`, `schemaVersion`).
- Hard-coding `schemaVersion` to any value other than `"1.0"` until the schema
  version is formally incremented via an ADR update.

## Audit Command

The canonical CI guard is `scripts/audit-interactive-gates.sh` (story-0043-0006).
Run it directly to verify the production codebase:

```bash
# Standard run during migration (suppresses baseline-listed files):
scripts/audit-interactive-gates.sh --baseline

# Strict run (fails if ANY file has violations, including baseline files):
scripts/audit-interactive-gates.sh
```

**Exit codes:** 0 = AUDIT PASSED, 1 = AUDIT FAILED, 2 = execution error.

The script enforces two rules simultaneously:

- **Regex 1** — HALT text (`HALT`, `Skill pausada`, `paused.`) without
  `AskUserQuestion` in the same ±30-line window.
- **Regex 2** — Deprecated flags (`--interactive`, `--manual-task-approval`,
  `--manual-contract-approval`, `--manual-batch-approval`) outside the
  allowlisted `## Triggers` and `## Examples` sections. Uses a tokenised
  end-of-token lookahead `([^a-zA-Z0-9_-]|$)` so that `--interactive-merge`
  is **not** a false positive.

**Migration mode:** `audits/interactive-gates-baseline.txt` lists the source
SKILL.md files pending retrofit (stories 0043-0002 to 0043-0005). Once all
retrofits complete and the baseline file is empty, the `--baseline` flag can
be removed from the `mvn test` invocation to switch CI to strict mode.

**Scope:** `java/src/main/resources/targets/claude/skills/core/{ops,dev,review}/**`
`core/lib/**` is explicitly excluded (utility skills, not interactive gates).

> **Legacy manual commands** (pre-story-0043-0006, kept for reference only):
>
> ```bash
> # Audit 1 (manual fallback):
> grep -rlE "(Skill pausada|paused\.|HALT)" \
>     java/src/main/resources/targets/claude/skills/core/ \
>     --include=SKILL.md \
>   | xargs -I{} bash -c \
>     'grep -qE "AskUserQuestion" "{}" || echo "VIOLATION: {}"'
>
> # Audit 2 (manual fallback):
> grep -rnE "\-\-(interactive|manual-task-approval|manual-contract-approval|manual-batch-approval)" \
>     java/src/main/resources/targets/claude/skills/core/ \
>     --include=SKILL.md \
>   | grep -v "## Triggers" | grep -v "## Examples" | grep -v "DEPRECATED"
> ```

## Rationale

The v3.6.0 release incident (PR #392) demonstrated the real cost of HALT-text
gates: the operator had to memorise a resume command, manually invoke `x-pr-fix`
outside the skill, and re-enter the flow with a second resume command — three
separate interactions for what should have been one. The existing opt-in flags
(`--interactive`, `--manual-*`) were poorly discovered and left the default
experience broken for interactive use.

EPIC-0039 introduced the structured `approval-gate-workflow.md` for `x-release`
as a partial remedy; EPIC-0042 established the principle that structured
interaction should be the default, not an opt-in. This rule generalises both
insights across the full set of orchestrating skills, establishing a single
auditable contract so that future skills do not re-invent the shape of their
decision gates.
