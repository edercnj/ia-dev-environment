---
name: x-dev-lifecycle
description: "Orchestrates the complete feature implementation cycle: branch creation, planning, task decomposition, implementation, parallel review, fixes, PR creation, and final verification. Delegates heavy phases to subagents for context efficiency."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[STORY-ID or feature-name]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Feature Lifecycle (Orchestrator)

## When to Use

- Full story/feature implementation with review cycle
- End-to-end development workflow (plan → code → review → PR)

## CRITICAL EXECUTION RULE

**9 phases (0-8). ALL mandatory. NEVER stop before Phase 8.**

After each phase 0–7: `>>> Phase N/8 completed. Proceeding to Phase N+1...`
After Phase 8: `>>> Phase 8/8 completed. Lifecycle complete.`

## Complete Flow

```
Phase 0: Preparation          (orchestrator — inline)
{% if has_contract_interfaces == 'True' %}Phase 0.5: API-First Contract  (orchestrator — conditional, pauses for approval)
{% endif %}Phase 1: Planning              (subagent — reads architecture KPs)
Phase 1B-1E: Parallel Planning (up to 4 subagents — SINGLE message)
Phase 2: Implementation        (subagent — reads coding + layer KPs)
Phase 3: Documentation         (orchestrator — inline)
Phase 4: Review                (invoke /x-review skill — launches its own subagents)
Phase 5-6: Fixes + PR          (orchestrator — inline)
Phase 7: Tech Lead Review      (invoke /x-review-pr skill)
Phase 8: Verification          (orchestrator — inline)
```

---

## Phase 0 — Preparation (Orchestrator — Inline)

1. Read story file and extract acceptance criteria, sub-tasks, dependencies
2. Verify dependencies (predecessor stories complete)
3. Check if test plan exists at `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`
   - If present: Phase 2 will use TDD mode
   - If absent: Phase 1B will produce it; if 1B also fails, Phase 2 falls back to G1-G7
4. Check if architecture plan exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
   - If present: Phase 1 will skip architecture planning (use existing plan)
   - If absent: Phase 1 will evaluate decision tree and invoke x-dev-architecture-plan
5. Extract epic ID from story ID (e.g., `story-0001-0003` → epic ID `0001`)
6. Ensure directories exist: `mkdir -p plans/epic-XXXX/plans plans/epic-XXXX/reviews`
7. Create branch: `git checkout -b feat/{STORY_ID}-description`
8. **Scope Assessment** — classify the story to determine lifecycle phase optimization:

### Scope Assessment

Analyze the story content to classify its scope tier. The classification determines which phases execute:

**Classification Criteria:**

| Criterion | How to detect |
|-----------|--------------|
| Components affected | Count distinct `.java`/`.kt`/`.py`/`.ts`/`.go`/`.rs` file mentions in tech description |
| New endpoints | Count `POST/GET/PUT/DELETE/PATCH /path` patterns in data contracts |
| Schema changes | Presence of "migration script", "ALTER TABLE", "CREATE TABLE", "DROP TABLE", "ADD COLUMN" |
| Compliance | `compliance:` field with value other than "none" |
| Dependents | Count stories that depend on this one (from IMPLEMENTATION-MAP) |

**Tier Classification:**

| Tier | Criteria | Phase Behavior |
|------|----------|---------------|
| SIMPLE | ≤1 component, 0 endpoints, 0 schema changes, no compliance | Skip phases 1B, 1C, 1D, 1E |
| STANDARD | 2-3 components OR 1-2 new endpoints | All phases execute normally |
| COMPLEX | ≥4 components OR schema changes OR compliance requirement | All phases + stakeholder review after Phase 4 |

**Elevation Rules:**
- Compliance **always** elevates to COMPLEX regardless of other criteria
- Schema changes **always** elevate to at least COMPLEX
- A single COMPLEX criterion is sufficient for COMPLEX classification

**Display the assessment before proceeding:**

```
Scope Assessment: [TIER]
> [Phases that will execute]
> Rationale: [justification]
> [Override instruction if SIMPLE]
```

**`--full-lifecycle` Flag:**
If the user passes `--full-lifecycle`, force full execution regardless of tier:
- All phases execute (equivalent to STANDARD)
- Display: "Scope override: running full lifecycle as requested"

**SIMPLE Execution Flow:**
Phases 1A (Prepare) > 2 (TDD) > 4 (Docs) > 5 (PR) > 6 (Verify) — skips 1B, 1C, 1D, 1E, 3 (Review)

**COMPLEX Execution Flow:**
All phases 1A through 4 execute normally. After Phase 4, **pause** with:
"Scope COMPLEX — stakeholder review required. Review the implementation and confirm to proceed with PR creation."
Wait for developer confirmation before executing Phases 5-6.

**Default Behavior:**
If scope assessment cannot be performed (e.g., story content unavailable), default to STANDARD (all phases execute). No error is raised.
{% if has_contract_interfaces == 'True' %}

## Phase 0.5 — API-First Contract Generation (Orchestrator — Conditional)

> **RULE-005:** Formal contract before implementation. This phase generates and validates
> API contracts (OpenAPI 3.1, AsyncAPI 2.6, Protobuf 3) before any implementation code.

**Activation:** This phase is ONLY executed when the story declares interfaces of type
`rest`, `grpc`, `event-consumer`, `event-producer`, or `websocket`. Stories without
these interface types skip directly to Phase 1.

### Step 0.5.1 — Interface Detection

Read the story file and identify declared interface types:

| Interface Type | Contract Format | Output Path |
|---------------|----------------|-------------|
| `rest` | OpenAPI 3.1 | `contracts/{STORY_ID}-openapi.yaml` |
| `grpc` | Protobuf 3 | `contracts/{STORY_ID}.proto` |
| `event-consumer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `event-producer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `websocket` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |

### Step 0.5.2 — Contract Generation

Generate a draft contract in the appropriate format using data contracts from the story:

- **REST (OpenAPI 3.1):** Extract endpoints, DTOs, status codes from story data contracts.
  Generate `openapi: "3.1.0"` spec with `info`, `paths`, `components/schemas`, RFC 7807 errors.
- **gRPC (Protobuf 3):** Extract service definitions, request/response messages.
  Generate `.proto` file with `syntax = "proto3"`, service, and message definitions.
- **Event (AsyncAPI 2.6):** Extract event names, channels, payload schemas.
  Generate `asyncapi: "2.6.0"` spec with `channels`, `components/messages`, `components/schemas`.

Ensure directory exists: `mkdir -p contracts/`

### Step 0.5.3 — Contract Validation

Invoke `/x-contract-lint {CONTRACT_PATH}` to validate the generated contract against
its specification. If validation errors are found:
1. Fix the errors in the generated contract
2. Re-run validation until the contract passes

### Step 0.5.4 — Approval Gate

Emit the following message and **pause the lifecycle**:

```
CONTRACT PENDING APPROVAL

Contract generated: {CONTRACT_PATH}
Format: {CONTRACT_FORMAT}
Status: PENDING_APPROVAL

Please review the contract and respond with:
  - APPROVE: Proceed to Phase 1 (Architecture Planning)
  - REJECT: Return to Step 0.5.2 for regeneration with feedback
```

- **On APPROVE:** Set `contractStatus = APPROVED` and proceed to Phase 1.
- **On REJECT:** Return to Step 0.5.2, incorporating user feedback into regeneration.
{% endif %}

## Phase 1 — Architecture Planning (Skill Invocation + Subagent Fallback)

**If the architecture plan file already exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` (as checked in Phase 0), skip Step 1A and proceed directly to Step 1B, ensuring Step 1B reads the existing plan.**

### Step 1A: Architecture Plan via x-dev-architecture-plan

Evaluate change scope using the decision tree:

| Condition | Plan Level |
|-----------|-----------|
| New service / new integration / contract change / infra change | **Full** |
| New feature, no contract or infra change | **Simplified** |
| Bug fix / refactor / docs-only | **Skip** |

**If Full or Simplified:**

Invoke skill `/x-dev-architecture-plan {STORY_PATH}`.

- Output: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
- If the skill invocation fails: emit `WARNING: Architecture plan generation failed. Continuing without architecture plan.` and proceed to Step 1B.

**If Skip:**

Log `"Architecture plan not needed for this change scope"` and proceed to Step 1B.

### Step 1B: Implementation Plan (Subagent via Task)

Launch a **single** `general-purpose` subagent:

> You are a **Senior Architect** planning feature implementation for {{PROJECT_NAME}}.
>
> **Step 1 — Read context:**
> - Read story file: `{STORY_PATH}`
> - Read `skills/architecture/references/architecture-principles.md` — layer structure, dependency direction
> - Read `skills/layer-templates/SKILL.md` — code templates per architecture layer
> - Read any relevant ADRs in `adr/`
> - If architecture plan exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`, read it for architectural decisions and constraints
>
> **Step 2 — Produce implementation plan** with these sections:
> 1. Affected layers and components
> 2. New classes/interfaces to create (with package locations)
> 3. Existing classes to modify
> 4. Dependency direction validation
> 5. Integration points
> 6. Database changes (if applicable)
> 7. API changes (if applicable)
> 8. Event changes (if applicable)
> 9. Configuration changes
> 10. Risk assessment
>
> Save to `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (where XXXX is the epic ID and YYYY is the story sequence, extracted from the story ID).

### Fallback: Inline Architecture Planning

If skill `x-dev-architecture-plan` is not available in the project (skill file `skills/x-dev-architecture-plan/SKILL.md` does not exist), combine architecture planning into the Step 1B subagent by expanding its prompt to also read:
- `skills/protocols/references/` — protocol conventions
- `skills/security/references/` — OWASP, headers, secrets
- `skills/observability/references/` — tracing, metrics, logging
- `skills/resilience/references/` — circuit breaker, retry, fallback

This preserves the pre-integration behavior for projects that do not include the architecture-plan skill.

## Phases 1B-1E — Parallel Planning (Subagents via Task — SINGLE message)

**CRITICAL: ALL planning subagents MUST be launched in a SINGLE message.**

### 1B: Test Planning (MANDATORY DRIVER for Phase 2)
Invoke skill `x-test-plan` → produces `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

The test plan is the **implementation roadmap** for Phase 2. It produces:
- Acceptance tests (AT-N) as outer loop (Double-Loop TDD)
- Unit tests (UT-N) in TPP order as inner loop (Levels 1-6: degenerate → edge cases)
- Integration tests (IT-N) positioned after related UTs
- `Depends On: TASK-N` and `Parallel` markers per scenario

**Gate:** If Phase 1B fails or produces no output, Phase 2 MUST use G1-G7 fallback mode.

### 1C: Task Decomposition
Invoke skill `x-lib-task-decomposer` → produces `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`

The task decomposer auto-detects decomposition mode:
- If test plan with TPP markers exists → test-driven tasks (RED/GREEN/REFACTOR per task, with `Parallel` flags)
- If no test plan → fallback to G1-G7 layer-based decomposition

### 1D: Event Schema Design (if event_driven)
Launch `general-purpose` subagent:

> You are an **Event Engineer** designing event schemas.
> Read `skills/protocols/references/event-driven-conventions.md` for standards.
> Read the implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce event schema design: event names (past tense), CloudEvents envelope, topic naming, partition key, producer/consumer contracts.
> Save to `plans/epic-XXXX/plans/events-story-XXXX-YYYY.md`.

### 1E: Compliance Assessment (if compliance active)
Launch `general-purpose` subagent:

> You are a **Security Engineer** assessing compliance impact.
> Read `skills/security/SKILL.md` → then read its references.
> Read `skills/compliance/SKILL.md` → then read its references.
> Read the implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce compliance impact assessment: data classification, encryption requirements, audit logging needs, regulatory considerations.
> Save to `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md`.

## Phase 2 — TDD Implementation (Subagent via Task)

Launch a **single** `general-purpose` subagent for implementation:

> You are a **Developer** implementing story {STORY_ID} for {{PROJECT_NAME}}.
>
> **Step 1 — Read context:**
> - Read test plan: `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` — **MANDATORY** (implementation roadmap)
> - Read implementation plan: `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
> - If architecture plan was generated in Phase 1, read `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` for architectural decisions and constraints; if it does not exist, proceed without it
> - Read task breakdown: `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`
> - Read `skills/coding-standards/references/coding-conventions.md` — {{LANGUAGE}} conventions
> - Read `skills/coding-standards/references/version-features.md` — {{LANGUAGE}} {{LANGUAGE_VERSION}} features
> - Read `skills/layer-templates/SKILL.md` — code templates per layer
> - Read `skills/architecture/references/architecture-principles.md` — layer boundaries
>
> If no test plan found: emit WARNING and use **G1-G7 Fallback** (see below).
>
> **Step 2 — TDD Loop (Red-Green-Refactor per scenario):**
>
> 2.0. **Write Acceptance Test First (Double-Loop — Outer Loop):**
>    - Write AT-1 from the test plan (integration/API/E2E depending on scope)
>    - Run it — it MUST be RED (failing)
>    - AT-1 stays RED throughout the inner loop until all related UTs complete
>
> 2.1. **Inner Loop — For each UT-N in TPP order** (degenerate first, edge cases last):
>    - **RED:** Write the failing unit test for UT-N
>    - **GREEN:** Implement the MINIMUM production code to pass (respecting layer order: domain → ports → adapters → application → inbound)
>    - **REFACTOR:** Improve design (extract method if > 25 lines, eliminate duplication) — NEVER add behavior
>    - **Compile check:** `{{COMPILE_COMMAND}}` — error blocks next cycle
>    - **Atomic commit:** test + implementation in one commit using TDD format (`[TDD]` suffix)
>
> 2.2. **Verify Acceptance Test:**
>    - After all UT-N for AT-1 complete → run AT-1, it should now be GREEN
>    - If still RED, identify missing unit test cycles and add them
>    - Repeat for AT-2, AT-3, etc.
>
> 2.3. **Final Validation:**
>    - Run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}`
>    - Coverage targets: line ≥ 95%, branch ≥ 90%
>
> **Step 3 — Commit each TDD cycle** atomically following git conventions:
> - Use `feat(scope): implement [behavior] [TDD]` for combined cycles
> - Or `test(scope): [TDD:RED]` + `feat(scope): [TDD:GREEN]` + `refactor(scope): [TDD:REFACTOR]` for fine-grained history
> - Git history should tell TPP progression (simple to complex)
>
> Report: TDD cycles completed, acceptance tests status, coverage numbers.

### Parallelism in Phase 2

Independent test scenarios (no shared state/data dependencies) CAN run in parallel:

- Use `Parallel: yes/no` markers from the test plan and task breakdown
- Subagents working on independent layers MUST be launched in a SINGLE message
- Example: UT for outbound adapter can run in parallel with UT for inbound DTO if they share no state
- Dependent scenarios (marked `Depends On: TASK-N`) run sequentially

### G1-G7 Fallback (No Test Plan)

If no test plan with TPP markers was produced by Phase 1B, use legacy group-based implementation:

> **Step 2 (Fallback) — Implement groups G1-G7** following the task breakdown:
> - For each group: implement all tasks, then compile: `{{COMPILE_COMMAND}}`
> - If compilation fails: fix errors before proceeding
> - After G7: run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}`
> - Coverage targets: line ≥ 95%, branch ≥ 90%
>
> **Step 3 (Fallback) — Commit each group** atomically following git conventions.

Emit warning: `WARNING: No TDD test plan available. Using G1-G7 group-based implementation. Consider running /x-test-plan for future implementations.`

## Phase 3 — Documentation (Orchestrator — Inline)

Read the `interfaces` field from the project identity to determine which documentation
generators to invoke. For each configured interface, launch the corresponding generator.
Always generate a changelog entry regardless of interfaces.

**Interface Dispatch:**

| Interface | Generator | Output |
|-----------|-----------|--------|
| `rest` | OpenAPI/Swagger generator | `contracts/api/openapi.yaml` |
| `grpc` | gRPC/Proto documentation generator | `contracts/api/grpc-reference.md` |
| `cli` | CLI documentation generator | `contracts/api/cli-reference.md` |
| `graphql` | GraphQL schema documentation generator | `contracts/api/graphql-reference.md` |
| `websocket`, `kafka`, `event-consumer`, `event-producer` | Event-driven documentation generator | `contracts/api/event-reference.md` |

If no documentable interfaces configured: skip interface generators with log
`"No documentable interfaces configured"`. Always generate changelog entry.

Documentation output saved to `contracts/` with subdirectories per type:
- API docs → `contracts/api/`
- Architecture docs → `steering/`

**Changelog Entry:**
- Read commits since branch point (`git log main..HEAD --oneline`)
- Generate Conventional Commits summary by type (feat, fix, refactor, test, docs, chore)
- Append to CHANGELOG.md

**Performance Baseline (Recommended):**
If the implemented feature affects the request path, startup, or memory footprint:
1. Read `.claude/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` for measurement guide
2. Record "before" metrics (prior to the feature branch)
3. Record "after" metrics (with the feature branch)
4. Append a row to `results/performance/baselines.md`
5. If Delta > 10%, add a WARNING note
6. If Delta > 25%, add an INVESTIGATION note with optimization plan

This step is recommended but not mandatory. Skip does not block the phase.

**Architecture Document Update (Recommended):**
If an architecture plan exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`:
1. Invoke `x-dev-arch-update` to incrementally update `steering/service-architecture.md`
2. New components, integrations, flows, and ADR references are added to the appropriate sections
3. Change History (Section 10) is updated with the story reference
4. If `steering/service-architecture.md` does not exist, create it from the template

If no architecture plan found: skip with log
`"No architecture plan found; skipping architecture doc update"`.

This step is recommended but not mandatory. Skip does not block the phase.

### CLI Documentation Generator (interface: cli)

> Invoked when project identity `interfaces` contains `"cli"`.
> Output: `contracts/api/cli-reference.md`

**Scan** the project's CLI command definitions using framework-specific patterns:
- **Commander.js**: `.command()`, `.option()`, `.argument()` chains
- **Click**: `@click.command()`, `@click.option()`, `@click.argument()` decorators
- **Cobra**: `cobra.Command{}` structs
- **Clap**: `#[derive(Parser)]` and `#[arg()]` attributes

**Generate** `contracts/api/cli-reference.md` with:

1. `# CLI Reference` — title with project name
2. `## Quick Start` — at least 2 basic usage examples in code blocks
3. `## Global Flags` — table of flags applicable to all commands
   (columns: Flag, Type, Default, Description)
4. `## Command: {name}` — one section per top-level command:
   - Usage line: `$ {tool-name} {command} [flags] [args]`
   - Flags table: | Flag | Type | Default | Required | Description |
   - Arguments table: | Argument | Type | Required | Description |
   - At least 1 example in code block
5. `### Subcommand: {parent} {child}` — nested sections with same structure
6. `## Exit Codes` — table: | Code | Meaning |

If `interfaces` does NOT contain `"cli"`: skip silently (no output, no warning).

### Event-Driven Documentation Doc Generator

**Trigger:** Invoke when `interfaces` contains `websocket`, `event-consumer`, or `event-producer`.

Launch a `general-purpose` subagent:

> You are a **Documentation Engineer** generating an event catalog for {{PROJECT_NAME}}.
>
> **Step 1 — Read context:**
> - Read `skills/protocols/references/event-driven-conventions.md` — event naming, CloudEvents envelope, topic naming
> - Read `skills/protocols/references/websocket-conventions.md` — WebSocket channel and message conventions
> - Read the project source code to identify event definitions: producers, consumers, schemas, topics, channels
>
> **Step 2 — Generate `contracts/api/event-catalog.md`** with the following structure:
>
> 1. **Topics Overview** table: Topic/Channel, Events, Partitioning/Routing
> 2. **Per-event sections** (one `## Event: {name}` per event):
>    - Topic/Channel name
>    - Producer service
>    - Consumer services
>    - Payload Schema table: Field, Type, Required, Description
>    - Headers table (if applicable): correlation-id, content-type, custom headers
> 3. **Event Flows**: Mermaid `sequenceDiagram` showing Producer → Broker → Consumer for each flow
> 4. **CloudEvents envelope** details (if applicable): specversion, type, source, subject, datacontenttype
> 5. **Schema versioning** and backward compatibility notes
>
> **Step 3 — Protocol-specific handling:**
> - For `event-consumer`/`event-producer` (Kafka): document topics, partition keys, consumer groups, offset management
> - For `websocket`: document channels, message types, connection lifecycle
> - If both are present, produce a unified catalog covering multiple protocol types
>
> Save output to `contracts/api/event-catalog.md`.

## Phase 4 — Parallel Review

Invoke skill `/x-review` for the current story. The review skill launches its own parallel subagents (one per specialist engineer), each reading their own knowledge pack.

If `x-review` includes TDD checklist items, it validates: test-first pattern, TPP ordering, atomic TDD commits. If TDD checklist is not yet available, the review proceeds with existing criteria (backward compatible).

If an architecture plan was generated in Phase 1 (`plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`), provide it as additional context to reviewers. Reviewers validate that the implementation conforms to the architectural decisions documented in the plan.

Collect the consolidated review report with scores and severity counts.

## Phase 5 — Fixes + Feedback (Orchestrator — Inline)

1. Fix ALL failed items from Phase 4 review (every specialist engineer must reach STATUS: Approved — all items at 2/2)
2. For each fix, follow TDD discipline: write/update the test FIRST, then apply the fix
3. Use atomic TDD commits for fixes (same commit format as Phase 2)
4. Run `{{COMPILE_COMMAND}}` + `{{TEST_COMMAND}}`
5. Update common-mistakes document with newly found errors

## Phase 6 — Commit & PR (Orchestrator — Inline)

1. Push: `git push -u origin feat/{STORY_ID}-description`
2. Create PR via `gh pr create` with review summary in body, including TDD compliance:
   - Number of TDD cycles completed
   - Test-first pattern verified
   - TPP progression in commit history

## Phase 7 — Tech Lead Review

Invoke skill `x-review-pr` for holistic 40-point review. Requires 40/40 for GO. If NO-GO, fix all failed items and re-review (max 2 cycles).

If `x-review-pr` includes TDD criteria, it validates TDD compliance in the checklist. If TDD criteria are not yet available, the review proceeds with existing checklist (backward compatible).

## Phase 8 — Final Verification + Cleanup (Orchestrator — Inline)

1. Update README if needed
2. Update IMPLEMENTATION-MAP:
   a. Read `plans/epic-XXXX/IMPLEMENTATION-MAP.md`
   b. Find the current story's row in the dependency matrix (Section 1 table)
   c. Update the Status column: replace current value with `Concluída`
   d. Write the updated file
3. Update Story File Status:
   a. Read `plans/epic-XXXX/story-XXXX-YYYY.md`
   b. Update the `**Status:**` line from `Pendente` to `Concluída`
   c. In Section 8 (Sub-tarefas), mark completed sub-tasks: change `- [ ]` to `- [x]`
      for tasks that were implemented (based on commits and test results from this run)
   d. Write the updated file
4. Jira Status Sync (conditional):
   a. Read the story file's `**Chave Jira:**` field
   b. If the value is not `—` and not `<CHAVE-JIRA>` (i.e., has a real Jira key):
      - Call `mcp__atlassian__getTransitionsForJiraIssue` with the story's Jira key
      - Find the transition to "Done" (match by name containing "Done", "Concluído", or "Resolved")
      - Call `mcp__atlassian__transitionJiraIssue` to transition the issue
      - If transition fails: log warning, continue (non-blocking)
5. Run DoD checklist (24+ checks across phases, quality, git, artifacts)
6. TDD DoD items:
   - [ ] Commits show test-first pattern (test precedes or accompanies implementation in git log)
   - [ ] Acceptance tests exist and pass (AT-N GREEN)
   - [ ] Tests follow TPP ordering (simple to complex)
   - [ ] No test-after commits (all tests written before or with implementation)
   - [ ] Story markdown file updated with Status: Concluída
   - [ ] IMPLEMENTATION-MAP Status column updated for this story
   - [ ] At least 1 automated test validates the story's primary acceptance criterion
   - [ ] Smoke test passes (if testing.smoke_tests == true)
7. Conditional DoD items:
   - Contract tests pass (if testing.contract_tests == true)
   - Event schemas registered (if event_driven)
   - Compliance requirements met (if security.compliance active)
   - Gateway configuration updated (if api_gateway != none)
   - gRPC proto backward compatible (if interfaces contain grpc)
   - GraphQL schema backward compatible (if interfaces contain graphql)
   - [ ] Threat model updated (if security findings with severity >= Medium) — extract findings from Phase 3 review reports, map to STRIDE categories, and update `results/security/threat-model.md` using `resources/templates/_TEMPLATE-THREAT-MODEL.md` as format reference. See `/x-review` Phase 3d for the incremental update algorithm.
   - Post-deploy verification passed or skipped (if testing.smoke_tests == true)
8. Post-Deploy Verification (conditional: `testing.smoke_tests == true`):
   - If `testing.smoke_tests` is `false` in project identity → SKIP with log: "Post-deploy verification skipped (testing.smoke_tests=false)"
   - If `testing.smoke_tests` is `true`, execute the following checks (invoke `/run-e2e` or configured smoke test script):
     - **Health Check**: GET /health (or configured endpoint) → 200 OK
     - **Critical Path**: Execute primary request flow → valid response
     - **Response Time**: Verify p95 latency < configured SLO
     - **Error Rate**: Verify error rate < 1% threshold
   - Result outcomes:
     - **PASS**: All checks green → "Deploy confirmed"
     - **FAIL**: Any check red → "Investigate rollback"
     - **SKIP**: testing.smoke_tests=false → "Verification skipped"
   - Non-blocking: emit result for human decision, do NOT auto-rollback
9. Report PASS/FAIL/SKIP result
10. `git checkout main && git pull origin main`

**Phase 8 is the ONLY legitimate stopping point.**

## Roles and Models (Adaptive)

| Role | Phase | Tier |
|------|-------|------|
| Architect | Phase 1 | Senior |
| Task Decomposer | Phase 1C | Mid |
| Developer | Phase 2 | Adaptive (per Layer Task Catalog) |
| Specialist Reviews | Phase 4 | Adaptive (max task tier in domain) |
| Tech Lead | Phase 7 | Adaptive (story max tier) |

## Integration Notes

- Invokes: `x-dev-architecture-plan` (Phase 1, conditional), `x-test-plan`, `x-lib-task-decomposer`, `x-lib-group-verifier` (fallback only), `x-git-push`, `x-review`, `x-review-pr`
- TDD commit format follows `x-git-push` conventions (`[TDD]`, `[TDD:RED]`, `[TDD:GREEN]`, `[TDD:REFACTOR]` suffixes)
- All `{{PLACEHOLDER}}` tokens (e.g. `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`) are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
