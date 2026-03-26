
# my-java-cli

Describe your CLI tool purpose here

## Architecture

- **Style:** library
- **Language:** java 21
- **Framework:** picocli 4.7

### Dependency Direction

Dependencies point inward toward the domain. Domain NEVER imports adapter or framework code.

```
adapter.inbound → application → domain ← adapter.outbound
                                  ↑
                           (ports/interfaces)
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | java 21 |
| Framework | picocli 4.7 |
| Build Tool | maven |
| Container | docker |

## Commands

| Command | Script |
|---------|--------|
| Build | `./mvnw package -DskipTests` |
| Test | `./mvnw verify` |
| Compile | `./mvnw compile -q` |
| Coverage | `./mvnw verify jacoco:report` |

## Coding Standards

### Hard Limits

| Constraint | Limit |
|-----------|-------|
| Method/function length | ≤ 25 lines |
| Class/module length | ≤ 250 lines |
| Parameters per function | ≤ 4 (use parameter object if more) |
| Line width | ≤ 120 characters |

### Naming

- Intent-revealing names: `elapsedTimeInMs` not `d`
- Verbs for methods: `processTransaction()`, `extractAmount()`
- Nouns for types: `TransactionHandler`, `DecisionEngine`
- Named constants: never magic numbers or strings

### SOLID Principles

- **SRP**: One class = one reason to change
- **OCP**: New behavior = new class, never modify existing handlers
- **LSP**: Every implementation must fulfill its interface contract
- **ISP**: Small focused interfaces; no empty method implementations
- **DIP**: Depend on abstractions (ports), not concrete implementations

### Error Handling

- NEVER return null — use Optional, empty collection, or Result type
- NEVER pass null as argument
- Exceptions MUST carry context (values that caused the error)
- Catch at the right level, not where convenient

### Forbidden Patterns

- Boolean flags as function parameters
- Comments that repeat what code says
- Mutable global state
- God classes / train wrecks (chained calls across objects)
- Wildcard imports
- `sleep()` for synchronization

### TDD Practices

- **Red-Green-Refactor** is mandatory for all production code
- Refactoring criteria: extract method when > 25 lines, eliminate duplication, improve naming
- Refactoring NEVER adds behavior
- Full TDD reference: `skills/testing/SKILL.md`

### Language-Specific

Follow java 21 idiomatic conventions for formatting, constructor injection, and mapper patterns.

## Quality Gates

### Coverage Thresholds

| Metric | Minimum |
|--------|---------|
| Line Coverage | ≥ 95% |
| Branch Coverage | ≥ 90% |

### Test Categories

- **Unit** — domain models, engines, business rules
- **Integration** — database + framework
- **API** — HTTP/gRPC/GraphQL endpoints
- **E2E** — full flow with real database
- **Smoke** — black-box against running environment

### Test Naming

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

### Merge Checklist

- [ ] All tests passing
- [ ] Coverage ≥ 95% line, ≥ 90% branch
- [ ] Zero compiler/linter warnings
- [ ] Security review for sensitive changes
- [ ] Commits show test-first pattern (test precedes implementation in git log)
- [ ] Explicit refactoring after green
- [ ] Tests are incremental (simple to complex via TPP)
- [ ] Tests precede or accompany implementation (no test-after in later commits)
- [ ] Acceptance tests exist and validate end-to-end behavior

### TDD Compliance

- **Double-Loop TDD**: Outer loop (acceptance test) drives inner loop (unit tests)
- **TPP**: Order tests from simple to complex
- **Atomic TDD commits**: Each cycle produces atomic commits

## Domain

This project follows Domain-Driven Design principles.

### Domain Model

Define your core entities, value objects, and aggregates here.

### Aggregate Boundaries

Each aggregate enforces its own invariants. Cross-aggregate communication happens through domain events or application services.

### Business Rules

Document business rules with unique identifiers (e.g., BR-001) for traceability.

## Conventions

### Commits

- Format: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`)
- Language: English

### Branches

- Feature: `feat/STORY-ID-description`
- Fix: `fix/STORY-ID-description`
- Chore: `chore/description`

### Code Language

- All code (classes, methods, variables): English
- All documentation: English
- Application logs: English

### Documentation

- Keep documentation close to the code it describes
- Update documentation when changing behavior

## Available Skills

| Skill | Description |
|-------|-------------|
| x-changelog | Generates CHANGELOG.md from Conventional Commits history. Parses git log, groups by commit type, maps to Keep a Changelog sections (Added, Changed, Fixed, etc.), and performs incremental updates preserving existing entries. |
| x-codebase-audit | Full codebase review against all project standards. Launches parallel subagents per audit dimension (Clean Code, SOLID, Architecture, Tests, Security, Cross-file), consolidates findings into a severity-categorized report with score. Use for periodic quality validation. |
| x-dependency-audit | Checks project dependencies for vulnerabilities, outdated versions, and license issues. Detects build tool automatically, runs language-specific audit commands, and generates a severity-categorized report. |
| x-dev-adr-automation | Automates ADR generation from architecture plan mini-ADRs: extracts inline decisions, expands to full ADR format, assigns sequential numbering, updates the ADR index, and adds cross-references. |
| x-dev-arch-update | Incrementally updates the service architecture document with changes from architecture plans. Adds new components, integrations, flows, and ADR references without rewriting existing content. Use after implementation to keep architecture documentation current. |
| x-dev-architecture-plan | Generates a comprehensive architecture plan with component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability strategies. Use before implementation to document design decisions. |
| x-dev-epic-implement | Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents. |
| x-dev-implement | Implements a feature/story using TDD (Red-Green-Refactor) workflow. Delegates preparation to a subagent that reads architecture, coding, and test plan KPs, then implements test-first with Double-Loop TDD, layer-by-layer with compile checks after each cycle. |
| x-dev-lifecycle | Orchestrates the complete feature implementation cycle: branch creation, planning, task decomposition, implementation, parallel review, fixes, PR creation, and final verification. Delegates heavy phases to subagents for context efficiency. |
| x-fix-pr-comments | Reads PR review comments and fixes actionable ones automatically. Detects PR from argument or branch, classifies comments (actionable/suggestion/question/praise), implements fixes, and commits with proper conventional commit messages. |
| x-jira-create-epic | Creates a Jira Epic from an existing local epic markdown file. Reads the epic file, maps fields to Jira, creates the issue, and syncs the Jira key back to the local file. Use when the user has an existing epic file and wants to create it in Jira, or when the user says "create this epic in Jira", "sync epic to Jira", or "push epic to Jira". |
| x-jira-create-stories | Creates Jira Stories from existing local story markdown files. Reads all story files in an epic directory, maps fields to Jira, creates issues with parent epic link, creates dependency links between stories, and syncs Jira keys back to local files. Use when the user has existing story files and wants to create them in Jira, or when the user says "create stories in Jira", "sync stories to Jira", or "push stories to Jira". |
| x-mcp-recommend | Analyzes project tech stack and recommends relevant MCP (Model Context Protocol) servers. Auto-detects language, framework, database, cache, and message broker from project config, then matches against a built-in catalog of MCP servers with installation instructions. |
| x-story-create | Generate detailed User Story files from an Epic and system specification. This skill reads an Epic file (with its story index and rules table) and the original system spec, then produces one file per story with full data contracts, Gherkin acceptance criteria, Mermaid sequence diagrams, dependency declarations, and tagged sub-tasks. Use this skill whenever the user asks to create stories, generate user stories from an epic, detail stories with acceptance criteria, write Gherkin scenarios for a spec, create story files with data contracts, or any variation of "generate stories from this epic/spec". Also trigger when the user mentions writing acceptance criteria, detailing technical stories, creating story files with contracts and diagrams, or breaking an epic into implementable stories — even if they don't use the word "story" explicitly. |
| x-story-epic | Generate an Epic document from a system specification file. This skill reads a technical spec (following the _TEMPLATE.md format) and produces an Epic file with cross-cutting business rules, global quality definitions (DoR/DoD), and a complete story index with dependency declarations. Use this skill whenever the user asks to create an epic, generate an epic from a spec, extract business rules from a system document, decompose a specification into an epic, build a story index, or any variation of "read this spec and create an epic". Also trigger when the user mentions extracting cross-cutting rules, defining quality gates for a project, or building a story backlog from a technical document — even if they don't use the word "epic" explicitly. |
| x-story-epic-full | Complete decomposition of a system specification into an Epic, individual Story files, and an Implementation Map with dependency graph and phased execution plan. This is the orchestrator skill that guides the full workflow: spec analysis, rule extraction, story identification, and implementation planning. Use this skill whenever the user asks to decompose a spec into stories and epic, break down a system document into implementable work items, generate a complete project backlog from a specification, create epic stories and implementation plan from a technical document, or any variation of "read this spec and create everything". Also trigger when the user wants the full decomposition pipeline — epic + stories + map — in a single pass, or mentions planning the complete implementation of a system from its specification. Prefer this skill over the individual x-story-epic, x-story-create, or x-story-map skills when the user wants all three deliverables. |


