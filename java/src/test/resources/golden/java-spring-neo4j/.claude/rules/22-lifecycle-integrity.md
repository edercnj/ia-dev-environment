# Rule 22 — Lifecycle Integrity (RULE-046-01)

> **Related:** Rule 13 (Skill Invocation Protocol). Rule 19 (Backward
> Compatibility — planning schema version gating). ADR-0007 (Planning
> SoT is Markdown). Applies to every skill, assembler, and test that
> reads or writes the `**Status:**` field of Epic / Story / Task
> markdown files.

## Rule

Lifecycle status of every Epic, Story, and Task artifact lives in the
Markdown file as a single canonical `**Status:**` header line. That
file is the single source of truth (SoT). Every state transition —
planning → in-progress → done — MUST be written atomically to the
Markdown, then optionally mirrored to `execution-state.json` which
serves strictly as orchestrator telemetry. When the two diverge, the
Markdown wins and the JSON is rewritten.

The canonical enum is `dev.iadev.domain.lifecycle.LifecycleStatus`
with six values: `Pendente`, `Planejada`, `Em Andamento`,
`Concluída`, `Falha`, `Bloqueada`. Every read / parse / write of the
Status field MUST route through
`dev.iadev.application.lifecycle.StatusFieldParser` (never ad-hoc
regex in skills) and every transition MUST be validated by
`dev.iadev.application.lifecycle.LifecycleTransitionMatrix` before
the write is applied.

## Scope

| Surface | Example | Responsibility |
| :--- | :--- | :--- |
| Planning skills | `x-epic-create`, `x-story-create`, `x-epic-decompose`, `x-story-plan` | Emit artifacts with `**Status:** Pendente` and later transition to `Planejada` when DoR passes |
| Implementation skills | `x-story-implement`, `x-task-implement`, `x-epic-implement` | Transition `Planejada → Em Andamento → Concluída / Falha / Bloqueada` as execution proceeds |
| Reconcile skill | `x-status-reconcile` (story-0046-0006) | Detect drift between Markdown and `execution-state.json`; apply canonical write on `--apply` |
| Audit infrastructure | `LifecycleIntegrityAuditTest` (story-0046-0007) | CI gate: fail the build when planning/implementation artifacts violate the matrix |

## Transition Matrix

The matrix below is normative. Blank cells mean "forbidden"; a tick
means "allowed". The Java source of truth for this table is
`LifecycleTransitionMatrix` under
`dev.iadev.application.lifecycle`.

| De \ Para | Pendente | Planejada | Em Andamento | Concluída | Falha | Bloqueada |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| Pendente | — | ✅ | ✅ | — | ✅ | ✅ |
| Planejada | — | — | ✅ | — | ✅ | ✅ |
| Em Andamento | — | — | — | ✅ | ✅ | ✅ |
| Concluída | — | — | ✅ (reopen) | — | — | — |
| Falha | ✅ (reopen) | — | — | — | — | — |
| Bloqueada | ✅ | ✅ | ✅ | — | ✅ | — |

**Reopening notes.**

- `Concluída → Em Andamento` is only permitted via
  `x-status-reconcile --apply` when a prior "done" artifact must be
  revisited (e.g., post-release regression, re-planning after ADR
  change). This path MUST be logged with code
  `LIFECYCLE_REOPEN_CONCLUIDA`.
- `Falha → Pendente` represents a retry after a failed implementation
  attempt. Same `LIFECYCLE_REOPEN_FALHA` log code contract.
- No path ever re-enters `Pendente` directly from `Planejada` or
  `Em Andamento` — abandonment flows through `Falha` or `Bloqueada`
  first.

## Canonical Field Format

The `**Status:**` line MUST match the canonical regex
(`StatusFieldParser.STATUS_REGEX`):

```
^\*\*Status:\*\*\s+(Pendente|Planejada|Em Andamento|Concluída|Falha|Bloqueada)\s*$
```

Flags: MULTILINE. Only the FIRST occurrence in the file (header) is
considered authoritative. Additional `**Status:**` matches inside
tables or code blocks are ignored.

## Enforcement

- `StatusFieldParser` / `LifecycleTransitionMatrix` /
  `LifecycleAuditRunner` (story-0046-0001): helpers under
  `dev.iadev.application.lifecycle` — single code path for every
  read, validation, and write.
- `RuleAssemblerTest.listRules_includesLifecycleIntegrity`:
  verifies this file is copied into generated `.claude/rules/`.
- `LifecycleIntegrityAuditTest` (story-0046-0007): CI gate that
  scans skills and artifacts for three violations:
  - orphan `TELEMETRY: phase.*` markers not paired with the helper
    call;
  - planning/implementation reports written without a corresponding
    commit;
  - `--skip-*` flags on the happy path of a production skill.
- Three template headers (`_TEMPLATE-TASK.md`, `_TEMPLATE-STORY.md`,
  `_TEMPLATE-EPIC.md`) publish the same matrix inline so that
  template consumers do not need to cross-reference this file.

## Forbidden

- Writing the `**Status:**` line via direct `Files.writeString` or
  ad-hoc regex — always go through `StatusFieldParser` so the atomic
  `.tmp + rename(ATOMIC_MOVE)` contract is honoured.
- Mutating `execution-state.json` as if it were the SoT. The JSON is
  telemetry — a write there without the matching Markdown write is a
  violation of RULE-046-07.
- Introducing a new status value outside the six-valued enum. Adding
  a status requires an ADR update, an enum change, a matrix change,
  and a template update in a single story.
- Swallowing a write failure (`IOException`, partial `.tmp` write).
  Parsers MUST fail loud with `StatusSyncException` carrying the
  path and the attempted transition (RULE-046-08).
- Bypassing the matrix and allowing `Concluída → Pendente` or
  `Concluída → Planejada` — these are not reopen paths, they are
  data loss.

## Audit Command

```bash
# Validate a single artifact against the matrix:
mvn -pl java test -Dtest=LifecycleIntegrityAuditTest

# Scan a directory of skills / artifacts (story-0046-0007):
java -cp java/target/classes:java/target/test-classes \
    dev.iadev.application.lifecycle.LifecycleAuditRunner \
    java/src/main/resources/targets/claude/skills
```

**Exit codes:** 0 = AUDIT PASSED, 1 = AUDIT FAILED (violations
listed).
