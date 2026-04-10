# EPIC-0036 — Skill Renames Staging Document

> **Status:** Temporary staging document. This file is the working checklist used by STORY-0036-0002 through STORY-0036-0006. It will be archived (or referenced as an appendix of ADR-0003) once EPIC-0036 completes.
>
> **Authoritative source:** [ADR-0003 — Skill Taxonomy and Naming Refactor](../../adr/ADR-0003-skill-taxonomy-and-naming.md).

---

## 1. Category assignment (D2)

Ten categories applied to `java/src/main/resources/targets/claude/skills/core/**` and `conditional/**`. `core/lib/` is untouched. `knowledge-packs/` remains flat (not categorized).

| Category    | Skills (by current name)                                                                                                                                                                                                                      |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `plan`      | `x-story-epic`, `x-story-create`, `x-story-epic-full`, `x-story-plan`, `x-story-map`, `x-task-plan`, `x-epic-plan`, `x-dev-architecture-plan`, `x-dev-arch-update`, `x-dev-adr-automation`, `x-threat-model`                                 |
| `dev`       | `x-dev-implement`, `x-dev-story-implement`, `x-dev-epic-implement`, `x-setup-env`, `x-setup-stack` (conditional), `x-mcp-recommend`, `x-ci-generate`, `x-spec-drift`                                                                          |
| `test`      | `x-test-plan`, `x-test-tdd`, `x-test-run`, `x-test-contract-lint` (conditional), `run-e2e` (conditional), `run-smoke-api` (conditional), `run-smoke-socket` (conditional), `run-contract-tests` (conditional), `run-perf-test` (conditional) |
| `review`    | `x-review`, `x-review-pr`, `x-review-qa`, `x-review-perf`, `x-review-api`, `x-review-db`, `x-review-devops`, `x-review-events`, `x-review-obs`, `x-review-security`, `x-review-graphql` (cond), `x-review-grpc` (cond), `x-review-gateway` (cond), `x-review-compliance` (cond), `x-review-data-modeling` (cond), `x-code-audit` |
| `security`  | `x-security-dashboard`, `x-security-pipeline`, `x-security-sast` (cond), `x-security-dast` (cond), `x-security-container` (cond), `x-security-secret-scan` (cond), `x-security-sonar` (cond), `x-security-pentest` (cond), `x-security-infra` (cond), `x-owasp-scan`, `x-hardening-eval`, `x-runtime-protection`, `x-dependency-audit`, `x-supply-chain-audit` |
| `code`      | `x-code-format`, `x-code-lint`                                                                                                                                                                                                                |
| `git`       | `x-git-commit`, `x-git-push`, `x-git-worktree`                                                                                                                                                                                                 |
| `pr`        | `x-pr-create`, `x-pr-fix-comments`, `x-pr-fix-epic-comments`                                                                                                                                                                                   |
| `ops`       | `x-ops-troubleshoot`, `x-ops-incident`, `x-perf-profile`, `x-release`, `x-release-changelog`, `x-doc-generate`, `x-obs-instrument` (conditional)                                                                                               |
| `jira`      | `x-jira-create-epic`, `x-jira-create-stories`                                                                                                                                                                                                  |

**Unchanged (not in any category):**
- `core/lib/` — `x-lib-task-decomposer`, `x-lib-audit-rules`, `x-lib-group-verifier` (internal utilities)
- `knowledge-packs/` — all packs remain flat

---

## 2. Rename table (D3)

### 2.1 Primary cluster — STORY-0036-0004

These are the renames that eliminate the `x-story-*` / `x-epic-*` / `x-task-*` / `x-dev-*` ambiguity.

| # | Current                      | New                    | Category  | Rationale                                                          |
|---|------------------------------|------------------------|-----------|--------------------------------------------------------------------|
| 1 | `x-story-epic`               | `x-epic-create`        | `plan`    | Creates the epic document (verb explicit)                         |
| 2 | `x-story-epic-full`          | `x-epic-decompose`     | `plan`    | Decomposes an epic into stories + implementation map             |
| 3 | `x-story-map`                | `x-epic-map`           | `plan`    | Produces an **epic-level** IMPLEMENTATION-MAP                     |
| 4 | `x-epic-plan`                | `x-epic-orchestrate`   | `plan`    | Multi-story orchestrator, not a planner                           |
| 5 | `x-dev-implement`            | `x-task-implement`     | `dev`     | Runs a single task (TDD loop)                                     |
| 6 | `x-dev-story-implement`      | `x-story-implement`    | `dev`     | Removes redundant `dev-` prefix                                   |
| 7 | `x-dev-epic-implement`       | `x-epic-implement`     | `dev`     | Removes redundant `dev-` prefix                                   |
| 8 | `x-dev-architecture-plan`    | `x-arch-plan`          | `plan`    | Shorter, unambiguous                                              |
| 9 | `x-dev-arch-update`          | `x-arch-update`        | `plan`    | Parallel with `x-arch-plan`                                       |
| 10| `x-dev-adr-automation`       | `x-adr-generate`       | `plan`    | Verb + artifact                                                   |

**Kept as-is in this cluster** (already clear, no rename):
- `x-story-create`, `x-story-plan`, `x-task-plan`, `x-threat-model`

### 2.2 `run-*` unification — STORY-0036-0005

| # | Current                  | New                     | Category |
|---|--------------------------|-------------------------|----------|
| 11| `run-e2e`                | `x-test-e2e`            | `test`   |
| 12| `run-smoke-api`          | `x-test-smoke-api`      | `test`   |
| 13| `run-smoke-socket`       | `x-test-smoke-socket`   | `test`   |
| 14| `run-contract-tests`     | `x-test-contract`       | `test`   |
| 15| `run-perf-test`          | `x-test-perf`           | `test`   |

### 2.3 Pointwise simplifications — STORY-0036-0005

| # | Current                      | New                      | Category   | Rationale                                     |
|---|------------------------------|--------------------------|------------|-----------------------------------------------|
| 16| `x-pr-fix-comments`          | `x-pr-fix`               | `pr`       | "comments" is redundant in the `pr` category |
| 17| `x-pr-fix-epic-comments`     | `x-pr-fix-epic`          | `pr`       | Same                                          |
| 18| `x-runtime-protection`       | `x-runtime-eval`         | `security` | Symmetry with `x-hardening-eval`              |
| 19| `x-security-secret-scan`     | `x-security-secrets`     | `security` | Consistency with `x-security-*` siblings      |

**Total renames:** 19 (10 in STORY-04, 9 in STORY-05).

---

## 3. File-update checklist (per rename story)

For each rename in STORY-04 and STORY-05, the following surfaces must be updated in a single atomic PR. This list is copied into each story's PR description as a checklist.

### 3.1 Source-of-truth skill directory
- [ ] Rename directory: `targets/claude/skills/{category}/{old-name}/` → `targets/claude/skills/{category}/{new-name}/`
- [ ] Update `name:` field in `SKILL.md` frontmatter
- [ ] Update `name:` field in `README.md` if present (metadata table)
- [ ] Update intra-skill references in the skill's own body text

### 3.2 Cross-skill references
- [ ] `grep -rn "{old-name}" java/src/main/resources/targets/claude/skills/` — update every hit
- [ ] Verify `Skill(skill: "{old-name}", ...)` calls become `Skill(skill: "{new-name}", ...)`
- [ ] Verify any references in `references/*.md` inside other skill directories

### 3.3 Rules
- [ ] `java/src/main/resources/targets/claude/rules/13-skill-invocation-protocol.md` — Rule 13 examples may cite skill names
- [ ] All other `targets/claude/rules/*.md` — grep and update

### 3.4 Templates and internal docs
- [ ] `java/src/main/resources/targets/claude/skills/_README-TEMPLATES.md`
- [ ] `_TEMPLATE-IMPLEMENTATION-PLAN.md`
- [ ] `_TEMPLATE-TEST-PLAN.md`
- [ ] `_TEMPLATE-ARCHITECTURE-PLAN.md`
- [ ] `_TEMPLATE-TASK-BREAKDOWN.md`
- [ ] `_TEMPLATE-SECURITY-ASSESSMENT.md`
- [ ] `_TEMPLATE-COMPLIANCE-ASSESSMENT.md`
- [ ] `_TEMPLATE-SPECIALIST-REVIEW.md`
- [ ] `_TEMPLATE-TECH-LEAD-REVIEW.md`
- [ ] `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`
- [ ] `_TEMPLATE-REVIEW-REMEDIATION.md`
- [ ] `_TEMPLATE-EPIC-EXECUTION-PLAN.md`
- [ ] `_TEMPLATE-PHASE-COMPLETION-REPORT.md`

### 3.5 Other targets
- [ ] `java/src/main/resources/targets/github-copilot/**` — GitHub Copilot instructions, skills, prompts
- [ ] `java/src/main/resources/targets/codex/templates/*.njk` — Codex AGENTS.md templates

### 3.6 Project-level documentation
- [ ] `CLAUDE.md` (project root)
- [ ] `README.md` (project root) — "Generated Skills Reference" section
- [ ] `CHANGELOG.md` — add entry under Unreleased
- [ ] `adr/` — any ADR body that cites renamed skills gets an inline rename note or footnote
- [ ] `adr/README.md` — ensure index is current

### 3.7 Tests
- [ ] `java/src/test/java/**/*SkillsTest*.java` — hardcoded names
- [ ] `java/src/test/java/**/*AssemblerTest*.java` — hardcoded paths and names
- [ ] `java/src/test/java/**/*GoldenTest*.java` — expected path assertions
- [ ] `java/src/test/resources/golden/**` — regenerate via `GoldenFileRegenerator` after running `mvn process-resources`

### 3.8 Verification after each rename PR
- [ ] `cd java && mvn clean verify` — green
- [ ] Grep sanity: `grep -rn "{old-name}" .` returns only permitted hits (this staging doc, `CHANGELOG.md`, release notes, ADR-0003)
- [ ] Smoke test: manually invoke one of the renamed skills in a test project to confirm `name:` resolves

---

## 4. Guard script scope — STORY-0036-0006

The CI guard script must fail the build if any of the following strings appear outside permitted locations:

**Forbidden strings (all 19 old names):**
```
x-story-epic
x-story-epic-full
x-story-map
x-epic-plan
x-dev-implement
x-dev-story-implement
x-dev-epic-implement
x-dev-architecture-plan
x-dev-arch-update
x-dev-adr-automation
run-e2e
run-smoke-api
run-smoke-socket
run-contract-tests
run-perf-test
x-pr-fix-comments
x-pr-fix-epic-comments
x-runtime-protection
x-security-secret-scan
```

**Permitted locations (exclusions):**
- `plans/epic-0036/skill-renames.md` (this file)
- `adr/ADR-0003-skill-taxonomy-and-naming.md` (historical record)
- `CHANGELOG.md` (migration history)
- `docs/release-notes/**` (if created)
- The guard script itself
- `.git/`, `java/target/`, `node_modules/` and other build/ignore directories
