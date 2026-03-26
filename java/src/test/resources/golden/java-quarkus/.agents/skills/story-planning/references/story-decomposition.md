# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Rule 13 — Story Decomposition Principles

## Principles
Story decomposition is the bridge between system design and implementation. A well-decomposed spec produces stories that are independently implementable, testable, and correctly ordered. These rules are **mandatory** when breaking down specifications into work items.

## SD-01: Layer-by-Layer Decomposition

Every system specification MUST be decomposed following five mandatory layers:

| Layer | Name | Purpose | Signal |
|-------|------|---------|--------|
| 0 | Foundation | Infrastructure that other stories depend on | Stack rows, configuration CRUD, protocol setup |
| 1 | Core Domain | The single operation that establishes architectural patterns | Story that blocks the most others |
| 2 | Extensions | New operations reusing patterns from Layer 1 | Independent journeys in the spec |
| 3 | Compositions | Flows combining 2+ extension capabilities | Journeys referencing multiple other journeys |
| 4 | Cross-Cutting | Quality, observability, security, tech debt | Metrics, platform specs, DoD requirements |

**FORBIDDEN**: Skipping layers. A spec with 6 journeys that produces only 3 stories (one per layer 0-2) is under-decomposed.

## SD-02: Story Self-Containment

Each story MUST be implementable without consulting the original system specification. This requires:

- **Data contracts** with every field: name, type, format, mandatory/optional flag, derivation rule
- **Gherkin acceptance criteria** with concrete values (not abstractions)
- **Mermaid sequence diagrams** using real component names from the spec
- **Sub-tasks** individually estimable at 2-4 hours, tagged `[Dev]`, `[Test]`, or `[Doc]`

### Gherkin Completeness Requirements

Every story's Gherkin acceptance criteria MUST include:

- **Degenerate cases** (mandatory): At minimum, scenarios for null input, empty collection, zero value, and missing required field
- **Boundary values** (mandatory): Triplet pattern for each bounded input — (at-minimum, at-maximum, past-maximum)
- **Error paths** (complete): Every documented error type in the specification MUST have a corresponding Gherkin scenario with expected error code/message
- **Minimum floor**: 4 scenarios per story (1 happy path + 2 error paths + 1 edge case is the floor, not the ceiling)

```
// BAD — forces developer back to the spec
"Implementar a operação principal conforme a especificação"

// GOOD — developer has everything needed
"Implementar endpoint POST /api/v1/operations"
+ Data contract table with all fields
+ 6 Gherkin scenarios (minimum 4: happy + errors + edge cases)
+ Sequence diagram showing Client → Controller → Service → Repository → DB
+ 6 sub-tasks: [Dev] Controller, [Dev] Service, [Dev] Repository, [Dev] Migration, [Test] Unit, [Test] Integration
```

## SD-03: Dependency DAG

Story dependencies MUST form a Directed Acyclic Graph (DAG):

- **No circular dependencies**: If A → B → C → A exists, at least two of those are actually one story
- **All extensions depend on core**: Every Layer 2 story depends on the Layer 1 story
- **Bidirectional consistency**: If A's "Blocks" lists B, then B's "Blocked By" MUST list A
- **Hard dependencies only**: Soft dependencies (where a mock suffices) are NOT declared in Blocked By

**Dependency types:**

| Type | Signal | Example |
|------|--------|---------|
| Structural | Story B uses a class/table created by Story A | Handler depends on server setup |
| Data | Story B needs data produced by Story A | Operation depends on configuration CRUD |
| Pattern | Story B reuses patterns established by Story A | Second handler depends on first handler |

**FORBIDDEN**: Circular dependencies. If detected, merge the stories or redesign the boundary.

## SD-04: Cross-Cutting Rule Extraction

Business rules that appear in 2+ journeys MUST be extracted into the Epic's rules table:

- Each rule gets a unique sequential ID: `RULE-001`, `RULE-002`, ...
- Rule description must be **implementation-ready** (a developer can build it without asking questions)
- Stories reference rules by ID — creating a single source of truth

```
// BAD — too vague, developer must ask for clarification
RULE-001: "Validar entidade"

// GOOD — implementation-ready
RULE-001: "Entidade deve estar ativa (status=ACTIVE) consultando cache L1 → L2 → DB.
           Se inativa, retornar HTTP 400 com código ENTITY_INACTIVE.
           TTL do cache: 5min L1, 30min L2."
```

Rules that apply to **only one** journey stay in that journey's story, not in the Epic.

## SD-05: Story Sizing

A well-sized story is implementable by one developer in 1-3 sprints.

**MAXIMUM per story:**

| Metric | Limit | Action if exceeded |
|--------|-------|--------------------|
| Endpoints | 2 | Split into separate stories |
| Protocol flows | 1 | Split by flow type |
| Gherkin scenarios | 8 | Split by capability or error category |
| Sub-tasks | 10 | Split into smaller stories |

**MINIMUM per story:**

| Metric | Minimum | Action if below |
|--------|---------|-----------------|
| Testable endpoint/flow | 1 | Merge with another story |
| Gherkin scenarios | 4 | Add degenerate/boundary/error scenarios or merge if scope too narrow |

**Split signal**: "AND" in the title (e.g., "Operation X AND Operation Y").
**Merge signal**: "Helper" or "Utility" in the title — likely a sub-task, not a story.

### SD-05a: Scenario Ordering (TPP-Based)

Gherkin scenarios within each story MUST be ordered following the Transformation Priority Premise (TPP):

| Order | Category | Rationale |
|-------|----------|-----------|
| 1 | Degenerate cases | Null, empty, zero — simplest transformations, fail-fast guards |
| 2 | Happy path (basic) | Single successful flow with minimal valid input |
| 3 | Happy path (variations) | Alternate valid inputs, optional fields present |
| 4 | Error paths | Business rule violations, validation failures |
| 5 | Boundary values | At-min, at-max, past-max for each bounded input |
| 6 | Complex edge cases | Combinations, race conditions, state transitions |

**Rationale:** TPP ordering ensures the implementation grows incrementally from simple to complex, matching the natural TDD red-green-refactor cycle.

## SD-06: Phase Computation

Implementation phases derive automatically from the dependency DAG:

1. **Phase 0**: Stories with no dependencies (DAG roots)
2. **Phase 1**: Stories whose ALL dependencies are in Phase 0
3. **Phase N**: Stories whose ALL dependencies are in Phase 0..N-1

**Rules:**
- Stories within the same phase CAN be implemented in parallel
- **Critical path** = longest chain of phases (not stories) from root to leaf
- Any delay in a critical-path story directly delays final delivery
- **Leaf stories** (no dependents) can absorb schedule variance — good for junior developers or parallel streams

## SD-07: Generated Content Language

All decomposition artifacts (Epics, Stories, Implementation Maps) follow these language rules:

| Content | Language | Example |
|---------|----------|---------|
| Artifact body text | pt-BR | "Como **Operador**, eu quero..." |
| Industry-standard terms | English | cache, handler, endpoint, timeout, state machine |
| Code identifiers and field names | English | `merchant_id`, `response_code` |
| Rule IDs | English format | RULE-001, RULE-002 |
| Story IDs | Composite format | story-0001-0001 (epic ID + story sequence) |
| Epic IDs | Kebab-case format | epic-0001 |
| Gherkin keywords | Portuguese | DADO, QUANDO, ENTÃO, E, MAS |
| Mermaid node IDs and classDef | English | `S0001["story-0001-0001<br/>Title"]` |

## SD-08: Template-Driven Generation

All artifacts MUST be generated by reading templates from `.claude/templates/` at runtime:

- `_TEMPLATE-EPIC.md` → Epic structure
- `_TEMPLATE-STORY.md` → Story structure
- `_TEMPLATE-IMPLEMENTATION-MAP.md` → Map structure

**FORBIDDEN**: Hardcoding template structure in skills or agent instructions. Templates may evolve independently — always read fresh from disk.

**FORBIDDEN**: Hardcoding template structure in skills or agent instructions. Templates may evolve independently — always read fresh from disk.

## SD-09: Directory Structure Convention

All decomposition artifacts MUST follow a mandatory directory structure rooted under `docs/stories/`:

```
docs/stories/
└── epic-XXXX/
    ├── epic-XXXX.md                        ← Epic document
    ├── story-XXXX-0001.md                  ← First story of this epic
    ├── story-XXXX-0002.md                  ← Second story
    ├── story-XXXX-YYYY.md                  ← Nth story
    ├── implementation-map-XXXX.md          ← Implementation map for this epic
    ├── plans/                              ← Planning artifacts for this epic's stories
    │   ├── plan-story-XXXX-YYYY.md         ← Architecture implementation plan
    │   ├── tasks-story-XXXX-YYYY.md        ← Task decomposition
    │   ├── tests-story-XXXX-YYYY.md        ← Test plan
    │   ├── events-story-XXXX-YYYY.md       ← Event schema design (if event_driven)
    │   └── compliance-story-XXXX-YYYY.md   ← Compliance assessment (if compliance active)
    └── reviews/                            ← Review artifacts for this epic's stories
        ├── review-security-story-XXXX-YYYY.md
        ├── review-qa-story-XXXX-YYYY.md
        ├── review-performance-story-XXXX-YYYY.md
        ├── review-database-story-XXXX-YYYY.md    (if database active)
        ├── review-observability-story-XXXX-YYYY.md (if observability active)
        ├── review-devops-story-XXXX-YYYY.md      (if container/orch active)
        ├── review-api-story-XXXX-YYYY.md         (if protocols defined)
        ├── review-event-story-XXXX-YYYY.md       (if event_driven)
        ├── review-tech-lead-story-XXXX-YYYY.md
        └── correction-story-XXXX-YYYY.md         (if CRITICAL/MEDIUM findings)
```

**Naming rules:**
- Epic IDs use 4-digit zero-padded format: `epic-0001`, `epic-0002`, ...
- Story IDs carry their parent epic ID: `story-XXXX-YYYY` where `XXXX` = epic number, `YYYY` = story sequence within that epic
- Implementation map carries its epic ID: `implementation-map-XXXX`
- Planning artifacts use prefix `{type}-story-XXXX-YYYY.md` (e.g., `plan-story-0001-0003.md`, `tasks-story-0001-0003.md`)
- Review artifacts use prefix `review-{engineer}-story-XXXX-YYYY.md` (e.g., `review-security-story-0001-0003.md`)
- Correction stories use `correction-story-XXXX-YYYY.md`
- All filenames are lowercase kebab-case

**Directory rules:**
- Each epic gets its own folder: `docs/stories/epic-XXXX/`
- ALL artifacts for an epic (epic file, stories, implementation map, plans, reviews) live inside this folder
- Planning artifacts go in `docs/stories/epic-XXXX/plans/`
- Review artifacts go in `docs/stories/epic-XXXX/reviews/`
- The `docs/stories/` root directory must be created if it does not exist
- The `plans/` and `reviews/` subdirectories must be created when the first artifact of that type is saved
- When the user provides a different base path, replace `docs/stories/` with the user-specified path but maintain the `epic-XXXX/` subfolder structure

**Sequential ID assignment:**
- When creating the first epic, use `epic-0001`
- When adding an epic to a project with existing epics, scan `docs/stories/` for existing `epic-XXXX` folders and use the next available number
- Story numbering always starts at `0001` within each epic

**FORBIDDEN**: Saving epic/story/plan/review files at the project root, in flat directories, or in `docs/plans/` / `docs/reviews/` outside the `epic-XXXX/` folder structure.

## Anti-Patterns (FORBIDDEN)

- Story that says "implement X" without a data contract → **FORBIDDEN**
- Gherkin with "um valor qualquer" instead of concrete value → **FORBIDDEN**
- Cross-cutting rule duplicated across multiple stories instead of extracted to Epic → **FORBIDDEN**
- Circular dependency between stories → **FORBIDDEN**
- Story with 2+ protocol flows → **MUST SPLIT**
- Epic without DoR/DoD → **FORBIDDEN**
- Story without Mermaid sequence diagram → **FORBIDDEN**
- Rule in Epic that applies to only one journey → **MOVE TO STORY**
- Dependency declared in one direction only (A blocks B but B doesn't list A) → **FORBIDDEN**
- Hardcoded template structure (not reading from `.claude/templates/`) → **FORBIDDEN**
- Story with fewer than 4 Gherkin scenarios → **FORBIDDEN** (add degenerate/boundary/error)
- Gherkin scenarios without degenerate cases (null, empty, zero) → **FORBIDDEN**
- Boundary-dependent behavior without triplet scenarios (at-min, at-max, past-max) → **FORBIDDEN**
- Gherkin scenarios ordered happy-path-first (degenerate cases must come first) → **REORDER**
- Story decomposed by technical artifact (classes, layers, modules) instead of business value → **REFRAME**
- Story without "Entrega de Valor" section (Section 3.5) → **FORBIDDEN**
- Story without at least one smoke/E2E test sub-task in Section 8 → **FORBIDDEN**

## SD-10: Business Value as Decomposition Driver

Each story MUST deliver measurable business value. Technical decomposition (by layer, by class,
by module) is forbidden as the primary decomposition axis.

- **Test:** Can a product owner understand and validate the value delivered? If not, reframe.
- **Minimum:** Each story has a "Entrega de Valor" section with Valor Principal + Métrica de Sucesso + Impacto no Negócio.
- **Exception:** Layer 0 foundation stories express enablement value (what domain stories they unblock).
- **Exception:** Layer 4 cross-cutting stories express risk reduction (e.g., "Test coverage ≥ 95%, reducing regression risk").

**FORBIDDEN decomposition patterns:**
- "Migrate classes A, B, C" (grouping by technical artifact)
- "Implement repository layer" (grouping by architecture layer alone)
- "Refactor module X" (grouping by code location)
- "Create DTOs and mappers" (grouping by code type)

**REQUIRED decomposition patterns:**
- "Enable payment processing via new endpoint" (capability delivery)
- "Support real-time balance queries" (user-facing value)
- "Migrate payment endpoint to Java (decommission legacy)" (business outcome)

## SD-11: Mandatory Automated Test per Story

Every story MUST include at least one automated end-to-end validation sub-task in Section 8:
- `[Test] Smoke/E2E: <test validating the primary acceptance criterion end-to-end>`
- `[Test] Integração: <integration test validating the complete flow>`

A story without ANY automated end-to-end validation sub-task is INCOMPLETE.
This is a Definition of Done requirement — stories that lack automated tests cannot be marked as Concluída.
