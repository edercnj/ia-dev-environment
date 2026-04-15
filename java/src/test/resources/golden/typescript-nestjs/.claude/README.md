# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **my-nestjs-service** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** The `.claude/` directory is a **generated output** produced by `ia-dev-env`.
> Do not edit it manually -- regenerate instead.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

## Structure

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

## Platform Selection

The generator currently produces Claude Code artifacts only. Support for `copilot`, `codex`, and the generic `agents` target was removed (see EPIC-0034). Legacy `--platform` values are rejected by the CLI.

| Value | Description | Directories Generated |
|-------|-------------|-----------------------|
| `claude-code` | Anthropic Claude Code (only accepted value) | `.claude/` + docs |

### CLI Examples

```bash
# Generate Claude Code artifacts (default behavior)
ia-dev-env generate --platform claude-code --config my-config.yaml

# Platform flag can be omitted — claude-code is the only supported target
ia-dev-env generate --config my-config.yaml
```

### YAML Configuration

You can also specify the platform in your YAML config file:

```yaml
platform: claude-code
```

### Default Behavior

When no `--platform` flag is provided and no `platform:` key exists in the YAML config, the generator produces artifacts for `claude-code` (the only supported target). Any legacy value (`copilot`, `codex`, `agents`, `all`) is rejected with a clear error message.

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
| 09 | `09-branching-model.md` | branching model |
| 10 | `10-anti-patterns.md` | anti patterns |
| 12 | `12-security-anti-patterns.md` | security anti patterns |
| 13 | `13-skill-invocation-protocol.md` | skill invocation protocol |
| 14 | `14-worktree-lifecycle.md` | worktree lifecycle |
| 15 | `15-task-testability.md` | task testability |
| 16 | `16-task-io-contracts.md` | task io contracts |
| 17 | `17-topological-execution.md` | topological execution |
| 18 | `18-atomic-task-commits.md` | atomic task commits |
| 19 | `19-backward-compatibility.md` | backward compatibility |

**Total: 18 rules**

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

| Skill | Path | Description |
|-------|------|-------------|
| **patterns** | `/patterns` |  |
| **x-adr-generate** | `/x-adr-generate` | Automates ADR generation from architecture plan mini-ADRs: extracts inline decisions, expands to full ADR format, assigns sequential numbering, updates the ADR index, and adds cross-references. |
| **x-arch-plan** | `/x-arch-plan` | Generates a comprehensive architecture plan with component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability strategies. Use before implementation to document design decisions. |
| **x-arch-update** | `/x-arch-update` | Incrementally updates the service architecture document with changes from architecture plans. Adds new components, integrations, flows, and ADR references without rewriting existing content. Use after implementation to keep architecture documentation current. |
| **x-ci-generate** | `/x-ci-generate` | Generate or update CI/CD pipelines based on project stack: detect language, analyze existing workflows, generate CI/CD/release/security pipelines, validate with actionlint, support monorepo triggers. |
| **x-code-audit** | `/x-code-audit` | Full codebase review against all project standards. Launches parallel subagents per audit dimension (Clean Code, SOLID, Architecture, Tests, Security, Cross-file), consolidates findings into a severity-categorized report with score. Use for periodic quality validation. |
| **x-code-format** | `/x-code-format` | Formats source code using the appropriate formatter for {{LANGUAGE}}. First step of the pre-commit chain (format -> lint -> compile -> commit). Supports --check (dry-run) and --changed-only modes. |
| **x-code-lint** | `/x-code-lint` | Analyzes source code with the appropriate linter for {{LANGUAGE}}. Second step in the pre-commit chain (RULE-007: format -> lint -> compile -> commit). Supports --fix, --changed-only, and --strict modes. |
| **x-dependency-audit** | `/x-dependency-audit` | Checks project dependencies for vulnerabilities, outdated versions, and license issues. Detects build tool automatically, runs language-specific audit commands, and generates a severity-categorized report. |
| **x-doc-generate** | `/x-doc-generate` | Documentation automation: detects documentation type needed (API, README, ADR, changelog) from code changes, delegates to specialized skills or generates inline. Single entry point for all documentation updates. |
| **x-epic-create** | `/x-epic-create` | Generate an Epic document from a system specification file with cross-cutting business rules, global quality definitions (DoR/DoD), a complete story index with dependency declarations, and optional Jira integration. |
| **x-epic-decompose** | `/x-epic-decompose` | Complete decomposition of a system specification into an Epic, individual Story files, and an Implementation Map with dependency graph and phased execution plan. Orchestrates spec analysis, rule extraction, story identification, and implementation planning. |
| **x-epic-implement** | `/x-epic-implement` | Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via explicit git worktrees (per ADR-0004 §D2 and Rule 14). Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-story-implement. EPIC-0038 simplification: epic orchestrator handles ONLY story-level concerns (phase order, story PR management, epic-level verification). Task management (TDD cycles, atomic commits per task, coalesced handling) is fully delegated to x-story-implement's v2 wave dispatcher. Tasks are invisible at the epic level. |
| **x-epic-map** | `/x-epic-map` | Generate an Implementation Map from an Epic and its Stories with dependency matrix, phase computation, critical path analysis, ASCII phase diagrams, Mermaid dependency graphs, phase summary tables, and strategic observations. |
| **x-epic-orchestrate** | `/x-epic-orchestrate` | Orchestrates multi-agent planning for all stories in an epic, respecting dependency order, with checkpoint and resume support. |
| **x-git-commit** | `/x-git-commit` | Creates Conventional Commits with Task ID in scope and pre-commit chain (format -> lint -> compile). Central commit point in the task-centric workflow with TDD tag support. |
| **x-git-push** | `/x-git-push` | Git operations: branch creation, atomic commits (Conventional Commits), push, and PR creation. Use for any git workflow task including branching, committing, pushing, creating PRs, or managing version control. |
| **x-git-worktree** | `/x-git-worktree` | Manages git worktrees for parallel task and story execution. Operations: create, list, remove, cleanup, detect-context. Follows Rule 14 (Worktree Lifecycle) naming convention under .claude/worktrees/{identifier}/. |
| **x-hardening-eval** | `/x-hardening-eval` | Evaluates application hardening posture against CIS and OWASP benchmarks: HTTP security headers, TLS configuration, CORS policy, cookie security, error handling, input limits, and information disclosure. Produces SARIF output with weighted scoring. |
| **x-jira-create-epic** | `/x-jira-create-epic` | Create a Jira Epic from an existing local epic markdown file. Read the epic file, map fields to Jira, create the issue via MCP, and sync the Jira key back to the local file. |
| **x-jira-create-stories** | `/x-jira-create-stories` | Create Jira Stories from existing local story markdown files. Read all story files in an epic directory, map fields to Jira, create issues with parent epic link, create dependency links between stories, and sync Jira keys back to local files. |
| **x-mcp-recommend** | `/x-mcp-recommend` | Analyzes project tech stack and recommends relevant MCP (Model Context Protocol) servers. Auto-detects language, framework, database, cache, and message broker from project config, then matches against a built-in catalog of MCP servers with installation instructions. |
| **x-ops-incident** | `/x-ops-incident` | Guides incident response with severity-based checklists, communication templates, and postmortem triggers. Interactive guide for SEV1-SEV4 incidents covering classification, response coordination, and action item tracking. |
| **x-ops-troubleshoot** | `/x-ops-troubleshoot` | Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach: reproduce, locate, understand, fix, verify. Use whenever something fails: compilation errors, test failures, runtime exceptions, coverage gaps, or performance issues. |
| **x-owasp-scan** | `/x-owasp-scan` | Automated OWASP Top 10 (2021) verification mapped to ASVS levels (L1/L2/L3). Checks all 10 categories (A01-A10) with per-category pass/fail, ASVS coverage percentage, score grading, SARIF 2.1.0 output, and CI integration. Delegates A06 to x-dependency-audit. |
| **x-perf-profile** | `/x-perf-profile` | Automated profiling: detect language/runtime, select appropriate profiler, execute session, generate flamegraph, identify hotspots, and suggest optimizations referencing the performance-engineering knowledge pack. |
| **x-pr-create** | `/x-pr-create` | Task-level PR creation with formatted title, automatic labels, structured body, and target branch logic. Creates standardized PRs for individual tasks with Task ID traceability. |
| **x-pr-fix** | `/x-pr-fix` | Reads PR review comments and fixes actionable ones automatically. Detects PR from argument or branch, classifies comments (actionable/suggestion/question/praise), implements fixes, and commits with proper conventional commit messages. |
| **x-pr-fix-epic** | `/x-pr-fix-epic` | Discovers all PRs from an epic via execution-state.json, fetches and classifies review comments in batch, generates a consolidated findings report, applies fixes, and creates a single correction PR. Supports dry-run, explicit PR list fallback, and idempotent re-execution. |
| **x-release** | `/x-release` | Orchestrates complete release flow using Git Flow release branches with approval gate, PR-flow (gh CLI) and deep validation: version bump (auto-detect or explicit), release branch creation from develop, deep validation (coverage, golden files, version consistency), version file updates, changelog generation, release commit, release PR via gh (optionally reviewed by x-review-pr), human approval gate with persistent state file, tag on main after merged PR, back-merge PR to develop with conflict detection, and cleanup. Supports hotfix releases from main, dry-run mode, resume via --continue-after-merge, in-session pause via --interactive, GPG-signed tags, skip-review opt-out, and custom state file path. |
| **x-release-changelog** | `/x-release-changelog` | Generates CHANGELOG.md from Conventional Commits history. Parses git log, groups by commit type, maps to Keep a Changelog sections (Added, Changed, Fixed, etc.), and performs incremental updates preserving existing entries. |
| **x-review** | `/x-review` | Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Invokes individual review skills in parallel via Skill tool, then consolidates into a scored report. Use for pre-PR quality validation. |
| **x-review-api** | `/x-review-api` | Validates REST API endpoints for RFC 7807 error responses, pagination, URL versioning, OpenAPI documentation, status codes, and DTO patterns. |
| **x-review-devops** | `/x-review-devops` | DevOps specialist review: validates Dockerfile, container security, CI/CD pipeline, resource limits, health probes, graceful shutdown, and deployment configuration. |
| **x-review-events** | `/x-review-events` | Validates event schemas, producer/consumer patterns, error handling, dead letter topics, and operational readiness for event-driven architectures. |
| **x-review-gateway** | `/x-review-gateway` | Reviews API gateway configuration for routing rules, authentication, rate limiting, CORS, security headers, TLS, and observability integration. |
| **x-review-graphql** | `/x-review-graphql` | Validates GraphQL schema design, resolver implementation, security patterns, and observability for compliance with best practices. |
| **x-review-perf** | `/x-review-perf` | Performance specialist review: validates N+1 queries, connection pools, async patterns, pagination, caching, timeouts, circuit breakers, and resource cleanup. |
| **x-review-pr** | `/x-review-pr` | Tech Lead holistic review with 57-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, TDD process, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge. |
| **x-review-qa** | `/x-review-qa` | QA specialist review: validates test coverage, TDD compliance, test naming, fixtures, parametrized tests, and acceptance criteria coverage. |
| **x-review-security** | `/x-review-security` | Reviews code changes for compliance with selected security frameworks. Verifies sensitive data handling, audit trails, and access control patterns. |
| **x-runtime-eval** | `/x-runtime-eval` | Evaluate runtime protection controls: rate limiting, WAF rules, bot protection, DDoS mitigation, account lockout, brute force protection, CSP enforcement, and permissions policy. Produce SARIF 2.1.0 output with ASVS compliance mapping and scored Markdown report. |
| **x-security-dashboard** | `/x-security-dashboard` | Aggregates results from all security scanning skills into a unified posture view with score 0-100, trend tracking, OWASP risk heatmap, per-dimension breakdown, and remediation priority queue. Never executes scans — reads existing results only (RULE-011). |
| **x-security-pipeline** | `/x-security-pipeline` | Generate CI/CD pipeline configurations with conditional security stages based on SecurityConfig flags. Support GitHub Actions, GitLab CI, and Azure DevOps with minimal and full stage modes, configurable severity thresholds, and SARIF artifact upload. |
| **x-setup-env** | `/x-setup-env` | Validate and configure local development environment: detect stack, check prerequisites, verify versions, validate IDE config, test database connectivity, run initial build, and report status with fix suggestions. |
| **x-spec-drift** | `/x-spec-drift` | Detects spec-code drift by comparing story data contracts, endpoints, and Gherkin scenarios against implemented code. Supports standalone mode (full report) and inline mode (compact output for TDD loop integration in x-story-implement Phase 2). |
| **x-story-create** | `/x-story-create` | Generate detailed User Story files from an Epic and system specification with full data contracts, Gherkin acceptance criteria, Mermaid sequence diagrams, dependency declarations, tagged sub-tasks, quality gate validation, and optional Jira integration. |
| **x-story-implement** | `/x-story-implement` | Orchestrates the complete feature implementation cycle with task-centric workflow: branch creation, planning, per-task TDD execution with individual PRs and approval gates, story-level verification, and final cleanup. Schema-aware: v1 (legacy) runs the monolithic coalesce-ad-hoc flow; v2 (EPIC-0038) reads task-implementation-map-STORY-*.md and dispatches x-task-implement in waves (declared parallelism) — ending the 'task embedded in story' anti-pattern. Delegates to x-test-tdd, x-git-commit, x-pr-create, and (v2) x-task-implement. |
| **x-story-plan** | `/x-story-plan` | Multi-agent story planning: launches 5 specialized agents (Architect, QA, Security, Tech Lead, Product Owner) in parallel to produce a consolidated task breakdown, individual task plans, planning report, and DoR validation. Schema-aware: v1 (legacy) runs the original 6-phase flow; v2 (task-first, EPIC-0038) adds Phases 4a-4c that emit task-TASK-NNN.md + plan-task-TASK-NNN.md per task and a task-implementation-map-STORY-*.md, wiring every task through x-task-plan in parallel. |
| **x-supply-chain-audit** | `/x-supply-chain-audit` | Enhanced supply chain security audit beyond x-dependency-audit. Analyzes maintainer risk, typosquatting detection, phantom dependencies, dependency age, EPSS scoring, and SLSA assessment. Produces SARIF 2.1.0 output with weighted risk scoring. |
| **x-task-implement** | `/x-task-implement` | Implements a feature/story/task using TDD (Red-Green-Refactor) workflow. Schema-aware: v1 (legacy) runs the original Double-Loop TDD flow with story-section task extraction; v2 (task-first, EPIC-0038) reads task-TASK-XXXX-YYYY-NNN.md + plan-task-TASK-XXXX-YYYY-NNN.md, honours declared I/O contracts, respects task-implementation-map dependencies, verifies post-conditions via grep/assert, and produces a single atomic commit per task via x-git-commit. |
| **x-task-plan** | `/x-task-plan` | Generates a detailed per-task implementation plan (plan-task-TASK-XXXX-YYYY-NNN.md) with TDD cycles in TPP order, file impact analysis by architecture layer, security checklist by task type, and exit criteria. Two invocation modes: task-file-first (--task-file) consumes a standalone task-TASK-XXXX-YYYY-NNN.md contract (EPIC-0038); story-scoped (STORY-ID --task TASK-ID) reads the task from story Section 8 (legacy). Invocable standalone OR via x-story-plan (future). |
| **x-test-contract** | `/x-test-contract` | Runs consumer-driven contract tests (Pact, Spring Cloud Contract) to verify API compatibility between services. |
| **x-test-contract-lint** | `/x-test-contract-lint` | Validates API contracts (OpenAPI 3.1, AsyncAPI 2.6, Protobuf 3) against their specifications. Reports structural errors, missing fields, and spec violations. |
| **x-test-e2e** | `/x-test-e2e` | Runs integration tests that validate the complete flow from request through all application layers to response, using a real database. |
| **x-test-perf** | `/x-test-perf` | Runs performance tests to validate latency SLAs, throughput targets, and resource stability under load. Supports baseline, normal, peak, and sustained scenarios. |
| **x-test-plan** | `/x-test-plan` | Generates a Double-Loop TDD test plan with TPP-ordered scenarios before implementation. Delegates KP reading to a context-gathering subagent, then produces structured Acceptance Tests (outer loop) and Unit Tests in Transformation Priority Premise order (inner loop). |
| **x-test-run** | `/x-test-run` | Runs tests with coverage reporting and threshold validation. Use whenever writing, running, or analyzing tests. Triggers on: test, coverage, TDD, unit test, integration test, test failure, coverage gap, or Definition of Done validation. |
| **x-test-smoke-api** | `/x-test-smoke-api` | Runs automated smoke tests against the REST API using Newman/Postman. Supports local, container-orchestrated, and staging environments. |
| **x-test-tdd** | `/x-test-tdd` | Executes systematic Red-Green-Refactor TDD cycles for a task. Reads the task plan generated by x-task-plan, runs each cycle in TPP order, validates RED/GREEN/REFACTOR phases, delegates atomic commits to x-git-commit with TDD tags, and supports resume and dry-run. |
| **x-threat-model** | `/x-threat-model` | Generate threat models using STRIDE analysis: identify components, map data flows, analyze threats per category, classify severity, suggest mitigations, and produce threat model document. |

**Total: 85 skills**

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
| `ci-cd-patterns` | Referenced internally by agents |
| `coding-standards` | Referenced internally by agents |
| `compliance` | Referenced internally by agents |
| `data-management` | Referenced internally by agents |
| `disaster-recovery` | Referenced internally by agents |
| `dockerfile` | Referenced internally by agents |
| `feature-flags` | Referenced internally by agents |
| `iac-terraform` | Referenced internally by agents |
| `infrastructure` | Referenced internally by agents |
| `k8s-deployment` | Referenced internally by agents |
| `k8s-kustomize` | Referenced internally by agents |
| `layer-templates` | Referenced internally by agents |
| `nestjs-patterns` | Referenced internally by agents |
| `observability` | Referenced internally by agents |
| `performance-engineering` | Referenced internally by agents |
| `protocols` | Referenced internally by agents |
| `release-management` | Referenced internally by agents |
| `resilience` | Referenced internally by agents |
| `security` | Referenced internally by agents |
| `sre-practices` | Referenced internally by agents |
| `story-planning` | Referenced internally by agents |
| `testing` | Referenced internally by agents |

---

## Agents (AI Personas)

Agents are system prompts that define specialized personas. They are not invoked directly --
they are used by skills (via Task tool) to delegate work to agents with specific expertise.

| Agent | File |
|-------|------|
| **api-engineer** | `api-engineer.md` |
| **appsec-engineer** | `appsec-engineer.md` |
| **architect** | `architect.md` |
| **compliance-auditor** | `compliance-auditor.md` |
| **devops-engineer** | `devops-engineer.md` |
| **devsecops-engineer** | `devsecops-engineer.md` |
| **event-engineer** | `event-engineer.md` |
| **performance-engineer** | `performance-engineer.md` |
| **product-owner** | `product-owner.md` |
| **qa-engineer** | `qa-engineer.md` |
| **security-engineer** | `security-engineer.md` |
| **sre-engineer** | `sre-engineer.md` |
| **tech-lead** | `tech-lead.md` |
| **typescript-developer** | `typescript-developer.md` |

**Total: 14 agents**

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
| Agents | `.md` | `{name}.md` | None |
| Hooks | `.sh` / `.json` | Event-based naming | N/A |

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
| Rules (.claude) | 18 |
| Skills (.claude) | 61 |
| Knowledge Packs (.claude) | 24 |
| Agents (.claude) | 14 |
| Hooks (.claude) | 1 |
| Settings (.claude) | 2 |
| Plan Templates (.claude) | 16 |

Generated by `ia-dev-env v0.1.0`.
