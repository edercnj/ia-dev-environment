# Implementation Plan — story-0038-0001

**Story:** Task as Primary Artifact (`task-TASK-XXXX-YYYY-NNN.md` schema)
**Branch:** `feat/story-0038-0001-task-as-primary-artifact`
**Phase:** 0 (Foundation — bottleneck for stories 0002, 0003, 0005, 0009)

## 1. Affected Layers and Components

| Layer | Component | Action |
| :--- | :--- | :--- |
| Documentation | `plans/epic-0038/schemas/task-schema.md` | Create |
| Documentation | `plans/epic-0038/examples/task-TASK-0037-0001-001.md` | Create (migrated example) |
| Domain (model) | `dev.iadev.domain.taskfile.*` | Create new sub-package |
| Test (unit) | `dev.iadev.domain.taskfile.*Test` | Create unit tests |
| Test (integration) | `dev.iadev.smoke.TaskExampleMigrationIT` | Create IT |
| Test (smoke fixture) | `java/src/test/resources/smoke/epic-0038/examples/task-TASK-0037-0001-001.md` | Create reference fixture |

Pure additive change. Zero touch in existing classes.

## 2. New Classes and Interfaces

Package: `dev.iadev.domain.taskfile`

| Class | Type | Responsibility |
| :--- | :--- | :--- |
| `TestabilityKind` | enum | `INDEPENDENT`, `REQUIRES_MOCK`, `COALESCED` |
| `Severity` | enum | `ERROR`, `WARN` |
| `ValidationViolation` | record | `String ruleId`, `Severity severity`, `String message` |
| `TaskFileValidationResult` | record | `String taskId`, `boolean valid`, `List<ValidationViolation> violations`, `TestabilityKind testabilityKind` (nullable) |
| `ParsedTaskFile` | record | Internal extraction result: id, story, status, sections (objective, inputs, outputs, dod, dependencies, plan ref), testability checkboxes |
| `TaskFileParser` | class | Reads markdown content, extracts sections via regex, delegates to `TaskValidator`, returns `TaskFileValidationResult` |
| `TaskValidator` | class | Composite of 6 rules; aggregates `List<ValidationViolation>` |
| `rules.IdMatchesFilenameRule` | class | TF-SCHEMA-001 |
| `rules.StatusInEnumRule` | class | TF-SCHEMA-002 |
| `rules.TestabilityExactlyOneRule` | class | TF-SCHEMA-003 |
| `rules.OutputsNotEmptyRule` | class | TF-SCHEMA-004 |
| `rules.DodMinSixItemsRule` | class | TF-SCHEMA-005 (WARN) |
| `rules.CoalescedReferencesValidIdRule` | class | TF-SCHEMA-006 |
| `rules.ValidationRule` | interface | `List<ValidationViolation> validate(ParsedTaskFile, ValidationContext)` |
| `ValidationContext` | record | Holds `String filename`, optional `Set<String> knownTaskIds` (used by TF-SCHEMA-006) |

Immutable records. Constructor injection. SRP per rule. No null returns (empty list on no violations).

## 3. Existing Classes Modified

None.

## 4. Method Signatures (key)

```java
public final class TaskFileParser {
    public static ParsedTaskFile parse(String markdown); // no I/O; pure string -> VO
}

public interface ValidationRule {
    List<ValidationViolation> validate(ParsedTaskFile parsed, ValidationContext ctx);
}

public final class TaskValidator {
    public TaskValidator(List<ValidationRule> rules);
    public List<ValidationViolation> validateAll(ParsedTaskFile parsed, ValidationContext ctx);
    public static TaskValidator defaultValidator(); // wires the 6 rules
}
```

Validation is performed by `TaskValidator` (returns `List<ValidationViolation>`); `TaskFileValidationResult` is the aggregate VO produced by callers that combine parsing + validation.

## 5. Dependency Direction

```
test -> domain.taskfile (parser, validator, VOs)
domain.taskfile -> standard library only (Path, regex, List)
```

Domain purity respected (no Jackson, no I/O annotations, no filesystem access). `TaskFileParser.parse(String)` is pure: it takes markdown content and returns `ParsedTaskFile`. Callers (tests, future skills) read the file outside the domain and pass the string in.

## 6. Integration Points

- None at runtime in this story (parser is a library, not invoked by pipeline).
- Future consumers (story-0038-0003 `x-task-plan`, story-0038-0005 `x-task-implement`) will depend on these VOs.

## 7. Database / API / Event Changes

None.

## 8. Configuration Changes

None.

## 9. TDD Strategy

Map per task — see `tests-story-0038-0001.md`. Each task is a Red-Green-Refactor cycle. Order:
1. TASK-001 (doc) — verification only (grep checks).
2. TASK-002 (VOs) — UT-1..UT-3 (record equality, valid derivation, immutability).
3. TASK-004 (Validator + 6 rules) — UT-4..UT-9 (one test per rule, pass + fail per rule).
4. TASK-003 (Parser) — UT-10..UT-13 (section extraction, delegation to validator, malformed input).
5. TASK-005 (example doc) — verification only.
6. TASK-006 (IT) — AT-1: parse migrated example -> valid=true, kind=INDEPENDENT.
7. TASK-007 (Golden) — smoke regeneration check.

TASK-004 precedes TASK-003 in execution order: validator can be tested in isolation; parser then composes it. (Story Section 8 lists 003 before 004 in numbering, but dependencies are equal — both depend only on TASK-002. We execute in the order that maximizes Red-Green rhythm: validator first.)

## 10. Mini-ADR — Parser placement in domain

**Context:** Schema parser must be in domain, which forbids I/O.
**Decision:** Place `TaskFileParser` in `dev.iadev.domain.taskfile` with a single pure API: `parse(String markdown) -> ParsedTaskFile`. No filesystem access inside the parser.
**Rationale:** Keeps the domain layer I/O-free and trivially unit-testable without temp files. File reading is the caller's responsibility (e.g., the IT reads the fixture and passes the string).
**Consequences:** Callers must do their own `Files.readString`. Acceptable — validator/parser stay pure.

## 11. Risk Assessment

| Risk | Mitigation |
| :--- | :--- |
| Schema drift between doc and parser | Golden file test (TASK-007) anchors example; example must round-trip valid. |
| Coverage shortfall (parser regex paths) | Negative tests for each rule + malformed sections + boundary cases. |
| Parser coupling to Java file I/O | Public API is `(filename, content)` String overload — testable without filesystem. |
