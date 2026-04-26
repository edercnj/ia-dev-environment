# Rule 45 — CI-Watch Integrity

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 21 (Epic Branch Model), Rule 22 (Skill Visibility), Rule 24 (Execution Integrity).
> **Introduced by:** EPIC-0057 (Extensão da Rule 24 — Pós-mortem EPIC-0053).

## Purpose

The skill `x-pr-watch-ci` is the canonical entry point for "wait for CI + Copilot review" inside every orchestrator that creates a PR. Before EPIC-0057 the contract for this skill (`RULE-045-*`) lived only inside `x-pr-watch-ci/SKILL.md`, so neither Camada 1 (normative) nor Camada 4 (audit) could reference a formal rule when an orchestrator silently skipped the CI-watch step. Rule 45 promotes the contract to a first-class, always-loaded rule and consolidates the previously scattered `RULE-045-01..06` into a single auditable surface.

The rule defines:

1. **When `x-pr-watch-ci` is mandatory** — every PR-creation step in an orchestrator MUST follow it with a CI-watch invocation, unless the explicit opt-out flag below is in scope.
2. **The 8 stable exit codes** that orchestrators may dispatch on (changes are SemVer events — see Rule 08).
3. **The fallback matrix** when the CI environment misbehaves (no checks configured, Copilot bot absent, network failure).
4. **The `--no-ci-watch` opt-out**, restricted to the `## Recovery` section of a calling skill.
5. **Forbidden behaviors** that bypass the contract.
6. **The CI audit** (`scripts/audit-bypass-flags.sh`) that catches violations.

## Exit Codes Matrix (RULE-045-05)

The 8 codes below are the **public contract** of `x-pr-watch-ci`. Orchestrators MUST dispatch on them by name, never by raw integer. Adding a new code is a MINOR version bump; changing the semantics of an existing code is a MAJOR version bump (Rule 08 — SemVer).

| Exit | Code | Condition | Suggested orchestrator action |
| :--- | :--- | :--- | :--- |
| 0 | `SUCCESS` | All CI checks green AND Copilot review present (or `--require-copilot-review=false`). | Proceed with merge. |
| 10 | `CI_PENDING_PROCEED` | All checks green BUT Copilot review timeout elapsed without a review. | Proceed with caution; surface a WARNING in the gate menu. |
| 20 | `CI_FAILED` | At least one check returned `failure`, `timed_out`, `cancelled`, or `action_required`. | Block merge; route to `x-pr-fix` or interactive FIX-PR slot. |
| 30 | `TIMEOUT` | Global polling timeout elapsed with checks still pending. | Surface to operator; offer ABORT / extend / proceed. |
| 40 | `PR_ALREADY_MERGED` | PR was already merged before / during polling — idempotent exit. | Treat as SUCCESS; no further action. |
| 50 | `NO_CI_CONFIGURED` | `statusCheckRollup` is empty — no CI configured for this PR. | Skip CI gate; rely on review-only. |
| 60 | `PR_CLOSED` | PR was closed without merge during polling. | ABORT; require operator decision. |
| 70 | `PR_NOT_FOUND` | PR does not exist or caller lacks permission. | Fail-fast; investigate auth / PR id. |

## Fallback Matrix

When the CI environment misbehaves, the skill MUST resolve to one of the codes above — no exit code outside this table is ever emitted.

| Scenario | Resolved exit code | Rationale |
| :--- | :--- | :--- |
| Repository has zero check runs | `50` (`NO_CI_CONFIGURED`) | Fail-open — caller decides whether to proceed without CI. |
| Copilot bot is not a reviewer on the PR | `0` if other checks green AND `--require-copilot-review=false`; otherwise `10` after Copilot timeout | Copilot absence is operational, not a CI failure. |
| `gh pr view` fails with non-zero exit | `70` (`PR_NOT_FOUND`) after one retry | Treat auth/network failures as PR unreachable — caller decides. |
| Polling exceeds global timeout | `30` (`TIMEOUT`) | Operator MUST intervene; never silent-pass. |
| PR is closed (not merged) mid-poll | `60` (`PR_CLOSED`) | Stop polling; report state to caller. |
| PR was merged before first poll iteration | `40` (`PR_ALREADY_MERGED`) | Idempotent — caller invocation can be safely retried. |

The skill never emits a non-table code; emitting one is a Rule 45 violation and a `LifecycleIntegrityAuditTest` failure.

## `--no-ci-watch` Constraints

The opt-out flag `--no-ci-watch` exists for two narrowly scoped contexts:

1. **`## Recovery` blocks of a calling skill** — when an orchestrator is recovering from a partial failure and CI has already been validated upstream. The opt-out MUST appear inside (or be argued for inside) a `## Recovery` heading of the calling SKILL.md.
2. **CI / automated environments** where the surrounding pipeline already gates on the same CI signal (e.g., the GitHub Actions workflow itself) and a second poll would deadlock waiting for itself.

Every `--no-ci-watch` occurrence outside one of these contexts is caught by `scripts/audit-bypass-flags.sh` (story-0057-0005) and fails the CI build with `BYPASS_FLAG_VIOLATION`. Per-line escape hatch: `<!-- audit-exempt: <reason> -->` immediately preceding the opt-out, used only for reviewed exceptions.

## Mandatory Invocation Sites

Every orchestrator that creates a PR via `x-pr-create` MUST follow the creation with one `x-pr-watch-ci` invocation, in this order:

```
... task implementation ...
Skill(skill: "x-pr-create", model: "haiku", args: "...")    # creates PR
Skill(skill: "x-pr-watch-ci", args: "--pr-number <PR>")     # MANDATORY (Rule 45)
... dispatch on exit code ...
```

The MANDATORY marker MUST be visible (Rule 24 §Camada-1). Silent omission of the watch step is a `PROTOCOL_VIOLATION` and fails the Camada 3 audit (story-0057-0002) when the merged PR has no `.claude/state/pr-watch-{PR}.json` artifact.

Canonical orchestrators with mandatory invocation sites:

| Orchestrator | Phase carrying the watch step |
| :--- | :--- |
| `x-task-implement` | Step 4.5 (post `x-pr-create`) |
| `x-story-implement` | Phase 2.2.8.5 (post task PRs and post story PR) |
| `x-release` | Step 8 (post release PR) |
| `x-epic-implement` | Phase 5 (post final epic-to-develop PR) |
| `x-pr-merge-train` | Per-PR (after auto-merge gate) |

## Forbidden

- Inlining a manual `gh pr checks <PR> --watch` instead of invoking `x-pr-watch-ci` — bypasses the state-file contract (RULE-045-03) used for resume.
- Catching a non-zero exit and treating it as success without a documented `--no-ci-watch` opt-out.
- Hard-coding a numeric exit code (e.g., `if [ $? -eq 20 ]`) instead of the canonical name — orchestrators MUST use the public contract names.
- Removing the `.claude/state/pr-watch-*.json` artifact post-merge — it IS the Camada 2/3 evidence that the watch ran (Rule 24 §Mandatory Evidence Artifacts).
- Calling `x-pr-watch-ci` without a `--pr-number` argument when the PR is known — the skill cannot resume from state-file alone if the caller does not pre-supply the PR id.

## Audit

Three layers verify Rule 45 enforcement, mirroring the four-layer model of Rule 24:

1. **Camada 1 — normative.** This rule is loaded into every conversation. CLAUDE.md cross-links to it from the "EXECUTION INTEGRITY" block.
2. **Camada 2 — runtime Stop hook.** `verify-story-completion.sh` (extended in story-0057-0006) checks for `.claude/state/pr-watch-{PR}.json` whenever the conversation merged a PR.
3. **Camada 3 — CI audit.** `scripts/audit-execution-integrity.sh` (story-0057-0002) verifies the state-file exists for every merged story PR. `scripts/audit-bypass-flags.sh` (story-0057-0005) verifies `--no-ci-watch` only appears inside `## Recovery` blocks.
4. **Camada 4 — observability.** The state-file IS the proof. If `x-pr-watch-ci` ran, the file exists. If the file does not exist, the skill was not invoked.

Self-check: `scripts/audit-execution-integrity.sh --self-check` MUST verify this rule file exists and that `x-pr-watch-ci/SKILL.md` references `RULE-045-05`. Missing either fails CI with `RULE_45_ENFORCEMENT_BROKEN`.

## Backward Compatibility

Rule 45 is **additive**. Pre-existing orchestrators retrofitted in story-0057-0004 add the MANDATORY marker around their existing `x-pr-watch-ci` invocations; orchestrators that genuinely never gated on CI (e.g., pure planning skills) remain unaffected. Legacy state-file format from EPIC-0045 is the same contract — no data migration required.

## Forbidden Additions

- Adding a 9th exit code without a SemVer MINOR bump and a migration entry in this rule.
- Renaming an existing code (e.g., `SUCCESS` → `OK`) — public contract, MAJOR bump only.
- Allowing a per-orchestrator opt-out flag distinct from `--no-ci-watch` — single opt-out path, no aliases.
