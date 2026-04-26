# Audit Gates Catalog

> **Version:** 1.0 (EPIC-0058)
> **Maintained by:** Engineering Platform
> **Last updated:** 2026-04-26

---

## How to Read This Catalog

Every governance gate in this repository belongs to one of four canonical layers
(Rule 26 — Audit Gate Lifecycle):

| Layer | Prefix | Lives in | Runs when |
| :--- | :--- | :--- | :--- |
| **Hook runtime** | `verify-*.sh` | `.claude/hooks/` | Claude Code `Stop` / `PreToolUse` event |
| **CI script** | `audit-*.sh` | `scripts/` | PR open/sync via GitHub Actions |
| **Java test** | `*AuditTest.java` / `*Lint.java` | `src/test/java/` | `mvn verify` |
| **CI workflow** | `*.yml` | `.github/workflows/` | GitHub Actions on push/PR |

**RULE-004 — Catalog-before-Add:** No gate may be introduced in any Rule without a
simultaneous entry in this catalog. A PR introducing a Rule reference to an `audit-*.sh`
script without a catalog entry fails `scripts/audit-skill-visibility.sh`.

---

## Master Table

> Ordered by Layer → Rule. Future entries append to the appropriate layer group.

| Nome | Camada | Rule | Localização | Exit Codes | Self-check | Invocado por |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `audit-flow-version.sh` | ci-script | Rule 19 | `scripts/audit-flow-version.sh` | 0,1,2,3 | Sim | `.github/workflows/audit.yml` |
| `audit-epic-branches.sh` | ci-script | Rule 21 | `scripts/audit-epic-branches.sh` | 0,1,2,3 | Sim | `.github/workflows/audit.yml` |
| `audit-skill-visibility.sh` | ci-script | Rule 22 | `scripts/audit-skill-visibility.sh` | 0,1,2,22,3 | Sim | `.github/workflows/audit.yml` |
| `audit-model-selection.sh` | ci-script | Rule 23 | `scripts/audit-model-selection.sh` | 0,1,2,3 | Sim | `.github/workflows/audit.yml` |
| `audit-execution-integrity.sh` | ci-script | Rule 24 | `scripts/audit-execution-integrity.sh` | 0,1,2,3 | Sim | `.github/workflows/audit.yml` |
| `audit-task-hierarchy.sh` | ci-script | Rule 25 | `scripts/audit-task-hierarchy.sh` | 0,25,2,3 | Sim | `.github/workflows/audit.yml` |
| `audit-phase-gates.sh` | ci-script | Rule 25 | `scripts/audit-phase-gates.sh` | 0,26,2,3 | Sim | `.github/workflows/audit.yml` |
| `verify-story-completion.sh` | hook | Rule 24 | `.claude/hooks/verify-story-completion.sh` | 0,2 | N/A | Claude Code `Stop` |
| `verify-phase-gates.sh` | hook | Rule 25 | `.claude/hooks/verify-phase-gates.sh` | 0,2 | N/A | Claude Code `Stop` |
| `enforce-phase-sequence.sh` | hook | Rule 25 | `.claude/hooks/enforce-phase-sequence.sh` | 0,2 | N/A | Claude Code `PreToolUse` |
| `LifecycleIntegrityAuditTest` | java-test | EPIC-0046 | `java/src/test/java/dev/iadev/lifecycle/LifecycleIntegrityAuditTest.java` | N/A (JUnit) | N/A | `mvn verify` |
| `TelemetryMarkerLint` | java-test | Rule 13 | `java/src/test/java/dev/iadev/telemetry/TelemetryMarkerLint.java` | N/A (JUnit) | N/A | `mvn verify` |

**Total: 12 gates** (7 CI scripts, 3 Hook runtimes, 2 Java tests)

---

## Gate Details

### `audit-flow-version.sh` (CI script — Rule 19)

**Purpose:** Validates that every `execution-state.json` under `plans/epic-*/` carries a `flowVersion`
field with a legal value (`"1"` or `"2"`). Prevents the silent fallback to legacy flow when
`flowVersion` is absent or malformed.

**Exit codes:** 0 = no violations | 1 = `FLOW_VERSION_VIOLATION` | 2 = `OPERATIONAL_ERROR` | 3 = `BASELINE_CORRUPT`

**Flag:** `--self-check` validates that `jq` is on `PATH` and the baseline file is readable.

**Cross-ref:** [Rule 19 — Backward Compatibility](../.claude/rules/19-backward-compatibility.md)

---

### `audit-epic-branches.sh` (CI script — Rule 21)

**Purpose:** Scans open PRs whose head is `epic/*` and verifies each has `flowVersion: "2"` in its
`execution-state.json` (when present). Detects force-push events and confirms `x-git-cleanup-branches`
excludes `epic/*` from its sweep.

**Exit codes:** 0 = no violations | 1 = `EPIC_BRANCH_VIOLATION` | 2 = `OPERATIONAL_ERROR` | 3 = `BASELINE_CORRUPT`

**Flag:** `--self-check` validates `gh` CLI is on `PATH`.

**Cross-ref:** [Rule 21 — Epic Branch Model](../.claude/rules/21-epic-branch-model.md)

---

### `audit-skill-visibility.sh` (CI script — Rule 22)

**Purpose:** Scans every `SKILL.md` under the skills source-of-truth and verifies: (a) prefix/frontmatter
consistency for `x-internal-*` skills; (b) `🔒 **INTERNAL SKILL**` body marker present; (c) no user-facing
trigger in internal skills; (d) no user-facing doc references `/x-internal-` in prose. Also checks for
orphan script references — `audit-*.sh` mentioned in any Rule without a catalog entry.

**Exit codes:** 0 = no violations | 1 = `SKILL_VISIBILITY_VIOLATION` | 2 = `OPERATIONAL_ERROR` | 3 = `INVALID_EXEMPTION` | 22 = `SKILL_VISIBILITY_VIOLATION` (named exit)

**Flag:** `--self-check` validates `jq` on `PATH` and the skills source-of-truth directory exists.

**Cross-ref:** [Rule 22 — Skill Visibility](../.claude/rules/22-skill-visibility.md)

---

### `audit-model-selection.sh` (CI script — Rule 23)

**Purpose:** Scans orchestrator SKILL.md files for: (a) missing `model:` in frontmatter; (b) `Agent(...)`
calls without explicit `model:`; (c) `Skill(...)` calls missing `model:` when tier differs from parent;
(d) agent files with `Recommended Model: Adaptive` or no `Recommended Model`.

**Exit codes:** 0 = no violations | 1 = `MODEL_SELECTION_VIOLATION` | 2 = `OPERATIONAL_ERROR`

**Flag:** `--self-check` validates skills root directory exists.

**Cross-ref:** [Rule 23 — Model Selection](../.claude/rules/23-model-selection.md)

---

### `audit-execution-integrity.sh` (CI script — Rule 24)

**Purpose:** For each story-branch merge detected via `git log`, verifies the four mandatory evidence
artifacts exist: `verify-envelope-STORY-ID.json`, `review-story-STORY-ID.md`,
`techlead-review-story-STORY-ID.md`, `story-completion-report-STORY-ID.md`. Prevents silent skill
execution bypass (the "inline simulation" anti-pattern).

**Exit codes:** 0 = `OK` | 1 = `EIE_EVIDENCE_MISSING` | 2 = `EIE_BASELINE_CORRUPT` | 3 = `EIE_INVALID_EXEMPTION`

**Flag:** `--self-check` validates the baseline file and Stop hook registration.

**Cross-ref:** [Rule 24 — Execution Integrity](../.claude/rules/24-execution-integrity.md)

---

### `audit-task-hierarchy.sh` (CI script — Rule 25)

**Purpose:** Scans every orchestrator SKILL.md for: (a) `TaskCreate` per `## Phase N` section;
(b) matching `TaskUpdate(status: "completed")` per task; (c) `x-internal-phase-gate` PRE/POST gates
per phase; (d) `subject:` literals matching the Rule 25 hierarchy regex.

**Exit codes:** 0 = no violations | 25 = `TASK_HIERARCHY_VIOLATION` | 2 = `OPERATIONAL_ERROR` | 3 = baseline corrupt

**Flag:** `--self-check` validates the baseline file and skills root exist.

**Cross-ref:** [Rule 25 — Task Hierarchy](../.claude/rules/25-task-hierarchy.md)

---

### `audit-phase-gates.sh` (CI script — Rule 25)

**Purpose:** Companion to `audit-task-hierarchy.sh`. Verifies phase-gate contract completeness:
every numbered phase has a `--mode pre` invocation AND at least one POST-family gate
(`--mode post`, `--mode wave`, or `--mode final`).

**Exit codes:** 0 = no violations | 26 = `PHASE_GATE_VIOLATION` | 2 = `OPERATIONAL_ERROR` | 3 = baseline corrupt

**Flag:** `--self-check` validates baseline and skills root exist.

**Cross-ref:** [Rule 25 — Task Hierarchy](../.claude/rules/25-task-hierarchy.md)

---

### `verify-story-completion.sh` (Hook runtime — Rule 24)

**Purpose:** Fires on Claude Code `Stop` event. Detects recent PR-creation or story-completion
activity and checks that mandatory evidence artifacts exist for that story. Emits a blocking
WARNING on missing evidence (exit 2) so the LLM receives the signal before the conversation ends.

**Exit codes:** 0 = no violation | 2 = evidence missing (warning; Claude Code surfaces to LLM)

**Cross-ref:** [Rule 24 — Execution Integrity](../.claude/rules/24-execution-integrity.md)

---

### `verify-phase-gates.sh` (Hook runtime — Rule 25)

**Purpose:** Fires on Claude Code `Stop` event. Reads `taskTracking.phaseGateResults` in
`execution-state.json`; emits WARNING + exit 2 if a phase gate result is missing or has `passed=false`.
Prevents silent phase transition bypass.

**Exit codes:** 0 = no violation | 2 = gate missing or failed

**Cross-ref:** [Rule 25 — Task Hierarchy](../.claude/rules/25-task-hierarchy.md)

---

### `enforce-phase-sequence.sh` (Hook runtime — Rule 25)

**Purpose:** Fires on Claude Code `PreToolUse` event when a `Skill(...)` call targets an orchestrator.
Blocks invocation when a predecessor phase has no `passed=true` record in `phaseGateResults`.
Enforces the invariant that Phase N+1 cannot start before Phase N gates pass.

**Exit codes:** 0 = allowed | 2 = blocked (predecessor phase gate missing)

**Cross-ref:** [Rule 25 — Task Hierarchy](../.claude/rules/25-task-hierarchy.md)

---

### `LifecycleIntegrityAuditTest` (Java test — EPIC-0046)

**Purpose:** Maven CI-blocking test that scans every `SKILL.md` under the skills source-of-truth for
three Rule 22 regressions: `ORPHAN_PHASE` (dotted sub-section documented but not referenced),
`WRITE_WITHOUT_COMMIT` (write to `plans/epic-*/reports/` with no `x-git-commit` in the next 20 lines),
and `SKIP_IN_HAPPY_PATH` (`--skip-verification` outside `## Recovery`). Baseline at
`audits/lifecycle-integrity-baseline.txt`.

**Exit codes:** N/A (JUnit: pass / fail / error)

**Cross-ref:** [EPIC-0046 story](../plans/epic-0046/story-0046-0007.md)

---

### `TelemetryMarkerLint` (Java test — Rule 13)

**Purpose:** Scans every `SKILL.md` for telemetry marker balance violations: `DUPLICATE_START`,
`DUPLICATE_END`, `DANGLING_END`, and `UNCLOSED_START`. Ensures `phase.start` / `phase.end` pairs
are symmetric in implementation skills.

**Exit codes:** N/A (JUnit: pass / fail / error)

**Cross-ref:** [Rule 13 — Skill Invocation Protocol](../.claude/rules/13-skill-invocation-protocol.md)

---

## Gap Detection

Gates referenced in Rules but NOT in this catalog are bugs. Run `scripts/audit-skill-visibility.sh`
to detect orphan references. Any CI failure with `ORPHAN_SCRIPT_REFERENCE` means this catalog
is out of date.

**Current gap status:** No gaps detected as of 2026-04-26 (EPIC-0058 verification).
