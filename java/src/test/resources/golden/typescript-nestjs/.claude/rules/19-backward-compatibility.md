# Rule 19 — Backward Compatibility of Planning Schema (RULE-TF-05)

> **Related:** Rule 15-18 (the task-first rules). Applies universally — guards
> legacy v1 epics against regressions introduced by v2 changes.

## Rule

Every change to the execution skills (`x-task-implement`, `x-story-implement`,
`x-epic-implement`) MUST preserve v1 behaviour for epics that declare
`planningSchemaVersion == "1.0"` or leave the field absent. The resolver
`dev.iadev.domain.schemaversion.SchemaVersionResolver` (story-0038-0008) is the
single source of truth for version detection; all v2-gated logic sits behind
`if (resolved == V2)` branches.

## Fallback Matrix

| Condition | Resolved Version | Log Code | Behaviour |
|-----------|-----------------|----------|-----------|
| execution-state.json absent | V1 | `SCHEMA_VERSION_FALLBACK_NO_FILE` | Run legacy flow |
| File present, field absent | V1 | `SCHEMA_VERSION_FALLBACK_MISSING_FIELD` | Run legacy flow |
| Field explicitly `"1.0"` | V1 | — | Run legacy flow |
| Field explicitly `"2.0"` | V2 | — | Run task-first flow |
| Field has other value (`"3.0"`, `"legacy"`) | V1 | `SCHEMA_VERSION_INVALID_VALUE` | Run legacy flow |
| File unparseable JSON | — | — | Hard fail (`UncheckedIOException`) |

## Rationale

EPIC-0038 introduced a new planning paradigm mid-flight. Dozens of legacy epics
(0025-0037) had already committed execution-state.json files in the old shape. A
hard schema requirement would have broken every in-flight orchestration and blocked
the merge. The soft-fallback-to-V1 semantic lets v2 features ship without touching
legacy state.

## Enforcement

- `SchemaVersionResolver` unit tests (11 scenarios covering each row of the matrix).
- `PlanningSchemaBackwardCompatSmokeTest` proves legacy epic shapes resolve to V1.
- Code review: every new branch introduced in `x-*-implement` SKILL.md MUST be
  gated on the resolved version; ungated additions risk silent regressions in v1
  callers.

## Forbidden

- Introducing v2-only behaviour into code paths reached by v1 callers.
- Hard-failing when `planningSchemaVersion` is absent (use fallback).
- Removing the `SCHEMA_VERSION_FALLBACK_*` log codes (they are an interop contract
  with external observability tooling).
- Writing to `execution-state.json` to "upgrade" a legacy epic implicitly — version
  changes must be explicit human-authored commits.
