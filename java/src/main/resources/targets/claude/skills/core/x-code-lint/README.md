# x-lint

> Analyzes source code with the appropriate linter for {{LANGUAGE}}. Second step in the pre-commit chain (RULE-007: format -> lint -> compile -> commit). Supports --fix, --changed-only, and --strict modes.

| | |
|---|---|
| **Category** | Dev/Quality |
| **Invocation** | `/x-lint [--fix \| --changed-only \| --strict]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Runs static analysis on project source code using the language-appropriate linter. Detects potential bugs (null dereference, resource leaks), code smells (long methods, cyclomatic complexity), and convention violations (naming, imports). Supports auto-fix mode with automatic re-staging, changed-only mode for incremental analysis, and strict mode that treats warnings as errors.

## Usage

```
/x-lint
/x-lint --fix
/x-lint --changed-only
/x-lint --strict
/x-lint --fix --changed-only
```

## Workflow

1. **Detect** -- Identify language and select appropriate linter(s)
2. **Scope** -- Determine file set (all project files or changed-only)
3. **Lint** -- Execute primary linter, then secondary linter if configured
4. **Fix** -- Apply auto-corrections if `--fix`; re-stage corrected files
5. **Categorize** -- Classify findings as ERROR, WARNING, or INFO
6. **Report** -- Output summary with exit code (0 = pass, 1 = fail)

## Supported Languages

| Language | Primary Linter | Secondary Linter(s) |
|----------|---------------|---------------------|
| Java | checkstyle | spotbugs, pmd |
| Kotlin | ktlint | detekt |
| TypeScript | eslint | -- |
| Python | ruff | pylint |
| Go | golangci-lint | -- |
| Rust | clippy | -- |

## Outputs

| Artifact | Description |
|----------|-------------|
| Console report | Findings categorized by severity with file, line, and rule ID |
| Exit code | 0 (pass) or 1 (fail based on errors or strict warnings) |
| Re-staged files | Files corrected by `--fix` are automatically re-staged |

## See Also

- [x-format](../x-format/) -- Precedes lint in the pre-commit chain (code formatting)
- [x-git-push](../x-git-push/) -- Follows lint in the pre-commit chain (commit and push)
- [x-review](../x-review/) -- Deeper parallel code review with specialist engineers
- [x-codebase-audit](../x-codebase-audit/) -- Full codebase audit including lint checks
