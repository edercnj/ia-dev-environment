---
name: x-lint
description: >
  Analyzes source code with the appropriate linter for {{LANGUAGE}}.
  Second step in the pre-commit chain (RULE-007: format -> lint -> compile -> commit).
  Supports --fix, --changed-only, and --strict modes.
  Reference: `.github/skills/x-lint/SKILL.md`
---

# Skill: Code Linting

## Purpose

Runs static analysis on {{PROJECT_NAME}} source code using the appropriate linter for **{{LANGUAGE}}**. Detects potential bugs, code smells, naming violations, and convention deviations before code is committed. This is the second step in the pre-commit chain (RULE-007): `format -> lint -> compile -> commit`.

## Triggers

- `/x-lint` -- lint entire project, report errors and warnings
- `/x-lint --fix` -- auto-fix violations where supported, re-stage corrected files
- `/x-lint --changed-only` -- lint only modified files (staged + unstaged)
- `/x-lint --strict` -- treat warnings as errors (exit code 1 on any warning)
- `/x-lint --fix --changed-only` -- auto-fix only changed files

## Arguments

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `--fix` | Flag | false | Auto-correct fixable violations; re-stage corrected files |
| `--changed-only` | Flag | false | Analyze only modified files (staged + unstaged) |
| `--strict` | Flag | false | Treat warnings as errors; exit 1 on any warning |

## Workflow

```
1. DETECT     -> Identify language and build tool
2. SCOPE      -> Determine file set (all or changed-only)
3. LINT       -> Execute primary linter
4. SECONDARY  -> Execute secondary linter (if configured)
5. FIX        -> Apply auto-fixes if --fix (re-stage files)
6. CATEGORIZE -> Classify findings as ERROR / WARNING / INFO
7. REPORT     -> Output summary; exit code based on findings
```

### Step 1 -- Detect Language and Linter

The project uses **{{LANGUAGE}}** with **{{BUILD_TOOL}}**. Select linters:

| Language | Primary Linter | Secondary Linter(s) | Primary Command |
|----------|---------------|---------------------|-----------------|
| java | checkstyle | spotbugs, pmd | `mvn checkstyle:check` |
| kotlin | ktlint | detekt | `ktlint` |
| typescript | eslint | -- | `npx eslint .` |
| python | ruff | pylint | `ruff check .` |
| go | golangci-lint | -- | `golangci-lint run ./...` |
| rust | clippy | -- | `cargo clippy -- -D warnings` |

### Step 2 -- Determine File Scope

With `--changed-only`: collect files from `git diff --name-only` and `git diff --cached --name-only`, filter by language extension. Without: analyze entire project.

### Step 3-4 -- Execute Linters

Run primary linter, then secondary if configured. With `--fix`, use fix-mode commands (e.g., `ruff check --fix`, `npx eslint --fix`, `ktlint --format`).

### Step 5 -- Re-stage Fixed Files

When `--fix` modifies files, detect changes via `git diff --name-only` and re-stage with `git add`.

### Step 6 -- Categorize Findings

| Severity | Behavior | Examples |
|----------|----------|---------|
| ERROR | Blocks commit | Null dereference, resource leak |
| WARNING | Reports only (blocks with --strict) | Method > 25 lines, high complexity |
| INFO | Informational | Refactoring suggestion |

### Step 7 -- Report and Exit

Exit code 0 on pass, 1 on ERROR or WARNING with `--strict`.

## Error Handling

| Scenario | Action |
|----------|--------|
| Linter not installed | Suggest installation command; exit 1 |
| No source files found | Report "No files to analyze"; exit 0 |
| Linter config missing | Use defaults; warn about missing config |
| `--changed-only` with no changes | Report "No modified files"; exit 0 |

## Integration Notes

- Precedes compile step in pre-commit chain (RULE-007)
- Follows x-format (code formatting must pass first)
- Works with any detected language stack (Java, Kotlin, TypeScript, Python, Go, Rust)
- Re-staged files from --fix mode are included in the subsequent commit
