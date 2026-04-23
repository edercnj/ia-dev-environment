---
name: x-git-commit
model: haiku
description: "Creates Conventional Commits with Task ID in scope and pre-commit chain (format -> lint -> compile). Central commit point in the task-centric workflow with TDD tag support."
user-invocable: true
allowed-tools: Bash, Read, Grep, Glob, Write, Edit, Skill
argument-hint: "--task TASK-XXXX-YYYY-NNN --type <type> --subject <subject> [--tdd RED|GREEN|REFACTOR] [--body <body>] [--skip-chain] [--amend]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Conventional Commit with Task ID (slim — ADR-0012)

Creates standardized commits for {{PROJECT_NAME}} with the Task ID in the scope, enforces the pre-commit chain (RULE-007: `x-code-format -> x-code-lint -> compile -> commit`), and annotates commits with TDD tags (RULE-008). Central commit point in the task-centric workflow.

## Triggers

- `/x-git-commit --task TASK-XXXX-YYYY-NNN --type feat --subject "add detection logic"` -- normal commit
- `/x-git-commit --task TASK-XXXX-YYYY-NNN --type test --subject "add unit tests" --tdd RED` -- TDD-tagged commit
- `/x-git-commit --task TASK-XXXX-YYYY-NNN --type chore --subject "update config" --skip-chain` -- skip pre-commit chain (emergency only)

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--task` | Yes | Task ID `TASK-XXXX-YYYY-NNN` (4-digit epic, 4-digit story, 3-digit task) |
| `--type` | Yes | Conventional Commits type: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `perf` |
| `--subject` | Yes | Commit subject, imperative mood, no period, max 72 characters |
| `--tdd` | No | TDD tag: `RED`, `GREEN`, `REFACTOR`, or generic `TDD` |
| `--body` | No | Multi-line commit body (additional context) |
| `--skip-chain` | No | Skip pre-commit chain (emergency use only, emits WARNING) |
| `--amend` | No | Amend the last commit instead of creating a new one |

## Output Contract

- **Commit message format:** `<type>(<TASK-XXXX-YYYY-NNN>): <subject> [TDD:TAG]` (tag suffix omitted when `--tdd` absent) followed by optional body/footer.
- **Git state:** one new commit on the current branch (or last commit amended when `--amend`); staged files are re-staged after format/lint auto-fix.
- **Report to stdout:** task id, type, subject, TDD tag, chain status (passed/skipped), short SHA, full message first line.
- **Exit codes:** see `## Error Envelope` below.

## Pre-Commit Chain (RULE-007)

```
x-code-format -> x-code-lint -> compile ({{COMPILE_COMMAND}}) -> commit
```

Each step invoked via the Skill tool (Rule 13 — INLINE-SKILL pattern). Files modified by format/lint are automatically re-staged. Chain-wide error rows (exit codes, soft-vs-hard classification) live in [`_shared/error-handling-pre-commit.md`](../_shared/error-handling-pre-commit.md).

## Error Envelope

> **Chain-wide error matrix.** The canonical `format -> lint -> compile -> commit` error rows (shared across the pre-commit cluster) live in [`_shared/error-handling-pre-commit.md`](../_shared/error-handling-pre-commit.md). The rows below cover `x-git-commit`-specific scenarios only.

| Scenario | Behavior |
|----------|----------|
| Invalid task ID format (does not match `TASK-\d{4}-\d{4}-\d{3}`) | ABORT with format hint |
| Subject exceeds 72 characters | ABORT with character count |
| Invalid commit type | ABORT with valid types list (`feat, fix, test, refactor, docs, chore, perf`) |
| Invalid TDD tag | ABORT with valid tags list (`RED, GREEN, REFACTOR, TDD`) |
| No staged files | ABORT with `"No staged files for commit"` |
| x-code-format fails | ABORT with `"Pre-commit chain failed at step 'x-code-format'"` |
| x-code-lint finds ERRORs | ABORT with `"Pre-commit chain failed at step 'x-code-lint'"` |
| Compile fails | ABORT with `"Pre-commit chain failed at step 'compile'"` |
| git commit fails | ABORT with git error message |
| Non-imperative subject (e.g., "adds", "fixed") | WARN but proceed (soft validation) |
| `--skip-chain` used | Proceed with WARNING "Pre-commit chain skipped — emergency use only" |

## Full Protocol

Minimum viable contract above. Detailed validation rules, the full workflow with shell snippets, message-building examples, TDD tag semantics, and the amend flow live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default). Reading the full protocol is only required for atypical scenarios (custom amend flow, diagnosing a chain failure); happy-path invocations need only this SKILL.md.

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-code-format` | invoked by | First step of pre-commit chain |
| `x-code-lint` | invoked by | Second step of pre-commit chain |
| `x-git-push` | followed by | Push after commit is created |
| `x-story-implement` | orchestrated by | Lifecycle invokes x-git-commit for each task |
| `x-test-run` | precedes | Tests should pass before committing |

## Template Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `{{COMPILE_COMMAND}}` | Project compile command | `mvn compile -q` |
| `{{LANGUAGE}}` | Project language | `java` |
| `{{BUILD_TOOL}}` | Project build tool | `maven` |
| `{{PROJECT_NAME}}` | Project name | `my-java-cli` |
