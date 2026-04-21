---
name: x-code-format
description: "Formats source code using the appropriate formatter for {{LANGUAGE}}. First step of the pre-commit chain (format -> lint -> compile -> commit). Supports --check (dry-run) and --changed-only modes."
user-invocable: true
allowed-tools: Bash, Read, Grep, Glob
argument-hint: "[--check | --changed-only]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

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

```bash
# Example: Check if primary formatter is available
command -v ruff >/dev/null 2>&1 || {
    echo "WARN: ruff not found, trying black as fallback..."
    command -v black >/dev/null 2>&1 || {
        echo "ERROR: No formatter found for python. Install ruff or black."
        exit 1
    }
}
```

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

# Combine and filter by language extension
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

Run the formatter in check/verify mode. Do not modify any files. Return exit code 1 if any file needs formatting, exit code 0 if all files are correctly formatted.

### Step 4 -- Execute Formatter

Run the selected formatter command. Capture output for the report.

### Step 5 -- Re-stage Modified Files

After formatting, detect which previously staged files were modified by the formatter and re-stage them automatically:

```bash
# Record the currently staged paths in a temp file (NUL-delimited)
STAGED_BEFORE="$(mktemp)"
git diff --cached --name-only -z > "$STAGED_BEFORE"

# After formatting, re-stage files that were already staged
while IFS= read -r -d '' file; do
    if git diff --name-only -z | grep -zFx "$file" > /dev/null; then
        git add "$file"
        echo "Re-staged: $file"
    fi
done < "$STAGED_BEFORE"

rm -f "$STAGED_BEFORE"
```

### Step 6 -- Report

Output a summary:

```
x-code-format complete:
  Language:    {{LANGUAGE}}
  Formatter:   <formatter-used>
  Files checked: <count>
  Files reformatted: <count>
  Files re-staged: <count>
```

## Error Handling

> **Chain-wide error matrix.** For the canonical `format -> lint -> compile -> commit` chain error rows (exit codes, soft-vs-hard classification) see [`_shared/error-handling-pre-commit.md`](../_shared/error-handling-pre-commit.md). The rows below cover `x-code-format`-specific scenarios only.

| Scenario | Behavior |
|----------|----------|
| Unsupported language | Report warning, exit 0 (do not block chain) |
| No formatter installed | Report error with install instructions, exit 1 |
| Formatter execution fails | Report error with stderr output, exit 1 |
| No files to format | Report "No files to format", exit 0 |
| --check finds violations | List files, exit 1 |

## Integration with Pre-Commit Chain

This skill is designed to be the first step in the chain:

```
x-code-format -> x-code-lint -> compile -> commit
```

- If `x-code-format` succeeds (exit 0), the chain continues with reformatted files.
- If `x-code-format --check` fails (exit 1), the chain stops with an error message listing files that need formatting.
- The `--check` flag is used in CI/CD pipelines to enforce formatting without modifying files.

## Build Tool Integration

### Maven (Java)

```xml
<!-- pom.xml: Spotless plugin configuration -->
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <configuration>
        <java>
            <googleJavaFormat/>
        </java>
    </configuration>
</plugin>
```

### Gradle (Java/Kotlin)

```groovy
// build.gradle: Spotless plugin
plugins {
    id 'com.diffplug.spotless'
}
spotless {
    java { googleJavaFormat() }
    kotlin { ktfmt().googleStyle() }
}
```

### npm (TypeScript)

```json
{
    "scripts": {
        "format": "prettier --write .",
        "format:check": "prettier --check ."
    }
}
```

### Python

```toml
# pyproject.toml
[tool.ruff]
line-length = 120

[tool.black]
line-length = 120
```

### Go

No configuration needed. `gofmt` uses the canonical Go style.

### Rust

```toml
# rustfmt.toml
edition = "2021"
max_width = 120
```

## Slim Mode

> **When to use:** When this skill is invoked programmatically from another skill (e.g., x-git-commit pre-commit chain), read ONLY this section for minimum context.

### Formatter Commands

| Language | Format Command | Check Command |
|----------|----------------|---------------|
| java | `mvn spotless:apply` / `gradle spotlessApply` | `mvn spotless:check` |
| typescript | `npx prettier --write .` | `npx prettier --check .` |
| python | `ruff format .` | `ruff format --check .` |
| go | `gofmt -w .` | `gofmt -l .` |
| rust | `cargo fmt` | `cargo fmt --check` |
| kotlin | `ktfmt --google-style .` | `ktfmt --dry-run .` |

### Re-Stage After Format

```bash
# Record the currently staged paths in a temp file (NUL-delimited)
STAGED_BEFORE="$(mktemp)"
git diff --cached --name-only -z > "$STAGED_BEFORE"

# After running the formatter, re-stage exactly those previously staged files
if [ -s "$STAGED_BEFORE" ]; then
  xargs -0 git add -- < "$STAGED_BEFORE"
fi

rm -f "$STAGED_BEFORE"
```

### Error Handling

- Unsupported language -> exit 0 (do not block chain)
- No formatter installed -> exit 1 with install hint
- Formatter fails -> exit 1 with stderr
- No files to format -> exit 0
