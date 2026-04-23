---
name: x-internal-epic-integrity-gate
description: "Executes the epic-level verification gate end-to-end (Phase 1.7 / Phase 4 carve-out of x-epic-implement) on the epic/XXXX branch HEAD: checks out the branch, runs mvn clean test + jacoco:report, parses filtered coverage against epic-level thresholds (default line >=95, branch >=90), runs a declarative DoD checklist (presence of tests, tasks DONE, CHANGELOG entry, ADR references), and emits a single-line JSON envelope {passed, failures, coverageDelta, dodChecklist}. Seventh skill in the x-internal-* convention and the fourth under internal/plan/ (after x-internal-story-load-context, x-internal-story-build-plan, and x-internal-story-verify). Isolates ~180 inline lines of integrity-gate logic from x-epic-implement."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--epic-id <XXXX> [--branch <name>] [--coverage-threshold-line <N>] [--coverage-threshold-branch <N>]"
category: internal-plan
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Caller principal: `x-epic-implement` (Phase 1.7 / Phase 4 carve-out).
> Sétima skill da convenção `x-internal-*` (após x-internal-status-update
> pilot 0049-0005, x-internal-report-write 0049-0006,
> x-internal-args-normalize 0049-0007, x-internal-story-load-context
> 0049-0011, x-internal-story-build-plan 0049-0012, e
> x-internal-story-verify 0049-0014). Quarta skill na subdir
> `internal/plan/` — o subdir `plan/` agrupa skills que orquestram
> planejamento e verificação (load → build → verify → integrity-gate).
> Difere de `internal/ops/`, cujas sibling skills mutam estado
> (`execution-state.json`, reports).

# Skill: x-internal-epic-integrity-gate

## Purpose

Carve out the epic-level integrity gate of `x-epic-implement` (the
~180 inline lines documented in `references/integrity-gate.md` of
`x-epic-implement`) into a single, single-responsibility skill with a
stable JSON envelope. The orchestrator shrinks to a read-the-envelope
consumer that drives its phase-advance / regression-diagnosis branching
off the four response fields.

Responsibilities (single):

1. Verify the target branch exists (local or remote); fail fast with
   `BRANCH_NOT_FOUND` when absent.
2. Check out the branch (`git checkout <name>`) with a detached-HEAD
   guard that restores the caller's original branch on any downstream
   failure.
3. Run `mvn clean test jacoco:report` (Maven default) and parse the
   exit code. Non-Maven stacks use their build-tool-native equivalent
   (listed in the full protocol).
4. Parse the coverage CSV (Maven default:
   `target/site/jacoco/jacoco.csv`), aggregate the line and branch
   totals across every module in scope, and compare against the
   line / branch thresholds.
5. Run the declarative DoD checklist — a static list of boolean
   predicates (tests present per touched file, tasks DONE in every
   story, CHANGELOG entry present, ADR references resolvable). Each
   item contributes one `{item, passed}` tuple to the envelope.
6. Emit a single-line JSON envelope on stdout:
   `{passed, failures, coverageDelta, dodChecklist}`.

Non-responsibilities (explicit):

- The skill does NOT dispatch specialist reviews or the Tech Lead
  review — those stay in `x-epic-implement` Phase 4.4 / 4.6.
- The skill does NOT mutate `execution-state.json`, `**Status:**`
  headers, or IMPLEMENTATION-MAP — those belong to
  `x-internal-status-update`.
- The skill does NOT write reports; `x-internal-report-write` renders
  the phase-completion report off the envelope this skill emits.
- The skill does NOT perform regression diagnosis (failed-test → story
  correlation) — that stays in the orchestrator, which consumes
  `failures[]` and branches accordingly.
- The skill does NOT advance to the next phase or version-bump — those
  are orchestrator concerns downstream of `passed=true`.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/plan/x-internal-epic-integrity-gate/` | `internal/` prefix scopes visibility; `plan/` co-locates with sibling verification carve-outs (`x-internal-story-verify`, `x-internal-epic-build-plan`) |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash` only | The skill shells out to git + Maven, parses CSV with `awk`, emits JSON with `jq -n`; no `Skill` / `Agent` needed — the gate is a leaf computation |
| Naming | `x-internal-{subject}-{action}` | `epic-integrity-gate` — subject (epic) + action (integrity-gate) per Rule 04 taxonomy |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never
invoked by a human typing `/x-internal-epic-integrity-gate` in chat.
All invocations follow the Rule 13 INLINE-SKILL pattern from a
calling orchestrator:

```markdown
Skill(skill: "x-internal-epic-integrity-gate",
      args: "--epic-id 0049")
```

```markdown
Skill(skill: "x-internal-epic-integrity-gate",
      args: "--epic-id 0049 --branch epic/0049 --coverage-threshold-line 90 --coverage-threshold-branch 85")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`) — used to resolve default branch name and `plans/epic-XXXX/` |
| `--branch <name>` | O | `epic/<id>` | Branch to validate; defaults to `epic/<epic-id>` (the phase-integration branch) |
| `--coverage-threshold-line <N>` | O | `95` | Minimum line coverage percentage (integer, 0-100) |
| `--coverage-threshold-branch <N>` | O | `90` | Minimum branch coverage percentage (integer, 0-100) |

All four argument forms (`--key value`, `--key=value`, empty, and
unknown-flag rejection) are supported; unknown flags and missing
required flags exit with `64` (sysexits `EX_USAGE`). The `--epic-id`
value is zero-padded to 4 digits and validated against `^[0-9]{4}$`;
`--branch` is trimmed and validated against `refs/heads/` naming rules
(no spaces, no `..`, no leading `-`). Thresholds outside `[0, 100]`
exit `64`.

## Response Contract

On success (any exit code ≤ 4) the skill writes a single-line JSON
object to stdout. The envelope is always well-formed JSON; consumers
MUST read `passed` as the authoritative gate.

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `passed` | `Boolean` | yes | Global gate: `true` iff all sub-gates pass (tests green, coverage ≥ thresholds, every DoD item `passed=true`) |
| `failures` | `Array<String>` | yes | One human-readable line per failure (empty array on happy path). Categories: `branch:`, `test:`, `coverage:`, `dod:` |
| `coverageDelta` | `Object` | yes | `{line: Number, branch: Number, lineThreshold: Number, branchThreshold: Number, lineDelta: Number, branchDelta: Number}` — `*Threshold` echo inputs; `*Delta` is `actual − threshold` (negative when below) |
| `dodChecklist` | `Array<{item:String, passed:Boolean}>` | yes | One entry per DoD predicate; `passed=false` always corresponds to a `dod:` failure in `failures` |

### Example envelope (happy path)

```json
{"passed":true,"failures":[],"coverageDelta":{"line":96.0,"branch":91.0,"lineThreshold":95,"branchThreshold":90,"lineDelta":1.0,"branchDelta":1.0},"dodChecklist":[{"item":"all-tasks-done","passed":true},{"item":"changelog-entry","passed":true},{"item":"tests-present","passed":true}]}
```

### Example envelope (coverage below + DoD gap)

```json
{"passed":false,"failures":["coverage: line 88.0 < 95","dod: changelog-entry missing"],"coverageDelta":{"line":88.0,"branch":91.0,"lineThreshold":95,"branchThreshold":90,"lineDelta":-7.0,"branchDelta":1.0},"dodChecklist":[{"item":"all-tasks-done","passed":true},{"item":"changelog-entry","passed":false},{"item":"tests-present","passed":true}]}
```

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Envelope emitted with `passed=true` | — |
| 1 | BRANCH_NOT_FOUND | Target branch does not exist locally or on `origin` | `Branch <name> not found` |
| 2 | MVN_TEST_FAILED | Build tool returned non-zero before coverage could be parsed | `Test suite failed` |
| 3 | COVERAGE_BELOW_THRESHOLD | Envelope emitted with `passed=false` due to coverage | `Coverage below threshold: line=<L>, branch=<B>` |
| 4 | JACOCO_REPORT_MISSING | Coverage CSV not generated (build completed but no `target/site/jacoco/jacoco.csv`) | `JaCoCo report not found at <path>` |
| 64 | EX_USAGE | Unknown or malformed flag | `usage: --epic-id <id> [--branch <name>] [--coverage-threshold-line N] [--coverage-threshold-branch N]` |

When `passed=false` is driven purely by DoD gaps (not coverage, not
test-failure), the exit code is `0` and the caller MUST branch off
`passed`. The non-zero exit is reserved for infrastructure-level
failures (missing branch, build failure, missing report). This keeps
the envelope the single source of truth for the orchestrator.

## Workflow

### Step 1 — Argument parsing and path resolution

Parse the four flags; reject unknown and missing required flags with
exit `64`. Resolve canonical paths:

```bash
epic_id="$(printf '%04d' "${raw_epic_id}")"
branch="${branch:-epic/${epic_id}}"
epic_dir="plans/epic-${epic_id}"
jacoco_csv="target/site/jacoco/jacoco.csv"
```

### Step 2 — Branch existence + checkout

```bash
if ! git rev-parse --verify "${branch}" >/dev/null 2>&1 \
   && ! git rev-parse --verify "origin/${branch}" >/dev/null 2>&1; then
    echo "Branch ${branch} not found" >&2
    exit 1
fi

original_branch="$(git symbolic-ref --short HEAD 2>/dev/null || echo DETACHED)"
git checkout "${branch}" >/dev/null 2>&1 || {
    echo "Branch ${branch} checkout failed" >&2
    exit 1
}
trap 'git checkout "${original_branch}" >/dev/null 2>&1 || true' EXIT
```

The `trap` ensures the caller's working tree is restored even if a
later step aborts.

### Step 3 — Run mvn clean test + jacoco:report

```bash
mvn clean test jacoco:report -q
mvn_exit=$?
if [ "${mvn_exit}" -ne 0 ]; then
    echo "Test suite failed (exit=${mvn_exit})" >&2
    exit 2
fi
```

Non-Maven stacks use their own equivalents (listed in the full
protocol); the skill auto-detects the build tool by probing `pom.xml`,
`build.gradle(.kts)`, `package.json`, `Cargo.toml`, `go.mod`,
`pyproject.toml`. When no build tool is detected, the skill logs
`test: no build tool detected; skipping` to stderr, emits `passed=true`
for the test gate (degraded but non-blocking), and continues to
Step 4 best-effort.

### Step 4 — Parse filtered coverage (JaCoCo CSV)

Read `target/site/jacoco/jacoco.csv`. The JaCoCo CSV schema is
(header, then one row per class):

```
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,...
```

Sum `LINE_COVERED` and `LINE_MISSED` across all rows; compute:

```
line_pct = round(line_covered / (line_covered + line_missed) * 100, 1)
branch_pct = round(branch_covered / (branch_covered + branch_missed) * 100, 1)
```

Compare against `--coverage-threshold-line` and
`--coverage-threshold-branch` (inclusive: `>=` passes). When the CSV
is absent (build completed but report not generated), exit `4`
(`JACOCO_REPORT_MISSING`) with no envelope.

When coverage is below threshold, append
`coverage: line <actual> < <threshold>` and/or
`coverage: branch <actual> < <threshold>` to `failures`, compute
`lineDelta` / `branchDelta` as `actual - threshold` (negative when
below), set `passed=false`, exit `3`.

### Step 5 — DoD checklist (declarative, extensible)

The DoD checklist is a declarative list of boolean predicates
embedded in the skill. Each predicate is deterministic and read-only.
Default catalogue (v1):

| Item | Predicate | Failure Message |
| :--- | :--- | :--- |
| `all-tasks-done` | Every `- [ ]` checkbox in `plans/epic-<id>/story-*.md` Section 8 is either `- [x]` OR the story `**Status:**` is `Concluida` | `dod: <N> task(s) not marked DONE` |
| `changelog-entry` | `CHANGELOG.md` contains at least one `### Added` / `### Fixed` / `### Changed` entry under `## [Unreleased]` OR under a version header newer than the previous release tag | `dod: changelog-entry missing` |
| `tests-present` | Every `src/main/**/*.{java,kt,ts,py,go,rs}` file touched on this branch (diff vs `main`) has at least one corresponding test file under `src/test/` matching the class-name convention | `dod: <N> file(s) without corresponding test` |
| `adr-references-resolvable` | Every `adr/ADR-NNNN-*.md` referenced in story markdowns actually exists | `dod: <N> ADR reference(s) unresolved` |
| `story-status-concluida` | Every `plans/epic-<id>/story-*.md` has `**Status:** Concluida` (or `Concluída`, accent-agnostic) | `dod: <N> story(ies) not Concluida` |

Extension contract: additional predicates are appended to the
catalogue in the full-protocol reference; the envelope schema is
stable (each predicate maps to exactly one `dodChecklist` entry).
Predicates MUST be pure: no network I/O, no git write, no mutation.

### Step 6 — Assemble and emit envelope

Compose the JSON envelope via `jq -nc`. The exit code is the lowest
applicable (0 success; 1 branch missing; 2 test failure; 3 coverage
below; 4 missing report).

```bash
jq -nc \
  --argjson passed "${passed_bool}" \
  --argjson failures "${failures_json}" \
  --argjson coverageDelta "${coverage_json}" \
  --argjson dodChecklist "${dod_json}" \
  '{passed:$passed, failures:$failures,
    coverageDelta:$coverageDelta, dodChecklist:$dodChecklist}'
```

## Examples

### Example 1 — Happy path: gate passes on clean epic branch

```bash
Skill(skill: "x-internal-epic-integrity-gate",
      args: "--epic-id 0049")
```

Envelope:

```json
{"passed":true,"failures":[],"coverageDelta":{"line":96.0,"branch":91.0,"lineThreshold":95,"branchThreshold":90,"lineDelta":1.0,"branchDelta":1.0},"dodChecklist":[{"item":"all-tasks-done","passed":true},{"item":"changelog-entry","passed":true},{"item":"tests-present","passed":true},{"item":"adr-references-resolvable","passed":true},{"item":"story-status-concluida","passed":true}]}
```

Exit: 0.

### Example 2 — Coverage below threshold

```bash
Skill(skill: "x-internal-epic-integrity-gate",
      args: "--epic-id 0049")
```

Envelope includes `"passed":false` and
`"coverage: line 88.0 < 95"` in `failures`; `coverageDelta.lineDelta=-7.0`.
Exit: 3.

### Example 3 — Test failure

```bash
Skill(skill: "x-internal-epic-integrity-gate",
      args: "--epic-id 0049")
```

Stderr: `Test suite failed (exit=<N>)`. Exit: 2. No envelope
emitted.

### Example 4 — Branch not found

```bash
Skill(skill: "x-internal-epic-integrity-gate",
      args: "--epic-id 9999")
```

Stderr: `Branch epic/9999 not found`. Exit: 1. No envelope emitted.

### Example 5 — Boundary: coverage exactly on threshold

```bash
Skill(skill: "x-internal-epic-integrity-gate",
      args: "--epic-id 0049")
```

Envelope `"coverageDelta":{"line":95.0,"branch":90.0,"lineThreshold":95,"branchThreshold":90,"lineDelta":0.0,"branchDelta":0.0,...}` → `passed=true` (inclusive).
Exit: 0.

### Example 6 — Custom thresholds (per-epic override)

```bash
Skill(skill: "x-internal-epic-integrity-gate",
      args: "--epic-id 0049 --coverage-threshold-line 80 --coverage-threshold-branch 75")
```

Envelope reports `lineThreshold=80, branchThreshold=75`. Use case:
meta-epics touching only documentation/generator resources override
the project-wide defaults.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Response envelope | stdout | Single-line JSON matching the Response Contract |
| Error diagnostic | stderr | Single line, non-empty only on exit ≠ 0 |

No file is created or modified. The skill is **strictly read-only**
against the project source tree (enforced by `allowed-tools: Bash`
frontmatter — no `Write` / `Edit` available). It DOES execute Maven,
which mutates `target/` — that is the build tool's responsibility,
not a state mutation in the sense of `x-internal-ops/*`.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag (`--epic-id`) | Print `usage:` banner to stderr; exit 64 |
| Unknown flag | Print `unknown flag <name>; usage:`; exit 64 |
| Threshold outside `[0, 100]` | Print `threshold must be in [0,100]`; exit 64 |
| `jq` absent on PATH | Print `dependency missing: jq`; exit 127 |
| `pom.xml` / build config absent | Print `no build tool detected`; emit `test: no build tool detected` in failures; continue Steps 4–5 best-effort |
| Coverage CSV unreadable | Emit `coverage: report unreadable (<reason>)` in failures; `passed=false`; exit 4 |
| Test process segfaults / OOM | Exit 2 with `Test suite failed (<signal>)`; no envelope |
| Checkout fails (dirty working tree) | Exit 1 with `Branch <name> checkout failed: <git stderr>`; caller expected to stash first |
| Story file missing during DoD check | Append `dod: story file <id> missing` to failures; `passed=false`; exit 0 |
| CHANGELOG.md absent | Append `dod: changelog-entry missing (CHANGELOG.md absent)` to failures; `passed=false`; exit 0 |
| `mvn` absent on PATH (Maven project) | Print `dependency missing: mvn`; exit 127 |

## Performance Contract

Target: < 5 minutes for a medium epic (~10 stories, ~50 classes in
aggregate) on a laptop-class machine. Breakdown budget:

| Step | Target |
| :--- | :--- |
| Argument parse + branch validation | < 200 ms |
| Checkout | < 500 ms |
| `mvn clean test jacoco:report` | < 4 min |
| Coverage CSV parse | < 300 ms |
| DoD checklist evaluation | < 10 s |
| Envelope assembly | < 50 ms |

Large epics (20+ stories, 200+ classes) may exceed the total; the
full-protocol reference documents an opt-in `--skip-clean` knob that
reuses prior `target/` when safe.

## Testing

The story ships the following acceptance test scenarios, mirroring
Section 7 of story-0049-0010:

1. **Degenerate — gate passes on a clean branch.** All DoD items
   pass, coverage line=96 / branch=91 → `passed=true`, exit 0.
2. **Failure — coverage below threshold.** Line=88 with threshold=95 →
   `passed=false`, exit 3, `failures` contains
   `"coverage: line 88.0 < 95"`.
3. **Failure — test suite failure.** `mvn test` exits non-zero →
   exit 2, no envelope.
4. **Error — branch not found.** `--epic-id 9999` with no such
   branch → exit 1, no envelope.
5. **Boundary — coverage exactly on threshold.** Line=95, Branch=90 →
   `passed=true`, exit 0 (inclusive).

Goldens live under the smoke test resource path (see `Files:` of
TASK-0049-0010-005). Coverage requirement: ≥ 95% line / ≥ 90% branch
across the invoking Bash codepaths.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout)
so `Skill(skill: "x-internal-...")` invocations from other skills
resolve correctly. The invariant: **user cannot see them;
orchestrators can invoke them.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers —
telemetry is produced by the invoking orchestrator (the `phase`
wrapping the orchestrator's own step — Phase 1.7 / Phase 4 in
`x-epic-implement` — is the correct aggregation boundary). Passive
hooks still capture `tool.call` for the underlying `Bash` invocations
(git, Maven, jq).

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract), and ADR-0011
(Slim-SKILL / Fat-Reference Split).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-epic-implement` | caller (primary) | Phase 1.7 / Phase 4 carve-out: the skill's stdout envelope replaces the ~180 inline lines that currently live in `references/integrity-gate.md` and the Phase 4 orchestrator body — Phase 4 shrinks to ~80 lines (story-level metric per story-0049-0018) |
| `x-internal-story-verify` | peer | Sibling `internal/plan/` skill; runs the equivalent gate at story scope (coverage filtered to story's files, cross-file consistency, AC mapping). `x-internal-epic-integrity-gate` runs at the epic/phase scope — aggregate coverage, aggregate DoD |
| `x-internal-epic-build-plan` | peer | Sibling `internal/plan/` skill; computes the epic DAG + phase ordering upstream (Phase 0). This skill consumes the `epic/<id>` branch AFTER all phase stories are merged |
| `x-internal-report-write` | downstream | Consumes this skill's envelope to render the phase-completion report `phase-report-epic-XXXX.md` via `_TEMPLATE-PHASE-COMPLETION-REPORT.md` |
| `x-internal-status-update` | downstream | Consumes `passed` to transition phase status and trigger the semantic version bump (when `passed=true`) per RULE-013 |
| `x-parallel-eval` | indirect | Runs BEFORE this skill at Phase 1.5 to validate parallelism constraints; its output does not feed the integrity gate |

Downstream stories that depend on this carve-out:
- **story-0049-0018** (refactor `x-epic-implement`) consumes the
  envelope and deletes the inline Phase 4 block.

Full workflow detail (argument-parser rejection matrix, JaCoCo CSV
schema with worked parsing example, build-tool-native equivalents for
Gradle / npm / cargo / go / pytest, DoD-predicate catalogue with
extension examples, `--skip-clean` optimisation semantics, and the
coverage-aggregation formula for multi-module Maven projects) lives in
[`references/full-protocol.md`](references/full-protocol.md) per
ADR-0011.
