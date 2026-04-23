# Full Protocol — x-internal-story-verify

Detailed reference material carved out of `SKILL.md` per ADR-0011
(SKILL size budget). Consumed by operators investigating a verify
failure and by maintainers extending the heuristic catalogue.

## §1 — Argument-Parser Rejection Matrix

The parser in Step 1 of the skill body follows a strict decision
table. Every cell below is exercised by the acceptance tests under
TASK-0049-0014-005.

| Case | Input fragment | Behaviour | Exit |
| :--- | :--- | :--- | :--- |
| 1 | `--story-id story-0049-0014 --epic-id 0049` | accept | continue |
| 2 | `--story-id=story-0049-0014 --epic-id=0049` | accept (equals form) | continue |
| 3 | `--story-id STORY-0049-0014 --epic-id 0049` | normalise to lowercase → accept | continue |
| 4 | `--story-id story-0049-0014 --epic-id 49` | zero-pad epic to `0049` → accept | continue |
| 5 | `--story-id story-49-14 --epic-id 0049` | regex fail (expects `NNNN-NNNN`) | 64 |
| 6 | `--story-id story-0049-0014` (no epic) | missing required | 64 |
| 7 | `--epic-id 0049` (no story) | missing required | 64 |
| 8 | `--story-id story-0049-0014 --epic-id 0049 --bogus` | unknown flag | 64 |
| 9 | `--story-id story-0049-0014 --epic-id 0049 --coverage-threshold-line 150` | out-of-range threshold | 64 |
| 10 | `--story-id story-0049-0014 --epic-id 0049 --coverage-threshold-line abc` | non-integer threshold | 64 |
| 11 | `--story-id '' --epic-id 0049` | empty value | 64 |
| 12 | `--help` | print usage; exit 0 | 0 |

The banner printed to stderr for cases 5–11:

```
usage: x-internal-story-verify --story-id <story-XXXX-YYYY> --epic-id <XXXX>
       [--coverage-threshold-line <N>] [--coverage-threshold-branch <N>]
```

## §2 — File-List Heuristics per Build Tool

Step 2 of the skill body resolves the story's file list via the task
breakdown. The breakdown `Files:` bullet MAY include glob patterns
(e.g., `src/test/resources/golden/internal/plan/x-internal-story-verify/**`).
This section documents how those globs expand per build tool.

### Maven (default)

Classes are derived from the canonical Java convention `src/main/java/dev/...`:

```
src/main/java/dev/iadev/foo/Bar.java  → dev.iadev.foo.Bar
src/test/java/dev/iadev/foo/BarTest.java → dev.iadev.foo.BarTest
```

Surefire `-Dtest=<csv>` accepts fully-qualified classes OR glob syntax
(`dev.iadev.foo.*`). The skill prefers the FQCN form when the file
list is resolvable and falls back to package globs when the breakdown
references `**/*.java`.

### Gradle

`gradle test --tests dev.iadev.foo.Bar` accepts one class per flag;
the skill emits N flags in one invocation.

### npm / vitest

`--run <pattern>` accepts glob; file list is translated to
`src/**/{Foo,Bar}.test.ts`.

### Cargo

`cargo test <module>` accepts module prefix; file list is reduced to
the unique set of parent modules.

### Go

`go test ./pkg/foo/... ./pkg/bar/...` — directory-based filter.

### Pytest

`pytest tests/foo/test_bar.py` — file-based filter.

## §3 — Coverage XML Schemas

Step 4 parses coverage XML. Schemas supported:

### JaCoCo (Maven / Gradle default)

Path: `target/site/jacoco/jacoco.xml`. Counters are nested under
`<class>` elements; sum across:

```xml
<class name="dev/iadev/foo/Bar" sourcefilename="Bar.java">
  <counter type="LINE"   missed="2" covered="18"/>
  <counter type="BRANCH" missed="0" covered="4"/>
</class>
```

### Istanbul (npm)

Path: `coverage/coverage-final.json`. Line coverage percentage is
`statementMap` coverage summary; branch is `branchMap`.

### Cobertura (Python `coverage.xml`)

Path: `coverage.xml`. Line-rate and branch-rate are per-`class`
attributes; sum covered/missed across matching entries.

### llvm-cov (Rust / C++)

Path: `target/llvm-cov/codecov.json`. JSON structure with
`function_coverage` / `branch_coverage` objects.

### go-cover (Go)

Path: `coverage.out`. Text format; `go tool cover -func=coverage.out`
yields per-function coverage; aggregation is `(covered / total) * 100`.

The skill detects the first schema present and falls back to "report
not generated" when none is found.

## §4 — Cross-File Consistency Heuristic Catalogue

Step 5 scans pairs of files sharing a role suffix. The catalogue:

### 4.1 Role Suffix Table

| Suffix | Role |
| :--- | :--- |
| `*Controller.java` | REST adapter inbound |
| `*Resource.java` | REST adapter inbound (JAX-RS flavour) |
| `*Service.java` | Application-layer service |
| `*UseCase.java` | Application-layer use case |
| `*Assembler.java` | DTO assembler |
| `*Mapper.java` | Layer mapper (adapter ↔ domain) |
| `*Repository.java` | Outbound adapter (persistence) |
| `*Port.java` | Domain port interface |
| `*Dto.java` | Adapter DTO |
| `*Entity.java` | Outbound persistence entity |

### 4.2 Constructor Arity Check

Two files with the same role MUST declare the same number of
constructor parameters (public or package-private). Violation example:

```java
// FooAssembler.java
public FooAssembler(ProjectRepository projectRepository) { … }

// BarAssembler.java
public BarAssembler(                              // ← 2 params vs 1
    ProjectRepository projectRepository,
    Clock clock) { … }
```

Emits: `consistency: constructor arity mismatch between FooAssembler (1) and BarAssembler (2)`.

### 4.3 Return-Type Uniformity

When two siblings both declare one of the canonical method names, the
return types must match:

| Canonical name | Expected return shape |
| :--- | :--- |
| `handle(X)` | `R` (any, but uniform) |
| `execute(X)` | `R` (uniform) |
| `process(X)` | `R` (uniform) |
| `apply(X)` | `R` (uniform) |
| `create(X)` | `R` / `R` (uniform) |
| `update(X)` | `R` (uniform) |
| `delete(X)` | `void` or `R` (uniform) |
| `find*(X)` | `Optional<R>` or `Stream<R>` (uniform across siblings) |

Violation example:

```java
// FooRepository.java
Optional<Foo> findById(UUID id);

// BarRepository.java
Bar findById(UUID id);        // ← raw vs Optional
```

Emits: `consistency: findById return type diverges between FooRepository (Optional<Foo>) and BarRepository (Bar)`.

### 4.4 Error-Handling Shape

When one sibling throws a domain exception and another returns a
`Result` / `Either` / `Optional` for the same role and method, emit:

```
consistency: mixed error shape across FooAssembler (throws DomainException) and BarAssembler (returns Result<Bar, DomainError>)
```

### 4.5 Opt-In Strictness (`--strict-consistency`)

The default mode tolerates generic-parameter divergence
(`Optional<Foo>` vs `Optional<Bar>` is acceptable). The strict mode
(reserved for future Story-0049-0020 extension) enforces exact type
matching including generic parameters.

### 4.6 Suppression

A file may declare `// verify:consistency-ignore` on line 1 of the
source file to opt out of §4.2–§4.4. The suppression is logged to
stderr but does NOT count against `passed`.

## §5 — AC Scenario-Slug → Test-Name Algorithm

Step 7 maps each Gherkin scenario to a test method. The algorithm:

1. Extract the scenario title (everything after `Cenario:` / `Scenario:`
   up to newline).
2. Strip accents via `iconv -f UTF-8 -t ASCII//TRANSLIT`.
3. Lowercase, collapse whitespace to `-`, strip non-alphanumeric
   except `-`.
4. Split on `-`; retain the 3+ longest word tokens (length ≥ 4) as the
   signature set.
5. `grep -rnilw` across `src/test/` for test methods whose name
   contains EVERY token in the signature set (case-insensitive).
6. Early-exit on first match; report the file:line to `acCheckResults`.

Example:

```
Scenario: Verify passa em story limpa
         ↓ (steps 1-4)
tokens = {"verify", "passa", "story", "limpa"}
         ↓ (step 5)
grep pattern: (?=.*verify)(?=.*passa)(?=.*story)(?=.*limpa)
         ↓ (step 6)
match: src/test/.../StoryVerifySmokeTest.java:42 → verifyPassaEmStoryLimpa_happyPath()
```

### Known false negatives

- Abbreviated test names: `testVerifyHappyPath()` won't match
  `{"verify", "passa", "story", "limpa"}`. Mitigation: authors use
  `@DisplayName` with the scenario title verbatim; the algorithm also
  greps `@DisplayName` strings.
- Non-English scenarios with English test names: the skill's second
  pass translates common PT tokens (`passa→passes`, `falha→fails`,
  `erro→error`, `limpa→clean`, `sem→without`) before the final grep.

## §6 — Smoke Layer Parsing

Step 6 parses the smoke command's output per the project-configured
runner. Canonical shape (exit code + grep markers):

| Layer | Exit code | Stdout marker | Blocks `passed`? |
| :--- | :--- | :--- | :--- |
| Health Check | 0 | `HEALTH OK` | yes |
| Critical Path | 0 | `CRITICAL_PATH OK` | yes |
| Response Time | 0 | `RESPONSE_TIME OK` | no (advisory) |
| Error Rate | 0 | `ERROR_RATE OK` | no (advisory) |

A missing marker is treated as failure for that layer. When the smoke
runner emits JSON (future stories 0049-0020+), the skill falls back to
`jq -r .layers[].status`.

## §7 — Failure Envelope Examples

### 7.1 Build-tool non-zero

Exit 2, no envelope:

```
stderr: Test suite failed (mvn exit 1; see target/surefire-reports/)
```

### 7.2 Coverage XML missing

Exit 3, envelope:

```json
{"passed":false,"coverageDelta":{"line":0,"branch":0,"lineThreshold":95,"branchThreshold":90,"lineDelta":-95.0,"branchDelta":-90.0},"failures":["coverage: report not generated"],"acCheckResults":[]}
```

### 7.3 Mixed failures

Exit 0, envelope:

```json
{"passed":false,"coverageDelta":{"line":94.2,"branch":89.0,"lineThreshold":95,"branchThreshold":90,"lineDelta":-0.8,"branchDelta":-1.0},"failures":["coverage: line 94.2 < 95","coverage: branch 89.0 < 90","consistency: constructor arity mismatch between FooAssembler (1) and BarAssembler (2)","ac: 'Boundary case' has no test"],"acCheckResults":[{"scenario":"Happy path","hasTest":true},{"scenario":"Boundary case","hasTest":false}]}
```

Note exit 3 is reserved for coverage-ONLY gap; mixed failures exit 0
with `passed=false` so the orchestrator can compose a remediation
plan from all four categories at once.

## §8 — Performance Extensions

### 8.1 `--parallel-tests`

Opt-in knob (future story 0049-0020) that propagates to Maven as
`-DforkCount=0.5C` (half the CPU count). Trade-off: parallel forks
disable `@Isolated` tests; the caller must whitelist.

### 8.2 `--coverage-cache`

Opt-in: the skill computes a SHA256 of the story file list + the
`target/site/jacoco/jacoco.xml` mtime and caches the parsed coverage
delta under `target/x-internal-story-verify-cache/`. Cache-hit skips
Step 4; cache-miss repopulates. Target speedup: 3× on subsequent
verification passes during iterative debugging.

## §9 — Concurrency Contract

The skill acquires a shared `flock -s` on
`plans/epic-${epic_id}/execution-state.json` only to read; it does NOT
acquire exclusive locks. Multiple `x-internal-story-verify` invocations
across different stories of the same epic may run in parallel without
coordination. The build tool invocation itself is the bottleneck — use
your build tool's own concurrency controls (Maven `--threads`, Gradle
parallel) to tune.

## §10 — Rationale: Why Bash-only?

The five earlier `x-internal-*` skills variously declared `Bash`,
`Bash + Skill`, `Bash + Skill + Agent`. This skill is strictly `Bash`
because:

1. Verification is a **leaf** operation — no sub-planning, no parallel
   agent dispatch.
2. All I/O is: file reads (story, breakdown, coverage XML), process
   spawn (build tool), structured stdout.
3. JSON assembly is trivial via `jq -n` and benefits from no shell
   escape gymnastics.
4. Keeping the surface minimal eases auditability (Rule 22) and
   telemetry scope.

A future extension (e.g., `--auto-fix-coverage`) that dispatches a
subagent to write missing tests WOULD expand `allowed-tools` to
include `Agent`; it is deliberately out of scope for story-0049-0014.

## §11 — Downstream Consumer Contract (x-story-implement Phase 3)

The orchestrator (`x-story-implement`) consumes this envelope as
follows (pseudo):

```
envelope=$(Skill(x-internal-story-verify, --story-id ... --epic-id ...))
passed=$(jq -r '.passed' <<< "$envelope")

if [[ "$passed" == "false" ]]; then
  # Dispatch remediation subagent per failures[] category:
  for failure in $(jq -r '.failures[]' <<< "$envelope"); do
    case "$failure" in
      coverage:*)    Skill(x-test-plan, --missing-tests ...) ;;
      consistency:*) Skill(x-review, --dimension=cross-file) ;;
      ac:*)          Skill(x-test-plan, --gherkin-gap ...) ;;
      smoke:*)       Skill(x-ops-troubleshoot, --smoke) ;;
    esac
  done
fi
```

When `passed=true`, the orchestrator advances to Phase 3.4 (specialist
reviews) without reading any other field — the envelope is
self-describing.

## §12 — Forward Compatibility

Reserved fields in the envelope (present but not yet populated) for
future stories:

| Field | Reserved for | Planned story |
| :--- | :--- | :--- |
| `coverageDelta.mutationScore` | mutation testing (PIT) | 0049-0020+ |
| `coverageDelta.uncoveredLines` | file-level gap report | 0049-0020+ |
| `failures[].severity` | per-failure severity tier | 0049-0020+ |
| `acCheckResults[].testFile` | test location echo | 0049-0020+ |

Consumers MUST tolerate absence of these fields (schema-forward
compatibility via `jq // empty`). Adding fields is a PATCH-level
schema change; removing or retyping fields is a MINOR break and
requires the caller's envelope-version negotiation.
