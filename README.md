# ia-dev-environment

A CLI tool that generates complete `.claude/`, `.github/`, `.codex/`, and `.agents/` boilerplate for AI-assisted development environments. Produces rules, skills, agents, hooks, settings, and documentation -- everything a Claude Code, GitHub Copilot, or OpenAI Codex project needs to enforce engineering standards from day one.

---

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Bundled Stack Profiles](#bundled-stack-profiles)
- [What's Generated](#whats-generated)
- [Generated Skills Reference](#generated-skills-reference)
  - [Story Planning & Decomposition](#story-planning--decomposition)
  - [Development Lifecycle](#development-lifecycle)
  - [Architecture & Design](#architecture--design)
  - [Testing](#testing)
  - [Code Review & Quality](#code-review--quality)
  - [Git & Operations](#git--operations)
  - [Conditional Skills](#conditional-skills)
- [Development](#development)
- [License](#license)

---

## Overview

`ia-dev-env` reads a YAML configuration file describing your project's tech stack (language, framework, database, infrastructure, etc.) and generates a complete set of AI assistant configurations:

- **Claude Code** (`.claude/`) -- rules, skills, agents, hooks, settings
- **GitHub Copilot** (`.github/`) -- instructions, skills, agents, prompts, hooks
- **OpenAI Codex** (`.codex/`, `.agents/`) -- config, agent instructions, shared skills
- **Documentation** (`docs/`) -- architecture templates, ADR index, runbook
- **CI/CD** -- Dockerfile, docker-compose, GitHub Actions workflows, Kubernetes manifests

All generated artifacts enforce consistent engineering standards: coding conventions, architecture boundaries, TDD workflow, coverage thresholds, and security practices.

## Prerequisites

- Java 21 or later

## Installation

### From JAR (recommended)

```bash
git clone https://github.com/edercnj/ia-dev-environment.git
cd ia-dev-environment/java
mvn clean package -DskipTests

java -jar target/ia-dev-env-2.0.0-SNAPSHOT.jar --help
```

### Using the wrapper script

```bash
chmod +x bin/ia-dev-env
./bin/ia-dev-env --help
```

The wrapper auto-detects Java 21 and sets appropriate JVM flags.

### GraalVM native image (optional)

```bash
cd java
mvn clean package -Pnative -DskipTests
./target/ia-dev-env --help
```

Produces a standalone binary with ~50ms startup time (no JVM required at runtime).

## Usage

### Generate from a bundled profile

```bash
ia-dev-env generate --stack java-spring --output ./my-project
```

### Generate from a custom config

```bash
ia-dev-env generate --config my-config.yaml --output ./my-project
```

### Generate interactively

```bash
ia-dev-env generate --interactive --output ./my-project
```

### Validate a config file

```bash
ia-dev-env validate --config my-config.yaml --verbose
```

### Dry run (preview without writing)

```bash
ia-dev-env generate --config my-config.yaml --dry-run
```

### CLI Reference

```
ia-dev-env [OPTIONS] COMMAND [ARGS]...

Commands:
  generate    Generate AI dev environment boilerplate
  validate    Validate a YAML configuration file

Global Options:
  -V, --version   Print version and exit
  -h, --help      Show help and exit

Generate Options:
  -c, --config <path>    Path to YAML config file
  -i, --interactive      Interactive mode (mutually exclusive with --config)
  -s, --stack <name>     Use a bundled stack profile (see profiles below)
  -o, --output <dir>     Output directory (default: current directory)
  -v, --verbose          Verbose logging
  -f, --force            Overwrite existing files without prompting
  --dry-run              Preview what would be generated

Validate Options:
  -c, --config <path>    Path to YAML config file (required)
  -v, --verbose          Per-category validation results
```

## Bundled Stack Profiles

8 ready-to-use profiles available via `--stack <name>`:

| Profile | Language | Framework | Database | Notes |
|---------|----------|-----------|----------|-------|
| `go-gin` | Go 1.22 | Gin | PostgreSQL | REST API |
| `java-quarkus` | Java 21 | Quarkus 3.17 | PostgreSQL | Cloud-native |
| `java-spring` | Java 21 | Spring Boot 3.4 | PostgreSQL | Enterprise |
| `kotlin-ktor` | Kotlin 2.0 | Ktor | PostgreSQL | Lightweight |
| `python-click-cli` | Python 3.9 | Click 8.1 | — | CLI tool |
| `python-fastapi` | Python 3.12 | FastAPI | PostgreSQL | Async API |
| `rust-axum` | Rust 2024 | Axum | PostgreSQL | High-performance |
| `typescript-nestjs` | TypeScript 5 | NestJS | PostgreSQL | Full-stack |

Each profile generates the complete set of skills, agents, and rules tailored to that stack.

## What's Generated

```
.claude/                          # Claude Code configuration
├── README.md                     # Auto-generated project guide
├── settings.json                 # Permissions and hooks
├── settings.local.json           # Local overrides template
├── rules/                        # Coding rules (loaded into system prompt)
├── skills/                       # Skills invocable via /command
│   └── {knowledge-packs}/        # Internal context for agents
├── agents/                       # AI personas (architect, tech-lead, etc.)
└── hooks/                        # Automation scripts (post-compile, etc.)

.github/                          # GitHub Copilot configuration
├── copilot-instructions.md       # Global Copilot instructions
├── instructions/                 # Contextual instructions
├── skills/                       # Reusable Copilot skills
├── agents/                       # Agent definitions (*.agent.md)
├── prompts/                      # Prompt templates
└── hooks/                        # Event hooks

.codex/                           # OpenAI Codex configuration
├── config.toml                   # Model, approval, sandbox settings
└── (AGENTS.md at project root)   # Consolidated agent instructions

.agents/                          # Shared skills (cross-platform)
└── skills/                       # Mirrored skills for Codex agents

docs/                             # Documentation templates
├── architecture/                 # Service architecture doc
├── adr/                          # ADR template and index
└── runbook/                      # Deploy runbook template
```

Plus CI/CD artifacts: `Dockerfile`, `docker-compose.yml`, `.github/workflows/ci.yml`, and Kubernetes manifests (when applicable).

---

## Generated Skills Reference

The generator produces **20 core skills** (always included) and up to **13 conditional skills** (based on your project config). Skills are invoked via `/skill-name` in the AI assistant chat. Each skill is self-contained -- the AI agent can execute it by reading only the SKILL.md file.

> **Note:** Skills are generated for both Claude Code (`.claude/skills/`) and GitHub Copilot (`.github/skills/`) with equivalent functionality. The descriptions below apply to both platforms.

### Story Planning & Decomposition

Skills for breaking down specifications into implementable work items.

#### `/x-story-epic-full` -- Complete Spec Decomposition

Orchestrates the full decomposition of a system specification into three deliverables: Epic, Stories, and Implementation Map.

| | |
|---|---|
| **When to use** | Starting a new feature or project from a specification document |
| **Input** | Path to a system specification file |
| **Output** | Epic file + individual story files + implementation map |

**What it does:**
1. Reads and analyzes the system specification
2. Extracts cross-cutting business rules (RULE-001..N)
3. Identifies stories by layer (Foundation → Core → Extensions → Compositions)
4. Maps dependencies between stories
5. Computes implementation phases and critical path
6. Generates all three deliverables in `docs/stories/epic-XXXX/`

**Generated artifacts:**
- `EPIC-XXXX.md` -- scope, rules table, DoR/DoD, story index
- `story-XXXX-YYYY.md` -- per-story files with data contracts, Gherkin, diagrams, sub-tasks
- `IMPLEMENTATION-MAP.md` -- dependency graph, phase diagram, critical path, bottleneck analysis

Internally delegates to `/x-story-epic`, `/x-story-create`, and `/x-story-map`.

---

#### `/x-story-epic` -- Generate Epic from Spec

| | |
|---|---|
| **When to use** | When you only need the Epic document, not the full decomposition |
| **Input** | Specification file |
| **Output** | `docs/stories/epic-XXXX/EPIC-XXXX.md` |

Generates the top-level Epic file with: scope overview, cross-cutting business rules table, global DoR/DoD (including TDD compliance requirements), and story index with dependency declarations.

---

#### `/x-story-create` -- Generate Story Files

| | |
|---|---|
| **When to use** | When the Epic exists but individual stories need to be generated |
| **Input** | Epic file with story index |
| **Output** | Individual `story-XXXX-YYYY.md` files |

Each generated story includes:
- Dependency table (Blocked By / Blocks)
- Applicable cross-cutting rules
- Technical description with architecture context
- Local DoR/DoD + global DoD
- Data contracts (request/response fields with M/O notation)
- Mermaid sequence diagrams
- Gherkin acceptance criteria (minimum 4 scenarios in TPP order)
- Tagged sub-tasks: `[Dev]`, `[Test]`, `[Doc]`

---

#### `/x-story-map` -- Generate Implementation Map

| | |
|---|---|
| **When to use** | When Epic and Stories exist but you need the execution plan |
| **Input** | Epic directory with story files |
| **Output** | `IMPLEMENTATION-MAP.md` |

Produces:
- Dependency matrix (story → blocked by / blocks)
- ASCII phase diagram showing parallel execution opportunities
- Mermaid dependency graph (color-coded by phase)
- Critical path identification with bottleneck analysis
- Phase summary with parallelism counts
- Strategic observations: bottleneck stories, leaf stories, convergence points

---

### Development Lifecycle

Skills for implementing features from planning through PR creation.

#### `/x-dev-lifecycle` -- Full Feature Lifecycle

The main orchestrator skill. Takes a story from branch creation to merged PR in 9 phases.

| | |
|---|---|
| **When to use** | Implementing a complete feature with review and PR |
| **Input** | Story ID or feature description |
| **Output** | Branch with implementation, reviews, and PR |

**Phases:**

| Phase | Name | Description |
|-------|------|-------------|
| 0 | Preparation | Branch creation, dependency validation |
| 1 | Architecture Planning | Component diagrams, ADRs, NFRs (via `/x-dev-architecture-plan`) |
| 1B-1E | Parallel Planning | Test plan, task decomposition, event schema, compliance (parallel subagents) |
| 2 | TDD Implementation | Red-Green-Refactor cycles with compile checks (subagent) |
| 3 | Documentation | API docs, changelogs, architecture updates |
| 4 | Specialist Review | Security, QA, Performance, DevOps reviews (via `/x-review`) |
| 5-6 | Fixes + Push | Fix review findings, push, create PR |
| 7 | Tech Lead Review | 45-point checklist review (via `/x-review-pr`) |
| 8 | Final Verification | DoD checklist, smoke tests |

---

#### `/x-dev-implement` -- Quick TDD Implementation

| | |
|---|---|
| **When to use** | Quick implementation without the full review lifecycle |
| **Input** | Story ID or feature description |
| **Output** | TDD implementation with atomic commits |

**How it differs from `/x-dev-lifecycle`:**

| Scenario | Use |
|----------|-----|
| Single class, small fix, coding without reviews | `/x-dev-implement` |
| Complete lifecycle: code → review → fix → PR | `/x-dev-lifecycle` |

**Workflow:**
1. **Prepare** -- Subagent reads architecture, coding standards, and test plan knowledge packs
2. **TDD Loop** -- For each scenario in TPP order: RED (write failing test) → GREEN (minimum code to pass) → REFACTOR (improve design) → compile check
3. **Validate** -- Coverage thresholds (≥95% line, ≥90% branch), all acceptance tests green
4. **Commit** -- One atomic commit per Red-Green-Refactor cycle

---

#### `/x-dev-epic-implement` -- Epic Orchestration

| | |
|---|---|
| **When to use** | Implementing an entire epic (multiple stories) |
| **Input** | Epic ID with optional flags |
| **Output** | All stories implemented, reviewed, and consolidated |

```
/x-dev-epic-implement EPIC-0007
/x-dev-epic-implement EPIC-0007 --story story-0007-0003    # single story
/x-dev-epic-implement EPIC-0007 --resume                    # resume from checkpoint
/x-dev-epic-implement EPIC-0007 --parallel                  # parallel worktrees
/x-dev-epic-implement EPIC-0007 --dry-run                   # preview only
```

**Features:**
- Dependency-aware execution order (respects IMPLEMENTATION-MAP.md phases)
- Checkpoint system with resume capability
- Integrity gates between phases
- Parallel worktree support for independent stories
- Per-story dispatch to `/x-dev-lifecycle`
- Epic-level Tech Lead review on full diff
- Final status: COMPLETE / PARTIAL / FAILED

---

### Architecture & Design

Skills for architecture planning, documentation, and decision records.

#### `/x-dev-architecture-plan` -- Architecture Plan

| | |
|---|---|
| **When to use** | Before implementing a feature that changes architecture |
| **Input** | Story ID or feature name |
| **Output** | `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` |

Generates a comprehensive architecture plan:
- Component diagrams (Mermaid)
- Sequence diagrams for key flows
- Deployment topology diagram
- Mini-ADRs (Context / Decision / Rationale)
- Non-functional requirements with measurable targets
- Observability strategy (metrics, traces, logs)
- Resilience strategy (circuit breakers, retries, fallback)
- Impact analysis with migration and rollback planning

Automatically evaluates scope (Full / Simplified / Skip) based on change impact.

---

#### `/x-dev-arch-update` -- Update Architecture Docs

| | |
|---|---|
| **When to use** | After implementing a feature, to keep architecture docs current |
| **Input** | Story ID or architecture plan path |
| **Output** | Updated `docs/architecture/service-architecture.md` |

Reads the architecture plan and incrementally updates the service architecture document. **Appends only** -- never rewrites existing content. Updates the Change History section with date, story ID, and summary.

---

#### `/x-dev-adr-automation` -- ADR Generation

| | |
|---|---|
| **When to use** | After creating an architecture plan with mini-ADRs |
| **Input** | Architecture plan path + Story ID |
| **Output** | `docs/adr/ADR-NNNN-title.md` files + updated index |

Extracts `### ADR:` markers from architecture plans, expands them to full ADR format (YAML frontmatter + Context/Decision/Consequences), assigns sequential numbering, checks for duplicates, updates the ADR index, and adds cross-references back to the source plan.

---

### Testing

Skills for test planning and execution.

#### `/x-test-plan` -- Generate Test Plan

| | |
|---|---|
| **When to use** | Before implementation, to define the TDD roadmap |
| **Input** | Story ID |
| **Output** | `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` |

Generates a Double-Loop TDD test plan:
- **Outer loop (Acceptance Tests):** AT-1..N mapping to Gherkin scenarios -- these start RED and drive the implementation
- **Inner loop (Unit Tests):** UT-1..N in strict TPP order (degenerate → constant → scalar → collection → complex)
- **Integration Tests:** IT-1..N for database and framework interactions
- Coverage estimation table per test category

---

#### `/x-test-run` -- Run Tests with Coverage

| | |
|---|---|
| **When to use** | Running tests, checking coverage, finding gaps |
| **Input** | Optional: class name, package, or `--coverage` |
| **Output** | Test results + coverage report + gap analysis |

```
/x-test-run                    # run all tests with coverage
/x-test-run UserServiceTest    # run specific test class
/x-test-run --coverage         # focus on coverage analysis
```

Executes tests, collects coverage report, analyzes per-class coverage (line % and branch %), identifies uncovered branches/exception paths, and provides specific remediation suggestions for gaps.

---

### Code Review & Quality

Skills for automated code review and quality auditing.

#### `/x-review` -- Parallel Specialist Review

| | |
|---|---|
| **When to use** | Pre-PR quality validation with multiple specialists |
| **Input** | Story ID or `--scope reviewer1,reviewer2` |
| **Output** | Per-engineer review reports + consolidated summary |

Launches **parallel subagents**, one per engineering specialty:

| Specialist | Focus Area | Condition |
|-----------|------------|-----------|
| Security | Vulnerabilities, auth, data exposure | Always |
| QA | Test quality, coverage, edge cases | Always |
| Performance | Latency, throughput, resource usage | Always |
| Database | Queries, indexes, migrations | If database configured |
| Observability | Metrics, traces, logging | If observability configured |
| DevOps | Containers, deployments, infra | If container/orchestrator configured |
| API | REST/gRPC/GraphQL design | If API interfaces configured |
| Event | Event schemas, async flows | If event-driven |

Each specialist reads their dedicated knowledge pack and produces findings with severity scores. The orchestrator consolidates all findings into a scored summary and optionally generates a correction story for CRITICAL/MEDIUM issues.

---

#### `/x-review-pr` -- Tech Lead Review

| | |
|---|---|
| **When to use** | Final review before merge (GO/NO-GO decision) |
| **Input** | PR number, Story ID, or current branch |
| **Output** | Review report with score and GO/NO-GO decision |

```
/x-review-pr           # review current branch vs main
/x-review-pr 123       # review PR #123
/x-review-pr STORY-ID  # review by story ID
```

Applies a **45-point checklist** across 11 dimensions:

| Section | Points | Covers |
|---------|--------|--------|
| A. Clean Code | 5 | Naming, method length, complexity |
| B. SOLID | 5 | SRP, OCP, LSP, ISP, DIP |
| C. Architecture | 5 | Layer boundaries, dependency direction |
| D. Framework | 4 | Idiomatic usage, injection patterns |
| E. Tests | 5 | Coverage, naming, isolation |
| F. TDD Process | 5 | Test-first evidence in git history |
| G. Security | 4 | OWASP Top 10, input validation |
| H. Error Handling | 3 | No null returns, context in exceptions |
| I. Performance | 3 | N+1 queries, resource leaks |
| J. Observability | 3 | Metrics, traces, structured logging |
| K. Cross-File | 3 | Consistency, unused code, duplication |

**Decision:** GO (≥38/45) or NO-GO (<38/45) with detailed findings.

---

#### `/x-codebase-audit` -- Full Codebase Audit

| | |
|---|---|
| **When to use** | Periodic quality assessment of the entire codebase |
| **Input** | Optional: `--scope all\|rules\|patterns\|architecture\|security\|cross-file` |
| **Output** | `docs/audits/codebase-audit-YYYY-MM-DD.md` |

Like `/x-review-pr` but for the **entire codebase**, not just a single PR. Launches parallel subagents for 6 audit dimensions:

1. **Clean Code & SOLID** -- method/class length, parameter count, boolean flags, SRP/OCP/LSP/ISP/DIP
2. **Architecture Layers** -- domain importing from adapter, circular dependencies, framework leaks
3. **Coding Standards** -- language-specific conventions, null handling, exception patterns
4. **Test Quality & TDD** -- coverage gaps, naming violations, mocked domain logic
5. **Security** -- SQL/XSS/command injection, hardcoded secrets, missing validation
6. **Cross-File Consistency** -- duplicated logic, inconsistent patterns, orphaned files

**Scoring:** Starts at 100, deducts per finding (CRITICAL: -10, MEDIUM: -3, LOW: -1). Final score 0-100.

---

#### `/x-dependency-audit` -- Dependency Audit

| | |
|---|---|
| **When to use** | Checking dependencies for security, freshness, and licensing |
| **Input** | Optional: `--scope all\|vulnerabilities\|outdated\|licenses` |
| **Output** | `docs/audits/dependency-audit-YYYY-MM-DD.md` |

Audits three dimensions with language-specific commands:

| Dimension | npm | Maven | Cargo | pip | Go |
|-----------|-----|-------|-------|-----|-----|
| Vulnerabilities | `npm audit` | ossindex plugin | `cargo audit` | `pip-audit` | `govulncheck` |
| Outdated | `npm outdated` | versions plugin | `cargo outdated` | `pip list --outdated` | `go list -m -u` |
| Licenses | `license-checker` | license plugin | `cargo license` | `pip-licenses` | `go-licenses` |

Categorizes findings by severity (CRITICAL/HIGH/MEDIUM/LOW) and generates remediation recommendations.

---

#### `/x-fix-pr-comments` -- Fix PR Review Comments

| | |
|---|---|
| **When to use** | After receiving PR review feedback |
| **Input** | PR number (or auto-detects from current branch) |
| **Output** | Fixed code + commits + replies to comment threads |

```
/x-fix-pr-comments         # fix comments on current branch's PR
/x-fix-pr-comments 123     # fix comments on PR #123
```

**Workflow:**
1. Fetches all review comments via `gh api`
2. Classifies each comment: Actionable / Suggestion / Question / Praise / Resolved
3. Implements fixes for actionable and accepted suggestions
4. Compiles and tests after each fix
5. **Replies to each comment thread in Portuguese (pt-BR):**
   - Fixed: `Corrigido. {descricao da alteracao}. Commit: {hash}`
   - Rejected: `Observacao analisada, mas nao faz sentido neste contexto. Motivo: {explicacao}`
   - Failed: `Tentei corrigir, mas a alteracao causou falha. Necessita intervencao manual.`
6. Commits with `fix(scope): address PR review comments`
7. Generates a summary report table

---

### Git & Operations

Skills for git workflow, troubleshooting, changelog, and MCP server management.

#### `/x-git-push` -- Git Workflow

| | |
|---|---|
| **When to use** | Any git operation: branching, committing, pushing, creating PRs |
| **Input** | Branch name or commit message |
| **Output** | Clean git history with atomic commits |

Standardizes the complete git workflow:
- **Branch naming:** `feat/story-XXXX-YYYY-short-description`
- **Commits:** Conventional Commits format (`type(scope): subject`)
- **TDD commits:** Optional markers `[TDD:RED]`, `[TDD:GREEN]`, `[TDD:REFACTOR]`
- **PR creation:** Via `gh pr create` with standardized title and body
- **Release tagging:** Semantic versioning tags

---

#### `/x-ops-troubleshoot` -- Diagnose & Fix Issues

| | |
|---|---|
| **When to use** | Compilation errors, test failures, runtime exceptions, build failures |
| **Input** | Error description or test name |
| **Output** | Root cause diagnosis + fix with regression test |

Systematic 5-step debug workflow:

```
1. REPRODUCE  → Get the exact error
2. LOCATE     → Find where it originates
3. UNDERSTAND → Why is it failing?
4. FIX        → Write a test that reproduces the bug, then fix
5. VERIFY     → Run full test suite, no regressions
```

Includes diagnostic tables for compilation errors, test failures, build failures, runtime errors, and performance issues -- all adapted to the project's language and build tool.

---

#### `/x-changelog` -- Generate Changelog

| | |
|---|---|
| **When to use** | Preparing a release or documenting changes |
| **Input** | Version tag (e.g., `v1.2.0`) or `--unreleased` |
| **Output** | Updated `CHANGELOG.md` |

```
/x-changelog                # unreleased changes since last tag
/x-changelog v1.2.0         # changelog for specific version
/x-changelog --full          # regenerate entire changelog
```

Parses `git log` for Conventional Commits and maps to [Keep a Changelog](https://keepachangelog.com/) sections:

| Commit Type | Changelog Section |
|-------------|------------------|
| `feat` | Added |
| `fix` | Fixed |
| `refactor`, `perf` | Changed |
| `BREAKING CHANGE` | Breaking Changes |
| `deprecate` | Deprecated |

Performs **incremental updates** -- inserts new version section while preserving existing entries. Includes commit hash links and version comparison URLs.

---

#### `/x-mcp-recommend` -- MCP Server Recommendations

| | |
|---|---|
| **When to use** | Setting up MCP servers for your AI development environment |
| **Input** | None (auto-detects from project config) |
| **Output** | Recommendations report + optional config file updates |

```
/x-mcp-recommend             # analyze and recommend
/x-mcp-recommend --install   # recommend and auto-configure
```

Auto-detects your tech stack and matches against a built-in catalog of 20+ MCP servers:

| Category | Servers |
|----------|---------|
| Database | PostgreSQL, MySQL, MongoDB, SQLite, Redis |
| DevOps | Docker, Kubernetes, AWS, Terraform |
| Productivity | GitHub, Slack, Linear, Jira |
| Development | Puppeteer, Filesystem, Memory, Fetch |
| Observability | Sentry, Datadog, Grafana |

Each recommendation includes priority (Essential / Recommended / Optional), rationale, and installation instructions for both Claude Code and GitHub Copilot. With `--install`, auto-updates `.claude/settings.local.json` and `.github/copilot-mcp.json`.

---

### Conditional Skills

These skills are generated only when your project config includes the relevant technology. They are invoked the same way as core skills.

#### Testing (conditional)

| Skill | Condition | Description |
|-------|-----------|-------------|
| `/run-e2e` | Always (if testing enabled) | End-to-end tests validating full flow with real database |
| `/run-smoke-api` | REST interfaces | Black-box smoke tests against running API |
| `/run-smoke-socket` | WebSocket interfaces | WebSocket connection and message validation |
| `/run-contract-tests` | Contract tests enabled | Parametrized business rule validation |
| `/run-perf-test` | Performance tests enabled | Latency SLAs, throughput, resource usage |

#### Review (conditional)

| Skill | Condition | Description |
|-------|-----------|-------------|
| `/x-review-api` | REST interfaces | REST API design review (RFC 7807, pagination, versioning) |
| `/x-review-grpc` | gRPC interfaces | gRPC service review (protobuf, streaming, error codes) |
| `/x-review-events` | Event-driven | Event schema review (naming, versioning, dead letters) |
| `/x-review-gateway` | API gateway | Gateway routing, rate limiting, auth review |
| `/x-review-graphql` | GraphQL interfaces | Schema design, N+1 queries, pagination review |
| `/x-review-security` | Security config | OWASP Top 10, auth flows, data exposure review |

#### Infrastructure (conditional)

| Skill | Condition | Description |
|-------|-----------|-------------|
| `/setup-environment` | Orchestrator configured | Environment setup (K8s namespaces, secrets, service mesh) |
| `/instrument-otel` | Observability configured | OpenTelemetry instrumentation (metrics, traces, logs) |

---

### Internal Skills (not user-invocable)

The following are **not** shown in the `/` menu. They are used internally by other skills:

| Internal Skill | Used By |
|---------------|---------|
| `x-lib-task-decomposer` | `/x-dev-lifecycle` (Phase 1B) |
| `x-lib-audit-rules` | `/x-codebase-audit`, `/x-review-pr` |
| `x-lib-group-verifier` | `/x-dev-epic-implement` |

**Knowledge packs** (architecture, coding-standards, testing, security, etc.) are also internal -- they provide domain knowledge to agents during skill execution but are never invoked directly.

---

## Development

```bash
cd java

# Build
mvn clean package

# Run tests
mvn test

# Run tests with coverage report
mvn verify

# Run only unit tests
mvn test -Punit-tests

# Run integration tests (golden file parity)
mvn verify -Pintegration-tests

# Run all tests
mvn verify -Pall-tests
```

### Project Structure

```
ia-dev-environment/
├── java/                         # Java 21 source
│   ├── pom.xml                   # Maven build (JUnit 5, JaCoCo, Picocli, Pebble)
│   ├── bin/                      # Wrapper script with Java 21 detection
│   ├── src/main/java/dev/iadev/
│   │   ├── cli/                  # Picocli commands (generate, validate)
│   │   ├── config/               # YAML loading, profiles, context builder
│   │   ├── model/                # 17 immutable data records
│   │   ├── domain/               # Stack resolution, DAG, implementation map
│   │   ├── assembler/            # 23 assemblers (rules, skills, agents, ...)
│   │   ├── template/             # Pebble engine with Python-bool filter
│   │   ├── checkpoint/           # Execution state management
│   │   ├── progress/             # Metrics and reporting
│   │   ├── exception/            # 7 context-rich exceptions
│   │   └── util/                 # I/O, path safety, resource discovery
│   ├── src/main/resources/       # ~470 template files on classpath
│   │   ├── config-templates/     # 8 bundled stack profiles (YAML)
│   │   ├── skills-templates/     # Claude Code skill templates
│   │   ├── github-skills-templates/  # GitHub Copilot skill templates
│   │   └── templates/            # Pebble/Nunjucks templates
│   └── src/test/
│       ├── java/                 # 1961 tests (unit + integration + golden)
│       └── resources/golden/     # Golden files for 8 profiles
├── docs/                         # Stories, specs, epics
│   ├── specs/                    # System specifications
│   └── stories/                  # Epic stories and implementation maps
├── CLAUDE.md                     # Executive summary (auto-loaded by Claude Code)
├── AGENTS.md                     # Codex agent instructions
└── README.md                     # This file
```

### Coverage

| Metric | Minimum | Current |
|--------|---------|---------|
| Line Coverage | ≥ 95% | 95.23% |
| Branch Coverage | ≥ 90% | 91.12% |

Enforced by JaCoCo in `mvn verify`. Build fails if thresholds are not met.

### Regenerating Golden Files

When templates change, regenerate the golden files used for byte-for-byte parity tests:

```bash
cd java
mvn compile test-compile
java -cp target/classes:target/test-classes:$(mvn dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout) \
  dev.iadev.golden.GoldenFileRegenerator
mvn test
```

## License

MIT
