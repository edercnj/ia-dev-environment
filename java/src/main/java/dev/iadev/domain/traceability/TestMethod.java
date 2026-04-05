package dev.iadev.domain.traceability;

import java.util.List;
import java.util.Optional;

/**
 * A test method discovered by scanning Java source files.
 *
 * @param className  the test class name
 * @param methodName the test method name
 * @param linkedAtId optional AT-N identifier found in name or
 *                   annotation
 * @param tags       list of {@code @Tag} annotation values
 */
public record TestMethod(
        String className,
        String methodName,
        Optional<String> linkedAtId,
        List<String> tags) {

    /**
     * Compact constructor enforcing immutability and non-null
     * invariants.
     */
    public TestMethod {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException(
                    "className must not be null or blank");
        }
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException(
                    "methodName must not be null or blank");
        }
        if (linkedAtId == null) {
            linkedAtId = Optional.empty();
        }
        tags = (tags != null)
                ? List.copyOf(tags) : List.of();
    }
}
