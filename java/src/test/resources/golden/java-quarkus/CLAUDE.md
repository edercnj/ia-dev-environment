<!-- placeholders: PROJECT_NAME, LANGUAGE, FRAMEWORK, ARCHITECTURE, DATABASES, INTERFACE_TYPES, BUILD_COMMAND, TEST_COMMAND -->
<!-- Schema authoritative: adr/ADR-0048-B-claude-md-contract.md §3.2 -->
<!-- Generator-owned: do not hand-edit — regenerated on every `ia-dev-env generate`. -->

# my-quarkus-service

## Project Overview

**my-quarkus-service** is a java project built with **quarkus**.

This repository follows a standardized layout produced by the `ia-dev-env` generator. Claude Code loads this `CLAUDE.md` automatically on every conversation, so everything Claude needs to work effectively on this codebase should appear here.

## Build

```bash
./mvnw package -DskipTests
```

## Test

```bash
./mvnw verify
```

## Architecture Notes

- **Style:** microservice
- **Databases:** none
- **Interfaces:** rest, grpc, event-consumer, event-producer

## Key Rules

- Follow the generated rules under `.claude/rules/` — they encode coding standards, quality gates, branching model, security baseline, and operations baseline for this project.
- Never edit `.claude/` directly; it is generator output. Regenerate via `ia-dev-env generate` after updating the source YAML.
- Keep test coverage at project thresholds (≥95% line / ≥90% branch by default — see `.claude/rules/05-quality-gates.md`).
- Atomic commits in Conventional Commits format; see `.claude/rules/08-release-process.md`.

## Related Skills

Project-specific skills and knowledge packs live under `.claude/skills/`. Invoke them via `/name` in Claude Code chat, or let Claude pick the right one based on context.
