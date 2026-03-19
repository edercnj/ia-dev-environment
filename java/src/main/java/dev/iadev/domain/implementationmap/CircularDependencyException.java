package dev.iadev.domain.implementationmap;

import java.util.List;

/**
 * Thrown when a circular dependency is detected in the DAG.
 *
 * <p>The cycle list contains the IDs forming the cycle. The
 * message includes a human-readable chain representation
 * (e.g., "A -> B -> C -> A").</p>
 */
public class CircularDependencyException extends RuntimeException {

    private final List<String> cycle;

    /**
     * Creates a circular dependency exception with the cycle IDs.
     *
     * @param cycle list of story IDs forming the cycle
     */
    public CircularDependencyException(List<String> cycle) {
        super(buildMessage(cycle));
        this.cycle = List.copyOf(cycle);
    }

    /**
     * Returns the list of story IDs forming the cycle.
     *
     * @return unmodifiable list of cycle IDs
     */
    public List<String> getCycle() {
        return cycle;
    }

    @Override
    public String toString() {
        return "CircularDependencyException{cycle=%s}"
                .formatted(cycle);
    }

    private static String buildMessage(List<String> cycle) {
        var chain = String.join(" -> ", cycle);
        if (!cycle.isEmpty()) {
            chain += " -> " + cycle.getFirst();
        }
        return "Circular dependency detected: " + chain;
    }
}
