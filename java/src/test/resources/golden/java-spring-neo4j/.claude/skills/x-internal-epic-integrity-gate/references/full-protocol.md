# Full Protocol — x-internal-epic-integrity-gate

> **Context:** Fat reference of the slim SKILL.md under this folder.
> ADR-0011 mandates this split for every skill > 250 lines. The
> SKILL.md contains the minimum viable contract (purpose,
> parameters, response, exit codes, examples); this file contains
> the deep detail: argument-parser rejection matrix, JaCoCo CSV
> schema, build-tool equivalents, DoD-predicate catalogue, and the
> `--skip-clean` optimisation.

## 1. Argument Parser Rejection Matrix

The parser is a hand-written loop over `$@` that tolerates all four
invocation forms and exits `64` on any malformation.

| Input | Outcome | Stderr |
| :--- | :--- | :--- |
| `--epic-id 0049` | Accept: `epic_id=0049` | — |
| `--epic-id=0049` | Accept: `epic_id=0049` | — |
| `--epic-id 49` | Accept (zero-pad): `epic_id=0049` | — |
| `--epic-id` (no value) | Reject | `--epic-id requires a value` |
| `--epic-id abc` | Reject | `--epic-id must match ^[0-9]{4}$` |
| `--epic-id 00049` | Reject | `--epic-id must match ^[0-9]{4}$` |
| (flag absent) | Reject | `--epic-id is required` |
| `--branch epic/0049` | Accept | — |
| `--branch=epic/0049` | Accept | — |
| `--branch "  epic/0049  "` | Accept (trimmed) | — |
| `--branch ""` | Reject | `--branch must not be empty` |
| `--branch "bad..name"` | Reject | `--branch fails refs/heads validation` |
| `--branch "-dangling"` | Reject | `--branch fails refs/heads validation` |
| `--coverage-threshold-line 95` | Accept | — |
| `--coverage-threshold-line 101` | Reject | `threshold must be in [0,100]` |
| `--coverage-threshold-line -1` | Reject | `threshold must be in [0,100]` |
| `--coverage-threshold-line 95.5` | Reject | `threshold must be integer` |
| `--unknown-flag` | Reject | `unknown flag --unknown-flag; usage: ...` |
| `positional-arg` | Reject | `positional args not allowed; usage: ...` |

All rejections exit `64` (sysexits `EX_USAGE`) and emit a trailing
`usage: --epic-id <id> [--branch <name>] [--coverage-threshold-line N] [--coverage-threshold-branch N]`.

## 2. JaCoCo CSV Schema and Parsing

The JaCoCo CSV generated at `target/site/jacoco/jacoco.csv` has a
fixed schema since JaCoCo 0.7.x:

```
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
```

Parsing contract:

1. Skip the header row (first line).
2. For each remaining row, split on `,` (no quoting in JaCoCo output
   — class names never contain commas).
3. Accumulate four sums: `line_missed`, `line_covered`, `branch_missed`,
   `branch_covered`.
4. Compute percentages with one-decimal rounding (half-up):
   - `line_pct = round(line_covered * 100.0 / (line_missed + line_covered), 1)`
   - `branch_pct = round(branch_covered * 100.0 / (branch_missed + branch_covered), 1)`
5. When the denominator is zero (no code / no branches), emit `100.0`
   for that dimension and append
   `coverage: no <line|branch> data (denominator zero)` to `failures`
   as an advisory (does not block `passed`).

### Worked parsing example

Input CSV:

```
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,...
my-proj,com.example,Foo,10,90,2,8,4,96,...
my-proj,com.example,Bar,5,45,0,0,1,24,...
```

Aggregate:
- `line_covered = 96 + 24 = 120`
- `line_missed = 4 + 1 = 5`
- `line_pct = round(120 * 100 / 125, 1) = 96.0`
- `branch_covered = 8 + 0 = 8`
- `branch_missed = 2 + 0 = 2`
- `branch_pct = round(8 * 100 / 10, 1) = 80.0`

With default thresholds (95 line, 90 branch) →
`passed=false`, `failures=["coverage: branch 80.0 < 90"]`,
exit 3.

### Multi-module Maven projects

Multi-module projects generate per-module CSVs under
`<module>/target/site/jacoco/jacoco.csv`. The skill aggregates across
all CSVs discovered via:

```bash
find . -type f -path '*/target/site/jacoco/jacoco.csv'
```

The aggregation is additive — sum all four counters across every
module before computing percentages. This matches the Jacoco Maven
Plugin `report-aggregate` goal semantics, but does not require it.

## 3. Build-Tool Equivalents (Non-Maven Stacks)

The skill auto-detects the build tool via file probes; the first
match wins (probe order: Maven → Gradle → npm → cargo → go → pytest).

| Build tool | Test + coverage command | Coverage file |
| :--- | :--- | :--- |
| Maven | `mvn clean test jacoco:report -q` | `target/site/jacoco/jacoco.csv` |
| Gradle | `./gradlew clean test jacocoTestReport -q` | `build/reports/jacoco/test/jacocoTestReport.csv` |
| npm (jest) | `npm test -- --coverage --coverageReporters=text-summary --coverageReporters=cobertura` | `coverage/cobertura-coverage.xml` |
| cargo | `cargo tarpaulin --out Xml --skip-clean` | `cobertura.xml` |
| go | `go test ./... -coverprofile=coverage.out -covermode=atomic` | `coverage.out` |
| pytest | `pytest --cov=. --cov-report=xml` | `coverage.xml` |

For non-JaCoCo coverage formats, the parsing logic is equivalent —
extract LINE_COVERED / LINE_MISSED / BRANCH_COVERED / BRANCH_MISSED
from the format's schema (Cobertura XML: `<line>` elements; go-cover:
per-file aggregation). The envelope shape is stable across all
stacks.

When no supported build tool is detected, the skill logs
`test: no build tool detected; skipping` to stderr, skips Step 3 / 4,
emits a best-effort envelope with `coverageDelta.line=0,
coverageDelta.branch=0`, and `failures` containing
`test: no build tool detected`.

## 4. DoD Predicate Catalogue (v1)

Each predicate is a pure function over the filesystem at the checked-out
branch HEAD. Predicates MUST NOT mutate state, hit the network, or
depend on ordering.

### 4.1 `all-tasks-done`

Scan every `plans/epic-<id>/story-*.md`. For each story:

- Read Section 8 `## 8. Tasks`.
- Count lines matching `^- \[ \] TASK-` (undone) and `^- \[x\] TASK-`
  (done).
- If the story `**Status:**` header is `Concluida` / `Concluída`, the
  count of undone tasks is treated as zero (accepted as "closed out").
- Otherwise, every undone checkbox is a failure.

Failure message (aggregated):
`dod: <N> task(s) not marked DONE across <M> story(ies)`.

### 4.2 `changelog-entry`

Read `CHANGELOG.md`. The predicate passes iff at least one of:

- The `## [Unreleased]` section exists and contains at least one
  non-empty bullet under any of `### Added` / `### Changed` /
  `### Fixed` / `### Removed`.
- A version header newer than the previous release tag (determined
  via `git tag -l 'v*' | sort -V | tail -1`) exists and contains at
  least one non-empty bullet.

Failure message: `dod: changelog-entry missing` (CHANGELOG.md absent
or has no qualifying entry).

### 4.3 `tests-present`

Compute the diff between the checked-out branch and `main` (or the
project's configured integration branch if `main` is absent):

```bash
git diff --name-only main...HEAD \
  | grep -E '^src/main/.*\.(java|kt|ts|py|go|rs)$' \
  | sort -u
```

For each touched source file, check that a corresponding test file
exists under `src/test/` following the class-name convention
(`FooBar.java` → `FooBarTest.java` / `FooBarIT.java` /
`FooBarSmokeTest.java`). Languages without Java conventions map via:

| Language | Source pattern | Test pattern |
| :--- | :--- | :--- |
| Java / Kotlin | `src/main/java/.../Foo.{java,kt}` | `src/test/java/.../Foo{Test,IT,SmokeTest}.{java,kt}` |
| TypeScript | `src/foo.ts` | `src/foo.test.ts` or `src/foo.spec.ts` |
| Python | `src/foo.py` | `tests/test_foo.py` |
| Go | `pkg/foo.go` | `pkg/foo_test.go` |
| Rust | `src/foo.rs` | `src/foo.rs` (inline `#[cfg(test)]`) or `tests/foo.rs` |

Failure message: `dod: <N> file(s) without corresponding test`.

### 4.4 `adr-references-resolvable`

Scan every `plans/epic-<id>/story-*.md` for patterns `ADR-\d{4}` or
`adr/ADR-\d{4}-`. For each referenced ID, verify the file
`adr/ADR-<id>-*.md` exists (glob match).

Failure message: `dod: <N> ADR reference(s) unresolved`.

### 4.5 `story-status-concluida`

For every `plans/epic-<id>/story-*.md`:
- Read the `**Status:**` header line (first 30 lines).
- Accept `Concluida` / `Concluída` / `Done` / `COMPLETE` (accent +
  case agnostic).
- Any other value (`Pendente`, `In Progress`, empty) is a failure.

Failure message: `dod: <N> story(ies) not Concluida`.

### 4.6 Extension contract

New predicates are appended to this section with:
1. A unique kebab-case `item` name (stable — used as join key for
   reports).
2. A pure predicate body (filesystem read-only).
3. A single-line failure message template.
4. A bump of the response-contract doc comment with the new
   `dodChecklist` entry.

The envelope schema does NOT version — new predicates always append;
callers consuming the envelope MUST be tolerant to unknown `item`
values (ignore or surface as-is).

## 5. `--skip-clean` Optimisation

When the caller knows `target/` is clean and consistent (e.g., the
orchestrator just ran a prior gate on a related branch), the
`--skip-clean` flag replaces `mvn clean test jacoco:report` with
`mvn test jacoco:report`. Speeds up large epics by ~60% on warm
build caches.

Invariants when `--skip-clean` is active:
- Caller asserts no branch-spanning cache pollution is possible.
- Coverage results MUST still match a cold-build run — verified by
  JaCoCo's own merge-across-runs behaviour (JaCoCo merges incremental
  executions unless the `exec` file is absent).
- The skill emits `advisory: --skip-clean active` to stderr on every
  run as a reminder.

Not enabled by default — the integrity gate prioritises reproducibility
over speed.

## 6. Integration With x-epic-implement Phase 4

The orchestrator (`x-epic-implement`) consumes this skill's envelope
in Phase 4 (post-phase integration gate). Pseudocode:

```pseudo
envelope = Skill(x-internal-epic-integrity-gate, --epic-id {id})
if envelope.passed:
    x-internal-status-update --phase {N} --status PASS
    x-internal-report-write --template _TEMPLATE-PHASE-COMPLETION-REPORT
    advance_to_next_phase()
else:
    if any(f.startswith("coverage:") for f in envelope.failures):
        dispatch_coverage_remediation_agent(envelope)
    if any(f.startswith("dod:") for f in envelope.failures):
        dispatch_dod_remediation_agent(envelope)
    if exit_code == 2:
        dispatch_regression_diagnosis_agent(envelope)
    pause_for_operator_review()
```

The orchestrator never re-runs the gate inline; it either advances
(on `passed=true`) or delegates remediation to dispatched agents
that themselves re-run this skill after fixes. The skill is
idempotent — repeat invocations against the same branch HEAD return
the same envelope.

## 7. Observability Hooks

The skill does NOT emit its own telemetry (see SKILL.md §Telemetry).
Passive `tool.call` hooks capture:

- `git rev-parse` / `git checkout` (Step 2)
- `mvn` invocation (Step 3) — typically the longest tool call in the
  phase
- `awk` / `jq` invocations (Steps 4–6) — sub-second

Aggregation happens at the orchestrator's Phase 4 boundary:
`phase.start x-epic-implement Phase-4-IntegrityGate` …
`phase.end x-epic-implement Phase-4-IntegrityGate ok`.

## 8. Known Gotchas

1. **Detached HEAD on checkout failure.** If `git checkout` fails
   mid-operation (e.g., merge conflict from a dirty working tree),
   the skill's `trap` restores the caller's original branch. Callers
   MUST stash dirty changes before invoking this skill.
2. **JaCoCo aggregate vs per-module.** Multi-module projects with
   the `report-aggregate` goal configured produce both per-module
   CSVs and an aggregate CSV. The skill discovers all CSVs via
   `find` and aggregates additively — this produces correct totals
   regardless of the aggregate goal's presence.
3. **Branch-name validation edge cases.** Git accepts branch names
   like `feat/foo/bar` (slashes as logical separators); the skill
   accepts those. It rejects names with `..`, trailing `/`, or
   leading `-` per the `git check-ref-format` contract.
4. **Zero-code modules.** Empty modules (test-only, docs-only)
   produce CSV rows with all zeros. The aggregation correctly handles
   these; percentage computation uses aggregate sums, so a single
   empty module does not skew the result.
5. **Coverage CSV present but malformed.** If the CSV is truncated
   or has fewer than 9 columns, the skill emits
   `coverage: report malformed (row N)` and exits `4` (treats as
   missing report for safety).

## 9. v2 Extensions (Deferred)

Candidates for a future v2 without breaking the envelope schema:

- **`--compare-baseline <sha>`**: compute coverage delta vs a named
  baseline SHA (requires a prior envelope for that SHA). Useful for
  "is coverage regressing within this epic?" analyses.
- **`--parallel-tests`**: pass through surefire `forkCount` to the
  Maven invocation. Currently the skill trusts whatever is configured
  in the project's `pom.xml`.
- **`--json-only`**: suppress Maven stdout/stderr, emit only the
  envelope. Orchestrators that multiplex gates in parallel benefit
  from this.

All v2 extensions MUST preserve the four envelope fields and exit-code
catalogue; new fields MUST be additive and default-optional for
backwards compatibility.
