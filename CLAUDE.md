# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **ia-dev-environment** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** Both `.claude/` and `.github/` directories are **generated outputs** produced by `ia-dev-env`.
> The generator writes `.github/` artifacts under `github/` in the output directory; rename to `.github/` when placing in a project root.
> Do not edit them manually -- regenerate instead.

> **CRITICAL — Source of Truth:**
> The source of truth for skills, knowledge packs, agents, rules, and templates is `java/src/main/resources/targets/`.
> The directories `.claude/`, `.github/`, `.codex/`, `.agents/`, and `src/test/resources/golden/` are generated outputs — NEVER edit them directly.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

> **In progress — EPIC-0036 (Skill Taxonomy Refactor).**
> The source of truth for skills under `java/src/main/resources/targets/claude/skills/` has been reorganized into 10 category subfolders (`plan/`, `dev/`, `test/`, `review/`, `security/`, `code/`, `git/`, `pr/`, `ops/`, `jira/`), and ~19 skills are being renamed to a consistent `x-{subject}-{action}` scheme. The generated output `.claude/skills/` remains **flat** — user-facing invocation paths are preserved.
> - Decision record: [`adr/ADR-0003-skill-taxonomy-and-naming.md`](adr/ADR-0003-skill-taxonomy-and-naming.md)
> - Rename staging checklist: [`plans/epic-0036/skill-renames.md`](plans/epic-0036/skill-renames.md)
> - Primary cluster renames landed (STORY-0036-0004): `x-story-epic`→`x-epic-create`, `x-story-epic-full`→`x-epic-decompose`, `x-story-map`→`x-epic-map`, `x-epic-plan`→`x-epic-orchestrate`, `x-dev-implement`→`x-task-implement`, `x-dev-story-implement`→`x-story-implement`, `x-dev-epic-implement`→`x-epic-implement`, `x-dev-architecture-plan`→`x-arch-plan`, `x-dev-arch-update`→`x-arch-update`, `x-dev-adr-automation`→`x-adr-generate`.
> - Still pending (STORY-0036-0005): `run-*` → `x-test-*`, `x-pr-fix-comments`→`x-pr-fix`, `x-pr-fix-epic-comments`→`x-pr-fix-epic`, `x-runtime-protection`→`x-runtime-eval`, `x-security-secret-scan`→`x-security-secrets`.

## Structure

### .claude/ (Claude Code)

```
CLAUDE.md                   <-- Executive summary (project root, loaded automatically)
.claude/
|-- README.md               <-- You are here
|-- settings.json           <-- Shared settings (committed to git)
|-- settings.local.json     <-- Local overrides (gitignored)
|-- hooks/                  <-- Automations (post-compile, etc.)
|-- rules/                  <-- Project rules (loaded into system prompt)
|-- skills/                 <-- Skills invocable via /command
|   +-- {knowledge-packs}/  <-- Knowledge packs (not invocable, referenced internally)
+-- agents/                 <-- AI personas (used by skills and lifecycle)
```

### .github/ (GitHub Copilot)

```
.github/
|-- copilot-instructions.md     <-- Global instructions (loaded in every Copilot session)
|-- copilot-mcp.json            <-- MCP server configuration for Copilot
|-- instructions/               <-- Contextual instructions (*.instructions.md)
|-- skills/                     <-- Reusable skills (*/SKILL.md)
|-- agents/                     <-- Agent definitions (*.agent.md)
|-- prompts/                    <-- Prompt templates (*.prompt.md)
+-- hooks/                      <-- Event hooks (*.json)
```

### .claude/ <-> .github/ <-> .codex/ Mapping

| .claude/ | .github/ | .codex/ | Notes |
|----------|----------|---------|-------|
| Rules (`rules/*.md`) | Instructions (`instructions/*.instructions.md`) | Sections in `AGENTS.md` | Rules -> consolidated sections |
| Skills (`skills/*/SKILL.md`) | Skills (`skills/*/SKILL.md`) | Skills (`.agents/skills/` + `.codex/skills/`) | Dual output with identical content |
| Agents (`agents/*.md`) | Agents (`agents/*.agent.md`) | Sections (`[agents.*]`) in `config.toml` | Agents represented as TOML sections |
| Hooks (`hooks/`) | Hooks (`hooks/*.json`) | Reference in `AGENTS.md` | Hooks influence approval_policy |
| Settings (`settings*.json`) | N/A | `.codex/config.toml` + `.codex/requirements.toml` | Runtime and enforced policies |
| N/A | N/A | `AGENTS.md` + `AGENTS.override.md` (root) | Base instructions + local override |
| N/A | Prompts (`prompts/*.prompt.md`) | N/A | GitHub Copilot prompt templates |
| N/A | MCP (`copilot-mcp.json`) | N/A | GitHub Copilot MCP server configuration |
| N/A | Global instructions (`copilot-instructions.md`) | N/A | Loaded in every Copilot session |

**Total .github/ artifacts: 52**

> Generated only when the corresponding platform is selected via `--platform`.

### settings.json vs settings.local.json

- **`settings.json`**: Team settings (permissions, hooks). Committed to git.
- **`settings.local.json`**: Local overrides. In `.gitignore`. Overrides `settings.json`.

---

## Rules

Rules are loaded automatically into the system prompt of EVERY conversation.
They define mandatory standards that Claude MUST follow when generating code.

| # | File | Scope |
|---|------|-------|
| 01 | `01-project-identity.md` | project identity |
| 02 | `02-domain.md` | domain |
| 03 | `03-coding-standards.md` | coding standards |
| 04 | `04-architecture-summary.md` | architecture summary |
| 05 | `05-quality-gates.md` | quality gates |
| 06 | `06-security-baseline.md` | security baseline |
| 07 | `07-operations-baseline.md` | operations baseline |
| 08 | `08-release-process.md` | release process |
| 09 | `09-branching-model.md` | branching model (Git Flow) |
| 13 | `13-skill-invocation-protocol.md` | skill invocation protocol (delegation syntax) |

**Total: 10 rules** (gaps at 10, 11, 12 reserved for conditional rules: `10-anti-patterns.*`, `11-security-pci`, `12-security-anti-patterns`)

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

| Skill | Path | Description |
|-------|------|-------------|
| **patterns** | `/patterns` |  |
| **run-e2e** | `/run-e2e` | Skill: End-to-End Tests — Runs integration tests that validate the complete flow from request through all application layers to response, using a real database. |
| **x-epic-implement** | `/x-epic-implement` | Orchestrates epic execution: parses implementation map, dispatches stories via subagents, manages checkpoints, integrity gates, retry/block propagation, resume, partial execution, dry-run, and progress reporting. |
| **x-task-implement** | `/x-task-implement` | Implements a feature/story following project conventions. Delegates preparation to a subagent that reads architecture and coding KPs, then implements layer-by-layer with intermediate compilation checks. |
| **x-story-implement** | `/x-story-implement` | Orchestrates the complete feature implementation cycle: branch creation, planning, task decomposition, implementation, parallel review, fixes, PR creation, and final verification. Delegates heavy phases to subagents for context efficiency. |
| **x-git-push** | `/x-git-push` | Git operations: branch creation, atomic commits (Conventional Commits), push, and PR creation. Use for any git workflow task including branching, committing, pushing, creating PRs, or managing version control. |
| **x-ops-troubleshoot** | `/x-ops-troubleshoot` | Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach: reproduce, locate, understand, fix, verify. Use whenever something fails: compilation errors, test failures, runtime exceptions, coverage gaps, or performance issues. |
| **x-review** | `/x-review` | Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Launches parallel subagents, each reading their own knowledge pack, then consolidates into a scored report. Use for pre-PR quality validation. |
| **x-review-pr** | `/x-review-pr` | Tech Lead holistic review with 40-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge. |
| **x-story-create** | `/x-story-create` | > |
| **x-epic-create** | `/x-epic-create` | > |
| **x-epic-decompose** | `/x-epic-decompose` | > |
| **x-epic-map** | `/x-epic-map` | > |
| **x-test-plan** | `/x-test-plan` | Generates a comprehensive test plan before implementation. Delegates KP reading to a context-gathering subagent, then produces structured test scenarios covering unit, integration, API, E2E, contract, and performance tests. |
| **x-test-run** | `/x-test-run` | Runs tests with coverage reporting and threshold validation. Use whenever writing, running, or analyzing tests. Triggers on: test, coverage, TDD, unit test, integration test, test failure, coverage gap, or Definition of Done validation. |

**Total: 28 skills**

### Usage Examples

```bash
# Run a specific skill
/skill-name argument

# Get help on available skills
# Type / in the chat to see the full list
```

---

## Knowledge Packs (Internal Context)

Knowledge Packs do NOT appear in the `/` menu. They are referenced internally by agents and skills
to inject domain knowledge. Configured with `user-invocable: false`.

| Pack | Usage |
|------|-------|
| `api-design` | Referenced internally by agents |
| `architecture` | Referenced internally by agents |
| `coding-standards` | Referenced internally by agents |
| `compliance` | Referenced internally by agents |
| `dockerfile` | Referenced internally by agents |
| `infrastructure` | Referenced internally by agents |
| `layer-templates` | Referenced internally by agents |
| `observability` | Referenced internally by agents |
| `protocols` | Referenced internally by agents |
| `resilience` | Referenced internally by agents |
| `security` | Referenced internally by agents |
| `story-planning` | Referenced internally by agents |
| `testing` | Referenced internally by agents |

---

## Agents (AI Personas)

Agents are system prompts that define specialized personas. They are not invoked directly --
they are used by skills (via Task tool) to delegate work to agents with specific expertise.

| Agent | File |
|-------|------|
| **architect** | `architect.md` |
| **devops-engineer** | `devops-engineer.md` |
| **performance-engineer** | `performance-engineer.md` |
| **product-owner** | `product-owner.md` |
| **qa-engineer** | `qa-engineer.md` |
| **security-engineer** | `security-engineer.md` |
| **tech-lead** | `tech-lead.md` |
| **typescript-developer** | `typescript-developer.md` |

**Total: 8 agents**

---

## Hooks (Automations)

Hooks are scripts executed automatically in response to Claude Code events.
Configured in `settings.json` under the `hooks` key.

### Post-Compile Check

- **Event:** `PostToolUse` (after `Write` or `Edit`)
- **Script:** `.claude/hooks/post-compile-check.sh`
- **Behavior:** When a `.ts` file is modified, runs `npx --no-install tsc --noEmit` automatically
- **Purpose:** Catch compilation errors immediately after file changes

---

## Settings

### settings.json

Permissions are configured in `settings.json` under `permissions.allow`.
This controls which Bash commands Claude Code can run without asking.

### settings.local.json

Local overrides (gitignored). Use for personal preferences or team-specific tools.

See the files directly for current configuration.

---

## Artifact Conventions

| Artifact | Extension | Naming | Frontmatter |
|----------|-----------|--------|-------------|
| Rules | `.md` | `NN-name.md` (numbered) | None |
| Skills | `SKILL.md` | `skills/{name}/SKILL.md` | YAML (name, description) |
| Agents (.claude) | `.md` | `{name}.md` | None |
| Agents (.github) | `.agent.md` | `{name}.agent.md` | YAML (tools, disallowed-tools) |
| Instructions | `.instructions.md` | `{topic}.instructions.md` | None |
| Prompts | `.prompt.md` | `{name}.prompt.md` | YAML (optional) |
| Hooks (.claude) | `.sh` / `.json` | Event-based naming | N/A |
| Hooks (.github) | `.json` | Event-based naming | N/A (JSON) |
| MCP | `.json` | `copilot-mcp.json` | N/A (JSON) |

---

## Plan & Review Templates

Templates provide standardized output formats for planning and review artifacts produced by skills.
They contain `{{PLACEHOLDER}}` tokens resolved at runtime by the LLM, not during generation.
Content is copied verbatim by `PlanTemplatesAssembler` (RULE-003).

> **Fallback:** Templates are optional -- skills degrade gracefully without them.
> If a template is not found, skills use inline formatting as fallback and log a warning.

> **Dual-target:** Templates are copied to both `.claude/templates/` and `.github/templates/`.

| Template | Produced By | Saved To | Pre-Check |
|----------|-------------|----------|-----------|
| `_TEMPLATE-IMPLEMENTATION-PLAN.md` | x-story-implement (Phase 1B) | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-TEST-PLAN.md` | x-test-plan | `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-ARCHITECTURE-PLAN.md` | x-arch-plan | `plans/epic-XXXX/plans/arch-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-TASK-BREAKDOWN.md` | x-lib-task-decomposer | `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-SECURITY-ASSESSMENT.md` | x-story-implement (Phase 1E) | `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` | x-story-implement (Phase 1F) | `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-SPECIALIST-REVIEW.md` | x-review | `plans/epic-XXXX/plans/review-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-TECH-LEAD-REVIEW.md` | x-review-pr | `plans/epic-XXXX/plans/techlead-review-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` | x-review | `plans/epic-XXXX/plans/review-dashboard-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-REVIEW-REMEDIATION.md` | x-story-implement (Phase 5) | `plans/epic-XXXX/plans/remediation-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-EPIC-EXECUTION-PLAN.md` | x-epic-implement | `plans/epic-XXXX/plans/execution-plan-epic-XXXX.md` | Yes |
| `_TEMPLATE-PHASE-COMPLETION-REPORT.md` | x-epic-implement | `plans/epic-XXXX/reports/phase-report-epic-XXXX.md` | No |

**Total: 12 plan & review templates** (copied to both `.claude/templates/` and `.github/templates/` = 24 files)

---

## Tips

- **Rules are always active** -- no need to invoke them, Claude already knows them.
- **Skills are lazy** -- they only load when you type `/name`.
- **Knowledge Packs do not appear in the `/` menu** -- they are internal context for agents.
- **Agents are not invoked directly** -- they are used by skills internally.
- **Hooks run automatically** -- compilation after editing source files detects errors early.
- **To create a new skill**: create `.claude/skills/{name}/SKILL.md` and it appears automatically.
- **To create a new rule**: add a `.md` file in `.claude/rules/` with the appropriate numbering.
- **Both directories are generated** -- run `ia-dev-env generate` to regenerate.

---

## Generation Summary

| Component | Count |
|-----------|-------|
| Rules (.claude) | 6 |
| Skills (.claude) | 14 |
| Knowledge Packs (.claude) | 13 |
| Agents (.claude) | 8 |
| Hooks (.claude) | 1 |
| Settings (.claude) | 2 |
| Plan Templates (.claude) | 12 |
| Instructions (.github) | 5 |
| Skills (.github) | 32 |
| Agents (.github) | 8 |
| Prompts (.github) | 4 |
| Hooks (.github) | 3 |
| Plan Templates (.github) | 12 |
| MCP (.github) | 0 |

Generated by `ia-dev-env v0.1.0`.
