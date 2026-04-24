# QA Review — EPIC-0055 Foundation (PR #633)

```
ENGINEER: QA
STORY: epic-0055-foundation (PR #633)
SCORE: 22/36
STATUS: Partial
```

## Context

PR #633 (`epic/0055 → develop`) delivers stories 0055-0001 and 0055-0002 of EPIC-0055
(Task Hierarchy & Phase Gate Enforcement). The change set is overwhelmingly
infrastructure / documentation / bash scripts:

- `.claude/rules/25-task-hierarchy.md` (174 lines)
- `.claude/skills/x-internal-phase-gate/SKILL.md` (348 lines) + `references/full-protocol.md` (190 lines)
- `adr/ADR-0014-task-hierarchy-and-phase-gates.md` (134 lines)
- `scripts/audit-task-hierarchy.sh` (223 lines) + `scripts/audit-phase-gates.sh` (205 lines)
- `.claude/hooks/verify-phase-gates.sh` (77 lines) + `.claude/hooks/enforce-phase-sequence.sh` (99 lines)
- `audits/task-hierarchy-baseline.txt` (22 lines)
- `HooksAssembler.java` + `HookConfigBuilder.java`: two small, additive edits
  registering the new hook scripts in `settings.json`
- Commits on branch since develop: `a46cdcab9`, `1ad0b9e68` (merge), `b44914a9d`, `0c1c42ea4`

Total diff: 86 files, ~10.7k insertions / 57 deletions (mostly golden-fixture copies
across 7 platforms: `java-spring`, `java-spring-hexagonal`, `java-spring-neo4j`,
`java-spring-fintech-pci`, `java-quarkus`, `java-spring-elasticsearch`,
`platform-claude-code`).

## Scoring (6 items × 2 pts each wouldn't cover DoD; used 6 × 6 = 36 pts)

### PASSED

- **[Q1] Java code coverage via golden + assembler tests (6/6)** —
  The only production Java in this PR is the additive change to
  `HooksAssembler.java` (register 2 new scripts in `SUPPORTED_HOOKS`) and
  `HookConfigBuilder.java` (emit `verify-phase-gates.sh` in Stop array,
  emit `enforce-phase-sequence.sh` under a new `PreToolUse` block).
  Coverage:
  - `HooksAssemblerTest` — 12 tests, all green.
  - `HookConfigBuilderTest` — 10 tests, all green.
  - `SettingsHooksAndJsonTest` — 4 tests, all green.
  - `GoldenFileTest` — 9 platforms compared byte-for-byte against
    regenerated fixtures, all green.
  - `PlatformGoldenFileTest` — 1 test green.
  Verified locally via `mvn test -Dtest='HooksAssemblerTest,HookConfigBuilderTest,SettingsHooksAndJsonTest,GoldenFileTest,PlatformGoldenFileTest'`:
  **36 tests, 0 failures**. Because the generator's output drives all 7
  platform fixtures, any regression in the assembler is caught by
  byte-equality.

- **[Q2] Audit script self-checks (6/6)** —
  Both `scripts/audit-task-hierarchy.sh --self-check` and
  `scripts/audit-phase-gates.sh --self-check` execute and exit 0. Verified:
  ```
  $ bash scripts/audit-task-hierarchy.sh --self-check
  self-check: OK          (exit 0)
  $ bash scripts/audit-phase-gates.sh --self-check
  self-check: OK          (exit 0)
  ```
  Additionally, running each audit in full-scan mode against the current
  repo state (`scripts/audit-task-hierarchy.sh` with no flags,
  `scripts/audit-phase-gates.sh` with no flags) also exits 0 — meaning the
  baseline (`audits/task-hierarchy-baseline.txt`, 22 lines, 5 comment lines
  + grandfathered entries) correctly exempts every pre-existing orchestrator
  that lacks `TaskCreate` calls, and no new violation has been introduced.
  The self-check wires: rule file exists, baseline file exists, skills-root
  directory exists — then returns `exit 4 / TASK_HIERARCHY_ENFORCEMENT_BROKEN`
  on any break. The pattern mirrors `scripts/audit-execution-integrity.sh`
  (Rule 24, EPIC-0052) which is the correct precedent.

- **[Q3] Regression risk on main test suite (6/6)** —
  `mvn test` on `epic/0055` HEAD (commit `0c1c42ea4`): **3890 tests,
  0 failures, 0 errors, 14 skipped. BUILD SUCCESS** in 1m 08s. Matches the
  baseline claim in the review brief. The `skipped` count (14) is
  unchanged from develop, confirming no tests were disabled to hide
  regressions. The two Java edits are additive and surgical: nothing removed,
  nothing behavior-changed for existing code paths. Risk of regression on
  existing hooks (`SessionStart`, `PostToolUse`, `SubagentStop`, `Stop`) is
  zero — only new entries added.

### PARTIAL

- **[Q4] Acceptance-criteria (Gherkin) coverage (1/6)** —
  Story 0055-0001 Section 7 declares **6 Gherkin scenarios** for the
  `x-internal-phase-gate` skill (PRE with clean predecessor; PRE with pending
  predecessor; POST with all artifacts; POST with missing artifacts; WAVE
  post-Batch-B; malformed invocation → exit 13). **NONE are implemented as
  tests in this PR.**
  Story 0055-0002 Section 6 declares **5 Gherkin scenarios** (audit OK;
  audit missing TaskCreate → exit 25; baseline grandfather; Stop-hook warns
  on failed gate; PreToolUse-hook blocks out-of-sequence Skill invocation).
  **NONE are implemented as tests in this PR.**
  The 1/6 credit is for the *golden-fixture presence check*: if the hook
  files or the rule file ever regress, the assembler will emit different
  bytes and `GoldenFileTest` will fail — so "files exist with exact
  content" is weakly covered. But behavioural validation (does the skill
  actually return exit 12 on missing artifacts? does the PreToolUse hook
  actually exit 2 when passed=false?) is not executed by any test.
  **Fix:** the team correctly flagged this in the review brief as "deferred
  to the post-hoc smoke test in story 0055-0011" — the DoD of this PR does
  not match Rule 05's "Acceptance tests exist and validate end-to-end
  behavior" checklist. This is a **HIGH** severity finding: the entire
  enforcement stack (Rule 25 + phase-gate skill + 2 CI scripts + 2 hooks) is
  being merged without a single behavioral test. A latent bug in
  `enforce-phase-sequence.sh`'s jq filtering would not be caught until it
  fails in production during a real story run.
  **Improvement:** either (a) bring forward STORY-0055-0011 before merging
  the enforcement layer into production use, or (b) add at least one
  bats-style smoke test per audit script and per hook that exercises the
  success / fail path with fixture JSON, and one per skill mode (pre / post
  / wave / final) with a fixture `execution-state.json`. Minimum: 4 skill
  mode tests + 4 scripts × 2 paths = 12 tests to satisfy the absolute
  minimum of "happy path + 1 failure path per entry point".
  Severity: **HIGH**.

- **[Q5] Bash coverage ≥ 95% (DoD of story 0055-0002) (1/6)** —
  Story 0055-0002 DoD requires ≥95% line coverage for the bash artifacts
  (`audit-task-hierarchy.sh`, `audit-phase-gates.sh`, `verify-phase-gates.sh`,
  `enforce-phase-sequence.sh`). Total 604 lines of bash. **No coverage
  instrumentation exists for bash in this repo** (no `bashcov`, no `kcov`,
  no `shellcheck --check-sourced` with coverage mapping). Effective
  coverage: 0%.
  The 1/6 partial credit is for static-quality positives:
  - The scripts follow a consistent structure (header docblock, exit-code
    table, `parse_args`, `self_check`, main loop).
  - Both audit scripts honour `--self-check` and `--json` flags.
  - Hooks are correctly fail-open for all ambiguous cases
    (`enforce-phase-sequence.sh` lines 27-58 short-circuit on missing jq /
    wrong tool_name / non-canonical skill / disabled tracking / bootstrap
    empty phaseGateResults).
  - Stop hook (`verify-phase-gates.sh`) follows the same fail-open contract.
  **Fix:** introduce `bats-core` (or `bashcov` + kcov bridge) in CI and wire
  it into a GitHub Actions step for `scripts/**/*.sh` and `.claude/hooks/**/*.sh`.
  Alternatively, accept that bash coverage is a non-goal and explicitly
  amend story 0055-0002 DoD to say "manual smoke verification" — but that
  is a spec-adjustment, not a test-quality substitute.
  Severity: **MEDIUM** (the DoD gap is declared by the operator as
  "deferred to story 0055-0011" — deferral is legitimate provided the
  deferral is tracked in `execution-state.json` and the main baseline never
  accepts a merged epic without the follow-up story being planned).

- **[Q6] Test quality / naming / consistency (2/6)** —
  For the Java side: the two edits to `HookConfigBuilder` and
  `HooksAssembler` are covered by tests that already follow the project's
  naming convention (`@DisplayName` + `[methodUnderTest]_[scenario]_[expected]`
  derivable patterns). The new `appendPreToolUseWithPhaseSequence(...)`
  helper is exercised indirectly by the existing
  `appendHooksSection — telemetry variants` suite (3 tests green). No new
  Java test class was added — this is fine for additive changes but weak if
  the PreToolUse branch structure ever needs independent mocking.
  For the bash side: no tests at all (see Q5). The scripts themselves are
  well-commented, but "well-commented" is not "tested".
  The 2/6 credit splits as:
  - (+2) Existing test quality + golden-fixture parity is high.
  - (−4) No new unit/contract tests for HookConfigBuilder's new branch,
    no tests for the bash scripts, no Gherkin acceptance tests for the
    skill modes.
  **Fix:** add a dedicated `HookConfigBuilderPhaseGateTest` with 3 tests:
  (a) telemetry-enabled → PreToolUse array has 2 hooks and Stop array has 3;
  (b) telemetry-disabled → PreToolUse absent, Stop has 2; (c) generated JSON
  is valid (delegate to `JsonAssertions.assertValidJson`).
  Severity: **MEDIUM**.

## Final totals

| Item | Score | Weight |
| :--- | :--- | :--- |
| Q1 — Java coverage (assemblers) | 6 | 6 |
| Q2 — Audit self-checks | 6 | 6 |
| Q3 — Regression risk | 6 | 6 |
| Q4 — Gherkin scenario coverage | 1 | 6 |
| Q5 — Bash coverage vs. DoD | 1 | 6 |
| Q6 — Test quality / new-branch coverage | 2 | 6 |
| **Total** | **22** | **36** |

**STATUS: Partial.** The Java-side portion of the PR is production-grade and
would pass a GO on its own merits. The bash + skill enforcement layer is
delivered without behavioral tests — the 11 Gherkin scenarios across the two
stories are zero-covered. Merging as-is is defensible **only if** story
0055-0011 is explicitly scheduled before EPIC-0055 closes and the merge is
into `epic/0055` (not `develop`), so the enforcement stack lives behind the
epic branch until tests exist. If this PR is proposed directly to `develop`,
the recommendation flips to **Rejected** pending Q4 + Q5 remediation.

## Evidence

- `mvn test` on `epic/0055` HEAD (`0c1c42ea4`): 3890 tests, 0 failures,
  14 skipped, BUILD SUCCESS (1m 08s).
- `scripts/audit-task-hierarchy.sh --self-check`: exit 0.
- `scripts/audit-phase-gates.sh --self-check`: exit 0.
- `scripts/audit-task-hierarchy.sh` (full scan): exit 0.
- `scripts/audit-phase-gates.sh` (full scan): exit 0.
- Targeted Java tests (36 tests: `HooksAssemblerTest`, `HookConfigBuilderTest`,
  `SettingsHooksAndJsonTest`, `GoldenFileTest`, `PlatformGoldenFileTest`):
  all green.
- `grep -rln x-internal-phase-gate /java/src/test` finds **only** golden
  fixture copies under `src/test/resources/golden/**/.claude/skills/` — no
  behavioral test.

## Recommended follow-up before epic → develop

1. Pull forward STORY-0055-0011 (post-hoc smoke tests) OR create a spike
   story that adds `bats-core` + at least 12 behavioral tests for the skill
   modes, 2 audit scripts, and 2 hooks (happy + one failure path each).
2. Add `HookConfigBuilderPhaseGateTest` Java unit test isolating the new
   `appendPreToolUseWithPhaseSequence` method.
3. Verify baseline immutability: the baseline file mentions "IMMUTABLE
   after EPIC-0055 merges into main" but no CI check enforces it yet.
   Add one in `audit-task-hierarchy.sh --self-check` (e.g., git blame
   check that forbids post-merge additions).
