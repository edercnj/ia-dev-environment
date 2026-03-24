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
Phase 1: Planning
  Wave 1 (parallel, SINGLE message): 1A arch plan + 1B-test test plan
  Sequential: 1B-impl implementation plan (depends on 1A output)
  Wave 2 (parallel, SINGLE message): 1C task decomp + 1D event schema + 1E compliance
Phase 2: Implementation        (subagent — reads coding + layer KPs)
Phase 3: Documentation         (parallel subagent dispatch + inline changelog)
Phase 4: Review                (invoke /x-review skill — launches its own subagents)
Phase 5-6: Fixes + PR          (orchestrator — inline)
Phase 7: Tech Lead Review      (invoke /x-review-pr skill)
Phase 8: Verification          (orchestrator — inline)
```

---

## Phase 0 — Preparation (Orchestrator — Inline)

1. Read story file and extract acceptance criteria, sub-tasks, dependencies
2. Verify dependencies (predecessor stories complete)
3. Check if test plan exists at `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md`
   - If present: Phase 2 will use TDD mode
   - If absent: Phase 1B-test (Wave 1) will produce it; if 1B-test fails, Phase 2 falls back to G1-G7
4. Check if architecture plan exists at `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
   - If present: Phase 1 will skip architecture planning (use existing plan)
   - If absent: Phase 1 will evaluate decision tree and invoke x-dev-architecture-plan
5. Extract epic ID from story ID (e.g., `story-0001-0003` → epic ID `0001`)
6. Ensure directories exist: `mkdir -p docs/stories/epic-XXXX/plans docs/stories/epic-XXXX/reviews`
7. Create branch: `git checkout -b feat/{STORY_ID}-description`

## Phase 1 — Planning (Wave 1 → Sequential → Wave 2)

Phase 1 uses three execution stages to maximize parallelism while respecting data dependencies:

```
Wave 1 (SINGLE message, parallel):
  ├── 1A: x-dev-architecture-plan → architecture-story-XXXX-YYYY.md
  └── 1B-test: x-test-plan → tests-story-XXXX-YYYY.md

Gate: Wait for Wave 1 completion

Sequential:
  └── 1B-impl: implementation plan subagent → plan-story-XXXX-YYYY.md
      (reads arch plan from 1A if generated)

Wave 2 (SINGLE message, parallel):
  ├── 1C: task decomposition (reads test plan + impl plan)
  ├── 1D: event schema design (reads impl plan, conditional)
  └── 1E: compliance assessment (reads impl plan, conditional)
```

### Wave 1 — Parallel (SINGLE message)

**CRITICAL: Wave 1 subagents MUST be launched in a SINGLE message (RULE-003).**

Wave 1 launches architecture planning and test planning in parallel. These two tasks have no data dependency on each other: the test plan reads static knowledge packs (`skills/architecture/references/`, `skills/testing/references/`), NOT the output of the architecture plan.

#### 1A: Architecture Plan via x-dev-architecture-plan

**If the architecture plan file already exists at `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` (as checked in Phase 0), skip 1A in Wave 1. Wave 1 dispatches ONLY the test plan.**

Evaluate change scope using the decision tree:

| Condition | Plan Level |
|-----------|-----------|
| New service / new integration / contract change / infra change | **Full** |
| New feature, no contract or infra change | **Simplified** |
| Bug fix / refactor / docs-only | **Skip** |

**If Full or Simplified:**

Invoke skill `/x-dev-architecture-plan {STORY_PATH}`.

- Output: `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
- If the skill invocation fails: emit `WARNING: Architecture plan generation failed. Continuing without architecture plan.` The test plan from 1B-test is preserved regardless.

**If Skip:**

Log `"Architecture plan not needed for this change scope"`. Wave 1 dispatches only the test plan.

##### Fallback: Inline Architecture Planning

If skill `x-dev-architecture-plan` is not available in the project (skill file `skills/x-dev-architecture-plan/SKILL.md` does not exist), combine architecture planning into the Step 1B-impl subagent by expanding its prompt to also read:
- `skills/protocols/references/` — protocol conventions
- `skills/security/references/` — OWASP, headers, secrets
- `skills/observability/references/` — tracing, metrics, logging
- `skills/resilience/references/` — circuit breaker, retry, fallback

This preserves the pre-integration behavior for projects that do not include the architecture-plan skill.

#### 1B-test: Test Planning (MANDATORY DRIVER for Phase 2)

Invoke skill `x-test-plan` → produces `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

The test plan is the **implementation roadmap** for Phase 2. It produces:
- Acceptance tests (AT-N) as outer loop (Double-Loop TDD)
- Unit tests (UT-N) in TPP order as inner loop (Levels 1-6: degenerate → edge cases)
- Integration tests (IT-N) positioned after related UTs
- `Depends On: TASK-N` and `Parallel` markers per scenario

**Gate:** If 1B-test fails or produces no output, Phase 2 MUST use G1-G7 fallback mode.

### Wave 1 Gate

After Wave 1 completes, verify outputs before proceeding:

- **Check architecture plan:** If 1A was dispatched, verify `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` exists. If missing (1A failed), emit `WARNING: Architecture plan generation failed.` and continue.
- **Check test plan:** Does `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` exist?
  - **If yes:** Pass the test plan path to Wave 2 (Phase 1C) as explicit input.
  - **If no:** Emit `WARNING: Test plan not produced by Phase 1B-test. Phase 2 will use G1-G7 fallback mode.` Continue to Sequential step and Wave 2 without test plan (backward compatible).

**Failure isolation:** A failure in one Wave 1 subagent does NOT block the other. If `x-test-plan` fails, the architecture plan output is preserved. If `x-dev-architecture-plan` fails, the test plan output is preserved.

### Sequential — Step 1B-impl: Implementation Plan (Subagent via Task)

**Executes AFTER Wave 1 completes.** This step depends on the architecture plan output from 1A (if generated).

Launch a **single** `general-purpose` subagent:

> You are a **Senior Architect** planning feature implementation for {{PROJECT_NAME}}.
>
> **Step 1 — Read context:**
> - Read story file: `{STORY_PATH}`
> - Read `skills/architecture/references/architecture-principles.md` — layer structure, dependency direction
> - Read `skills/layer-templates/SKILL.md` — code templates per architecture layer
> - Read any relevant ADRs in `docs/adr/`
> - If architecture plan exists at `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`, read it for architectural decisions and constraints
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
> Save to `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (where XXXX is the epic ID and YYYY is the story sequence, extracted from the story ID).

### Wave 2 — Parallel (SINGLE message, after 1B-impl completes)

**CRITICAL: Wave 2 subagents MUST be launched in a SINGLE message (RULE-003).**

Wave 2 launches after the implementation plan is ready. All Wave 2 subagents read the implementation plan; task decomposition also reads the test plan from Wave 1.

#### 1C: Task Decomposition
Invoke skill `x-lib-task-decomposer` with test plan path from Wave 1 Gate (or null if not available) → produces `docs/stories/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`

**Conditional:** If skill file `skills/x-lib-task-decomposer/SKILL.md` does not exist in the project, skip Phase 1C with log `"x-lib-task-decomposer not available, skipping task decomposition"`.

The task decomposer auto-detects decomposition mode:
- If test plan path provided and file contains TPP markers → test-driven tasks (RED/GREEN/REFACTOR per task, with `Parallel` flags)
- If no test plan path (null) → fallback to G1-G7 layer-based decomposition

#### 1D: Event Schema Design (if event_driven)
Launch `general-purpose` subagent:

> You are an **Event Engineer** designing event schemas.
> Read `skills/protocols/references/event-driven-conventions.md` for standards.
> Read the implementation plan at `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce event schema design: event names (past tense), CloudEvents envelope, topic naming, partition key, producer/consumer contracts.
> Save to `docs/stories/epic-XXXX/plans/events-story-XXXX-YYYY.md`.

#### 1E: Compliance Assessment (if compliance active)
Launch `general-purpose` subagent:

> You are a **Security Engineer** assessing compliance impact.
> Read `skills/security/SKILL.md` → then read its references.
> Read `skills/compliance/SKILL.md` → then read its references.
> Read the implementation plan at `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce compliance impact assessment: data classification, encryption requirements, audit logging needs, regulatory considerations.
> Save to `docs/stories/epic-XXXX/plans/compliance-story-XXXX-YYYY.md`.

## Phase 2 — TDD Implementation (Subagent via Task)

Launch a **single** `general-purpose` subagent for implementation:

> You are a **Developer** implementing story {STORY_ID} for {{PROJECT_NAME}}.
>
> **Step 1 — Read context:**
> - Read test plan: `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` — **MANDATORY** (implementation roadmap)
> - Read implementation plan: `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
> - If architecture plan was generated in Phase 1, read `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` for architectural decisions and constraints; if it does not exist, proceed without it
> - Read task breakdown: `docs/stories/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`
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

If no test plan with TPP markers was produced by Phase 1B-test (Wave 1), use legacy group-based implementation:

> **Step 2 (Fallback) — Implement groups G1-G7** following the task breakdown:
> - For each group: implement all tasks, then compile: `{{COMPILE_COMMAND}}`
> - If compilation fails: fix errors before proceeding
> - After G7: run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}`
> - Coverage targets: line ≥ 95%, branch ≥ 90%
>
> **Step 3 (Fallback) — Commit each group** atomically following git conventions.

Emit warning: `WARNING: No TDD test plan available. Using G1-G7 group-based implementation. Consider running /x-test-plan for future implementations.`

## Phase 3 — Documentation (Parallel Subagent Dispatch + Inline Changelog)

Phase 3 converts documentation generation from sequential inline execution to parallel subagent dispatch. Each documentation generator runs as an independent subagent, all launched in a SINGLE message (RULE-003). The changelog entry remains inline in the orchestrator, running concurrently with subagents.

### Phase 3 Execution Structure

```
Parallel subagent dispatch (SINGLE message):
  ├── Subagent: OpenAPI generator (if rest)
  ├── Subagent: gRPC docs (if grpc)
  ├── Subagent: CLI docs (if cli)
  ├── Subagent: GraphQL docs (if graphql)
  ├── Subagent: Event docs (if event interfaces)
  ├── Subagent: Architecture doc update (if arch plan exists)
  └── Subagent: Performance baseline (if applicable)

Inline concurrent (orchestrator):
  └── Changelog entry (git log + append)

Gate: all subagents + changelog complete → Phase 4
```

### Step 1 — Determine Active Generators

Read the `interfaces` field from the project identity to determine which documentation subagents to dispatch.

**Interface → Subagent Mapping:**

| Interface | Generator Type | Output |
|-----------|---------------|--------|
| `rest` | OpenAPI/Swagger generator | `docs/api/openapi.yaml` |
| `grpc` | gRPC/Proto documentation generator | `docs/api/grpc-reference.md` |
| `cli` | CLI documentation generator | `docs/api/cli-reference.md` |
| `graphql` | GraphQL schema documentation generator | `docs/api/graphql-reference.md` |
| `websocket`, `kafka`, `event-consumer`, `event-producer` | Event-driven documentation generator | `docs/api/event-reference.md` |

**Conditional generators (non-interface):**

| Condition | Generator Type | Output |
|-----------|---------------|--------|
| Architecture plan exists at `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` | Architecture doc update | `docs/architecture/service-architecture.md` |
| Feature affects request path, startup, or memory footprint | Performance baseline | `docs/performance/baselines.md` |

If no documentable interfaces configured AND no conditional generators apply: skip subagent dispatch with log `"No documentable interfaces configured; no conditional generators applicable"`. Generate only the changelog entry inline.

### Step 2 — Wave Management (Max 5 Subagents Per Wave)

**CRITICAL: All subagents in a wave MUST be launched in a SINGLE message (RULE-003).**

Collect all active generators from Step 1 into a dispatch list. Apply the 5-subagent limit:

- **If ≤ 5 generators:** Dispatch all in Wave 1 (SINGLE message).
- **If > 5 generators:** Dispatch the first 5 in Wave 1 (SINGLE message). After Wave 1 completes, dispatch remaining generators in Wave 2 (SINGLE message).

Priority order for wave assignment (first 5 get Wave 1):
1. Interface doc generators (in order: OpenAPI, gRPC, CLI, GraphQL, Event)
2. Architecture doc update
3. Performance baseline

The changelog entry is always generated inline during Wave 1, regardless of wave count.

### Step 3 — Dispatch Subagents (SINGLE Message Per Wave)

**CRITICAL: Each wave dispatches ALL its subagents in a SINGLE message (RULE-003).**

Each documentation subagent receives a prompt with:

| Field | Type | Description |
|-------|------|-------------|
| `generatorType` | `string` | Type: `openapi`, `grpc`, `cli`, `graphql`, `event`, `architecture`, `performance` |
| `outputPath` | `string` | Target output file path |
| `sourcePaths` | `string[]` | Source files/directories to read |
| `storyId` | `string` | ID of the story being documented |

Each subagent returns a result conforming to RULE-008:

| Field | Type | Required |
|-------|------|----------|
| `status` | `SUCCESS \| FAILED` | Yes |
| `commitSha` | `string` | Yes (if SUCCESS) |
| `findingsCount` | `number` | Yes |
| `summary` | `string` | Yes |

#### Subagent: OpenAPI/Swagger Generator (interface: rest)

Launch `general-purpose` subagent:

> You are a **Documentation Engineer** generating OpenAPI documentation for {{PROJECT_NAME}}.
>
> **Read context:** Scan the project's REST controllers, DTOs, and endpoint annotations.
> **Generate:** `docs/api/openapi.yaml` — OpenAPI 3.1 specification with paths, schemas, security definitions, and examples.
> Save output to `docs/api/openapi.yaml`.

#### Subagent: gRPC/Proto Documentation Generator (interface: grpc)

Launch `general-purpose` subagent:

> You are a **Documentation Engineer** generating gRPC documentation for {{PROJECT_NAME}}.
>
> **Read context:** Scan `.proto` files for service definitions, message types, and RPC methods.
> **Generate:** `docs/api/grpc-reference.md` — service catalog with method signatures, message schemas, and streaming patterns.
> Save output to `docs/api/grpc-reference.md`.

#### Subagent: CLI Documentation Generator (interface: cli)

Launch `general-purpose` subagent:

> You are a **Documentation Engineer** generating CLI reference documentation for {{PROJECT_NAME}}.
>
> **Read context:** Scan the project's CLI command definitions using framework-specific patterns:
> - **Commander.js**: `.command()`, `.option()`, `.argument()` chains
> - **Click**: `@click.command()`, `@click.option()`, `@click.argument()` decorators
> - **Cobra**: `cobra.Command{}` structs
> - **Clap**: `#[derive(Parser)]` and `#[arg()]` attributes
> - **Picocli**: `@Command`, `@Option`, `@Parameters` annotations
>
> **Generate** `docs/api/cli-reference.md` with:
> 1. `# CLI Reference` — title with project name
> 2. `## Quick Start` — at least 2 basic usage examples in code blocks
> 3. `## Global Flags` — table of flags applicable to all commands (columns: Flag, Type, Default, Description)
> 4. `## Command: {name}` — one section per top-level command: usage line, flags table, arguments table, at least 1 example
> 5. `### Subcommand: {parent} {child}` — nested sections with same structure
> 6. `## Exit Codes` — table: | Code | Meaning |
>
> Save output to `docs/api/cli-reference.md`.

#### Subagent: GraphQL Schema Documentation Generator (interface: graphql)

Launch `general-purpose` subagent:

> You are a **Documentation Engineer** generating GraphQL documentation for {{PROJECT_NAME}}.
>
> **Read context:** Scan GraphQL schema files for types, queries, mutations, subscriptions, and directives.
> **Generate:** `docs/api/graphql-reference.md` — schema reference with type definitions, query/mutation examples, and subscription patterns.
> Save output to `docs/api/graphql-reference.md`.

#### Subagent: Event-Driven Documentation Generator (interfaces: websocket, kafka, event-consumer, event-producer)

Launch `general-purpose` subagent:

> You are a **Documentation Engineer** generating an event catalog for {{PROJECT_NAME}}.
>
> **Step 1 — Read context:**
> - Read `skills/protocols/references/event-driven-conventions.md` — event naming, CloudEvents envelope, topic naming
> - Read `skills/protocols/references/websocket-conventions.md` — WebSocket channel and message conventions
> - Read the project source code to identify event definitions: producers, consumers, schemas, topics, channels
>
> **Step 2 — Generate `docs/api/event-catalog.md`** with the following structure:
> 1. **Topics Overview** table: Topic/Channel, Events, Partitioning/Routing
> 2. **Per-event sections** (one `## Event: {name}` per event): Topic/Channel name, Producer service, Consumer services, Payload Schema table, Headers table
> 3. **Event Flows**: Mermaid `sequenceDiagram` showing Producer → Broker → Consumer for each flow
> 4. **CloudEvents envelope** details (if applicable)
> 5. **Schema versioning** and backward compatibility notes
>
> **Step 3 — Protocol-specific handling:**
> - For `event-consumer`/`event-producer` (Kafka): document topics, partition keys, consumer groups, offset management
> - For `websocket`: document channels, message types, connection lifecycle
> - If both are present, produce a unified catalog covering multiple protocol types
>
> Save output to `docs/api/event-catalog.md`.

#### Subagent: Architecture Document Update (conditional)

**Condition:** Architecture plan exists at `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`.

Launch `general-purpose` subagent:

> You are a **Documentation Engineer** updating the architecture document for {{PROJECT_NAME}}.
>
> **Read context:**
> - Read architecture plan at `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
> - Read existing architecture doc at `docs/architecture/service-architecture.md` (if exists)
>
> **Update:** Invoke `x-dev-arch-update` to incrementally update `docs/architecture/service-architecture.md`:
> 1. New components, integrations, flows, and ADR references are added to the appropriate sections
> 2. Change History (Section 10) is updated with the story reference
> 3. If `docs/architecture/service-architecture.md` does not exist, create it from the template
>
> Save output to `docs/architecture/service-architecture.md`.

If no architecture plan found: do NOT dispatch this subagent. Log `"No architecture plan found; skipping architecture doc update subagent"`.

#### Subagent: Performance Baseline (conditional)

**Condition:** The implemented feature affects the request path, startup, or memory footprint.

Launch `general-purpose` subagent:

> You are a **Performance Engineer** recording performance baselines for {{PROJECT_NAME}}.
>
> **Read context:**
> - Read `.claude/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` for measurement guide
>
> **Record:**
> 1. Record "before" metrics (prior to the feature branch)
> 2. Record "after" metrics (with the feature branch)
> 3. Append a row to `docs/performance/baselines.md`
> 4. If Delta > 10%, add a WARNING note
> 5. If Delta > 25%, add an INVESTIGATION note with optimization plan
>
> Save output to `docs/performance/baselines.md`.

If the feature does not affect request path: do NOT dispatch this subagent. Log `"Feature does not affect request path; skipping performance baseline subagent"`.

### Step 4 — Inline Changelog (Concurrent with Subagents)

While subagents execute, the orchestrator generates the changelog entry inline:

1. Read commits since branch point: `git log main..HEAD --oneline`
2. Generate Conventional Commits summary by type (feat, fix, refactor, test, docs, chore)
3. Append to CHANGELOG.md

The changelog runs concurrently with Wave 1 subagents. It does NOT wait for subagents to complete, and subagents do NOT wait for it.

### Step 5 — Phase 3 Gate

**Gate: ALL subagents + inline changelog MUST complete before proceeding to Phase 4.**

Wait for all dispatched subagents to return results. Verify:

- **All subagents returned:** Count returned results vs. dispatched count.
- **Changelog completed:** Verify CHANGELOG.md was updated.

**Failure handling (best-effort):**
- Documentation generation is **best-effort**. A failed subagent does NOT block Phase 3 completion.
- If a subagent returns `status: FAILED`: emit `WARNING: {generatorType} documentation generator failed: {summary}`. Continue.
- If ALL subagents fail: emit `WARNING: All documentation generators failed. Proceeding to Phase 4 with changelog only.`
- Phase 3 is marked FAILED only if the changelog entry itself fails (which should not happen under normal conditions).

**Multi-wave gate:** If overflow caused Wave 2 dispatch, the gate waits for BOTH waves to complete before proceeding.

Documentation output saved to `docs/` with subdirectories per type:
- API docs → `docs/api/`
- Architecture docs → `docs/architecture/`
- Performance docs → `docs/performance/`

## Phase 4 — Parallel Review

Invoke skill `/x-review` for the current story. The review skill launches its own parallel subagents (one per specialist engineer), each reading their own knowledge pack.

If `x-review` includes TDD checklist items, it validates: test-first pattern, TPP ordering, atomic TDD commits. If TDD checklist is not yet available, the review proceeds with existing criteria (backward compatible).

If an architecture plan was generated in Phase 1 (`docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`), provide it as additional context to reviewers. Reviewers validate that the implementation conforms to the architectural decisions documented in the plan.

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
2. Update IMPLEMENTATION-MAP
3. Run DoD checklist (24+ checks across phases, quality, git, artifacts)
4. TDD DoD items:
   - [ ] Commits show test-first pattern (test precedes or accompanies implementation in git log)
   - [ ] Acceptance tests exist and pass (AT-N GREEN)
   - [ ] Tests follow TPP ordering (simple to complex)
   - [ ] No test-after commits (all tests written before or with implementation)
5. Conditional DoD items:
   - Contract tests pass (if testing.contract_tests == true)
   - Event schemas registered (if event_driven)
   - Compliance requirements met (if security.compliance active)
   - Gateway configuration updated (if api_gateway != none)
   - gRPC proto backward compatible (if interfaces contain grpc)
   - GraphQL schema backward compatible (if interfaces contain graphql)
   - [ ] Threat model updated (if security findings with severity >= Medium) — extract findings from Phase 3 review reports, map to STRIDE categories, and update `docs/security/threat-model.md` using `resources/templates/_TEMPLATE-THREAT-MODEL.md` as format reference. See `/x-review` Phase 3d for the incremental update algorithm.
   - Post-deploy verification passed or skipped (if testing.smoke_tests == true)
6. Post-Deploy Verification (conditional: `testing.smoke_tests == true`):
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
7. Report PASS/FAIL/SKIP result
8. `git checkout main && git pull origin main`

**Phase 8 is the ONLY legitimate stopping point.**

## Roles and Models (Adaptive)

| Role | Phase | Tier |
|------|-------|------|
| Architect | Phase 1 | Senior |
| Task Decomposer | Phase 1C | Mid |
| Developer | Phase 2 | Adaptive (per Layer Task Catalog) |
| Documentation Engineer | Phase 3 | Mid |
| Performance Engineer | Phase 3 | Mid |
| Specialist Reviews | Phase 4 | Adaptive (max task tier in domain) |
| Tech Lead | Phase 7 | Adaptive (story max tier) |

## Integration Notes

- Invokes: `x-dev-architecture-plan` (Wave 1, conditional), `x-test-plan` (Wave 1), `x-lib-task-decomposer` (Wave 2), `x-lib-group-verifier` (fallback only), `x-dev-arch-update` (Phase 3 subagent, conditional), `x-git-push`, `x-review`, `x-review-pr`
- Three-stage Phase 1: Wave 1 launches 1A+1B-test in parallel (SINGLE message); Sequential 1B-impl reads arch plan output; Wave 2 launches 1C+1D+1E in parallel (SINGLE message)
- Phase 3 parallel dispatch: documentation generators launch as independent subagents in a SINGLE message (max 5 per wave, overflow to Wave 2); changelog runs inline concurrently; gate waits for all subagents + changelog before Phase 4; failures are best-effort (WARNING, not blocking)
- TDD commit format follows `x-git-push` conventions (`[TDD]`, `[TDD:RED]`, `[TDD:GREEN]`, `[TDD:REFACTOR]` suffixes)
- All `{{PLACEHOLDER}}` tokens (e.g. `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`) are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
