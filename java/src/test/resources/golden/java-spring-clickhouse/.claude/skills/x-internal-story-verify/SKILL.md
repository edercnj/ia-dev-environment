---
name: x-internal-story-verify
description: "Executes the story-level verification gate (Phase 3 carve-out of x-story-implement): identifies files touched by the story via the task breakdown, runs the test suite scoped to those files, parses filtered coverage against the story-specific thresholds (default line >=95, branch >=90), performs cross-file consistency checks (constructor patterns, return-type uniformity per role), optionally runs the smoke suite, and validates every Section 7 Gherkin scenario has a matching acceptance test. Emits a single-line JSON envelope {passed, coverageDelta, failures, acCheckResults}. Sixth skill in the x-internal-* convention and the third under internal/plan/ (after x-internal-story-load-context and x-internal-story-build-plan)."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--story-id <story-XXXX-YYYY> --epic-id <XXXX> [--coverage-threshold-line <N>] [--coverage-threshold-branch <N>]"
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
> Caller principal: x-story-implement (Phase 3 carve-out).
> Sexta skill da convenção `x-internal-*` (após x-internal-status-update
> pilot 0049-0005, x-internal-report-write 0049-0006,
> x-internal-args-normalize 0049-0007, x-internal-story-load-context
> 0049-0011, e x-internal-story-build-plan 0049-0012). Terceira skill
> na subdir `internal/plan/` — o subdir `plan/` agrupa orquestração de
> planejamento e verificação da story (load → build → verify). Difere
> de `internal/ops/`, cujas sibling skills mutam estado (execution-state,
> reports).

# Skill: x-internal-story-verify

## Purpose

Carve out Phase 3 (Story-Level Verification) of `x-story-implement`
into a single, single-responsibility skill. The ~160 inline lines
currently inside `x-story-implement` Phase 3 (Step 3.1 coverage, Step
3.2 cross-file consistency, Step 3.8 smoke gate, implicit AC → test
mapping) become a single
`Skill(skill: "x-internal-story-verify", …)` invocation; the
orchestrator shrinks to a read-the-envelope consumer that drives its
remediation / tech-lead-review branching off the four response fields.

Responsibilities (single):

1. Identify the files "owned" by the story via the task breakdown at
   `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` (Section 8
   fallback of the story file when the breakdown is absent).
2. Run `{{TEST_COMMAND}}` (Maven default: `mvn test`) filtered to the
   identified file set via the build-tool-native filter (Maven:
   `-Dtest=<classes>`; other stacks in the full protocol).
3. Parse the build-tool coverage XML (Maven default: JaCoCo at
   `target/site/jacoco/jacoco.xml`), re-scope the totals to the story's
   file list, and compare against the line / branch thresholds.
4. Perform cross-file consistency checks on the story's files
   (constructor arity uniformity, return-type uniformity per role)
   using the heuristic catalogue described in the full protocol.
5. When `testing.smoke_tests == true` in the project config, run the
   smoke suite; the smoke result contributes to `passed` only when its
   Health Check or Critical Path layer fails (Response-Time and
   Error-Rate layers are advisory per EPIC-0042).
6. Validate every Section 7 Gherkin scenario (`Cenario:` / `Scenario:`)
   maps to at least one test class / method; a missing mapping emits
   an `acCheckResults` entry with `hasTest=false` and appends to
   `failures`.
7. Emit a single-line JSON envelope on stdout:
   `{passed, coverageDelta, failures, acCheckResults}`.

Non-responsibilities (explicit):

- The skill does NOT create branches, run PR operations, or mutate
  git state — the caller (`x-story-implement` Phase 3) and
  `x-internal-status-update` handle those.
- The skill does NOT write reports; `x-internal-report-write` renders
  the consolidated dashboard off the envelope this skill emits.
- The skill does NOT update `execution-state.json`; the caller owns
  the transition to `COMPLETE` / `FAILED` after consuming `passed`.
- The skill does NOT dispatch specialist reviews or the Tech Lead
  review — Phase 3.4 / 3.6 stay in the orchestrator.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/plan/x-internal-story-verify/` | `internal/` prefix scopes visibility; `plan/` co-locates with the other carve-outs of the story-level planning/verification pipeline (`x-internal-story-load-context`, `x-internal-story-build-plan`) |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash` only | The skill shells out to the build tool, parses XML with `xmllint` / `jq`, and emits JSON with `jq -n`; no `Skill` / `Agent` needed — verification is a leaf operation |
| Naming | `x-internal-{subject}-{action}` | Mirrors Rule 04 skill taxonomy; `story-verify` = subject+action |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never
invoked by a human typing `/x-internal-story-verify` in chat. All
invocations follow Rule 13 INLINE-SKILL pattern from a calling
orchestrator:

```markdown
Skill(skill: "x-internal-story-verify",
      args: "--story-id story-0049-0014 --epic-id 0049")
```

```markdown
Skill(skill: "x-internal-story-verify",
      args: "--story-id story-0049-0014 --epic-id 0049 --coverage-threshold-line 90 --coverage-threshold-branch 85")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--story-id <id>` | M | — | Story identifier (`story-XXXX-YYYY` canonical form) |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`) — used to resolve `plans/epic-XXXX/` |
| `--coverage-threshold-line <N>` | O | `95` | Minimum line coverage percentage (integer, 0-100) |
| `--coverage-threshold-branch <N>` | O | `90` | Minimum branch coverage percentage (integer, 0-100) |

All four argument forms (`--key value`, `--key=value`, empty, and
unknown-flag rejection) are supported; unknown flags and missing
required flags exit with `64` (sysexits `EX_USAGE`). The `--story-id`
value is normalised to lowercase and validated against
`^story-[0-9]{4}-[0-9]{4}$`; `--epic-id` is zero-padded to 4 digits.
Thresholds outside `[0, 100]` exit `64`.

## Response Contract

On success (any exit code ≤ 3) the skill writes a single-line JSON
object to stdout. The envelope is always well-formed JSON; consumers
MUST read `passed` as the authoritative gate.

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `passed` | `Boolean` | yes | Global gate: `true` iff all four sub-gates pass (tests green, coverage ≥ thresholds, cross-file consistency clean, every AC scenario has a test, smoke Health Check + Critical Path green when smoke is enabled) |
| `coverageDelta` | `Object` | yes | `{line: Number, branch: Number, lineThreshold: Number, branchThreshold: Number, lineDelta: Number, branchDelta: Number}` — the four `*Threshold` fields echo the inputs; `*Delta` is `actual − threshold` (negative when below) |
| `failures` | `Array<String>` | yes | One human-readable line per failure (empty array on happy path). Categories: `test:`, `coverage:`, `consistency:`, `ac:`, `smoke:` |
| `acCheckResults` | `Array<{scenario:String, hasTest:Boolean}>` | yes | One entry per Section 7 scenario; `hasTest=false` always corresponds to an `ac:` failure in `failures` |

### Example envelope (happy path)

```json
{"passed":true,"coverageDelta":{"line":96.4,"branch":92.1,"lineThreshold":95,"branchThreshold":90,"lineDelta":1.4,"branchDelta":2.1},"failures":[],"acCheckResults":[{"scenario":"Verify passa em story limpa","hasTest":true},{"scenario":"Falha — AC sem teste","hasTest":true}]}
```

### Example envelope (coverage below + AC gap)

```json
{"passed":false,"coverageDelta":{"line":88.2,"branch":91.0,"lineThreshold":95,"branchThreshold":90,"lineDelta":-6.8,"branchDelta":1.0},"failures":["coverage: line 88.2 < 95","ac: 'Boundary — coverage exatamente no threshold' has no test"],"acCheckResults":[{"scenario":"Verify passa em story limpa","hasTest":true},{"scenario":"Boundary — coverage exatamente no threshold","hasTest":false}]}
```

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Envelope emitted with `passed=true` | — |
| 1 | STORY_FILES_NOT_FOUND | Task breakdown empty AND no file-owning tasks in Section 8 | `Could not identify story files` |
| 2 | MVN_TEST_FAILED | Build tool returned non-zero before coverage could be parsed | `Test suite failed` |
| 3 | COVERAGE_BELOW_THRESHOLD | Envelope emitted with `passed=false` due to coverage | `Coverage below threshold` |
| 64 | EX_USAGE | Unknown or malformed flag | `usage: --story-id <id> --epic-id <id> [--coverage-threshold-line N] [--coverage-threshold-branch N]` |

When `passed=false` is driven by AC gap, consistency, or smoke (not
coverage), the exit code is `0` and the caller MUST branch off
`passed` — the non-zero exit is reserved for infrastructure-level
failures (build errors, missing files). This keeps the envelope the
single source of truth for the orchestrator.

## Workflow

### Step 1 — Argument parsing and path resolution

Parse the four flags; reject unknown and missing required flags with
exit `64`. Resolve canonical paths:

```bash
epic_dir="plans/epic-${epic_id}"
story_file="${epic_dir}/${story_id}.md"
tasks_file="${epic_dir}/plans/tasks-${story_id}.md"
```

The skill does NOT exit when `tasks_file` is absent — Step 2 falls
back to parsing Section 8 of `story_file`. It DOES exit `64` when
either `--story-id` or `--epic-id` fails regex validation.

### Step 2 — Identify story files

Prefer the task breakdown (`tasks-story-XXXX-YYYY.md`), whose `Files:`
bullet under each task is the authoritative source of truth. When the
breakdown is absent (legacy v1 stories), fall back to Section 8 of the
story file and extract `**Files:**` lines. De-duplicate and normalise
each path to repo-relative form.

If the resulting list is empty, exit `1` (`STORY_FILES_NOT_FOUND`)
with message `Could not identify story files`.

### Step 3 — Run scoped tests

Dispatch the build-tool-native filtered invocation. Maven default:

```bash
class_csv=$(file_list_to_maven_classes "${files[@]}")
mvn test -Dtest="${class_csv}" -q
```

Non-Maven stacks use their own filters (listed in the full protocol);
the skill auto-detects the build tool by probing `pom.xml`,
`build.gradle(.kts)`, `package.json`, `Cargo.toml`, `go.mod`,
`pyproject.toml`. When no build tool is detected, the skill logs
`test: no build tool detected; skipping` to stderr, emits `passed=true`
for the test gate (degraded but non-blocking), and continues to Step 4.

Non-zero build-tool exit with parseable failure → exit `2`
(`MVN_TEST_FAILED`). Non-zero with the build tool complaining about
pattern-match (e.g., `-Dtest=` matched zero classes) → treat as
`test: no matching classes for pattern <...>` failure, exit `0` with
`passed=false`.

### Step 4 — Parse filtered coverage

Read the coverage XML (Maven default: `target/site/jacoco/jacoco.xml`).
For each file in the story's identified list, sum `counter type="LINE"`
and `counter type="BRANCH"` across the matching `<class>` / `<sourcefile>`
elements; compute percentage = `covered / (covered + missed) * 100`
(rounded to one decimal). Compare against `--coverage-threshold-line`
and `--coverage-threshold-branch`.

When the coverage XML is absent (build tool produced no report), emit
`coverage: report not generated` to `failures` with `line=0` /
`branch=0` in `coverageDelta`, `passed=false`, exit `3`.

### Step 5 — Cross-file consistency heuristics

For every pair of files in the story's list that share a **role**
(heuristic: identical suffix — `*Controller.java`, `*Service.java`,
`*Assembler.java`, `*Mapper.java`, `*Repository.java`, `*Port.java`,
`*UseCase.java`, `*Dto.java`, `*Entity.java`), compare:

1. **Constructor arity** — number of explicit constructor parameters
   must match across siblings of the same role.
2. **Return-type uniformity for common method names** — when two
   siblings both declare `handle`, `execute`, `process`, `apply`,
   `create`, `update`, `delete`, or `find*`, their return types must
   match (ignoring generic parameters in Level 1; full generic match
   in the `--strict-consistency` extension defined in the full
   protocol).
3. **Error-handling shape** — identical exception-thrown or Result
   wrapper (domain convention): if one sibling throws and the other
   returns `Optional`/`Result`, emit `consistency: mixed error shape
   across <A> and <B>`.

Each violation appends to `failures` as `consistency: <short desc>`.
Consistency violations are MEDIUM severity and contribute to
`passed=false` but do NOT change the exit code. Rationale: RULE-005
treats cross-file divergence as MEDIUM; blocking the build on every
heuristic miss is too strict for mixed-role stories.

### Step 6 — Smoke gate (conditional)

Activate only when the project config declares `testing.smoke_tests == true`
(read from the generator YAML or the merged project context). Delegate
to the project smoke command (Maven default: `mvn verify -Psmoke`).
Parse the four smoke layers from the output:

| Layer | Blocks `passed`? |
| :--- | :--- |
| Health Check | yes (hard) |
| Critical Path | yes (hard) |
| Response Time | no (advisory) |
| Error Rate | no (advisory) |

Hard failures append `smoke: <layer> failed` to `failures`. Advisory
layers emit stderr warnings but do not alter `passed`.

### Step 7 — AC validation (Gherkin → test mapping)

Extract Section 7 scenarios from the story file (match `^Cenario:` or
`^Scenario:` inside the fenced gherkin block). For each scenario,
attempt to locate a test method whose name (case-insensitively)
contains a 3+ word slug derived from the scenario title (strip
accents, collapse whitespace to `-`). Matching uses a breadth-first
search rooted at `src/test/` with an early-exit on first match.

When no match is found, append an `acCheckResults` entry with
`hasTest=false` AND a `failures` line
`ac: '<scenario title>' has no test`. Matches populate the same array
with `hasTest=true`.

### Step 8 — Assemble and emit envelope

Compose the JSON envelope via `jq -nc`. The exit code is the lowest
applicable (0 success; 1 no files; 2 test failure; 3 coverage gap).

```bash
jq -nc \
  --argjson passed "${passed_bool}" \
  --argjson coverageDelta "${coverage_json}" \
  --argjson failures "${failures_json}" \
  --argjson acCheckResults "${ac_json}" \
  '{passed:$passed, coverageDelta:$coverageDelta,
    failures:$failures, acCheckResults:$acCheckResults}'
```

## Examples

### Example 1 — Happy path: clean story, all gates green

```bash
Skill(skill: "x-internal-story-verify",
      args: "--story-id story-0049-0014 --epic-id 0049")
```

Envelope:

```json
{"passed":true,"coverageDelta":{"line":96.4,"branch":92.1,"lineThreshold":95,"branchThreshold":90,"lineDelta":1.4,"branchDelta":2.1},"failures":[],"acCheckResults":[{"scenario":"Verify passa em story limpa","hasTest":true}]}
```

Exit: 0.

### Example 2 — AC gap

```bash
Skill(skill: "x-internal-story-verify",
      args: "--story-id story-0049-0014 --epic-id 0049")
```

Envelope includes `"passed":false` and `"ac: 'Boundary — coverage exatamente no threshold' has no test"` in `failures`. Exit: 0 (AC gap is envelope-reported, not a process error).

### Example 3 — Coverage below threshold

```bash
Skill(skill: "x-internal-story-verify",
      args: "--story-id story-0049-0014 --epic-id 0049")
```

Envelope includes `"passed":false` and `coverageDelta.line=88.2, lineDelta=-6.8`. Exit: 3.

### Example 4 — No story files identified

```bash
Skill(skill: "x-internal-story-verify",
      args: "--story-id story-0049-0014 --epic-id 0049")
```

Stderr: `Could not identify story files`. Exit: 1. No envelope emitted.

### Example 5 — Boundary: coverage exactly on threshold

```bash
Skill(skill: "x-internal-story-verify",
      args: "--story-id story-0049-0014 --epic-id 0049")
```

Envelope `"coverageDelta":{"line":95.0,"lineThreshold":95,"lineDelta":0.0,…}` → `passed=true`. Exit: 0.

### Example 6 — Custom thresholds (per-story override)

```bash
Skill(skill: "x-internal-story-verify",
      args: "--story-id story-0049-0014 --epic-id 0049 --coverage-threshold-line 80 --coverage-threshold-branch 75")
```

Envelope reports `lineThreshold=80, branchThreshold=75`. Use case:
stories touching only documentation files override the project-wide
defaults.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Response envelope | stdout | Single-line JSON matching the Response Contract |
| Error diagnostic | stderr | Single line, non-empty only on exit ≠ 0 |

No file is created or modified. The skill is **strictly read-only**
against the project source tree (enforced by `allowed-tools: Bash`
frontmatter — no `Write` / `Edit` available). It DOES execute the
build tool, which mutates `target/` (Maven) or equivalent — that is
the build tool's responsibility, not a state mutation in the sense of
the `x-internal-ops/*` skills.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 |
| Unknown flag | Print `unknown flag <name>; usage:`; exit 64 |
| Threshold outside `[0, 100]` | Print `threshold must be in [0,100]`; exit 64 |
| `jq` / `xmllint` absent on PATH | Print `dependency missing: <tool>`; exit 127 |
| `pom.xml` / build config absent | Print `no build tool detected`; emit `test: no build tool detected` in failures; continue Steps 4–7 best-effort |
| Coverage XML unreadable | Emit `coverage: report unreadable (<reason>)`; `passed=false`; exit 3 |
| Test process segfaults / OOM | Exit 2 with `Test suite failed (<signal>)`; no envelope |
| Smoke suite absent while `smoke_tests=true` | Emit `smoke: configured but runner absent`; `passed=false`; no exit-code change |
| Gherkin block malformed (unterminated fence) | Treat as zero scenarios; append `ac: gherkin block unparseable` to failures; continue |
| Section 8 malformed (no `Files:` bullets) | Fall back to heuristic: treat every `*.java` / `*.ts` / `*.py` under `src/main/` mentioned in commit messages of the story's task branches; if none, exit 1 |
| Stat flavour mismatch (GNU vs BSD) | Try `stat -f %m` (BSD) first; fall back to `stat -c %Y` (GNU) |

## Performance Contract

Target: < 3 minutes for a typical "medium" story (5–7 tasks, ~20
classes in scope) on a laptop-class machine. Breakdown budget:

| Step | Target |
| :--- | :--- |
| Argument parse + file-list build | < 200 ms |
| Scoped test run | < 120 s |
| Coverage XML parse | < 500 ms |
| Cross-file consistency | < 2 s |
| Smoke (when enabled) | < 45 s |
| AC scenario mapping | < 5 s (fanout bounded by scenario count) |
| Envelope assembly | < 50 ms |

Large stories (10+ tasks, 50+ classes) may exceed the total; the
full-protocol reference documents an opt-in `--parallel-tests` knob
(Maven surefire `forkCount`) for those cases.

## Testing

The story ships the following acceptance test scenarios, mirroring
Section 7 of story-0049-0014:

1. **Degenerate — verify passes on a clean story.** All ACs have tests,
   coverage line=96 / branch=92 → `passed=true`, exit 0.
2. **Failure — AC without a test.** One scenario has no matching test
   method → `passed=false`, `failures` contains the AC entry, exit 0.
3. **Failure — coverage below threshold.** Line=88 with threshold=95 →
   `passed=false`, exit 3.
4. **Error — empty task breakdown.** No files identified → exit 1, no
   envelope.
5. **Boundary — coverage exactly on threshold.** Line=95 → `passed=true`,
   exit 0.

Goldens live under the smoke test resource path (see `Files:` of
TASK-0049-0014-005). Coverage requirement: ≥ 95% line / ≥ 90% branch
across the invoking Bash codepaths.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout)
so `Skill(skill: "x-internal-...")` invocations from other skills
resolve correctly. The invariant: **user cannot see them; orchestrators
can invoke them.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers —
telemetry is produced by the invoking orchestrator (the `phase`
wrapping the orchestrator's own step — Phase 3 in this case — is the
correct aggregation boundary). Passive hooks still capture `tool.call`
for the underlying `Bash` invocations (build tool, `jq`, `xmllint`).

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-story-implement` | caller (primary) | Phase 3 carve-out: the skill's stdout envelope replaces Steps 3.1 (coverage), 3.2 (cross-file consistency), 3.8 (smoke), and the implicit AC → test mapping — ~160 inline lines |
| `x-epic-implement` | caller (indirect, via story-0049-0019 downstream) | Consumes the same envelope when iterating stories at the epic scope; `passed=false` drives per-story remediation loops |
| `x-internal-story-load-context` | peer | Sibling `internal/plan/` skill; provides the upstream envelope consumed by Phase 0 (`storyFile`, `taskCount`, `scopeTier`); this skill reads the story file independently for Section 7 scenarios (idempotent mtime-aware) |
| `x-internal-story-build-plan` | peer | Sibling `internal/plan/` skill; runs the Phase 1 planning dispatch; this skill runs the Phase 3 verification gate at the end of the same story's lifecycle |
| `x-internal-report-write` | downstream | Consumes this skill's envelope to render the story dashboard `dashboard-story-XXXX-YYYY.md` |
| `x-internal-status-update` | downstream | Consumes `passed` to transition `execution-state.json` → `COMPLETE` / `FAILED` |
| `x-test-e2e` | indirect | Project smoke runner invoked in Step 6 when `testing.smoke_tests == true` |

Downstream stories that depend on this carve-out: story-0049-0019
(orchestrator consumes the envelope and deletes the inline Phase 3
block).

Full workflow detail (argument-parser rejection matrix, file-list
heuristics per build tool, coverage-XML schema for JaCoCo / Istanbul /
Cobertura / llvm-cov / go-cover, cross-file consistency heuristic
catalogue with worked examples, AC scenario-slug → test-name matching
algorithm, smoke-layer parsing contract, and the
`--parallel-tests` / `--strict-consistency` extensions) lives in
[`references/full-protocol.md`](references/full-protocol.md) per
ADR-0011.
