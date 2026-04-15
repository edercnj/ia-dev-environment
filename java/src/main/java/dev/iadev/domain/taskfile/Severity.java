package dev.iadev.domain.taskfile;

/**
 * Severity of a {@link ValidationViolation}.
 *
 * <p>{@link #ERROR} violations invalidate a task file
 * ({@link TaskFileValidationResult#valid()} returns {@code false}).
 * {@link #WARN} violations are advisory and do not invalidate the file.</p>
 */
public enum Severity {
    ERROR,
    WARN
}
