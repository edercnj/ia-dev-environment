# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **my-cli-tool** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

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
| **api-design** | `/api-design` | API design principles: {{LANGUAGE}}-specific patterns for REST/gRPC/GraphQL. URL structure, status codes, RFC 7807 errors, pagination, content negotiation, validation, request/response shaping, versioning strategies, and protocol conventions. |
| **architecture** | `/architecture` | Full architecture reference: {{ARCHITECTURE}} principles, package structure, dependency rules, thread-safety, mapper patterns, persistence rules, and architecture variants. Read before designing or implementing features. |
| **click-cli-patterns** | `/click-cli-patterns` | Click CLI patterns: command groups, options/arguments, Jinja2 templating, atomic file operations, pyproject.toml packaging, CLI testing with CliRunner, structured logging for CLI. |
| **coding-standards** | `/coding-standards` | Complete coding conventions: Clean Code rules (CC-01 to CC-10), SOLID principles, {{LANGUAGE}} {{LANGUAGE_VERSION}} idioms, naming patterns, constructor injection, mapper conventions, version-specific features, and approved libraries. Read before writing any code. |
| **compliance** | `/compliance` | Compliance frameworks (conditionally included): GDPR, HIPAA, LGPD, PCI-DSS, SOX. Data classification, rights enforcement, processing records, international transfers, security measures, audit logging, and framework-specific requirements. |
| **dockerfile** | `/dockerfile` | Dockerfile patterns per language covering multi-stage builds, security hardening, .dockerignore templates, layer optimization, health checks, and OCI labels. Internal reference for agents managing infrastructure. |
| **infrastructure** | `/infrastructure` | Infrastructure patterns: Docker multi-stage builds, Kubernetes manifests (cloud-agnostic), security context, 12-Factor App principles, graceful shutdown, resource management, and cloud-native design. |
| **layer-templates** | `/layer-templates` | Reference code templates for each hexagonal architecture layer. Provides consistent patterns for domain model, ports, DTOs, mappers, entities, repositories, use cases, REST resources, exception mappers, migrations, and configuration. Uses {{LANGUAGE}}, {{FRAMEWORK}} placeholders. |
| **observability** | `/observability` | Observability principles: distributed tracing (span trees, mandatory attributes), metrics naming conventions, structured logging with mandatory fields, health checks (liveness/readiness/startup), correlation IDs, and OpenTelemetry integration. |
| **patterns** | `/patterns` |  |
| **protocols** | `/protocols` | Protocol conventions: REST (OpenAPI 3.1), gRPC (Proto3), GraphQL, WebSocket, and event-driven messaging. URL structure, versioning, error handling per protocol, schema design, and integration patterns. |
| **resilience** | `/resilience` | Resilience patterns: circuit breaker, rate limiting, bulkhead isolation, timeout control, retry with exponential backoff + jitter, fallback/graceful degradation, backpressure, and resilience metrics. |
| **run-e2e** | `/run-e2e` | Skill: End-to-End Tests — Runs integration tests that validate the complete flow from request through all application layers to response, using a real database. |
| **security** | `/security` | Complete security reference: OWASP Top 10, security headers, secrets management, input validation, cryptography (TLS, hashing, key management), and pentest readiness checklist. Read during security reviews or when implementing security-sensitive features. |
| **story-planning** | `/story-planning` | Story decomposition and planning: layer-by-layer decomposition (foundation, core domain, extensions, compositions, cross-cutting), story self-containment (data contracts, acceptance criteria), dependency DAG, sizing rules, and phase computation. |
| **testing** | `/testing` | Complete testing reference: testing philosophy, 8 test categories, coverage thresholds, fixture patterns, data uniqueness, async handling, database strategy, and {{LANGUAGE}}-specific test frameworks. Read before writing tests. |
| **x-dev-implement** | `/x-dev-implement` | Implements a feature/story following project conventions. Delegates preparation to a subagent that reads architecture and coding KPs, then implements layer-by-layer with intermediate compilation checks. |
| **x-dev-lifecycle** | `/x-dev-lifecycle` | Orchestrates the complete feature implementation cycle: branch creation, planning, task decomposition, implementation, parallel review, fixes, PR creation, and final verification. Delegates heavy phases to subagents for context efficiency. |
| **x-git-push** | `/x-git-push` | Git operations: branch creation, atomic commits (Conventional Commits), push, and PR creation. Use for any git workflow task including branching, committing, pushing, creating PRs, or managing version control. |
| **x-ops-troubleshoot** | `/x-ops-troubleshoot` | Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach: reproduce, locate, understand, fix, verify. Use whenever something fails: compilation errors, test failures, runtime exceptions, coverage gaps, or performance issues. |
| **x-review** | `/x-review` | Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Launches parallel subagents, each reading their own knowledge pack, then consolidates into a scored report. Use for pre-PR quality validation. |
| **x-review-pr** | `/x-review-pr` | Tech Lead holistic review with 40-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge. |
| **x-story-create** | `/x-story-create` | > |
| **x-story-epic** | `/x-story-epic` | > |
| **x-story-epic-full** | `/x-story-epic-full` | > |
| **x-story-map** | `/x-story-map` | > |
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

No knowledge packs configured.

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

## Tips

- **Rules are always active** -- no need to invoke them, Claude already knows them.
- **Skills are lazy** -- they only load when you type `/name`.
- **Knowledge Packs do not appear in the `/` menu** -- they are internal context for agents.
- **Agents are not invoked directly** -- they are used by skills internally.
- **Hooks run automatically** -- compilation after editing source files detects errors early.
- **To create a new skill**: create `.claude/skills/{name}/SKILL.md` and it appears automatically.
- **To create a new rule**: add a `.md` file in `.claude/rules/` with the appropriate numbering.
