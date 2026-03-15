# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **my-cli-tool** project.
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

### .codex/ (OpenAI Codex)

```
.codex/
|-- AGENTS.md                   <-- Agent instructions (generated from .claude/ context)
+-- config.toml                 <-- Codex configuration (model, approval, sandbox)
```

### .claude/ <-> .github/ <-> .codex/ Mapping

| .claude/ | .github/ | .codex/ | Notes |
|----------|----------|---------|-------|
| Rules (`rules/*.md`) | Instructions (`instructions/*.instructions.md`) | Sections in `AGENTS.md` | Rules → consolidated sections |
| Skills (`skills/*/SKILL.md`) | Skills (`skills/*/SKILL.md`) | Skills (`.agents/skills/`) | Same structure across platforms |
| Agents (`agents/*.md`) | Agents (`agents/*.agent.md`) | Agent personas in `AGENTS.md` | Agents as section |
| Hooks (`hooks/`) | Hooks (`hooks/*.json`) | Reference in `AGENTS.md` | Hooks influence approval_policy |
| Settings (`settings*.json`) | N/A | `.codex/config.toml` | Permissions → approval policy |
| N/A | N/A | `AGENTS.md` (project root) | Codex project instructions |
| N/A | Prompts (`prompts/*.prompt.md`) | N/A | GitHub Copilot prompt templates |
| N/A | MCP (`copilot-mcp.json`) | N/A | GitHub Copilot MCP server configuration |
| N/A | Global instructions (`copilot-instructions.md`) | N/A | Loaded in every Copilot session |

**Total .github/ artifacts: 55**

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
| **run-e2e** | `/run-e2e` | Skill |
| **x-dev-implement** | `/x-dev-implement` | Implements a feature/story following project conventions. Delegates preparation to a subagent that reads architecture and coding KPs, then implements layer-by-layer with intermediate compilation checks. |
| **x-dev-lifecycle** | `/x-dev-lifecycle` | Orchestrates the complete feature implementation cycle |
| **x-git-push** | `/x-git-push` | Git operations |
| **x-ops-troubleshoot** | `/x-ops-troubleshoot` | Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach |
| **x-review** | `/x-review` | Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Launches parallel subagents, each reading their own knowledge pack, then consolidates into a scored report. Use for pre-PR quality validation. |
| **x-review-pr** | `/x-review-pr` | Tech Lead holistic review with 40-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge. |
| **x-story-create** | `/x-story-create` | > |
| **x-story-epic** | `/x-story-epic` | > |
| **x-story-epic-full** | `/x-story-epic-full` | > |
| **x-story-map** | `/x-story-map` | > |
| **x-test-plan** | `/x-test-plan` | Generates a Double-Loop TDD test plan with TPP-ordered scenarios before implementation. Delegates KP reading to a context-gathering subagent, then produces structured Acceptance Tests (outer loop) and Unit Tests in Transformation Priority Premise order (inner loop). |
| **x-test-run** | `/x-test-run` | Runs tests with coverage reporting and threshold validation. Use whenever writing, running, or analyzing tests. Triggers on |

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
| `click-cli-patterns` | Referenced internally by agents |
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
| **python-developer** | `python-developer.md` |
| **qa-engineer** | `qa-engineer.md` |
| **security-engineer** | `security-engineer.md` |
| **tech-lead** | `tech-lead.md` |

**Total: 8 agents**

---

## Hooks (Automations)

Hooks are scripts executed automatically in response to Claude Code events.
Configured in `settings.json` under the `hooks` key.

No hooks configured.

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
| Skills (.claude) | 14 |
| Knowledge Packs (.claude) | 14 |
| Agents (.claude) | 8 |
| Hooks (.claude) | 0 |
| Settings (.claude) | 2 |
| Instructions (.github) | 5 |
| Skills (.github) | 32 |
| Agents (.github) | 8 |
| Prompts (.github) | 4 |
| Hooks (.github) | 3 |
| MCP (.github) | 0 |
| AGENTS.md (root) | 1 |
| Codex (.codex) | 1 |
| Skills (.agents) | 58 |

Generated by `ia-dev-env v0.1.0`.
