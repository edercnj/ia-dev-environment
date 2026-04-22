# x-code-lint — Full Protocol

> **Slim/Full split** per [ADR-0012 — Skill Body Slim-by-Default](../../../../../../../../../adr/ADR-0012-skill-body-slim-by-default.md).
> The `SKILL.md` sibling carries the minimum viable contract; this file
> holds the workflow detail, linter mapping, severity classification, and
> per-language notes.

## 1. Language / Linter Mapping

| Language | Primary Linter | Secondary Linter(s) | Primary Command | Secondary Command(s) |
|----------|---------------|---------------------|-----------------|---------------------|
| java | checkstyle | spotbugs, pmd | `{{BUILD_TOOL_CMD}} checkstyle:check` | `{{BUILD_TOOL_CMD}} spotbugs:check`, `{{BUILD_TOOL_CMD}} pmd:check` |
| kotlin | ktlint | detekt | `ktlint` | `detekt` |
| typescript | eslint | -- | `npx eslint .` | -- |
| python | ruff | pylint | `ruff check .` | `pylint **/*.py` |
| go | golangci-lint | -- | `golangci-lint run ./...` | -- |
| rust | clippy | -- | `cargo clippy -- -D warnings` | -- |

`{{BUILD_TOOL_CMD}}` resolves from the project build tool:

| Build Tool | Command Prefix |
|-----------|----------------|
| maven | `mvn` |
| gradle | `./gradlew` |
| npm | `npx` |
| yarn | `yarn` |
| pnpm | `pnpm` |
| cargo | `cargo` |
| go | `go` |
| pip / poetry | (direct CLI) |

## 2. Workflow

```
1. DETECT     -> Identify language and build tool
2. SCOPE      -> Determine file set (all or changed-only)
3. LINT       -> Execute primary linter
4. SECONDARY  -> Execute secondary linter (if configured)
5. FIX        -> Apply auto-fixes if --fix (re-stage files)
6. CATEGORIZE -> Classify findings as ERROR / WARNING / INFO
7. REPORT     -> Output summary; exit code based on findings
```

### Step 1 — Detect Language and Linter

The project uses **{{LANGUAGE}}** with **{{BUILD_TOOL}}** as its build tool. Select linters from §1 above.

### Step 2 — Determine File Scope

**Without `--changed-only`:** Analyze the entire project (default behavior for each linter).

**With `--changed-only`:**

```bash
# Collect modified files (staged + unstaged)
CHANGED_FILES=$(git diff --name-only && git diff --cached --name-only)

# Filter by language extension
# java/kotlin: *.java, *.kt
# typescript: *.ts, *.tsx
# python: *.py
# go: *.go
# rust: *.rs
```

If no files match after filtering, report `"No files to analyze"` and exit 0.

### Step 3 — Execute Primary Linter

Run the primary linter command from §1. Capture stdout and stderr.

**Fix mode (`--fix`):**

| Language | Fix Command |
|----------|------------|
| java | `{{BUILD_TOOL_CMD}} checkstyle:check` (manual fix required) |
| kotlin | `ktlint --format` |
| typescript | `npx eslint . --fix` |
| python | `ruff check . --fix` |
| go | `golangci-lint run ./... --fix` |
| rust | `cargo clippy --fix --allow-dirty --allow-staged` |

### Step 4 — Execute Secondary Linter

If a secondary linter is configured (see §1 table), run it after the primary linter completes. Consolidate results from both linters into a single report.

### Step 5 — Re-stage Fixed Files

When `--fix` is active and files were modified:

```bash
# Detect which staged files were changed by the linter
FIXED_FILES=$(git diff --name-only)

# Re-stage the fixed files
git add $FIXED_FILES
```

Report: `"{count} files corrected and re-staged"`.

### Step 6 — Categorize Findings

Classify each finding into one of three severity levels:

| Severity | Behavior | Examples |
|----------|----------|---------|
| **ERROR** | Blocks commit; chain interrupted with exit code 1 | Null dereference, resource leak, SQL injection, unused import causing compilation issue |
| **WARNING** | Reported but does not block (unless `--strict`) | Method > 25 lines, cyclomatic complexity > 10, magic numbers, missing Javadoc |
| **INFO** | Informational only; never blocks | Refactoring suggestion, style preference |

### Step 7 — Report and Exit

**Output format:**

```
== x-code-lint Report ==

Language:  {{LANGUAGE}}
Linter(s): {primary} [+ {secondary}]
Scope:     {all | changed-only ({count} files)}
Mode:      {default | fix | strict}

Findings:
  ERROR:   {count}
  WARNING: {count}
  INFO:    {count}

{If --fix}: {count} files corrected and re-staged

Details:
  [{SEVERITY}] {file}:{line} -- {rule-id}: {description}
  [{SEVERITY}] {file}:{line} -- {rule-id}: {description}
  ...

Result: {PASS | FAIL}
```

**Exit code logic:**

| Condition | Exit Code | Result |
|-----------|-----------|--------|
| No findings | 0 | PASS |
| Only INFO findings | 0 | PASS |
| Only WARNING findings (no `--strict`) | 0 | PASS |
| WARNING findings with `--strict` | 1 | FAIL |
| Any ERROR findings | 1 | FAIL |

## 3. Pre-Commit Chain Integration (RULE-007)

This skill is the **second step** in the pre-commit chain:

```
x-code-format --> x-code-lint --> compile --> commit
```

- **Precondition:** `x-code-format` has already ensured code style compliance.
- **On FAIL (exit 1):** Chain is interrupted; commit is blocked with the violation list.
- **On PASS (exit 0):** Chain continues to the compile step.
- **With `--fix`:** Corrected files are re-staged and chain continues.

## 4. Language-Specific Notes

### Java (checkstyle + spotbugs + pmd)

- checkstyle validates code style and naming conventions.
- spotbugs detects potential bugs (null dereference, resource leaks, infinite loops).
- pmd detects code smells (unused variables, empty catch blocks, god classes).
- All three run via `{{BUILD_TOOL_CMD}}` plugins; ensure plugins are declared in `pom.xml` or `build.gradle`.

### Kotlin (ktlint + detekt)

- ktlint enforces Kotlin coding conventions (official style guide).
- detekt analyzes complexity, naming, and potential bugs.
- `ktlint --format` auto-fixes style violations.

### TypeScript (eslint)

- eslint with project `.eslintrc` configuration.
- Supports `--fix` for auto-correctable rules (unused imports, formatting overlap).
- Respects `.eslintignore` for exclusions.

### Python (ruff + pylint)

- ruff is the primary linter (fast, covers most rules).
- pylint as secondary for deeper analysis (type inference, design patterns).
- `ruff check --fix` auto-fixes import sorting, unused imports, and simple patterns.

### Go (golangci-lint)

- golangci-lint aggregates multiple linters (govet, staticcheck, errcheck, etc.).
- Configuration via `.golangci.yml`.
- `--fix` supported for select linters.

### Rust (clippy)

- clippy is the standard Rust linter (part of rustup).
- `-D warnings` treats clippy warnings as errors by default.
- `--fix` applies machine-applicable suggestions.

## 5. Rationale

The slim `SKILL.md` intentionally omits the 7-step workflow prose, the
per-language `--fix` command table, severity examples, and the
linter-specific notes. The pre-commit chain needs to know only the
command to run and the exit semantics; those facts are already in the
slim file (§Output Contract and §Error Envelope). Per-language
implementer detail is diagnostic material that the happy path does not
consult.

See [ADR-0012 — Skill Body Slim-by-Default](../../../../../../../../../adr/ADR-0012-skill-body-slim-by-default.md)
for the architectural rationale.
