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

### .codex/ (OpenAI Codex)

```
.codex/
|-- AGENTS.md                   <-- Agent instructions (generated from .claude/ context)
+-- config.toml                 <-- Codex configuration (model, approval, sandbox)
+-- requirements.toml           <-- Enforced minimum policy constraints
+-- skills/                     <-- Codex-native skills mirror
```

### .claude/ <-> .github/ <-> .codex/ Mapping

| .claude/ | .github/ | .codex/ | Notes |
|----------|----------|---------|-------|
| Rules (`rules/*.md`) | Instructions (`instructions/*.instructions.md`) | Sections in `AGENTS.md` | Rules → consolidated sections |
| Skills (`skills/*/SKILL.md`) | Skills (`skills/*/SKILL.md`) | Skills (`.agents/skills/` + `.codex/skills/`) | Dual output with identical content |
| Agents (`agents/*.md`) | Agents (`agents/*.agent.md`) | Sections (`[agents.*]`) in `config.toml` | Agents represented as TOML sections |
| Hooks (`hooks/`) | Hooks (`hooks/*.json`) | Reference in `AGENTS.md` | Hooks influence approval_policy |
| Settings (`settings*.json`) | N/A | `.codex/config.toml` + `.codex/requirements.toml` | Runtime and enforced policies |
| N/A | N/A | `AGENTS.md` + `AGENTS.override.md` (root) | Base instructions + local override |
| N/A | Prompts (`prompts/*.prompt.md`) | N/A | GitHub Copilot prompt templates |
| N/A | MCP (`copilot-mcp.json`) | N/A | GitHub Copilot MCP server configuration |
| N/A | Global instructions (`copilot-instructions.md`) | N/A | Loaded in every Copilot session |

**Total .github/ artifacts: 77**

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

**Total: 6 rules**

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

| Skill | Path | Description |
|-------|------|-------------|
| **patterns** | `/patterns` |  |
| **run-contract-tests** | `/run-contract-tests` | Skill: Contract Tests — Runs consumer-driven contract tests (Pact, Spring Cloud Contract) to verify API compatibility between services. |
| **run-e2e** | `/run-e2e` | Skill: End-to-End Tests — Runs integration tests that validate the complete flow from request through all application layers to response, using a real database. |
| **run-perf-test** | `/run-perf-test` | Skill: Performance/Load Tests — Runs performance tests to validate latency SLAs, throughput targets, and resource stability under load. Supports baseline, normal, peak, and sustained scenarios. |
| **run-smoke-api** | `/run-smoke-api` | Skill: REST API Smoke Tests — Runs automated smoke tests against the REST API using Newman/Postman. Supports local, container-orchestrated, and staging environments. |
| **setup-environment** | `/setup-environment` | Skill: Dev Environment Setup — Sets up the local development environment including container orchestrator, database, and build tools. |
| **x-changelog** | `/x-changelog` | Generates CHANGELOG.md from Conventional Commits history. Parses git log, groups by commit type, maps to Keep a Changelog sections (Added, Changed, Fixed, etc.), and performs incremental updates preserving existing entries. |
| **x-codebase-audit** | `/x-codebase-audit` | Full codebase review against all project standards. Launches parallel subagents per audit dimension (Clean Code, SOLID, Architecture, Tests, Security, Cross-file), consolidates findings into a severity-categorized report with score. Use for periodic quality validation. |
| **x-dependency-audit** | `/x-dependency-audit` | Checks project dependencies for vulnerabilities, outdated versions, and license issues. Detects build tool automatically, runs language-specific audit commands, and generates a severity-categorized report. |
| **x-dev-adr-automation** | `/x-dev-adr-automation` | Automates ADR generation from architecture plan mini-ADRs: extracts inline decisions, expands to full ADR format, assigns sequential numbering, updates the ADR index, and adds cross-references. |
| **x-dev-arch-update** | `/x-dev-arch-update` | Incrementally updates the service architecture document with changes from architecture plans. Adds new components, integrations, flows, and ADR references without rewriting existing content. Use after implementation to keep architecture documentation current. |
| **x-dev-architecture-plan** | `/x-dev-architecture-plan` | Generates a comprehensive architecture plan with component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability strategies. Use before implementation to document design decisions. |
| **x-dev-epic-implement** | `/x-dev-epic-implement` | Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents. |
| **x-dev-implement** | `/x-dev-implement` | Implements a feature/story using TDD (Red-Green-Refactor) workflow. Delegates preparation to a subagent that reads architecture, coding, and test plan KPs, then implements test-first with Double-Loop TDD, layer-by-layer with compile checks after each cycle. |
| **x-dev-lifecycle** | `/x-dev-lifecycle` | Orchestrates the complete feature implementation cycle: branch creation, planning, task decomposition, implementation, parallel review, fixes, PR creation, and final verification. Delegates heavy phases to subagents for context efficiency. |
| **x-fix-pr-comments** | `/x-fix-pr-comments` | Reads PR review comments and fixes actionable ones automatically. Detects PR from argument or branch, classifies comments (actionable/suggestion/question/praise), implements fixes, and commits with proper conventional commit messages. |
| **x-git-push** | `/x-git-push` | Git operations: branch creation, atomic commits (Conventional Commits), push, and PR creation. Use for any git workflow task including branching, committing, pushing, creating PRs, or managing version control. |
| **x-jira-create-epic** | `/x-jira-create-epic` | > |
| **x-jira-create-stories** | `/x-jira-create-stories` | > |
| **x-mcp-recommend** | `/x-mcp-recommend` | Analyzes project tech stack and recommends relevant MCP (Model Context Protocol) servers. Auto-detects language, framework, database, cache, and message broker from project config, then matches against a built-in catalog of MCP servers with installation instructions. |
| **x-ops-troubleshoot** | `/x-ops-troubleshoot` | Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach: reproduce, locate, understand, fix, verify. Use whenever something fails: compilation errors, test failures, runtime exceptions, coverage gaps, or performance issues. |
| **x-review** | `/x-review` | Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Launches parallel subagents, each reading their own knowledge pack, then consolidates into a scored report. Use for pre-PR quality validation. |
| **x-review-api** | `/x-review-api` | Skill: REST API Design Review — Validates REST API endpoints for RFC 7807 error responses, pagination, URL versioning, OpenAPI documentation, status codes, and DTO patterns. |
| **x-review-events** | `/x-review-events` | Skill: Event-Driven Review — Validates event schemas, producer/consumer patterns, error handling, dead letter topics, and operational readiness. |
| **x-review-gateway** | `/x-review-gateway` | Review API gateway configuration for best practices |
| **x-review-graphql** | `/x-review-graphql` | Skill: GraphQL Schema & Resolver Review — Validates GraphQL schema design, resolver implementation, security patterns, and observability. |
| **x-review-pr** | `/x-review-pr` | Tech Lead holistic review with 45-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, TDD process, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge. |
| **x-setup-dev-environment** | `/x-setup-dev-environment` | Validate and configure local development environment: detect stack, check prerequisites, verify versions, validate IDE config, test database connectivity, run initial build, and report status with fix suggestions |
| **x-story-create** | `/x-story-create` | > |
| **x-story-epic** | `/x-story-epic` | > |
| **x-story-epic-full** | `/x-story-epic-full` | > |
| **x-story-map** | `/x-story-map` | > |
| **x-test-plan** | `/x-test-plan` | Generates a Double-Loop TDD test plan with TPP-ordered scenarios before implementation. Delegates KP reading to a context-gathering subagent, then produces structured Acceptance Tests (outer loop) and Unit Tests in Transformation Priority Premise order (inner loop). |
| **x-test-run** | `/x-test-run` | Runs tests with coverage reporting and threshold validation. Use whenever writing, running, or analyzing tests. Triggers on: test, coverage, TDD, unit test, integration test, test failure, coverage gap, or Definition of Done validation. |

**Total: 51 skills**

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
| Rules (.claude) | 6 |
| Skills (.claude) | 34 |
| Knowledge Packs (.claude) | 17 |
| Agents (.claude) | 10 |
| Hooks (.claude) | 1 |
| Settings (.claude) | 2 |
| Instructions (.github) | 5 |
| Skills (.github) | 48 |
| Agents (.github) | 10 |
| Prompts (.github) | 4 |
| Hooks (.github) | 3 |
| MCP (.github) | 0 |
| AGENTS.md (root) | 1 |
| AGENTS.override.md (root) | 1 |
| Codex (.codex) | 107 |
| Skills (.agents) | 105 |

Generated by `ia-dev-env v0.1.0`.
