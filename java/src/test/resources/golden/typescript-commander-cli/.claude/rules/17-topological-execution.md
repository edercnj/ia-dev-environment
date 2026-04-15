# Rule 17 — Topological Execution (RULE-TF-03)

> **Related:** Rule 16 (I/O Contracts), Rule 18 (Atomic Commits). Applies only when
> `planningSchemaVersion == "2.0"`.

## Rule

Task execution order MUST be derived from `task-implementation-map-STORY-*.md`
waves. Within a wave, tasks are independent by construction and MUST be dispatched
in parallel when the orchestrator has the capacity. Cross-wave order is strict: a
task in wave N+1 cannot start until ALL tasks in wave N complete (DONE status +
successful post-wave integration verification).

## Algorithm

Waves are produced by Kahn's algorithm over the dependency DAG (story-0038-0002):

1. Detect coalesced groups (COALESCED pairs, per Rule 15) and collapse them to
   super-nodes.
2. Validate no cycles remain (DFS; throws `CyclicDependencyException`).
3. Repeatedly take nodes with zero remaining in-degree as the next wave; remove
   them and decrement successors' in-degrees until empty.

## Enforcement

- `x-story-implement` Phase 2 (v2): wave dispatch loop. Fires all tasks in a wave
  as sibling tool calls in a single assistant message; awaits completion before
  advancing.
- Integration verification after each wave: `mvn compile` + `mvn test`; a failure
  pinpoints the offending TASK-ID via last-writer analysis per failing file.

## Forbidden

- Running tasks from two different waves concurrently.
- Skipping the integration verification between waves.
- Modifying wave membership at runtime (the map is immutable during execution).
- Running a coalesced pair as two separate invocations (Rule 18 governs).
