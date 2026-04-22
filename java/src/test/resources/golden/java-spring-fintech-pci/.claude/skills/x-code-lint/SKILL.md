---
name: x-code-lint
description: "Analyzes source code with the appropriate linter for {{LANGUAGE}}. Second step in the pre-commit chain (RULE-007: format -> lint -> compile -> commit). Supports --fix, --changed-only, and --strict modes."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[--fix | --changed-only | --strict]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Code Linting (slim — ADR-0012)

Runs static analysis on {{PROJECT_NAME}} source code using the appropriate linter for **{{LANGUAGE}}**. Second step of the pre-commit chain (RULE-007: `format -> lint -> compile -> commit`). Detects bugs, code smells, naming violations, and convention deviations before commit.

## Triggers

- `/x-code-lint` -- lint entire project, report errors and warnings
- `/x-code-lint --fix` -- auto-fix violations where supported; re-stage corrected files
- `/x-code-lint --changed-only` -- lint only modified files (staged + unstaged)
- `/x-code-lint --strict` -- treat warnings as errors (exit 1 on any warning)
- `/x-code-lint --fix --changed-only` -- auto-fix only changed files

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--fix` | Flag | off | Auto-correct fixable violations; re-stage corrected files via `git add` |
| `--changed-only` | Flag | off | Analyze only files from `git diff --name-only` + `git diff --cached --name-only` |
| `--strict` | Flag | off | Treat warnings as errors; exit 1 if any warning exists |

## Output Contract

- **Mutation:** none by default; with `--fix`, source files are corrected in place and re-staged.
- **Report to stdout:** language, linter(s) used, scope (all / changed-only), mode (default/fix/strict), ERROR/WARNING/INFO counts, per-finding detail lines `[{SEVERITY}] {file}:{line} -- {rule-id}: {description}`, and final `Result: {PASS | FAIL}`.
- **Exit codes:** see `## Error Envelope` below.
- **Supported linters (summary):** checkstyle+spotbugs+pmd (java), ktlint+detekt (kotlin), eslint (typescript), ruff+pylint (python), golangci-lint (go), clippy (rust). Per-language commands and secondary linter mapping: see `references/full-protocol.md` §1.

## Error Envelope

> **Chain-wide error matrix.** Canonical `format -> lint -> compile -> commit` rows live in [`_shared/error-handling-pre-commit.md`](../_shared/error-handling-pre-commit.md). Rows below are `x-code-lint`-specific.

| Scenario | Exit | Behavior |
|----------|------|----------|
| No findings (or INFO-only) | 0 (PASS) | Proceed to next chain step |
| WARNING-only findings without `--strict` | 0 (PASS) | Proceed; warnings listed in report |
| WARNING findings with `--strict` | 1 (FAIL) | Block chain; exit with warning count |
| Any ERROR findings | 1 (FAIL) | Block chain; exit with error list |
| Linter not installed | 1 | Suggest installation command |
| No source files found (empty scope) | 0 | Report `"No files to analyze"` |
| Linter config missing | 0 (with WARN) | Use linter defaults; warn about missing config |
| Linter command fails (internal error) | 1 | Report stderr details |
| `--changed-only` with no changed files | 0 | Report `"No modified files"` |
| `--fix` leaves unfixable errors | 1 | Report remaining errors after fix attempt |

## Full Protocol

Minimum viable contract above. The 7-step workflow (detect → scope → primary lint → secondary lint → fix → categorize → report), full language/linter mapping with `--fix` commands, severity-classification rules, and per-language notes (Java checkstyle/spotbugs/pmd details, Kotlin ktlint/detekt, TypeScript eslint, Python ruff/pylint, Go golangci-lint, Rust clippy) live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default). Happy-path invocation from the pre-commit chain reads only this SKILL.md.

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-code-format` | precedes | Format runs before lint in pre-commit chain |
| `x-git-commit` | orchestrator | Invokes this skill as step 3b of its chain |
| `x-review` | complementary | Review provides deeper analysis; lint catches basics |
| `x-code-audit` | complementary | Audit runs lint as part of full codebase check |

## Template Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `{{LANGUAGE}}` | Project language | `java` |
| `{{BUILD_TOOL}}` | Project build tool | `maven` |
| `{{BUILD_TOOL_CMD}}` | CLI prefix for build tool | `mvn` / `./gradlew` / `npx` |
| `{{PROJECT_NAME}}` | Project name | `my-java-cli` |
