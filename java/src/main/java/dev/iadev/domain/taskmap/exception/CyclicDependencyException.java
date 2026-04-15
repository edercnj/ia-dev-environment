package dev.iadev.domain.taskmap.exception;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when the topological sort detects a cycle that is NOT a declared coalescence.
 * Carries the discovered cycle path for actionable diagnostics.
 */
public final class CyclicDependencyException extends TaskMapGenerationException {

    private static final long serialVersionUID = 1L;

    private final List<String> cyclePath;

    public CyclicDependencyException(List<String> cyclePath) {
        super(buildMessage(cyclePath));
        this.cyclePath = List.copyOf(cyclePath);
    }

    public List<String> cyclePath() {
        return cyclePath;
    }

    private static String buildMessage(List<String> path) {
        Objects.requireNonNull(path, "cyclePath");
        if (path.isEmpty()) {
            throw new IllegalArgumentException("cyclePath must not be empty");
        }
        return "Ciclo detectado: " + String.join(" -> ", path)
                + ". Declare COALESCED ou quebre dep.";
    }
}
