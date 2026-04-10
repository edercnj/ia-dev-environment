---
name: x-code-format
description: >
  Formats source code using the appropriate formatter for {{LANGUAGE}}.
  First step of the pre-commit chain (format -> lint -> compile -> commit).
  Supports --check (dry-run) and --changed-only modes.
  Reference: `.github/skills/x-code-format/SKILL.md`
---

# Skill: Format Code

## Purpose

Ensures all source code in {{PROJECT_NAME}} follows a consistent formatting style before commits. This skill is the first step of the pre-commit chain (RULE-007): `x-code-format -> x-code-lint -> compile -> commit`. It detects the project language via `{{LANGUAGE}}`, selects the appropriate formatter, executes it, and re-stages any modified files automatically.

## Triggers

- `/x-code-format` -- format all project files
- `/x-code-format --check` -- verify formatting without modifying files (CI mode)
- `/x-code-format --changed-only` -- format only modified files in the working tree

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--check` | No | Dry-run mode: exit 0 if formatted, exit 1 if not. No files modified. |
| `--changed-only` | No | Format only staged + unstaged modified files (detected via git diff). |

## Language Detection and Formatter Mapping

| Language | Primary Formatter | Fallback | Format Command | Check Command |
|----------|------------------|----------|----------------|---------------|
| java | spotless ({{BUILD_TOOL}} plugin) | google-java-format | `mvn spotless:apply` / `gradle spotlessApply` | `mvn spotless:check` / `gradle spotlessCheck` |
| typescript | prettier | -- | `npx prettier --write .` | `npx prettier --check .` |
| python | ruff format | black | `ruff format .` | `ruff format --check .` |
| go | gofmt | -- | `gofmt -w .` | `gofmt -l .` |
| rust | rustfmt | -- | `cargo fmt` | `cargo fmt --check` |
| kotlin | ktfmt | -- | `ktfmt --google-style .` | `ktfmt --google-style --dry-run .` |

## Workflow

```
1. DETECT      -> Identify {{LANGUAGE}} and {{BUILD_TOOL}}
2. SELECT      -> Choose formatter (primary, fallback if unavailable)
3. SCOPE       -> Determine target files (all, changed-only, or check)
4. EXECUTE     -> Run formatter
5. RE-STAGE    -> Automatically git add files that were staged and reformatted
6. REPORT      -> Output summary of changes
```

### Step 1 -- Detect Language

The project language is determined by `{{LANGUAGE}}`. If the language is not in the supported list, report the unsupported language and exit with code 0 (do not block the pre-commit chain).

### Step 2 -- Select Formatter

For each language, attempt the primary formatter first. If it is not available (command not found), fall back to the secondary formatter. If neither is available, report an error with installation instructions.

### Step 3 -- Determine Scope

#### All files (default)

Run the formatter on the entire project source tree.

#### --changed-only

Detect modified files using git:

```bash
# Staged files
git diff --cached --name-only --diff-filter=ACMR

# Unstaged modified files
git diff --name-only --diff-filter=ACMR
```

| Language | Extensions |
|----------|-----------|
| java | `.java` |
| typescript | `.ts`, `.tsx`, `.js`, `.jsx` |
| python | `.py` |
| go | `.go` |
| rust | `.rs` |
| kotlin | `.kt`, `.kts` |

#### --check

Run the formatter in check/verify mode. Do not modify any files. Return exit code 1 if any file needs formatting.

### Step 4 -- Execute Formatter

Run the selected formatter command.

### Step 5 -- Re-stage Modified Files

After formatting, detect which previously staged files were modified by the formatter and re-stage them automatically.

### Step 6 -- Report

Output a summary with language, formatter used, files checked, reformatted, and re-staged counts.

## Error Handling

| Scenario | Behavior |
|----------|----------|
| Unsupported language | Report warning, exit 0 (do not block chain) |
| No formatter installed | Report error with install instructions, exit 1 |
| Formatter execution fails | Report error with stderr output, exit 1 |
| No files to format | Report "No files to format", exit 0 |
| --check finds violations | List files, exit 1 |

## Integration with Pre-Commit Chain

```
x-code-format -> x-code-lint -> compile -> commit
```

- If `x-code-format` succeeds (exit 0), the chain continues with reformatted files.
- If `x-code-format --check` fails (exit 1), the chain stops with an error.
