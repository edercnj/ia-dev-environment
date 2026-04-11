# Epic Execution Report — EPIC-0034

> **Epic:** EPIC-0034 — Remoção de Targets Não-Claude do Gerador
> **Status:** COMPLETE
> **Started:** 2026-04-10
> **Finished:** 2026-04-11
> **Duration:** ~2 days (orchestration session)
> **Final develop SHA:** `1ae00ea85`
> **Final version:** `2.3.0 → 3.0.0-SNAPSHOT` (MAJOR / BREAKING)

---

## Executive Summary

EPIC-0034 removed support for all non-Claude generation targets (GitHub Copilot, Codex, generic Agents) from the `ia-dev-env` generator. The repository is now Claude-only. The CLI rejects legacy platform values (`copilot`, `codex`, `agents`) with a clear error and exit code 2. A MAJOR version bump communicates the breaking change per Rule 08 (Semantic Versioning).

The epic executed as 5 sequential stories plus 2 infrastructure PRs (baseline fix + planning artifacts). All 7 PRs merged to `develop` with green builds and preserved coverage thresholds.

## Completion Metrics

| Metric | Baseline | Final | Delta |
|---|---|---|---|
| Line coverage | 95.69% | 95.21% | **−0.48pp** (≥ 95% threshold) |
| Branch coverage | 90.69% | 90.00% | **−0.69pp** (= 90% threshold, floor) |
| Total tests | 837 | 6105 (5359 surefire + 746 failsafe) | — |
| Test failures | 0 | 0 | 0 |
| Java main classes | — | **−18** (8 Copilot + 7 Codex + 2 Agents + ReadmeGithubCounter) | |
| Java test classes | — | **−~29 + 2 fixtures** (scope expanded to ~50+ via dependencies) | |
| Resource files | — | **−146** (131 github-copilot/ + 15 codex/) | |
| Golden files | 14285 | 6073 (verified in `reports/task-005-004/verification-report.md` Step 7) | **−8212** |
| `.github/workflows/` (protected) | 95 | 95 | **0** (RULE-003 preserved) |
| `shared/templates/` (protected) | 57 | 57 | **0** (RULE-004 preserved) |
| CLAUDE.md lines | 289 | 135 | **−154** |
| Assembler descriptor count | 34 | 22 | **−12** |

## Pull Request Timeline

| Order | PR | Title | Merge commit | Type |
|---|---|---|---|---|
| 1 | [#266](https://github.com/edercnj/ia-dev-environment/pull/266) | `fix(golden): remove duplicate rule 10 and regenerate manifest` | `6e7e3aae0` | Baseline fix |
| 2 | [#267](https://github.com/edercnj/ia-dev-environment/pull/267) | `chore(epic-0034): planning artifacts for 5 stories (28 tasks)` | `0f85578b6` | Planning |
| 3 | [#274](https://github.com/edercnj/ia-dev-environment/pull/274) | `feat!: remove GitHub Copilot support (story-0034-0001)` | `23d44da54` | Story 1 |
| 4 | [#276](https://github.com/edercnj/ia-dev-environment/pull/276) | `feat(epic-0034): remove Codex target support (story-0034-0002)` | `5a2a5535d` | Story 2 |
| 5 | [#277](https://github.com/edercnj/ia-dev-environment/pull/277) | `feat(epic-0034): remove AssemblerTarget.CODEX_AGENTS and residual .agents/ refs (story-0034-0003)` | `e367aad1c` | Story 3 |
| 6 | [#278](https://github.com/edercnj/ia-dev-environment/pull/278) | `refactor(epic-0034): hygienize shared classes (story-0034-0004)` | `99ff58296` | Story 4 |
| 7 | [#280](https://github.com/edercnj/ia-dev-environment/pull/280) | `feat(cli)!: close EPIC-0034 — claude-only generator (story-0034-0005)` | `1ae00ea85` | Story 5 (closer) |

## Story Results

| # | Story | Phase | Duration (exec) | Coverage after | PR # | Status |
|---|---|---|---|---|---|---|
| 1 | Remove GitHub Copilot | 0 | ~2.6 h | 95.47 / 90.44 | 274 | ✅ |
| 2 | Remove Codex | 1 | ~70 min | 95.23 / 90.06 | 276 | ✅ |
| 3 | Remove Agents target | 2 | ~24 min | 95.22 / 90.04 | 277 | ✅ |
| 4 | Hygienize shared classes | 3 | ~31 min | 95.22 / 90.00 | 278 | ✅ |
| 5 | Docs + final verification | 4 | ~24 min | 95.21 / 90.00 | 280 | ✅ |

**Observation:** Story 1 took the longest because it established the atomic removal pattern (6 coalesced commits including 5 orphan helper deletions to maintain coverage). Stories 2–5 benefited from the pattern and executed faster.

## Breaking Change

Per Rule 08 (Semantic Versioning), the version bumps from `2.3.0` → `3.0.0-SNAPSHOT`.

**CLI contract change:**

- `--platform claude-code` — accepted
- `--platform all` — accepted (declared as the CLI default in `GenerateCommand` `@Option` description; now effectively equivalent to `claude-code` because it is the only remaining platform)
- `--platform` omitted — resolves to the `all` default, which currently produces `claude-code` output
- `--platform copilot` — **REJECTED** with exit code 2
- `--platform codex` — **REJECTED** with exit code 2
- `--platform agents` — **REJECTED** with exit code 2

Error messages contain no class names, stack traces, or file paths (CWE-209 compliance verified).

**Migration path for users:**

1. Replace `--platform copilot|codex|agents` with `--platform claude-code`, or drop the flag (the CLI default is still `all`, which now produces `claude-code` output because it is the only remaining platform).
2. Remove downstream tooling that consumes `.github/instructions/`, `.github/skills/`, `.github/prompts/`, `.codex/config.toml`, `.codex/requirements.toml`, or `.agents/skills/` artifacts.
3. `.github/workflows/` in generated projects is unaffected — CI/CD pipelines continue to work (RULE-003).

## Grep Sanity (final, clean)

```
grep -rn "GithubInstructionsAssembler\|CodexConfigAssembler\|AgentsAssembler\b" java/src/main/java  # 11 hits (only legitimate AgentsAssembler for .claude/agents/)
grep -rn "Platform.COPILOT\|Platform.CODEX"                                      java/src/main/java  # 0
grep -rn "AssemblerTarget.GITHUB\|AssemblerTarget.CODEX\|AssemblerTarget.CODEX_AGENTS" java/src/main/java  # 0
grep -rn "ReadmeGithubCounter\|hasCopilot\|hasCodex"                             java/src/main/java  # 0
grep -rn "\.codex/\|\.agents/"                                                   java/src/main/java  # 0
```

The `AgentsAssembler\b` check uses a word boundary to preserve the legitimate Claude personas writer (discovered during story 3).

## Key Findings and Decisions

### 1. Baseline drift — rule 10 duplicates (PR #266)

Before the epic could start, `mvn clean verify` on `develop` was failing with 51 `PipelineSmokeTest` failures. Root cause: `13-skill-invocation-protocol.md` was added to the source in EPIC-0033 audit hardening (commit `63d2fdd98`) but the goldens still contained a stale `10-skill-invocation-protocol.md` (duplicate from an earlier rename). The commit had only run `mvn test` (surefire), not `mvn verify` (failsafe), so the drift went undetected and shipped to release v2.3.0. Fix PR #266 regenerated goldens + manifest via `GoldenFileRegenerator` + `ExpectedArtifactsGenerator`.

**Lesson:** release pipelines MUST run `mvn verify` (failsafe integration tests), not just `mvn test` (surefire unit tests). Silent golden drift is otherwise invisible until the next regen.

### 2. Forward-migration in story 2 (.agents/ + AGENTS.md)

`CodexSkillsAssembler` and `CodexAgentsMdAssembler` were the SOLE writers of `.agents/` output and `AGENTS.md`. Deleting them in story 2 forced golden regen to also remove ~2910 `.agents/` files and all `AGENTS.md` entries from all 17 profiles. This unavoidably pulled story 3's golden deletion scope into story 2.

Story 3 was consequently reduced to:
- `AssemblerTarget.CODEX_AGENTS` enum entry
- Residual references (FileCategorizer, FileTreeWalker, readme-template, tests)
- `ReadmeGithubCounter` orphan helper deletion

Story 3 completed in 24 min instead of the estimated 1–2 days.

**Lesson:** the story decomposition was correct in spirit but the mechanical coupling between assemblers and their outputs forced cross-story scope shifts. Planning could document "writer ownership" explicitly to predict such shifts upfront.

### 3. Story 3 naming confusion — AgentsAssembler saved

The planning artifacts for story 3 (written before story 2 executed) identified `AgentsAssembler.java` and `AgentsSelection.java` as `.agents/` writers to delete. This was wrong: those classes are the **legitimate writers of `.claude/agents/`** (Claude personas), using `AssemblerTarget.CLAUDE` as their target. They never wrote to the deprecated `.agents/` directory.

The story 3 subagent correctly identified this via grep-before-act and preserved the classes. Had the plan been executed blindly, the Claude personas feature would have been broken.

**Lesson:** planning artifacts can drift from reality as upstream stories execute. Subagents must grep before acting, especially for cross-story dependencies and name collisions. The prompt template for stories 3–5 explicitly instructed subagents to grep first.

### 4. Pre-existing drift — settings.local.json gitignore

Story 4 discovered 9 profile goldens were missing `.claude/settings.local.json` fixtures. Root cause: the user's global gitignore at `~/.config/git/ignore` excludes `**/.claude/settings.local.json`, and `SettingsAssembler` writes the file for all 17 profiles, but only 8 profiles had the fixture tracked. Any future story execution on develop would have failed `GoldenFileTest`.

Story 4 seeded the 9 missing fixtures via `git add -f`.

**Lesson:** global gitignores can silently exclude files that `git add` (non-forced) picks up. For repo fixtures, test the add path explicitly or use repo-level `.gitattributes` / documented `-f` requirements.

### 5. Coverage floor at 90.00% exact (story 4 → 5)

After story 4, branch coverage sat at **exactly 90.00%** — the JaCoCo gate floor. Story 4 had attempted to inline `PlatformContextBuilder.countActive()` as a refactor, which dropped branch coverage to 89.87% and would have tripped the gate. The change was reverted mid-story.

Story 5 was then instructed to **avoid touching production code unless absolutely necessary** to preserve the floor. The closer story was docs-only except for `pom.xml` version bump and a single test assertion update in `Epic0024DocumentationTest.java`.

**Lesson:** coverage floors erode incrementally across long sequential deletion epics. Monitor cumulative delta from baseline (not just per-story delta) and plan production-code refactors early when headroom is largest, not at the end.

### 6. Planning drift accumulates (stories 3, 4, 5)

By story 4, the planning artifacts had significant drift:
- `ReadmeGithubCounter` deletion had already happened (story 3)
- `PlanTemplatesAssembler.copyToTargets` rename had already happened (story 1 PR #274 review)
- `PlatformContextBuilder.hasCopilot/hasCodex` removal had already happened (story 2)
- `ReadmeAssembler` "target branches" never existed directly (discovered in story 2)

Each downstream story subagent needed a "scope drift alert" in its prompt listing which tasks were already done and warning to grep before acting. Without this, stories would have re-attempted already-completed work or failed when the expected code state didn't match.

**Lesson:** in long multi-story epics, planning artifacts become stale as each story executes. Either (a) update planning artifacts after each merge, or (b) pass explicit drift warnings in the dispatch prompt. Option (b) is cheaper and worked well here.

### 7. Concurrent-session worktree contention (entire epic)

This session ran concurrently with a sibling Claude Code session working on `epic-0036` (and later `epic-0037`) in the same repo. The first story-0034-0001 dispatch **failed** with `E_ENV_PARALLEL_WORKTREE_CONFLICT` — the other session's branch switches wiped the subagent's uncommitted work and introduced cross-branch contamination.

Switched to **isolated git worktrees** (one per story under `.claude/worktrees/story-0034-{id}/`) for all subsequent dispatches. Zero further conflicts. This pattern is documented in RULE-018 (Worktree Lifecycle).

**Lesson:** any long-running epic execution in a repo that may have concurrent activity should use isolated worktrees from the start, not as a retry. The overhead (~1 GB disk, 2–3 min setup) is negligible compared to the cost of mid-execution failure.

## RULE Compliance

| Rule | Check | Status |
|---|---|---|
| RULE-001 (Build always green) | `mvn clean verify` green at every commit boundary | ✅ |
| RULE-002 (Coverage ≤ 2pp degradation) | Line −0.48pp, Branch −0.69pp vs baseline | ✅ |
| RULE-003 (`.github/workflows/` protected) | 95 files preserved exactly | ✅ |
| RULE-004 (`shared/templates/` protected) | 57 files preserved exactly | ✅ |
| RULE-005 (Atomic removal per target) | 3 stories, each atomic with enum + code + tests + goldens | ✅ |
| RULE-006 (TDD compliance on removal) | Red-to-green-to-removed pattern respected | ✅ |
| Rule 08 (Semantic Versioning) | MAJOR bump + BREAKING CHANGE footer + CHANGELOG | ✅ |
| Rule 09 (Branching model) | All PRs target develop | ✅ |

## PR Review Metrics

Every execution PR was reviewed by the Copilot bot and had its comments addressed via `/x-pr-fix-comments`:

| PR | Comments | Actionable | Rejected | Fixed | Reply language |
|---|---|---|---|---|---|
| #274 | 4 | 4 | 3 | 1 | PT-BR |
| #276 | 7 | 7 | 0 | 7 | PT-BR |
| #277 | 1 | 1 | 0 | 1 | PT-BR |
| #278 | 1 | 1 | 0 | 1 | PT-BR |
| #280 | 4 | 4 | 0 | 4 | PT-BR |

Total: 17 review comments processed. 3 rejections (all in PR #274) had technical justifications about the difference between "files the generator writes" vs "test fixtures" — the reviewer misread the scope.

## Evidence Archive

Final verification evidence from story 5 TASK-004 is archived at:

```
plans/epic-0034/reports/task-005-004/
├── build-final.log                      (mvn clean verify output)
├── jacoco-final.xml                     (full JaCoCo coverage report)
├── grep-sanity-checks.log               (6 grep checks all clean)
├── cli-accept-default.log               (--platform default)
├── cli-accept-explicit.log              (--platform claude-code)
├── cli-reject-copilot.log               (+ exitcode)
├── cli-reject-codex.log                 (+ exitcode)
├── cli-reject-agents.log                (+ exitcode)
└── verification-report.md               (9-scenario Gherkin verification)
```

Planning artifacts are preserved at `plans/epic-0034/plans/` (43 files: 5 DoR + 5 tasks + 28 task-plans + 5 planning-reports).

## Next Steps (Post-Epic)

1. **Release 3.0.0:** version is at `3.0.0-SNAPSHOT`. The next formal release should cut a `release/3.0.0` branch from `develop`, finalize the CHANGELOG `[Unreleased]` → `[3.0.0]` section, and tag.
2. **Downstream cleanup follow-ups** (optional, out of epic scope):
   - `readme-template.md` still has hardcoded sections describing `.github/` and `.codex/` output — the Platform Selection table was fixed but the architecture diagrams remain. Not visible in generated output, low priority.
   - `MAPPING_TABLE` placeholder in `readme-template.md` has a no-op `ReadmeAssembler.replace()` call left in place (not removed to avoid touching production code with branch coverage at the floor). Cleanup opportunity when coverage headroom increases.
3. **Jira sync (if applicable):** the epic has no Jira key (`—`), so no sync required.
4. **Epic-0035 / Epic-0036 / Epic-0037 coordination:** during this execution, sibling Claude Code sessions worked on other epics in the same repo. The worktree-first pattern proved effective and is now documented for future parallel executions.

## Final Status

**EPIC-0034: COMPLETE**

- All 5 stories: ✅ SUCCESS and MERGED
- All acceptance criteria: ✅
- All RULEs: ✅
- Build: ✅ Green
- Version: ✅ 3.0.0-SNAPSHOT
- `develop` HEAD: `1ae00ea85`

The `ia-dev-env` generator is now Claude-only. Mission accomplished.
