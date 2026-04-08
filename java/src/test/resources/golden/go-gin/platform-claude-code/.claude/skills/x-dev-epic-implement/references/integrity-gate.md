# Integrity Gate Reference

> **Context:** This reference details the integrity gate between phases.
> Part of x-dev-epic-implement skill.

## Integrity Gate (Between Phases) (RULE-006)

After ALL stories in a phase complete AND all their PRs are merged to `develop`,
dispatch an integrity gate subagent before advancing to the next phase.

The gate runs on `develop` to validate the integrated code from all merged PRs.

### Pre-Phase SHA Capture

At the **start** of each phase, before dispatching any stories:

1. Capture: `mainShaBeforePhase[N] = git rev-parse develop`
2. Persist to checkpoint: `updateCheckpoint(epicDir, { mainShaBeforePhase: { [N]: sha } })`
3. On `--resume`: recover `mainShaBeforePhase[N]` from checkpoint (do NOT recalculate,
   since stories from the phase may already be merged)

### Gate Preconditions

The gate behavior depends on `mergeMode`:

**When `mergeMode != "no-merge"` (auto or interactive with PRs merged):**

Before running the gate, verify all PRs from the phase are merged:

```
for each story in currentPhase:
  assert story.prMergeStatus == "MERGED"
```

Then checkout `develop` with latest merges:
```
git checkout develop && git pull origin develop
```

**When `mergeMode == "no-merge"`:**

PRs are not merged to `develop`. The integrity gate is **DEFERRED**:
1. Per-story validation already runs within `x-dev-lifecycle` (compile, test, coverage per story)
2. Cross-story integration on `develop` cannot be validated (code not merged yet)
3. Log: `"--no-merge: integrity gate deferred for phase {N}. Cross-story integration will be validated after PRs are merged."`
4. Record: `integrityGate.status = "DEFERRED"` in checkpoint
5. Auto-rebase (Section 1.4e) still executes to keep branches current against `origin/develop`
6. Skip directly to phase completion report generation (no gate subagent dispatched)

### Gate Subagent Prompt

Launch a `general-purpose` subagent:

> You are an **Integrity Gate Validator** for {{PROJECT_NAME}}.
>
> **Step 1 — Compile:** Run `{{COMPILE_COMMAND}}` (e.g., `tsc --noEmit`).
> **Step 2 — Test:** Run `{{TEST_COMMAND}}` to execute the full test suite (not just current phase tests).
> **Step 3 — Coverage:** Run `{{COVERAGE_COMMAND}}` to collect coverage metrics.
> **Step 4 — Evaluate:**
> - If compilation fails → `{ status: "FAIL", testCount: 0, coverage: 0 }`
> - If any tests fail → correlate failed tests with commits from stories in the current phase
> - If line coverage < 95% or branch coverage < 90% → FAIL with coverage details
> - Otherwise → proceed to Step 5
> **Step 5 — Smoke Gate:** Execute the full smoke test suite as a regression validation.
> - If `--skip-smoke-gate` flag is set → log `"Integrity gate smoke tests skipped (--skip-smoke-gate)"` and record `smokeGate.status = "SKIP"` → proceed to PASS
> - Run: `{{SMOKE_COMMAND}}` (e.g., `cd java && mvn verify -P integration-tests`)
> - This runs ALL smoke tests, not just those for stories in the current phase
> - If all smoke tests pass → record `smokeGate.status = "PASS"` → overall gate is PASS
> - If any smoke test fails → correlate failures with stories in the current phase (based on files touched) → record `smokeGate.status = "FAIL"` → overall gate is FAIL
>
> Return: `{ status: "PASS"|"FAIL", testCount, coverage, branchCoverage?, failedTests?, regressionSource?, smokeGate?: { status, testsRun, testsFailed, failedTests?, suspectedStories? } }`

### Regression Diagnosis

If tests fail, the subagent:
1. Analyzes which tests broke (`failedTests` array)
2. Correlates failed tests with commits from stories in the current phase (via `git log`)
3. Identifies the most likely story as regression source (`regressionSource`)
4. If identified: orchestrator executes `git revert <commitSha>` for that story
5. Story is marked FAILED with summary: `"Regression detected by integrity gate"`
6. Block propagation is executed for dependents of the failed story

### Smoke Gate Regression Diagnosis

If smoke tests fail (Step 5), the subagent:
1. Identifies which smoke tests failed (`smokeGate.failedTests` array)
2. Correlates failures with stories in the current phase by analyzing files touched by each story's commits
3. Populates `smokeGate.suspectedStories` with the story IDs most likely responsible
4. Logs: `"INTEGRITY GATE SMOKE FAILURE: Phase {N}. {count} test(s) failed. Suspected stories: [{list}]"`
5. The phase is marked as FAILED in the checkpoint
6. The operator decides: `--resume` to retry after manual fix, or `--skip-smoke-gate` to bypass

### Gate Result Registration

```
updateIntegrityGate(epicDir, phaseNumber, {
  status: "PASS" | "FAIL" | "DEFERRED",
  testCount: number,
  coverage: number,        // line coverage %
  branchCoverage?: number, // branch coverage %
  failedTests?: string[],
  regressionSource?: string, // story ID
  smokeGate?: {
    status: "PASS" | "FAIL" | "SKIP",
    testsRun: number,
    testsFailed: number,
    failedTests?: string[],
    suspectedStories?: string[],
    timestamp: string        // ISO-8601
  }
});
```

- **PASS**: Advance to version bump (see below), then to next phase (requires both test gate and smoke gate to pass)
- **FAIL + regression identified**: revert + mark FAILED + block propagation
- **FAIL + regression unidentified**: pause execution, report to user
- **FAIL (smoke gate)**: phase marked FAILED; operator uses `--resume` after fix or `--skip-smoke-gate` to bypass
- **DEFERRED** (when `mergeMode == "no-merge"`): skip gate, advance directly to phase completion report

### Version Bump (Post-Gate) (RULE-013)

After the integrity gate **PASSES** for phase N, the orchestrator performs an automatic
semantic version bump on `develop`. This is skipped when `integrityGate.status == "DEFERRED"`.

1. Determine commit range: `mainShaBeforePhase[N]..develop`
2. Invoke `x-lib-version-bump` logic with the commit range:
   a. Analyze commits in range for highest-priority bump type (MAJOR > MINOR > PATCH > NONE)
   b. If bump type is **NONE**: skip. Log: `"No version-impacting changes in phase {N}. Version unchanged."`
   c. If bump type is MAJOR/MINOR/PATCH:
      - Read current version from pom.xml (strip -SNAPSHOT suffix for base calculation)
      - Calculate next version, append `-SNAPSHOT`
      - Update pom.xml on `develop`
      - Commit: `chore(version): bump to X.Y.Z-SNAPSHOT [phase-{N}]`
      - Push: `git push origin develop`
3. Record version bump in checkpoint:
   ```json
   "versionBump": {
     "phase": N,
     "previousVersion": "X.Y.Z-SNAPSHOT",
     "newVersion": "X.Y.Z-SNAPSHOT",
     "bumpType": "MAJOR|MINOR|PATCH|NONE",
     "commitSha": "abc123..."
   }
   ```
4. Include version bump details in the phase completion report (see Report Content below)

### Checkpoint Smoke Gate Format

The `smokeGate` field is added to each phase entry in `execution-state.json`:

```json
{
  "phases": {
    "0": {
      "status": "SUCCESS",
      "smokeGate": {
        "status": "PASS",
        "testsRun": 45,
        "testsFailed": 0,
        "failedTests": [],
        "suspectedStories": [],
        "timestamp": "2026-03-25T14:30:00Z"
      }
    }
  }
}
```

### Gate Enforcement (RULE-006)

The integrity gate is **mandatory** — there is no bypass. Every phase transition requires a PASS gate
result. The gate runs after phase 0, 1, 2, and 3 — one gate per phase.

The smoke gate within the integrity gate is also mandatory by default. It can only be bypassed with
the `--skip-smoke-gate` flag, which records `smokeGate.status = "SKIP"` in the checkpoint. When
`--skip-smoke-gate` is set, the integrity gate evaluates only Steps 1-4 (compile, test, coverage).
When not set, the smoke gate (Step 5) must also pass for the overall integrity gate to pass.

> **Note:** Each story already executes its own smoke gate via `x-dev-lifecycle` (Phase 2.5).
> The integrity gate smoke tests serve as an ADDITIONAL regression validation — they ensure
> that the combination of all stories in a phase did not break the overall smoke test suite.

### Cross-Story Consistency Gate (RULE-006)

After the integrity gate passes (compile + test + coverage + smoke), run a
cross-story consistency check on the `develop` diff for the phase:

1. Compute diff: `git diff {mainShaBeforePhase[N]}..develop`
2. Dispatch a consistency subagent with the diff for analysis
3. The subagent verifies:
   - Error handling patterns are uniform across classes of the same role
   - Constructor patterns and return types are consistent within modules
   - No cross-story inconsistencies introduced by parallel development
4. Result: `{ status: "PASS"|"FAIL", findings: [...] }`
5. If FAIL: log findings, mark phase as requiring attention (advisory, not blocking)

> **`mainShaBeforePhase` field in `execution-state.json`:**
> Type: `Map<Integer, String>` — SHA-1 hex (40 chars) per phase number.
> Example: `{ "0": "abc123...", "1": "def456..." }`
