---
status: Accepted
date: 2026-04-22
deciders:
  - Eder Celeste Nunes Junior
story-ref: "story-0048-0002"
---

# ADR-0048-A: Java-Only Scope for the `ia-dev-env` Generator

## Status

Accepted | 2026-04-22

## Context

The `ia-dev-env` generator was originally designed to support 6 programming languages — Java, Python, Go, Kotlin, TypeScript, Rust — with partial support for C#/.NET. In practice, **100% of observed usage is Java**, and the multi-language maintenance overhead has grown non-trivial:

- **3256 non-Java golden files** in `java/src/test/resources/golden/` (story-0048-0001 §4 inventory).
- **8 of 17 smoke profiles** (`SmokeProfiles.SMOKE_PROFILES`) exist solely to exercise non-Java stacks; each regeneration and assertion run incurs wall-clock cost.
- Duplicate agent / hook / skill / rule / anti-pattern artifacts per language in `java/src/main/resources/targets/claude/`.
- Dangling `csharp-dotnet` leftover at `StackMapping.java:61-66` — declared in `LANGUAGE_COMMANDS`, `FRAMEWORK_LANGUAGE_RULES`, `DOCKER_BASE_IMAGES`, `HOOK_TEMPLATE_MAP`, `SETTINGS_LANG_MAP` but **with no corresponding profile / golden** (`StackMappingJavaOnlyIntegrityTest` would fail). This leftover is documented evidence of drift between multi-language intent and single-language reality.
- Baseline `mvn test` wall-clock: **102.73s** (story-0048-0001 §5.1). Target post-epic: **~72s** (−30%).

The epic-0048 plan was agreed between the author and maintainer on 2026-04-16 after an exploration phase confirmed the three findings above. No active user exists for python / go / kotlin / typescript / rust / csharp perfis within the organization. The `csharp-dotnet` leftover is a regression signal, not a feature.

## Decision

The `ia-dev-env` generator is **restricted to Java only** starting with release **v4.0.0**.

Specifically:

- `LanguageFrameworkMapping.LANGUAGES == List.of("java")` — the only accepted value for `--language` / `language:` YAML.
- The following languages are **removed** from all mappings, templates, skills, rules, goldens, smoke tests, and YAMLs:
  - **python** (including `fastapi`, `click-cli`, `flask`, `django` frameworks).
  - **go** (including `gin`, `stdlib`, `fiber`).
  - **kotlin** (including `ktor`, `kotlin-gradle` build tool — note: kotlin maps back to `java-gradle` in `SETTINGS_LANG_MAP` today; both are removed).
  - **typescript** (including `nestjs`, `express`, `fastify`, `commander`).
  - **rust** (including `axum`, `actix-web`).
  - **csharp / dotnet** (explicitly included even though no profile / golden exists — eliminates the `csharp-dotnet` leftover and preserves invariant that the domain model matches the resource model).
- Orthogonal dimensions are **preserved** (RULE-048-02): databases (PostgreSQL, MySQL, ClickHouse, Elasticsearch, Neo4j, TimescaleDB), messaging (Kafka, RabbitMQ, SQS), architecture patterns (hexagonal, CQRS, event-driven, clean, layered, DDD), interface types (REST, gRPC, GraphQL, WebSocket, CLI, event-consumer / producer, scheduled), compliance frameworks (PCI, HIPAA, LGPD, SOX).
- A new `UnsupportedLanguageException` is introduced and fails-fast with an exact user-facing message (RULE-048-06):

  ```text
  Language '<x>' is not supported. Only 'java' is available
  (see CHANGELOG v4.0.0 / EPIC-0048).
  ```
- Two temporary feature flags ship in v4.0.0 for rollback / smoke-testing and are removed in v5.0.0:
  - `--legacy-empty-dirs` — disables the `pruneEmptyDirs` fix from story-0048-0009 if it is retained (story-0048-0009 scope is pending empirical review — see epic-0048 investigation-report.md §3).
  - `--no-claude-md` — disables `ClaudeMdAssembler` (story-0048-0011 Bug B fix).

## Consequences

### Positive

- **Maintainability** — ~10k+ lines of language-specific code deleted; smoke matrix drops from 17→9; `mvn test` wall-clock targets −30% (~102s → ~72s).
- **Clarity** — `csharp-dotnet` leftover removed; domain model matches reality.
- **Release discipline** — single-language scope aligns with actual usage; breaking change is honest about SemVer instead of maintaining unused code.

### Negative

- **Breaking change** — users depending on non-Java profiles must pin `v3.x` (latest: v3.11.0-SNAPSHOT at the time of this ADR). Users who invoke `--language python|go|kotlin|typescript|rust|csharp` see a fatal `UnsupportedLanguageException` with the exact message above.
- **No forward path for other languages in this generator** — adding Python support back in the future requires re-introducing infrastructure that this epic deletes; estimated cost is equal to the current removal effort (~85h). A future multi-language initiative would likely benefit from starting fresh.

### Neutral

- Branch `legacy/v3` is preserved as a read-only reference for users who need non-Java support; this is a pure git-branch operation with no ongoing maintenance burden.
- Feature flags `--legacy-empty-dirs` and `--no-claude-md` exist for one release cycle (v4.0.0) and are removed in v5.0.0 — documented in the `@Deprecated` annotation and in the help text.

## Alternatives Considered

### A1. Plugin system per language (rejected)

Introduce a plugin API so that python / go / etc. can be maintained as out-of-tree extensions. **Rejected** because: (i) no user exists for any of the candidate plugins — the complexity would be for a hypothetical audience; (ii) the plugin API surface (file categorization, template engine integration, golden file regeneration, YAML parsing) would add a new dimension of test matrix; (iii) SemVer impact is the same — breaking the monorepo API is still a MAJOR release.

### A2. Gradual deprecation over 3 minor releases (rejected)

Mark non-Java paths `@Deprecated` in v3.12, disable in v3.13, remove in v3.14 or v4.0. **Rejected** because: (i) ongoing maintenance during the deprecation window exceeds the value to phantom users; (ii) each minor release still triggers the same smoke matrix of 17 profiles — the cost is incurred until actual removal; (iii) the `csharp-dotnet` leftover is itself a signal that incremental cleanup has failed.

### A3. Remove only csharp leftover, keep other languages (rejected)

Delete `csharp-dotnet` entries from `StackMapping.java`; preserve python / go / kotlin / typescript / rust as-is. **Rejected** because: (i) it fixes the symptom, not the root cause — the other 5 languages have the same "no real user" condition; (ii) the golden / smoke matrix remains bloated (3256 non-Java golden files stay); (iii) the performance target of −30% `mvn test` wall-clock is not achievable without removing non-Java profiles.

## Related ADRs

- [ADR-0048-B: CLAUDE.md Contract](ADR-0048-B-claude-md-contract.md) — companion architectural decision introduced in the same epic to resolve Bug B. ADR-0048-A provides the single-language assumption that ADR-0048-B's `ClaudeMdAssembler` relies on for simplifying the `{{LANGUAGE}}` placeholder (always "java").
- [ADR-0001: Intentional Architectural Deviations for CLI Tool](ADR-0001-intentional-architectural-deviations-for-cli-tool.md) — establishes the precedent that pragmatic simplifications of the generator's scope are acceptable when they align with actual usage.

## Story Reference

- Epic: [EPIC-0048](../plans/epic-0048/epic-0048.md)
- Spec: [spec-epic-0048.md](../plans/epic-0048/spec-epic-0048.md)
- Investigation: [story-0048-0001 investigation-report.md](../plans/epic-0048/reports/investigation-report.md)
- Inventory: [removal-inventory.md](../plans/epic-0048/reports/removal-inventory.md)
- This ADR authored under: story-0048-0002, TASK-0048-0002-001.
