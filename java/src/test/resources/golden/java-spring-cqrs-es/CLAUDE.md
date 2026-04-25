<!-- placeholders: PROJECT_NAME, LANGUAGE, FRAMEWORK, ARCHITECTURE, DATABASES, INTERFACE_TYPES, BUILD_COMMAND, TEST_COMMAND -->
<!-- Schema authoritative: adr/ADR-0048-B-claude-md-contract.md §3.2 -->
<!-- Generator-owned: do not hand-edit — regenerated on every `ia-dev-env generate`. -->

# my-spring-cqrs

## Project Overview

**my-spring-cqrs** is a java project built with **spring-boot**.

This repository follows a standardized layout produced by the `ia-dev-env` generator. Claude Code loads this `CLAUDE.md` automatically on every conversation, so everything Claude needs to work effectively on this codebase should appear here.

## EXECUTION INTEGRITY — NÃO NEGOCIÁVEL

Every `Skill(skill: "...", args: "...")` block inside a SKILL.md is a **TOOL CALL**, never prose. The LLM executing a skill MUST emit the declared tool calls — inlining, summarizing, or "simulating" sub-skills is a violation of **Rule 24 — Execution Integrity** (`.claude/rules/24-execution-integrity.md`).

Sub-skills may be bypassed only with an explicit `--skip-review` / `--skip-verification` / `--skip-smoke` flag. Merged stories without mandatory evidence artifacts fail CI via `scripts/audit-execution-integrity.sh` with exit code `EIE_EVIDENCE_MISSING`.

| Sub-skill | Evidence required | Enforced by |
| :--- | :--- | :--- |
| `x-review` | `plans/epic-XXXX/plans/review-story-STORY-ID.md` | CI audit + Stop hook |
| `x-review-pr` | `plans/epic-XXXX/plans/techlead-review-story-STORY-ID.md` | CI audit + Stop hook |
| `x-internal-story-verify` | `plans/epic-XXXX/reports/verify-envelope-STORY-ID.json` | CI audit |
| `x-internal-story-report` | `plans/epic-XXXX/reports/story-completion-report-STORY-ID.md` | CI audit |

## Build

```bash
./gradlew build -x test
```

## Test

```bash
./gradlew test
```

## Architecture Notes

- **Style:** cqrs
- **Databases:** none
- **Interfaces:** rest, event-consumer, event-producer

## Key Rules

- Follow the generated rules under `.claude/rules/` — they encode coding standards, quality gates, branching model, security baseline, and operations baseline for this project.
- Never edit `.claude/` directly; it is generator output. Regenerate via `ia-dev-env generate` after updating the source YAML.
- Keep test coverage at project thresholds (≥95% line / ≥90% branch by default — see `.claude/rules/05-quality-gates.md`).
- Atomic commits in Conventional Commits format; see `.claude/rules/08-release-process.md`.

## Related Skills

Project-specific skills live under `.claude/skills/` (invocable via `/name` in Claude Code chat). Knowledge packs — reference docs for architecture, security, testing, coding standards, etc. — live under `.claude/knowledge/` and are read by skills as context (see ADR-0013).
