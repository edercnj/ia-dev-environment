---
status: Accepted
date: 2026-04-22
deciders:
  - Eder Celeste Nunes Junior
story-ref: "story-0048-0002"
---

# ADR-0048-B: `CLAUDE.md` Contract (Root File via Dedicated Assembler)

## Status

Accepted | 2026-04-22

## Context

Story-0048-0001 confirmed **Bug B** empirically: the `ia-dev-env generate` CLI does NOT produce a `CLAUDE.md` file at the root of generated Java projects. The `repro-bug-b.sh` script exits with code 1 and the message `"Bug B confirmed: CLAUDE.md not generated"` against develop commit `d8f7ff0c2` (see [investigation-report.md §4](../plans/epic-0048/reports/investigation-report.md)).

Partial machinery already exists, but is incomplete:

- `java/src/main/java/dev/iadev/cli/FileCategorizer.java:88` recognizes `CLAUDE.md` as a root file (returns `true` from `isRootFile("CLAUDE.md")`).
- **No `ClaudeMdAssembler` exists** in `java/src/main/java/dev/iadev/application/assembler/`.
- **No template exists** at `java/src/main/resources/shared/templates/CLAUDE.md`.
- `AssemblerFactory` currently registers 22 assemblers across 3 target groups (`ROOT`, `CLAUDE`); **none produces `CLAUDE.md`**.

The `CLAUDE.md` file is the auto-loaded executive summary contract for Claude-Code projects — it appears in every conversation's system prompt (see the root `CLAUDE.md` of this repository as an example). A Java generator producing Claude-Code artifacts without producing `CLAUDE.md` violates the contract.

Bug B will be fixed in story-0048-0011 (template + assembler registration). This ADR defines the contract that the fix must satisfy.

## Decision

Introduce a new `ClaudeMdAssembler` class implementing the `Assembler` interface with the following contract:

1. **Single Responsibility.** The assembler produces EXACTLY one output: `CLAUDE.md` at the root of the generated project. It does **not** handle `.claude/` subdirectory content (that remains the responsibility of the existing `CLAUDE`-target assemblers) and does not handle `README.md` (that remains `ReadmeAssembler`).

2. **Target and platforms.**
   - `target == AssemblerTarget.ROOT` (writes to output root, alongside `README.md`, `Dockerfile`, `docker-compose.yml`).
   - `platforms == { Platform.CLAUDE_CODE }` — only generated when Claude-Code output is requested (default via `--platform all` includes it).

3. **Template source.**
   - Template path: `java/src/main/resources/shared/templates/CLAUDE.md` (NEW — created in story-0048-0010).
   - Template engine: **Pebble** (already in use by `ReadmeAssembler`, `DockerAssembler`, et al.).
   - Resolution: standard `TemplateEngine` invocation, no template inheritance, no custom filters.

4. **Placeholders — minimum mandatory set (8).**
   Each placeholder is substituted from the `ResolvedStack` + `ProjectConfig` domain objects. None of these have sensible defaults that would produce useful content; therefore all 8 are mandatory inputs to the assembler.

   | Placeholder | Source | Example |
   | :--- | :--- | :--- |
   | `{{PROJECT_NAME}}` | `ProjectConfig.projectName` | `my-java-cli` |
   | `{{LANGUAGE}}` | Always `"java"` (ADR-0048-A) | `java` |
   | `{{FRAMEWORK}}` | `ResolvedStack.framework` | `spring-boot`, `quarkus` |
   | `{{ARCHITECTURE}}` | `ResolvedStack.architectureStyle` | `hexagonal`, `layered` |
   | `{{DATABASES}}` | `ResolvedStack.databases` joined by `, ` (empty → `none`) | `postgres, redis` |
   | `{{INTERFACE_TYPES}}` | `ResolvedStack.interfaceTypes` joined by `, ` | `rest, event-consumer` |
   | `{{BUILD_COMMAND}}` | From `LANGUAGE_COMMANDS[java-{buildTool}].build` | `./mvnw package -DskipTests` |
   | `{{TEST_COMMAND}}` | From `LANGUAGE_COMMANDS[java-{buildTool}].test` | `./mvnw verify` |

   Additional placeholders are permitted in the template but must have sensible defaults; any new mandatory placeholder requires a **minor-version** bump and an ADR amendment.

5. **Registration order.**
   `AssemblerFactory` adds `ClaudeMdAssembler` to the **last group** returned by `buildRootDocAssemblers()`. Ordering within `ROOT`-target assemblers follows the existing convention:
   - `README.md` first (navigation entry point)
   - `Dockerfile` / `docker-compose.yml` (infrastructure)
   - `CONSTITUTION.md` (existing root doc)
   - `CLAUDE.md` **last** (consumes information from the others; depends on no sibling output)

6. **Overwrite semantics.**
   `CLAUDE.md` is always **regenerated** on every `ia-dev-env generate` invocation. User edits to the file are NOT preserved. This mirrors the behavior of `README.md` and `Dockerfile`. The limitation is documented in the generated `CLAUDE.md` itself (e.g., in a header comment) so that users understand the file is generator-owned.

   The existing `--overwrite-constitution` flag (which explicitly protects `CONSTITUTION.md` from regeneration unless the flag is set) is **not applied** to `CLAUDE.md` — the generator owns this file unconditionally.

7. **Feature flag `--no-claude-md`.**
   Introduced in v4.0.0 for users migrating from v3.x who do not want `CLAUDE.md` regenerated yet. When the flag is present, `ClaudeMdAssembler` is skipped entirely (no output file created, existing file preserved if present). Flag is **removed in v5.0.0** — parallel to the treatment of `--legacy-empty-dirs` per ADR-0048-A.

8. **Interaction with `FileCategorizer.isRootFile`.**
   `isRootFile("CLAUDE.md")` already returns `true`. ADR-0048-B confirms this behavior is correct — no change needed to `FileCategorizer`. The existing declaration was incomplete (recognized without producer); the addition of `ClaudeMdAssembler` closes the loop.

## Consequences

### Positive

- **Bug B closed.** Every generated Java project contains a `CLAUDE.md` at the root (≥ 100 bytes, per `repro-bug-b.sh`), satisfying the Claude-Code contract.
- **Single-responsibility principle preserved.** `ClaudeMdAssembler` does one thing; no mixing with `ReadmeAssembler`.
- **9 Java golden files gain a new `CLAUDE.md`.** Regenerated in story-0048-0011 as a controlled, approved diff in the PR body.
- **Parameterized acceptance test.** `ClaudeMdRootPresenceTest` can iterate over all 9 Java profiles with the same invariant (file exists, ≥ 100 bytes, contains `{{PROJECT_NAME}}` substituted value).

### Negative

- **User edits not preserved.** Users who hand-edit `CLAUDE.md` lose changes on regeneration. Feature flag `--no-claude-md` (v4.0.0-only) is the explicit escape hatch for this case. Long-term this is the intended behavior — `CLAUDE.md` is generator-owned, like `Dockerfile`.
- **Template drift risk.** If the template `shared/templates/CLAUDE.md` is edited without updating goldens, `GoldenFileTest` fails. This is the standard cost of templated output and is offset by byte-for-byte parity guarantees.

### Neutral

- **`ReadmeAssembler` remains responsible for `.claude/README.md`** (inside the `.claude/` subdirectory). No overlap with `ClaudeMdAssembler` (which writes to root).
- **No migration required for existing users.** v3.x users who upgrade to v4.0.0 will see a new `CLAUDE.md` appear; if they do not want it, `--no-claude-md` skips generation.

## Alternatives Considered

### B1. Extend `ReadmeAssembler` to also produce `CLAUDE.md` (rejected)

`ReadmeAssembler` would gain a second output under the same invocation. **Rejected** because: (i) violates SRP — one assembler, one output; (ii) complicates unit tests (fixtures must set up two distinct expectations); (iii) blurs the boundary between "project navigation doc" (`README.md`) and "AI executive summary" (`CLAUDE.md`) — they are conceptually different artifacts.

### B2. Produce `CLAUDE.md` via declarative YAML config (no template engine) (rejected)

Define a `claudeMd:` section in `setup-config.*.yaml` with static fields; `ClaudeMdAssembler` reads and writes directly. **Rejected** because: (i) placeholders like `{{FRAMEWORK}}` and `{{BUILD_COMMAND}}` are per-profile dynamic values derived from `ResolvedStack` — they cannot be static without duplicating stack resolution logic in every YAML; (ii) Pebble is already the template engine for other root docs (`README.md`, `Dockerfile`); (iii) the Pebble template approach is the path of least architectural resistance.

### B3. Static `CLAUDE.md` in every generated project (rejected)

Ship a single canonical `CLAUDE.md` shared across all profiles; no per-profile customization. **Rejected** because: (i) the placeholders `{{FRAMEWORK}}`, `{{DATABASES}}`, `{{INTERFACE_TYPES}}` provide genuine value for Claude's contextual understanding — omitting them degrades the quality of generated-project agent interactions; (ii) the root-level `CLAUDE.md` of this repo itself is heavily customized per-project (layered architecture, hexagonal, etc.) — a static template would not match the pattern users expect.

## Related ADRs

- [ADR-0048-A: Java-Only Scope](ADR-0048-java-only-scope.md) — companion decision that fixes `{{LANGUAGE}}` to `"java"` and removes one layer of complexity from the placeholder resolver.
- [ADR-0009: Wide Records Bound to External Schemas](ADR-0009-wide-records-bound-to-external-schemas.md) — precedent for how domain objects (`ResolvedStack`) map into generated artifacts.

## Story Reference

- Epic: [EPIC-0048](../plans/epic-0048/epic-0048.md)
- Spec: [spec-epic-0048.md](../plans/epic-0048/spec-epic-0048.md) — §2 decision table specifies "single-responsibility, novo assembler dedicado".
- Bug evidence: [repro-bug-b.sh](../plans/epic-0048/reports/repro-bug-b.sh) returns exit 1 on current develop.
- Fix stories: [story-0048-0010](../plans/epic-0048/story-0048-0010.md) (template Pebble), [story-0048-0011](../plans/epic-0048/story-0048-0011.md) (assembler + registration).
- This ADR authored under: story-0048-0002, TASK-0048-0002-002.
