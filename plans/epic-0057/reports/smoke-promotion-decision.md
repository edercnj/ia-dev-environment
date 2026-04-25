# Smoke Promotion Decision — story-0057-0007 (EPIC-0057)

**Date:** 2026-04-25
**Author:** Eder Celeste Nunes Junior

## Decision

**Option B — pre-push hook** under `.githooks/pre-push`, installed via
`scripts/setup-hooks.sh` (one-time per clone with
`git config core.hooksPath .githooks`).

## Rationale

| Factor | Option A (mvn test) | Option B (pre-push hook) |
| :--- | :--- | :--- |
| DX impact on every `mvn test` run | +15-25s per run | 0s |
| Coverage of EPIC-0053 root cause | Yes | Yes |
| Setup per developer | None | One-line: `scripts/setup-hooks.sh` |
| Bypass mechanism | None (always runs) | `CLAUDE_SMOKE_DISABLED=1 git push` |
| Affects CI runtime | No (CI already runs failsafe) | No |
| Risk of "test pollution" of mvn test scope | Yes — surefire/failsafe boundary blurs | No |

The deciding factor is **DX impact on every `mvn test` run**. Developers
in this project commonly invoke `mvn test` dozens of times per day during
TDD inner-loop work (`x-task-implement` Red-Green-Refactor cycles). Adding
~20s to every iteration is a meaningful productivity tax. The pre-push
hook concentrates the smoke cost at the natural integration boundary
(push) without taxing the inner loop.

## Smoke candidates promoted

The following 10 tests run on every `git push` via the pre-push hook:

| Test | Package | Approx. solo time |
| :--- | :--- | :--- |
| `Epic0047CompressionSmokeTest` | `dev.iadev.smoke` | ~3s |
| `Epic0049StoryReportSmokeTest` | `dev.iadev.smoke` | ~3s |
| `Epic0054CompressionSmokeTest` | `dev.iadev.smoke` | ~3s |
| `Epic0055FoundationSmokeTest` | `dev.iadev.smoke` | ~5s |
| `Rule24EvidenceTableExpansionTest` | `dev.iadev.smoke` (EPIC-0057) | ~3s |
| `Rule45CiWatchIntegrityTest` | `dev.iadev.smoke` (EPIC-0057) | ~3s |
| `AuditExecutionIntegrityTest` | `dev.iadev.smoke` (EPIC-0057) | ~2s |
| `AuditBypassFlagsTest` | `dev.iadev.smoke` (EPIC-0057) | ~2s |
| `MandatoryMarkersSmokeTest` | `dev.iadev.smoke` (EPIC-0057) | ~1s |
| `StopHookExtendedTest` | `dev.iadev.smoke` (EPIC-0057) | ~1s |

Total wall-clock when invoked together via `mvn test -Dtest=A,B,C,...`:
**~12-15s** including Maven startup. Single-test invocation costs
dominated by Maven startup (~7s).

## Skip behavior

The hook short-circuits to `exit 0` when the local push only contains
files under `plans/`, `docs/`, `adr/`, `.githooks/`, `README.md`, or
`CHANGELOG.md`. Pure planning / documentation pushes incur zero cost.

## Bypass

```bash
CLAUDE_SMOKE_DISABLED=1 git push
```

NOT recommended — the same suite runs in CI and will block the PR
there. The bypass exists only for emergency hotfixes where CI must
be the gate.

## Setup (per clone)

```bash
scripts/setup-hooks.sh
# OK — core.hooksPath set to .githooks
```

## Re-evaluation triggers

The decision should be revisited if any of the following holds:

- Smoke suite total time exceeds **30s** wall-clock.
- Developers report frequent CI catches that the local hook missed
  (indicates the hook's exclusion list is too aggressive).
- A new smoke test category emerges that requires Docker / network and
  cannot run pre-push — that one should stay in `mvn verify`.
