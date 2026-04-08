# Multi-Agent Planning Guide

> Reference documentation for the x-story-plan skill. Defines the TASK_PROPOSAL format,
> consolidation rules, conflict resolution, and DoR validation checks.

## TASK_PROPOSAL Format

Each agent produces proposals in a standardized format to enable deterministic consolidation.

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `source` | string | M | Agent name that produced this proposal (Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner) |
| `id` | string | M | Agent-scoped ID with prefix: `ARCH-NNN`, `QA-NNN`, `SEC-NNN`, `TL-NNN`, `PO-NNN` |
| `type` | enum | M | Task type: `architecture`, `implementation`, `test`, `security`, `quality-gate`, `validation` |
| `description` | string | M | Clear description of what this task accomplishes |
| `layer` | enum | M | Architecture layer: `domain`, `application`, `adapter.inbound`, `adapter.outbound`, `config`, `cross-cutting` |
| `components` | list | M | Affected classes, interfaces, or modules |
| `tdd_phase` | enum | M | TDD phase: `RED` (write test), `GREEN` (make pass), `REFACTOR` (improve), `VERIFY` (validate), `N/A` |
| `tpp_level` | enum | M | TPP level: `nil`, `constant`, `scalar`, `collection`, `conditional`, `iteration`, `N/A` |
| `dod_criteria` | list | M | Definition of Done items (at least 1 required) |
| `dependencies` | list | O | Task IDs this depends on (empty if no dependencies) |
| `estimated_effort` | enum | M | Effort estimate: `XS` (< 15min), `S` (15-30min), `M` (30-60min), `L` (1-2h), `XL` (2-4h) |

### Agent Prefixes

| Agent | Prefix | Typical Types |
|-------|--------|---------------|
| Architect | `ARCH-NNN` | architecture, implementation (component creation, layer setup) |
| QA Engineer | `QA-NNN` | test (RED/GREEN pairs in TPP order) |
| Security Engineer | `SEC-NNN` | security (controls, validations, assessments) |
| Tech Lead | `TL-NNN` | quality-gate (compliance checks, thresholds) |
| Product Owner | `PO-NNN` | validation (acceptance criteria verification) |

### Example Proposal

```
TASK_PROPOSAL:
  source: QA Engineer
  id: QA-001
  type: test
  description: Write failing test for null input handling in UserValidator
  layer: domain
  components: [UserValidator, UserValidatorTest]
  tdd_phase: RED
  tpp_level: nil
  dod_criteria: [Test named validate_nullInput_throwsValidationException, Test fails with expected exception type, No production code changed]
  dependencies: []
  estimated_effort: XS
END_PROPOSAL

TASK_PROPOSAL:
  source: QA Engineer
  id: QA-002
  type: test
  description: Implement minimum code to pass null input validation test
  layer: domain
  components: [UserValidator]
  tdd_phase: GREEN
  tpp_level: nil
  dod_criteria: [QA-001 test passes, Only null check added, No other behavior changed]
  dependencies: [QA-001]
  estimated_effort: XS
END_PROPOSAL
```

## Consolidation Rules

Consolidation transforms agent-scoped proposals (ARCH-NNN, QA-NNN, etc.) into a unified task list (TASK-NNN).

### Rule 1: MERGE -- Union of DoD Criteria

**When:** Two or more proposals from different agents target the same component AND the same layer.

**Action:**
- Merge into a single task
- Union all DoD criteria from all contributing tasks
- Keep the more specific description (prefer Architect for implementation, QA for tests)
- Retain dependencies from all merged tasks
- Set source to `merged({agent1},{agent2})`

**Example:**
- `ARCH-003`: Create UserValidator in domain layer, DoD: [class exists, handles 3 validation rules]
- `SEC-002`: Add input sanitization to UserValidator, DoD: [XSS prevention, SQL injection prevention]
- **Result:** Single TASK with DoD: [class exists, handles 3 validation rules, XSS prevention, SQL injection prevention]

### Rule 2: AUGMENT -- Security Criteria Injection

**When:** An implementation task (from Architect) touches a security-sensitive component.

**Detection keywords:** `input`, `auth`, `password`, `token`, `file`, `path`, `query`, `sql`, `http`, `request`, `session`, `cookie`, `encrypt`, `decrypt`, `hash`, `secret`.

**Action:**
- Add relevant SEC-NNN DoD criteria to the implementation task
- The original SEC task is NOT removed -- it becomes a verification task
- The verification task depends on the augmented implementation task

**Example:**
- `ARCH-005`: Create AuthenticationService, DoD: [service class, login method, token generation]
- `SEC-003`: Verify authentication security, DoD: [bcrypt hashing, constant-time comparison, rate limiting]
- **Result:** ARCH-005 gains additional DoD: [bcrypt hashing, constant-time comparison]. SEC-003 remains as VERIFY task depending on ARCH-005.

### Rule 3: PAIR -- RED before GREEN

**When:** A QA-NNN GREEN task exists (make test pass).

**Action:**
- Verify a corresponding RED task exists (write failing test)
- RED task MUST appear before GREEN in final ordering
- If GREEN has no RED counterpart, create a synthetic RED task:
  - Description: "Write failing test for {GREEN task description}"
  - TDD phase: RED
  - Same layer, components, and TPP level as the GREEN task
- Set dependency: GREEN depends on RED

**Validation:** Every GREEN task MUST have exactly one RED predecessor.

### Rule 4: Tech Lead Wins Conflicts

**When:** Tech Lead (TL) and Architect (ARCH) propose conflicting approaches for the same component.

**Detection:** Both proposals target the same component with different implementation strategies or patterns.

**Action:**
- Tech Lead's approach is used for the final task
- Architect's approach is recorded in the task notes as "considered alternative"
- Rationale: Tech Lead has final authority on implementation standards and patterns

**Example:**
- `ARCH-007`: Use abstract class for PaymentProcessor hierarchy
- `TL-002`: Use interface + composition for PaymentProcessor (prefer composition over inheritance)
- **Result:** Task uses interface + composition. Notes: "Alternative: abstract class hierarchy (Architect)"

### Rule 5: PO Amends Acceptance Criteria

**When:** Product Owner identifies missing or unclear acceptance criteria.

**Action:**
- PO amendments are captured in PO-NNN validation tasks
- These tasks are positioned AFTER implementation tasks in the final ordering
- PO-NNN tasks reference specific Gherkin scenarios they validate
- If PO proposes new Gherkin scenarios, they are included in the story file update (Phase 4.5)

## Conflict Resolution Summary

| Conflict Type | Resolution | Priority |
|---------------|-----------|----------|
| Same component, different agents | MERGE (union DoD) | -- |
| Security-sensitive implementation | AUGMENT (inject security DoD) | -- |
| Missing RED for GREEN | PAIR (create synthetic RED) | -- |
| Architect vs Tech Lead approach | Tech Lead wins | TL > ARCH |
| Missing acceptance criteria | PO amends | PO adds |

## DoR Validation Checks

### Mandatory Checks (10)

| # | Check | Pass Criteria |
|---|-------|---------------|
| 1 | Architecture plan exists | File exists with layer analysis |
| 2 | Test plan with AT-N + UT-N in TPP order | AT entries exist AND UT entries follow TPP progression |
| 3 | Security assessment | Assessment exists with OWASP mapping |
| 4 | Minimum 4 tasks | `total_tasks >= 4` |
| 5 | Each task has >= 1 DoD criterion | No task has empty DoD column |
| 6 | No circular task dependencies | DAG is acyclic |
| 7 | Story Gherkin has >= 4 scenarios | `scenario_count >= 4` |
| 8 | Data contracts defined | Request/response types with typed fields |
| 9 | Implementation plan exists | File with component list |
| 10 | Planning report exists | File with all sections populated |

### Conditional Checks (2)

| # | Check | Condition | Pass Criteria |
|---|-------|-----------|---------------|
| 11 | Compliance assessment | `compliance != "none"` | Assessment with regulatory mapping |
| 12 | Contract tests | `contract_tests == true` | At least 1 contract test scenario |

### Verdict Logic

```
IF all mandatory checks pass AND all applicable conditional checks pass:
    verdict = READY
ELSE:
    verdict = NOT_READY
```

## Task Ordering Algorithm

The final task order follows this priority:

1. **Phase ordering:** RED > GREEN > REFACTOR > VERIFY > N/A
2. **Layer ordering (within phase):** domain > application > adapter.inbound > adapter.outbound > config > cross-cutting
3. **TPP ordering (within layer):** nil > constant > scalar > collection > conditional > iteration > N/A
4. **Dependency ordering:** If task A depends on task B, B comes before A regardless of other criteria
5. **Sequential ID assignment:** After ordering, assign TASK-001, TASK-002, ... TASK-NNN

## Effort Estimation Scale

| Size | Duration | Example |
|------|----------|---------|
| XS | < 15 minutes | Add a constant, rename a variable |
| S | 15-30 minutes | Write a single unit test, add a validation |
| M | 30-60 minutes | Implement a domain service method, write integration test |
| L | 1-2 hours | Create a new adapter, implement a use case |
| XL | 2-4 hours | Design and implement a new aggregate, complex integration |
