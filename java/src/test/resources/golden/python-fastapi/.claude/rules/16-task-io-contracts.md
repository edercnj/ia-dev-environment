# Rule 16 — Task I/O Contracts are Mandatory (RULE-TF-02)

> **Related:** Rule 15 (Task Testability), Rule 17 (Topological Execution). Applies
> only when `planningSchemaVersion == "2.0"` (EPIC-0038 task-first flow).

## Rule

Every task file MUST populate Sections 2.1 (Inputs) and 2.2 (Outputs). Outputs MUST
be **verifiable** by a deterministic check — a grep match, a passing test, a diff
line count, a filesystem existence probe. Vague outputs like "improved performance"
or "better structure" are REJECTED.

## Verifiable Output Patterns

| Pattern | Verification |
|---------|--------------|
| "class X created" | `grep -r "class X" java/src/main/java` returns 1+ matches |
| "method Y exists" | `grep -r "Y(" {scope}` matches |
| "test Z passes" | `mvn test -Dtest=Z` exits 0 |
| "file F exists" | `test -f F` |
| "build green" | `mvn compile` exits 0 |

## Rationale

The EPIC-0034 post-mortem identified "outputs are sub-section of story, never
verified" as a root cause of task drift. Tasks that only promise outcomes (not
verifiable artifacts) cannot be validated by an orchestrator and hide scope creep
and coalescing regressions.

## Enforcement

- `TaskFileParser` + `TaskValidator` (TF-SCHEMA-004): empty or dash-only Outputs are
  rejected.
- `x-task-implement` Phase 3 (v2): every declared output is re-verified AFTER the
  TDD cycles and BEFORE the atomic commit; failure emits `OUTPUT_CONTRACT_VIOLATION`.

## Forbidden

- Outputs listed as `—` or a single dash.
- Outputs expressed in terms of feelings / quality adjectives ("better", "cleaner").
- Outputs that reference files or artifacts outside the task's declared layer.
