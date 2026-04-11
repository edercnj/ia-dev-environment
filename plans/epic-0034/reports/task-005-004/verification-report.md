# EPIC-0034 — Story 0034-0005 — TASK-004 Verification Report

**Date:** 2026-04-11
**Worktree:** `.claude/worktrees/story-0034-0005`
**Branch:** `feat/story-0034-0005-docs-verify`
**Build host:** macOS Darwin 25.3.0, Java 21.0.10, Maven 3.9.14

## Summary

End-to-end verification of EPIC-0034 (remove non-Claude targets from generator).
All 8 in-scope Gherkin scenarios PASS. Scenario 9 (PR mergeable) is deferred to TASK-005.

## Step 1 — Full build and tests

`mvn -f java/pom.xml clean verify`

| Metric | Value |
|---|---|
| Result | **BUILD SUCCESS** |
| Total time | 03:26 min (well under 06:01 baseline) |
| Surefire tests | 5359 passed, 0 failures, 0 errors, 0 skipped |
| Failsafe tests | 746 passed, 0 failures, 0 errors, 0 skipped |
| Combined total | **6105 tests passing** |

Build log: `build-final.log`.

## Step 2 — JaCoCo coverage

Extracted from `target/site/jacoco/jacoco.xml` (archived as `jacoco-final.xml`).

| Metric | Covered/Total | Percentage | Threshold (Rule 05) | RULE-002 floor |
|---|---|---|---|---|
| INSTRUCTION | 30325 / 31830 | **95.27%** | — | — |
| BRANCH | 2242 / 2491 | **90.00%** | ≥ 90% | ≥ 88.69% (90.69 − 2pp) |
| LINE | 7441 / 7815 | **95.21%** | ≥ 95% | ≥ 93.69% (95.69 − 2pp) |
| COMPLEXITY | 2401 / 2662 | 90.20% | — | — |
| METHOD | 1385 / 1403 | 98.72% | — | — |
| CLASS | 276 / 278 | 99.28% | — | — |

**Verdict:** Both line and branch coverage meet the absolute Rule 05 thresholds AND stay above the RULE-002 2pp drop floor from the pre-epic baseline (95.69% line / 90.69% branch). Branch is at exactly 90.00% — at the floor — so no further production-code edits should be made in this story without re-running the gate.

## Step 3 — Six grep sanity checks

Full output: `grep-sanity-checks.log`.

| # | Check | Pattern | Expected | Actual | Status |
|---|---|---|---|---|---|
| 1 | Assembler classes | `GithubInstructionsAssembler\|CodexConfigAssembler\|AgentsAssembler\b` | 0 *removed* hits | 0 *removed*; 11 lines for the legitimate `AgentsAssembler.java` (Claude `.claude/agents/` writer, preserved per story-0034-0003 discovery) | PASS |
| 2 | Utility/flag references | `ReadmeGithubCounter\|hasCopilot\|hasCodex` in `java/src/main` | 0 hits | 0 hits | PASS |
| 3 | Target path strings | `\.codex/\|\.agents/` in `java/src/main` | 0 hits | 0 hits | PASS |
| 4 | Platform.java enum | `COPILOT\|CODEX\|CODEX_AGENTS` in `Platform.java` | 0 hits | 0 hits | PASS |
| 5 | AssemblerTarget enum | `AssemblerTarget.GITHUB\|.CODEX\|.CODEX_AGENTS` in `java/src/main/java` | 0 hits | 0 hits (file contains only ROOT and CLAUDE) | PASS |
| 6 | PlatformConverter ACCEPTED_VALUES | `ACCEPTED_VALUES` block in PlatformConverter | shows only `claude-code` (+ `all` keyword) | `Platform.allUserSelectable()` resolves to `claude-code` only at runtime; the appended literal `+ ", all"` keyword preserves backward compatibility | PASS |

## Step 4 — CLI smoke tests

### Rejected platforms (CWE-209 verified)

| Command | Exit code | stderr message | Stack trace leakage | Status |
|---|---|---|---|---|
| `--platform copilot` | 2 | `Invalid value for option '--platform' (<platforms>): Invalid platform: 'copilot'. Valid values: claude-code, all` | None | PASS |
| `--platform codex` | 2 | `Invalid platform: 'codex'. Valid values: claude-code, all` | None | PASS |
| `--platform agents` | 2 | `Invalid platform: 'agents'. Valid values: claude-code, all` | None | PASS |

CWE-209 grep on stderr (`Exception|at dev\.iadev|at java\.|\.java:[0-9]+`): **0 matches**.

Logs: `cli-reject-copilot.log`, `cli-reject-codex.log`, `cli-reject-agents.log` (and `*-exitcode.log` for the captured exit codes).

### Accepted platforms

| Command | Exit code | Files on disk (`find`) | CLI category summary total | Status |
|---|---|---|---|---|
| `generate --platform claude-code --stack java-spring --output /tmp/gen-test` | 0 | 343 | 219 | PASS |
| `generate --stack java-spring --output /tmp/gen-default` (default flag) | 0 | 343 | 219 | PASS |

Note on the two file counts: the **343** value is the raw file count reported by `find /tmp/gen-* -type f | wc -l` — it walks the full output directory tree and counts every file, including per-file contents inside skill subdirectories (each skill has a `SKILL.md` plus multiple reference files under `references/`). The **219** value comes from the CLI's verbose summary, which groups files into 15 categories via `FileCategorizer` — the Skills category, for example, counts 149 logical entries that expand to many more files on disk. Both numbers are correct under their respective measurement methods; the CLI display is category-oriented (human-readable summary), while the `find` count is filesystem-oriented (manifest comparison).

The story DoR target was ~830 ± 5%, derived from a pre-epic planning estimate. The actual claude-only output for `java-spring` is **343 files on disk** (matching the regenerated `expected-artifacts.json` manifest). The reduction from baseline (~9500) is approximately **96%**, exceeding the 91% projection.

Logs: `cli-accept-explicit.log`, `cli-accept-default.log` (contain the 219 CLI category summary).

## Step 5 — RULE-003 workflows preservation

```
find java/src/test/resources/golden -path '*/.github/workflows*' -type f | wc -l
```

Result: **95** (exact match to baseline-pre-epic). Status: **PASS**.

## Step 6 — RULE-004 shared templates preservation

```
git diff origin/main -- java/src/main/resources/shared/templates/
find java/src/main/resources/shared/templates -type f | wc -l
```

Result: diff is **empty**, count is **57** (exact match to baseline). Status: **PASS**.

## Step 7 — Golden total count and other invariants

| Path | Count | Expected range | Status |
|---|---|---|---|
| `java/src/test/resources/golden` (total files) | **6073** | [5801, 6413] | PASS |
| `.github/workflows/` golden files | **95** | exactly 95 (RULE-003) | PASS |
| Non-workflow `.github/` golden files | **0** | exactly 0 | PASS |
| `.codex/` golden files | **0** | exactly 0 | PASS |
| `.agents/` golden files | **0** | exactly 0 | PASS |
| `shared/templates/` files | **57** | exactly 57 (RULE-004) | PASS |

## Step 8 — Gherkin scenario evidence matrix

| # | Scenario | Evidence | Status |
|---|---|---|---|
| 1 | Build verde no estado final | Step 1 BUILD SUCCESS + Step 2 coverage | PASS |
| 2 | Grep sanity checks limpos | Step 3 (all 6 checks) | PASS |
| 3 | CLI rejects removed platforms | Step 4 reject tests (exit 2, no leakage) | PASS |
| 4 | CLI claude-code funciona | Step 4 accept tests (exit 0, 343 files) | PASS |
| 5 | RULE-004 templates shared intactos | Step 6 (diff empty, 57 files) | PASS |
| 6 | RULE-003 workflows preservados | Step 5 + Step 7 (95 files) | PASS |
| 7 | CLAUDE.md atualizado sem resíduos | TASK-001 grep (0 residual `copilot|codex|.github/|.codex/|.agents/`) | PASS |
| 8 | expected-artifacts.json regenerado | TASK-003 (only `.github/workflows` paths remain — RULE-003) | PASS |
| 9 | PR final mergeable em develop | Deferred to TASK-005 | PENDING |

## Step 9 — Archived evidence files

```
plans/epic-0034/reports/task-005-004/
├── build-final.log              -- mvn verify output
├── jacoco-final.xml             -- JaCoCo coverage XML
├── grep-sanity-checks.log       -- 6 grep checks results
├── cli-reject-copilot.log       -- --platform copilot rejection
├── cli-reject-copilot-exitcode.log
├── cli-reject-codex.log         -- --platform codex rejection
├── cli-reject-codex-exitcode.log
├── cli-reject-agents.log        -- --platform agents rejection
├── cli-reject-agents-exitcode.log
├── cli-accept-explicit.log      -- --platform claude-code success
├── cli-accept-default.log       -- default flag success
└── verification-report.md       -- this document
```

## Step 10 — No commit

Per task plan, this task produces no git commit. The verification report and archived evidence are the deliverables. TASK-005 picks up with PR creation.

## Risks observed

- **Branch coverage at the floor (90.00%):** any future production-code edit that removes an incidentally-covered branch will trip the JaCoCo gate. Subsequent stories should add unit tests to lift the floor, or treat the branch threshold as a hard guardrail.
- **`--platform all` still accepted:** the code keeps `all` as a backward-compatibility alias that now means "generate claude-code only" (since claude-code is the only target). The CHANGELOG was updated to document this. Future cleanup could remove `all` entirely in a follow-up minor release.
- **Unused MAPPING_TABLE replace in ReadmeAssembler:** removing the placeholder from `readme-template.md` left the `content.replace("{{MAPPING_TABLE}}", ...)` call as a no-op in `ReadmeAssembler.java`. Tests still pass (negative assertions). Cleanup is a low-priority follow-up — touching it now would risk the branch coverage floor.
