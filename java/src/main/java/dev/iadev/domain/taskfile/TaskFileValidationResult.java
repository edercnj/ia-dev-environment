package dev.iadev.domain.taskfile;

import java.util.List;
import java.util.Objects;

/**
 * Result of validating a task file (per the schema documented in
 * {@code plans/epic-0038/schemas/task-schema.md}).
 *
 * <p>{@link #valid()} is derived from the violation list: a result is valid iff there are no
 * violations of {@link Severity#ERROR}. WARN-level violations do not invalidate the file.</p>
 *
 * <p>{@link #testabilityKind()} is non-null only when {@link #valid()} is {@code true}; for
 * invalid results it MAY still be populated when the parser was able to recognise a single
 * checked declaration, but callers MUST NOT rely on it in that case.</p>
 *
 * @param taskId          task identifier extracted from the file (or {@code "<unknown>"} if absent)
 * @param valid           true iff zero ERROR-level violations
 * @param violations      immutable list of violations (never null, possibly empty)
 * @param testabilityKind declared testability kind, or null if absent / ambiguous
 */
public record TaskFileValidationResult(
        String taskId,
        boolean valid,
        List<ValidationViolation> violations,
        TestabilityKind testabilityKind
) {

    public TaskFileValidationResult {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(violations, "violations");
        violations = List.copyOf(violations);
        if (taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
    }

    /**
     * Factory: builds a result deriving {@code valid} from the violation list. {@code valid} is
     * true iff there are no {@link Severity#ERROR}-level violations.
     */
    public static TaskFileValidationResult of(
            String taskId,
            List<ValidationViolation> violations,
            TestabilityKind testabilityKind) {
        boolean hasErrors = violations.stream()
                .anyMatch(v -> v.severity() == Severity.ERROR);
        return new TaskFileValidationResult(taskId, !hasErrors, violations, testabilityKind);
    }
}
