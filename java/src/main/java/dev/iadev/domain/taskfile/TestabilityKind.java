package dev.iadev.domain.taskfile;

/**
 * Declared testability of a task, per RULE-TF-01.
 *
 * <p>Each task file (Section 2.3) MUST mark exactly one of these declarations.
 * The mapping from the checked checkbox to this enum is defined in
 * {@code plans/epic-0038/schemas/task-schema.md} §3.</p>
 */
public enum TestabilityKind {

    /** "Independentemente testável" — task can be tested in full isolation. */
    INDEPENDENT,

    /** "Requer mock de TASK-XXXX-YYYY-NNN" — depends on a mocked surface from another task. */
    REQUIRES_MOCK,

    /** "Coalescível com TASK-XXXX-YYYY-NNN" — must land in the same commit as another task. */
    COALESCED
}
