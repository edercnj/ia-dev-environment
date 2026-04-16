# PR Review Comments -- Consolidated Report

- **Epic:** EPIC-0039
- **Date:** 2026-04-15
- **PRs Analyzed:** 6
- **Total Comments:** 46
- **Unique Findings:** 46 (after deduplication)

## Summary

| Category | Count | % |
|----------|-------|---|
| Actionable | 33 | 71.7% |
| Suggestion | 12 | 26.1% |
| Question | 1 | 2.2% |
| Praise | 0 | 0.0% |
| Resolved | 0 | 0.0% |
| Duplicates Removed | 0 | -- |

## Actionable Findings

| # | PRs | File | Line | Summary | Has Suggestion | Theme |
|---|-----|------|------|---------|----------------|-------|
| 1 | #373 | java/src/main/resources/targets/claude/skills/core/ops/x-release/references/git-flow-cycle-explainer.md | 48 | The rendered summary body contains Portuguese headings/sentences, but the `x-rel... | Yes | placeholder |
| 2 | #373 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 1484 | This again cites “security RULE-003”, but in `x-release` RULE-003 is defined as ... | Yes | security |
| 3 | #373 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 1461 | Step 13.2 builds `LAST_TAG`/`NEW_TAG` with a leading `v`, but the Phase 13 templ... | Yes | placeholder |
| 4 | #373 | plans/epic-0039/story-0039-0005.md | 5 | `**Status:** Concluida` is inconsistent with the status enum used elsewhere in t... | Yes | consistency |
| 5 | #373 | plans/epic-0039/IMPLEMENTATION-MAP.md | 15 | Status value `Concluida` doesn’t match the documented allowed status values in t... | Yes | consistency |
| 6 | #373 | java/src/main/resources/targets/claude/skills/core/ops/x-release/references/git-flow-cycle-explainer.md | 8 | This references “security RULE-003”, but within `x-release` docs RULE-003 is alr... | Yes | security |
| 7 | #373 | java/src/main/resources/targets/claude/skills/core/ops/x-release/references/git-flow-cycle-explainer.md | 39 | The template renders `release/{{NEW_TAG}}` and `{{LAST_TAG}}-SNAPSHOT`, but the ... | No | placeholder |
| 8 | #373 | (review-level) | -- | ## Pull request overview  Adds a new read-only “Phase 13 — SUMMARY” to the `x-re... | No | placeholder |
| 9 | #374 | java/src/main/java/dev/iadev/release/GitTagReader.java | 11 | Unused import `java.util.Arrays` should be removed to keep the file clean. ```su... | Yes | other |
| 10 | #374 | plans/epic-0039/story-0039-0001.md | 5 | Status value is spelled without the accent ("Concluida"). Elsewhere in the epic ... | No | other |
| 11 | #374 | java/src/main/java/dev/iadev/release/SemVer.java | 18 | SemVer.parse(...) currently accepts a leading "v" (it strips it before matching)... | No | consistency |
| 12 | #374 | (review-level) | -- | ## Pull request overview  Implements story-0039-0001 by adding a small `dev.iade... | No | testing |
| 13 | #375 | plans/epic-0039/story-0039-0006.md | 5 | The status value is written as "Concluida" (no accent), but other story files co... | Yes | other |
| 14 | #375 | plans/epic-0039/implementation-map-0039.md | 16 | This implementation map documents the allowed status values as including "Conclu... | Yes | other |
| 15 | #375 | plans/epic-0039/IMPLEMENTATION-MAP.md | 16 | This implementation map documents the allowed status values as including "Conclu... | Yes | other |
| 16 | #375 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 1418 | This block includes Portuguese text ("Confirmação obrigatória"), but the skill's... | Yes | other |
| 17 | #375 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 1452 | The CHANGELOG extraction snippet uses an awk regex with `${VERSION}` unescaped. ... | Yes | other |
| 18 | #375 | java/src/main/java/dev/iadev/release/changelog/ChangelogBodyExtractor.java | 80 | trimBlankLines() only trims trailing '\n' characters. If the extracted section e... | Yes | testing |
| 19 | #375 | (review-level) | -- | ## Pull request overview  Implements EPIC-0039 story-0039-0006 for the `x-releas... | No | testing |
| 20 | #376 | java/src/main/java/dev/iadev/release/integrity/VersionExtractor.java | 15 | The comment says this regex targets the first `<version>` child of the `<project... | Yes | other |
| 21 | #376 | (review-level) | -- | ## Pull request overview  Adds “integrity drift” validation as sub-check 10 in t... | No | testing |
| 22 | #377 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 510 | `RESULTS_DIR` is removed unconditionally via `trap 'rm -rf "$RESULTS_DIR"' EXIT`... | No | other |
| 23 | #377 | plans/epic-0039/story-0039-0004.md | 5 | Spelling: "Concluida" should be "Concluída" (accent) in PT-BR. | No | other |
| 24 | #377 | plans/epic-0039/IMPLEMENTATION-MAP.md | 14 | Spelling: "Concluida" should be "Concluída" (accent) in PT-BR. | No | other |
| 25 | #377 | java/src/main/java/dev/iadev/release/validate/ParallelCheckExecutor.java | 121 | In awaitResult(), the catch-all `catch (Exception e)` always calls `Thread.curre... | No | other |
| 26 | #377 | (review-level) | -- | ## Pull request overview  Implements story-0039-0004 by parallelizing the `x-rel... | No | testing |
| 27 | #378 | java/src/main/java/dev/iadev/release/state/ReleaseState.java | 88 | `ReleaseState` is missing the `worktreePath` field, but the state-file schema do... | No | other |
| 28 | #378 | java/src/main/java/dev/iadev/release/state/StateFileValidator.java | 40 | `StateFileValidator` currently enforces only `schemaVersion` and `nextActions`. ... | No | other |
| 29 | #378 | java/src/main/resources/targets/claude/skills/core/ops/x-release/references/state-file-schema.md | 219 | The `STATE_SCHEMA_VERSION` message template is written in Portuguese (and withou... | No | naming |
| 30 | #378 | java/src/main/resources/targets/claude/skills/core/ops/x-release/references/state-file-schema.md | 74 | The schema doc says `nextActions[].command` MUST match `^/[a-z\-]+`, but the can... | Yes | other |
| 31 | #378 | java/src/main/java/dev/iadev/release/state/StateFileValidator.java | 63 | `validateNextActions` iterates the list without guarding against `null` entries;... | Yes | other |
| 32 | #378 | java/src/main/java/dev/iadev/release/state/StateFileValidator.java | 50 | The migration hint / schema-version rejection message is in Portuguese (`nao eh ... | No | other |
| 33 | #378 | (review-level) | -- | ## Pull request overview  Adds **schemaVersion 2** for the `x-release` persisted... | No | testing |

## Suggestion Findings

| # | PRs | File | Line | Summary | Theme |
|---|-----|------|------|---------|-------|
| 1 | #374 | java/src/main/java/dev/iadev/release/ConventionalCommitsParser.java | 89 | Breaking commits can be double-counted: a commit like "feat!: ..." that also con... | other |
| 2 | #374 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 280 | This section includes Portuguese banner strings (e.g., "Próxima versão detectada... | other |
| 3 | #374 | java/src/main/resources/targets/claude/skills/core/ops/x-release/references/auto-version-detection.md | 75 | The banner format/examples are written in Portuguese (e.g., "Próxima versão dete... | other |
| 4 | #374 | plans/epic-0039/IMPLEMENTATION-MAP.md | 11 | Status is spelled "Concluida" here, but the status legend below uses "Concluída"... | other |
| 5 | #374 | java/src/main/java/dev/iadev/release/GitTagReader.java | 126 | `run(...)` reads stdout fully and then reads stderr. If the `git` process writes... | other |
| 6 | #376 | java/src/main/java/dev/iadev/release/integrity/RepoFileReader.java | 80 | `resolveSafely` checks `normalize()` + `candidate.startsWith(repoRoot)`, which d... | other |
| 7 | #376 | java/src/main/java/dev/iadev/release/integrity/DiffTodoScanner.java | 42 | `scan()` adds the file path to `results` for every matched marker, so a diff wit... | other |
| 8 | #377 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 519 | `run_check` executes the provided command via `bash -c "$cmd"`. In this block, s... | other |
| 9 | #377 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 567 | The block claims `--max-parallel` is honored (and earlier docs state `--max-para... | other |
| 10 | #377 | java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md | 503 | The `MAX_PARALLEL` bounds check uses `-lt/-gt` directly on `$MAX_PARALLEL`. If t... | other |
| 11 | #378 | java/src/main/java/dev/iadev/release/state/StateFileValidator.java | 71 | `COMMAND_PATTERN` is anchored only at the start and validation uses `matcher(...... | other |
| 12 | #378 | java/src/test/java/dev/iadev/application/assembler/ReleaseStateFileSchemaTest.java | 187 | `ReleaseStateFileSchemaTest` now asserts the schema doc example uses `schemaVers... | consistency |

## Questions Requiring Human Response

| # | PR | File | Line | Reviewer | Question |
|---|-----|------|------|----------|----------|
| 1 | #374 | java/src/main/java/dev/iadev/release/GitTagReader.java | 43 | Copilot | The class Javadoc says `fromRef` is validated against a "narrow regex" before being forwarded to `git log`, but `SAFE_RE... |

## Recurring Themes

| Theme | Count | Affected PRs | Description |
|-------|-------|--------------|-------------|
| testing | 6 | #374,#375,#376,#377,#378 | Test-related improvements or corrections |
| placeholder | 4 | #373 | Ambiguous or incorrect placeholder/template variables |
| consistency | 4 | #373,#374,#378 | Inconsistent patterns requiring standardization |
| security | 2 | #373 | Security-related findings (OWASP, injection, XSS) |
| naming | 1 | #378 | Inconsistent placeholder or entity naming conventions |
| other | 29 | #374,#375,#376,#377,#378 | Uncategorized findings requiring manual review |

## Dry-Run Summary

Fixes NOT applied. Review the actionable findings above and re-run without --dry-run to apply corrections.

- Actionable findings ready for fix: 33
- Suggestion findings (requires --include-suggestions): 12
- Questions requiring human response: 1
