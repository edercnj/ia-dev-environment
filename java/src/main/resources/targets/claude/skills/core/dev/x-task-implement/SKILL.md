---
name: x-task-implement
model: sonnet
description: "Implements a feature/story/task using TDD (Red-Green-Refactor) workflow. Schema-aware: v1 (legacy) runs the original Double-Loop TDD flow with story-section task extraction; v2 (task-first, EPIC-0038) reads task-TASK-XXXX-YYYY-NNN.md + plan-task-TASK-XXXX-YYYY-NNN.md, honours declared I/O contracts, respects task-implementation-map dependencies, verifies post-conditions via grep/assert, and produces a single atomic commit per task via x-git-commit."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[TASK-ID (TASK-XXXX-YYYY-NNN) or STORY-ID or feature-description] [--worktree] [--no-ci-watch]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **EXECUTION INTEGRITY (Rule 24)** — Every `Skill(...)` block is a **MANDATORY TOOL CALL**. TDD cycles (`x-test-tdd`), atomic commits (`x-git-commit`), CI watch (`x-pr-watch-ci`), and PR creation (`x-pr-create`) MUST be invoked as real tool calls, not inlined. See `.claude/rules/24-execution-integrity.md`.

## Triggers

```
/x-task-implement STORY-ID          — implement a story by ID (v1 or v2 schema auto-detected)
/x-task-implement TASK-XXXX-YYYY-NNN  — implement a specific task (v2 task-file-first mode)
/x-task-implement feature-description  — implement a feature from description
/x-task-implement STORY-ID --worktree  — standalone worktree mode (ADR-0004 Mode 2)
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `STORY-ID` or `TASK-ID` or description | positional | (required) | Story ID, TASK-XXXX-YYYY-NNN, or feature description |
| `--worktree` | boolean | `false` | Create dedicated worktree (standalone Mode 2). Ignored inside existing worktree (Rule 14 §3). |
| `--no-ci-watch` | boolean | `false` | Skip Step 4.5 CI-Watch. Required for CI/automation. |

## Output Contract

**Schema dispatch:**

| `planningSchemaVersion` | Execution Mode | Input artifacts |
|--------------------------|---------------|-----------------|
| `"1.0"` (or absent) | v1 — Double-Loop TDD via story section 8 | story file, `plan-story-*.md`, `tests-story-*.md` |
| `"2.0"` | v2 — task-file-first | `task-TASK-*.md` + `plan-task-TASK-*.md` + `task-implementation-map-*.md` |

Emits structured result to caller:
```json
{"status":"DONE","taskId":"TASK-XXXX-YYYY-NNN","commitSha":"abc123","cycleCount":N,"coverageDelta":{"lineBefore":95.1,"lineAfter":95.3},"wallclockMs":12340}
```

**TDD Cycle (telemetry markers preserved):**

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Red-Phase`

**RED:** write failing test → run → MUST fail.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Red-Phase ok`

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Green-Phase`

**GREEN:** implement minimum code → run all tests → MUST pass.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Green-Phase ok`

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Refactor-Phase`

**REFACTOR:** improve design without adding behavior → tests MUST stay green.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Refactor-Phase ok`

**CI-Watch (Step 4.5):**

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Phase-4-5-CI-Watch`

Fires only when: `planningSchemaVersion=="2.0"` AND `--worktree` present AND `--no-ci-watch` absent AND `inWorktree==false` at Step 0.5 (standalone). Invokes: `Skill(skill: "x-pr-watch-ci", args: "--pr-number {N} --poll-interval-seconds 60 --timeout-minutes 30 --require-copilot-review=false")`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Phase-4-5-CI-Watch ok`

## Error Envelope

| Code | Condition |
|------|-----------|
| `TASK_ARTIFACT_NOT_FOUND` | task / plan / map file missing (v2) |
| `UNMET_DEPENDENCY` | declared `Depends on` TASK-ID not DONE |
| `SCHEMA_VIOLATION` | task file fails ERROR-level schema validation |
| `OUTPUT_CONTRACT_VIOLATION` | declared output failed post-exec verification (v2 Phase 3) |
| `RED_NOT_OBSERVED` | RED phase test didn't fail as expected |
| `REFACTOR_BROKE_TESTS` | refactor made previously-green tests fail |
| `STATUS_SYNC_FAILED` | Phase 3.5 (v2) failed to update `**Status:**` header or map row |
| Coverage below threshold | Add missing test scenarios; no bypass |

## Full Protocol

> Complete Step 0 (plan reuse + staleness check), Step 0.5 (worktree-first policy, ADR-0004 3-way REUSE/CREATE/LEGACY), Step 1 (subagent reads KPs + builds TDD plan), Step 2 (Double-Loop TDD + TPP ordering, full bash snippets), Step 3 (coverage + AC validation), Step 4 (TDD atomic commit conventions), v2 extensions (Phase 0c schema detection, Phase 0d–0e pre-execution gates, Phase 3 output-contract verification, Phase 3.5 status transition via `StatusFieldParser`/`TaskMapRowUpdaterCli`, Phase 5 status report), CI-Watch state-file schema, Mode-Aware cleanup (Rule 14 §5 creator-owns-removal), and all knowledge pack references in [`references/full-protocol.md`](references/full-protocol.md).
