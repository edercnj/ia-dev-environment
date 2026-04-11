---
status: Proposed
date: 2026-04-10
deciders:
  - Eder Celeste Nunes Junior
story-ref: "EPIC-0036"
---

# ADR-0003: Skill Taxonomy and Naming Refactor

## Status

Proposed | 2026-04-10

## Context

The `skills/` tree under `java/src/main/resources/targets/claude/` currently stores **~78 core+conditional skills and ~47 knowledge packs as flat directories** under `core/`, `conditional/`, and `knowledge-packs/`. This flat layout makes discovery and maintenance painful for new contributors — there is no visual clustering by functional domain, and several skill names are ambiguous or inconsistent with each other:

1. **Ambiguous naming in the `x-story-*` / `x-epic-*` / `x-task-*` cluster.**
   - `x-story-epic` creates an **epic** document, not a story.
   - `x-story-map` generates the **IMPLEMENTATION-MAP** of an epic, not of a story.
   - `x-epic-plan` is an **orchestrator**, not a planner (it dispatches per-story planning jobs).
   - `x-dev-implement` runs one **task** (TDD loop), not a whole dev cycle.

2. **Divergent prefixes.** Most skills use the `x-` namespace prefix (project convention, avoids collision with built-in Claude Code skills), but the 5 conditional test runners use `run-`:
   - `run-e2e`, `run-smoke-api`, `run-smoke-socket`, `run-contract-tests`, `run-perf-test`.

3. **Redundant category prefixes.** Once skills live under categorized subfolders, names like `x-dev-implement` under `core/dev/` become tautological.

4. **Hardcoded grouping in Java.** `SkillGroupRegistry.java` hardcodes 8 groups (`story`, `dev`, `review`, `testing`, `infrastructure`, `knowledge-packs`, `git-troubleshooting`, `lib`) used exclusively by `GithubSkillsAssembler` to partition skills for the GitHub Copilot output. The filesystem has no equivalent grouping, creating two sources of truth that must be kept in sync manually.

5. **Inconsistent post-fixes in the security cluster.** `x-hardening-eval` uses `-eval`, `x-runtime-protection` uses `-protection` (a noun), `x-security-secret-scan` uses `-scan` (while siblings like `x-security-sast` omit the suffix).

6. **Documented problem statement scale.** The refactor has to update approximately 133 textual references to skills across `rules/`, cross-skill invocation bodies, `CLAUDE.md`, `README.md`, and plan/review templates.

The EPIC-0033 work (see ADR-0002) established Rule 13 — Skill Invocation Protocol — which made all skill-to-skill delegation explicit. That rule is a prerequisite for the present refactor: hard-renames are only safe when every delegation is an explicit `Skill(skill: "x-foo")` call that can be rewritten mechanically.

## Decision

Establish a 10-category taxonomy for the skills source of truth, rename skills to a consistent verbal scheme, delete `SkillGroupRegistry`, and treat documentation updates as first-class deliverables. Six sub-decisions:

### D1 — SoT hierarchical, output flat

Reorganize **only** `java/src/main/resources/targets/claude/skills/` into category subfolders. `SkillsAssembler` flattens the hierarchy at assembly time. The generated output `.claude/skills/{name}/SKILL.md` remains flat.

**Rationale:** Claude Code resolves skills via the `name:` frontmatter field, not via filesystem path. A flat output preserves ~133 textual references (`skills/{name}/SKILL.md`, `skills/{name}/references/...`) and all cross-skill invocations without modification. The asymmetry SoT-hierarchical / output-flat is intentional: authoring benefits from grouping, runtime benefits from a flat namespace. This mirrors the current behavior of `GithubSkillsAssembler` in the opposite direction.

### D2 — 10-category taxonomy

| Category   | Purpose                                                                                    |
|------------|--------------------------------------------------------------------------------------------|
| `plan`     | Produces design artifacts (architecture, epics, stories, tasks, threat models, ADRs)      |
| `dev`      | Executes implementation and setup (task/story/epic implement, env setup, CI gen, spec drift) |
| `test`     | Plans, writes, and executes tests (TDD, test plan, run, e2e, smoke, contract, perf)      |
| `review`   | Human/agent code review (orchestrator + specialists + code audit)                         |
| `security` | Scans, posture, and dashboards (SAST, DAST, OWASP, dependency, hardening, runtime)        |
| `code`     | Format and lint (pre-commit chain steps)                                                   |
| `git`      | Low-level git operations (commit, push, worktree)                                          |
| `pr`       | Pull request lifecycle (create, fix comments, fix epic comments)                           |
| `ops`      | Runtime, release, docs, troubleshooting (incidents, profile, release, changelog, doc-gen) |
| `jira`     | Jira integration (create epic, create stories)                                             |

**Edge cases (explicit):**
- `x-review-security` → **review** (intention: review code; `security/` is for automated scans and posture evaluation).
- `x-code-audit` → **review** (it is a mega-review orchestrator, not a formatter).
- `x-threat-model` → **plan** (produces a design artifact, not a scan).
- `x-perf-profile` → **ops** (runtime profiler execution).
- Unified `run-*` runners → **test**.

**Out of scope:**
- `core/lib/` remains unchanged (existing convention for reusable, non-invocable components).
- `knowledge-packs/` remains flat (it is itself a category).

### D3 — Global rename to verbal scheme

The `x-` namespace prefix is kept (it avoids collision with built-in Claude Code skills). The `run-` prefix is killed. Naming follows `x-{subject}-{action}` with reserved action suffixes: `-create`, `-plan`, `-decompose`, `-map`, `-implement`, `-orchestrate`, `-update`, `-generate`, `-eval`, `-scan`.

**Primary cluster (epic/story/task/dev/arch/adr):**

| Current                     | New                    | Why                                                                   |
|-----------------------------|------------------------|------------------------------------------------------------------------|
| `x-story-epic`              | `x-epic-create`        | Explicit verb; creates the epic document                              |
| `x-story-epic-full`         | `x-epic-decompose`     | Distinguished from `-create`: decomposes an epic into stories + map   |
| `x-story-create`            | `x-story-create`       | Keep — already clear                                                  |
| `x-story-plan`              | `x-story-plan`         | Keep — multi-agent planning of a single story                         |
| `x-story-map`               | `x-epic-map`           | Produces an epic-level IMPLEMENTATION-MAP; current name is misleading |
| `x-task-plan`               | `x-task-plan`          | Keep                                                                  |
| `x-epic-plan`               | `x-epic-orchestrate`   | It is a multi-story orchestrator, not a planner                       |
| `x-dev-implement`           | `x-task-implement`     | Runs a single task (TDD loop)                                         |
| `x-dev-story-implement`     | `x-story-implement`    | Removes redundant `dev-` prefix (category is already `dev`)          |
| `x-dev-epic-implement`      | `x-epic-implement`     | Same                                                                  |
| `x-dev-architecture-plan`   | `x-arch-plan`          | Shorter, unambiguous                                                  |
| `x-dev-arch-update`         | `x-arch-update`        | Parallel to `x-arch-plan`                                             |
| `x-dev-adr-automation`      | `x-adr-generate`       | Verb + artifact                                                       |

The resulting **epic → story → task** axis is crisp:
- **Create document:** `x-epic-create`, `x-story-create`.
- **Plan / decompose:** `x-epic-decompose`, `x-epic-map`, `x-story-plan`, `x-task-plan`.
- **Orchestrate / execute:** `x-epic-orchestrate`, `x-story-implement`, `x-task-implement`, `x-epic-implement`.

**`run-*` prefix unification:**

| Current                | New                    |
|------------------------|------------------------|
| `run-e2e`              | `x-test-e2e`           |
| `run-smoke-api`        | `x-test-smoke-api`     |
| `run-smoke-socket`     | `x-test-smoke-socket`  |
| `run-contract-tests`   | `x-test-contract`      |
| `run-perf-test`        | `x-test-perf`          |

**Pointwise simplifications:**

| Current                      | New                     | Why                                                  |
|------------------------------|-------------------------|------------------------------------------------------|
| `x-pr-fix-comments`          | `x-pr-fix`              | "comments" is redundant in the `pr` category        |
| `x-pr-fix-epic-comments`     | `x-pr-fix-epic`         | Same                                                 |
| `x-runtime-protection`       | `x-runtime-eval`        | Symmetry with `x-hardening-eval`                     |
| `x-security-secret-scan`     | `x-security-secrets`    | Consistency with other `x-security-*` siblings       |

**Kept as is** (already clear and internally consistent within their cluster):
- `x-review`, `x-review-pr`, `x-review-qa`, `x-review-perf`, `x-review-api`, `x-review-db`, `x-review-devops`, `x-review-events`, `x-review-obs`, `x-review-security`, `x-review-graphql`, `x-review-grpc`, `x-review-gateway`, `x-review-compliance`, `x-review-data-modeling`
- `x-code-audit`, `x-code-format`, `x-code-lint`
- `x-git-commit`, `x-git-push`, `x-git-worktree`
- `x-pr-create`
- `x-security-dashboard`, `x-security-pipeline`, `x-security-sast`, `x-security-dast`, `x-security-container`, `x-security-sonar`, `x-security-pentest`, `x-security-infra`
- `x-owasp-scan`, `x-hardening-eval`, `x-threat-model`, `x-dependency-audit`, `x-supply-chain-audit`
- `x-ops-troubleshoot`, `x-ops-incident`
- `x-release`, `x-release-changelog`
- `x-doc-generate`, `x-perf-profile`, `x-spec-drift`, `x-ci-generate`, `x-setup-env`, `x-setup-stack`, `x-mcp-recommend`, `x-obs-instrument`, `x-test-plan`, `x-test-tdd`, `x-test-run`, `x-test-contract-lint`
- `x-jira-create-epic`, `x-jira-create-stories`

### D4 — Hard rename, no aliases

Claude Code does not support skill aliases natively. Introducing an alias layer would require either duplicate SKILL.md stubs or custom assembler logic — more code to maintain than the coordinated rename is worth. **Decision:** rename atomically per cluster inside a single PR. A CI guard script breaks the build if any old name is found in source files. Release notes document the mapping.

### D5 — Delete `SkillGroupRegistry`

With the filesystem categorized, the hardcoded Java grouping is guaranteed to drift. `GithubSkillsAssembler` will derive groups by walking `targets/github-copilot/skills/*/` (or from the `SkillsAssembler` output plus a category→github-group map if the mapping is not 1:1). `SkillGroupRegistry.java` is deleted.

### D6 — Documentation updates are first-class deliverables

Every story in EPIC-0036 includes an explicit "Docs obligatórias" subsection enumerating which documentation surfaces must be updated. Surfaces include: `CLAUDE.md`, `README.md`, `adr/`, rules under `targets/claude/rules/`, cross-skill references inside SKILL.md bodies, `_README-TEMPLATES.md`, plan/review templates (`_TEMPLATE-*.md`), GitHub Copilot instructions and prompts, Codex templates, Java test files that hardcode skill names, and release notes. "Update docs later" is not permitted.

## Consequences

### Positive

- **Navigable source of truth.** New contributors can locate a skill by intent (`plan/`, `dev/`, `review/`, ...) rather than scanning a flat ~78-entry directory listing.
- **Zero blast radius in consumer documentation.** Because the output remains flat (D1), the ~133 existing references to `skills/{name}/SKILL.md` continue to resolve without change. Only the 19 renamed skills require textual updates.
- **Unambiguous naming.** The `epic → story → task` axis becomes clear: `-create` produces a document, `-plan/-decompose/-map` produces a breakdown, `-implement/-orchestrate` executes code. The `x-story-map` → `x-epic-map` rename alone eliminates a recurring source of user confusion.
- **Single source of truth for grouping.** `SkillGroupRegistry` is deleted; the filesystem is authoritative. No more hardcoded Java lists to keep in sync with the actual skill set.
- **Unified invocation prefix.** Killing `run-*` in favor of `x-test-*` means users only ever type `/x-...` — no mental context switch between `run-` and `x-` skills.
- **Documentation debt is addressed in-epic, not deferred.** D6 makes updates to rules, templates, and onboarding docs part of every story's Definition of Done.

### Negative

- **Breaking change for external consumers.** Any CI/CD pipeline, script, or contributor muscle memory that invokes `/x-story-epic`, `/x-dev-implement`, `/run-e2e`, etc. by name will break the moment the renames land. Mitigated by: (a) release notes listing every rename; (b) CI guard script that prevents regressions; (c) MINOR version bump with explicit "breaking naming change" callout.
- **Golden test regeneration in stories 03, 04, and 05.** Each rename cluster forces a full regen via `GoldenFileRegenerator`. Reviewers must inspect the diff for structural sanity rather than line-by-line.
- **Asymmetry SoT-hierarchical / output-flat is a cognitive cost.** A reader of the source tree may expect `.claude/skills/` to mirror `targets/claude/skills/core/`. Mitigated by this ADR + a load-bearing comment at the top of `SkillsAssembler.selectCoreSkills()`.
- **Temporary sync burden during execution.** Between merging STORY-04 and STORY-05, the codebase contains a mix of new and old naming styles. Each story's PR is required to be self-consistent (all renames in a single atomic commit inside that story's scope).

### Neutral

- **Knowledge packs are not reorganized.** They remain flat under `knowledge-packs/`. They are not invocable and do not contribute to the flat-listing pain that motivates this refactor.
- **`core/lib/` is untouched.** Its existing nested structure is preserved because lib components are internal utilities with a different lifecycle than user-invocable skills.
- **The `x-` prefix survives.** The project namespace is intentionally kept; dropping it would be an independent, much larger change against built-in Claude Code skill collision guarantees.
- **Rule 13 (ADR-0002) continues to govern delegation syntax.** This refactor changes skill *names*, not the *protocol* by which skills invoke each other. Every `Skill(skill: "x-old-name")` call in a SKILL.md body becomes `Skill(skill: "x-new-name")` — the call shape is unchanged.

## Related ADRs

- **ADR-0002 — Skill Delegation Protocol (Rule 13).** Rule 13 made every skill-to-skill invocation an explicit `Skill(...)` tool call. That explicitness is a prerequisite for the present hard-rename: mechanical find-replace of skill names is only safe because there are no implicit bare-slash references left in delegation contexts.
- **ADR-0001 — Intentional Architectural Deviations for CLI Tool.** This refactor operates at the template layer (`skills/core/**`) and does not affect Java hexagonal layering; ADR-0001 remains orthogonal.

## Story Reference

- **EPIC-0036** — Skill Taxonomy and Naming Refactor
  - **STORY-0036-0001** — ADR + taxonomy agreed (this document)
  - **STORY-0036-0002** — Physical reorganization of the source of truth (no renames)
  - **STORY-0036-0003** — Delete `SkillGroupRegistry`; derive groups from filesystem
  - **STORY-0036-0004** — Rename the primary cluster (`x-story-*` / `x-epic-*` / `x-task-*` / `x-dev-*`)
  - **STORY-0036-0005** — Global rename remainder (`run-*` → `x-test-*`, `x-pr-fix-*`, security symmetry)
  - **STORY-0036-0006** — Guard script + release notes + final documentation pass
