# Rule 24 — Execution Integrity

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 22 (Skill Visibility), Rule 05 (Quality Gates).
> **Introduced by:** EPIC-0052 (Execution Integrity Enforcement).

## Purpose

Every `Skill(skill: "...", args: "...")` declaration inside a SKILL.md body is a **tool call**, never prose, never a conceptual description to be "simulated" by the LLM in its own reasoning window. When the LLM executing a skill inlines what a sub-skill would do instead of actually emitting the tool call, the multi-agent architecture collapses into single-agent best-effort, silently bypassing specialist reviews, verify gates, and evidence production.

Rule 24 codifies the invariant and defines the enforcement layers that detect and block violations: runtime (Stop hook), CI (audit script), and normative (this rule + assertive SKILL.md language).

## Non-inlining Contract

The LLM executing any skill MUST NOT:

1. **Summarize** what a declared sub-skill would do in place of invoking it.
2. **Simulate** the sub-skill's output in its own context window.
3. **Skip** a declared sub-skill on the grounds of "trivial case", "obvious result", "already verified manually", or any similar heuristic.
4. **Claim** that a phase passed without the corresponding sub-skill having been invoked.

The only legitimate way to not invoke a declared sub-skill is via an explicit `--skip-*` flag passed by the caller:

| Flag | Skill it bypasses |
| :--- | :--- |
| `--skip-review` | `x-review` + `x-review-pr` |
| `--skip-verification` | `x-internal-story-verify` |
| `--skip-smoke` | smoke test inside verify gate |
| `--skip-pr-comments` | `x-pr-fix-epic` post-gate |

Every `--skip-*` usage outside a `## Recovery` block in the calling skill is a Rule 22 violation AND a Rule 24 violation — both caught by the audit.

## Mandatory Evidence Artifacts

Certain sub-skills MUST produce a persistent artifact as proof of execution. The Camada 3 CI audit (`scripts/audit-execution-integrity.sh`) checks existence of these files for every merged story PR:

| Sub-skill | Artifact path | Enforced by |
| :--- | :--- | :--- |
| `x-internal-story-verify` | `plans/epic-XXXX/reports/verify-envelope-STORY-ID.json` | Camada 3 |
| `x-review` | `plans/epic-XXXX/plans/review-story-STORY-ID.md` | Camada 3 |
| `x-review-pr` | `plans/epic-XXXX/plans/techlead-review-story-STORY-ID.md` | Camada 3 |
| `x-internal-story-report` | `plans/epic-XXXX/reports/story-completion-report-STORY-ID.md` | Camada 3 |
| `x-arch-plan` | `plans/epic-XXXX/plans/arch-story-STORY-ID.md` | Camada 3 (soft) |

Absence of any mandatory artifact on a merged story fails the CI audit with `EIE_EVIDENCE_MISSING`.

## Enforcement Layers

Four defense-in-depth layers. A violation caught by any layer fails the lifecycle.

### Camada 1 — Normative (this rule + assertive SKILL.md + CLAUDE.md)

- This rule is loaded into every conversation (rules are always active).
- Root `CLAUDE.md` carries a top-level "EXECUTION INTEGRITY — NÃO NEGOCIÁVEL" block.
- Orchestrator SKILL.md files (`x-story-implement`, `x-epic-implement`, `x-task-implement`) phrase every sub-skill invocation as **MANDATORY TOOL CALL** and reference this rule by exit code.

### Camada 2 — Runtime Stop hook

- `.claude/hooks/verify-story-completion.sh` fires on every `Stop` event (end of LLM turn).
- Detects recent PR-creation / story-completion activity via telemetry NDJSON or `gh` output.
- Checks that required sub-skills appear in `plans/epic-*/telemetry/events.ndjson` for the active story.
- On missing skill, emits a visible WARNING on stderr and exits with code 1, which Claude Code surfaces to the LLM as a blocking notification.

### Camada 3 — CI audit

- `scripts/audit-execution-integrity.sh` runs on every PR to `develop` (via the CI workflow) and on every PR to `epic/*` branches.
- For each story-branch merge detected via `git log`, verifies the mandatory artifact set exists.
- Fails the CI build with `EIE_EVIDENCE_MISSING` if evidence is absent.
- Escape hatch: baseline file `audits/execution-integrity-baseline.txt` grandfathers stories merged before this rule was introduced. Newly-merged stories cannot be added to the baseline.
- Per-story escape: `<!-- audit-exempt: <reason> -->` line in the story markdown (rare, reviewed exceptions).

### Camada 4 — Observability (mandatory artifacts)

Sub-skills that "count" for Rule 24 are required by contract to emit their evidence file. The artifact IS the proof. If a sub-skill is genuinely invoked, the file exists. If the file does not exist, the sub-skill was not invoked — no room for ambiguity.

## Audit Script Contract

`scripts/audit-execution-integrity.sh` exit codes:

| Exit | Code | Meaning |
| :--- | :--- | :--- |
| 0 | `OK` | All merged stories have required evidence (or are grandfathered). |
| 1 | `EIE_EVIDENCE_MISSING` | At least one merged story lacks mandatory artifacts. |
| 2 | `EIE_BASELINE_CORRUPT` | `audits/execution-integrity-baseline.txt` malformed. |
| 3 | `EIE_INVALID_EXEMPTION` | `audit-exempt` marker missing a reason. |

## Baseline (Grandfather List)

`audits/execution-integrity-baseline.txt` lists stories merged before Rule 24 was introduced. Format: one `STORY-ID` per line, with trailing `# reason` comment. No new entries may be added after Rule 24 merges — CI refuses additions via a separate immutability check.

Example (initial baseline):

```
story-0051-0001  # pre-Rule-24, merged 2026-04-23 (PR #602), inline execution was legal at time
```

## Forbidden

- Removing or weakening any of the four layers.
- Adding stories to the baseline after the rule was introduced.
- Writing a SKILL.md that declares a sub-skill call in prose form without making it a MANDATORY TOOL CALL block.
- Bypassing the audit with `--no-verify` or `git commit -n`.

## Audit

The audit is self-auditing: `scripts/audit-execution-integrity.sh --self-check` verifies that this rule file exists, the Stop hook is registered in `settings.json`, and the baseline file is present. Any missing piece fails the CI build with `EIE_ENFORCEMENT_BROKEN`.
