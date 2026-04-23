---
name: x-code-format
model: haiku
description: "Formats source code using the appropriate formatter for {{LANGUAGE}}. First step of the pre-commit chain (format -> lint -> compile -> commit). Supports --check (dry-run) and --changed-only modes."
user-invocable: true
allowed-tools: Bash, Read, Grep, Glob
argument-hint: "[--check | --changed-only]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Format Code (slim — ADR-0012)

Ensures source code in {{PROJECT_NAME}} follows a consistent style before commits. First step of the pre-commit chain (RULE-007: `x-code-format -> x-code-lint -> compile -> commit`). Detects `{{LANGUAGE}}`, selects the appropriate formatter, runs it, and re-stages modified files automatically.

## Triggers

- `/x-code-format` -- format all project files
- `/x-code-format --check` -- verify formatting without modifying files (CI mode)
- `/x-code-format --changed-only` -- format only modified files (staged + unstaged)

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--check` | No | Dry-run: exit 0 if formatted, exit 1 if not. No files modified. |
| `--changed-only` | No | Format only files detected via `git diff` (staged + unstaged). |

## Output Contract

- **Mutation:** files formatted in place (unless `--check`). Previously-staged files are automatically re-staged after format.
- **Exit codes:** `0` success / nothing to do; `1` formatter not installed, execution failure, or violations under `--check`.
- **Report to stdout:** language, formatter used, files checked, files reformatted, files re-staged.
- **Supported languages / formatter commands:** see `references/full-protocol.md` §1 for the full mapping. Slim summary: spotless (java), prettier (typescript), ruff/black (python), gofmt (go), rustfmt (rust), ktfmt (kotlin).

## Error Envelope

> **Chain-wide error matrix.** Canonical `format -> lint -> compile -> commit` rows live in [`_shared/error-handling-pre-commit.md`](../_shared/error-handling-pre-commit.md). Rows below are `x-code-format`-specific.

| Scenario | Behavior | Exit |
|----------|----------|------|
| Unsupported language (`{{LANGUAGE}}` not in table) | Report warning, do NOT block chain | 0 |
| No formatter installed (primary + fallback missing) | Report error with install instructions | 1 |
| Formatter execution fails | Report error with stderr output | 1 |
| No files to format | Report `"No files to format"` | 0 |
| `--check` finds violations | List files needing formatting | 1 |
| Re-stage failure (`git add` error) | Report error with git stderr | 1 |

## Full Protocol

Minimum viable contract above. Detailed language/formatter mapping, the 6-step workflow (detect → select → scope → execute → re-stage → report), build-tool configuration examples (Maven/Gradle/npm/Python/Go/Rust), and per-language file extension lists live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default). Happy-path invocation needs only this SKILL.md; full protocol is consulted when a new language or fallback formatter is introduced.

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-code-lint` | follows | Second step of the pre-commit chain (after format). |
| `x-git-commit` | orchestrator | Invokes this skill as step 3a of its chain. |

## Template Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `{{LANGUAGE}}` | Project language | `java` |
| `{{BUILD_TOOL}}` | Project build tool | `maven` |
| `{{PROJECT_NAME}}` | Project name | `my-java-cli` |
