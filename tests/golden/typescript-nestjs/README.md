# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **my-nestjs-service** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** Both `.claude/` and `.github/` directories are **generated outputs** produced by `ia-dev-env`.
> The generator writes `.github/` artifacts under `github/` in the output directory; rename to `.github/` when placing in a project root.
> Do not edit them manually -- regenerate instead.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

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

### .claude/ <-> .github/ Mapping

| .claude/ | .github/ | Notes |
|----------|----------|-------|
| Rules (`rules/*.md`) | Instructions (`instructions/*.instructions.md`) | Rules are system-prompt loaded; instructions are contextual |
| Skills (`skills/*/SKILL.md`) | Skills (`skills/*/SKILL.md`) | Same structure, same YAML frontmatter |
| Agents (`agents/*.md`) | Agents (`agents/*.agent.md`) | GitHub agents use `.agent.md` extension with YAML frontmatter |
| Hooks (`hooks/`) | Hooks (`hooks/*.json`) | Both define event-driven automations |
| Settings (`settings*.json`) | N/A | Claude Code specific |
| N/A | Prompts (`prompts/*.prompt.md`) | GitHub Copilot prompt templates |
| N/A | MCP (`copilot-mcp.json`) | GitHub Copilot MCP server configuration |
| N/A | Global instructions (`copilot-instructions.md`) | Loaded in every Copilot session |

**Total .github/ artifacts: 61**

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

**Total: 5 rules**

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

| Skill | Path | Description |
|-------|------|-------------|
| **patterns** | `/patterns` |  |
| **run-contract-tests** | `/run-contract-tests` | Skill |
| **run-e2e** | `/run-e2e` | Skill |
| **run-perf-test** | `/run-perf-test` | Skill |
| **run-smoke-api** | `/run-smoke-api` | Skill |
| **setup-environment** | `/setup-environment` | Skill |
| **x-dev-implement** | `/x-dev-implement` | Implements a feature/story following project conventions. Delegates preparation to a subagent that reads architecture and coding KPs, then implements layer-by-layer with intermediate compilation checks. |
| **x-dev-lifecycle** | `/x-dev-lifecycle` | Orchestrates the complete feature implementation cycle |
| **x-git-push** | `/x-git-push` | Git operations |
| **x-ops-troubleshoot** | `/x-ops-troubleshoot` | Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach |
| **x-review** | `/x-review` | Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Launches parallel subagents, each reading their own knowledge pack, then consolidates into a scored report. Use for pre-PR quality validation. |
| **x-review-api** | `/x-review-api` | Skill |
| **x-review-events** | `/x-review-events` | Skill |
| **x-review-gateway** | `/x-review-gateway` | Review API gateway configuration for best practices |
| **x-review-graphql** | `/x-review-graphql` | Skill |
| **x-review-pr** | `/x-review-pr` | Tech Lead holistic review with 40-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge. |
| **x-story-create** | `/x-story-create` | > |
| **x-story-epic** | `/x-story-epic` | > |
| **x-story-epic-full** | `/x-story-epic-full` | > |
| **x-story-map** | `/x-story-map` | > |
| **x-test-plan** | `/x-test-plan` | Generates a comprehensive test plan before implementation. Delegates KP reading to a context-gathering subagent, then produces structured test scenarios covering unit, integration, API, E2E, contract, and performance tests. |
| **x-test-run** | `/x-test-run` | Runs tests with coverage reporting and threshold validation. Use whenever writing, running, or analyzing tests. Triggers on |

**Total: 39 skills**

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
| `iac-terraform` | Referenced internally by agents |
| `infrastructure` | Referenced internally by agents |
| `k8s-deployment` | Referenced internally by agents |
| `k8s-kustomize` | Referenced internally by agents |
| `layer-templates` | Referenced internally by agents |
| `nestjs-patterns` | Referenced internally by agents |
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
| **api-engineer** | `api-engineer.md` |
| **architect** | `architect.md` |
| **devops-engineer** | `devops-engineer.md` |
| **event-engineer** | `event-engineer.md` |
| **performance-engineer** | `performance-engineer.md` |
| **product-owner** | `product-owner.md` |
| **qa-engineer** | `qa-engineer.md` |
| **security-engineer** | `security-engineer.md` |
| **tech-lead** | `tech-lead.md` |
| **typescript-developer** | `typescript-developer.md` |

**Total: 10 agents**

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
| Rules (.claude) | 5 |
| Skills (.claude) | 22 |
| Knowledge Packs (.claude) | 17 |
| Agents (.claude) | 10 |
| Hooks (.claude) | 1 |
| Settings (.claude) | 2 |
| Instructions (.github) | 5 |
| Skills (.github) | 36 |
| Agents (.github) | 10 |
| Prompts (.github) | 4 |
| Hooks (.github) | 3 |
| MCP (.github) | 0 |

Generated by `ia-dev-env v0.1.0`.
