package dev.iadev.domain.taskmap;

import java.util.List;
import java.util.Objects;

/**
 * One execution wave of a {@link TaskGraph}: a set of {@link TaskNode}s with no
 * inter-dependencies, parallelisable by definition.
 *
 * @param ordinal 1-based wave number (wave 1 = nodes with zero dependencies)
 * @param nodes   non-empty, immutable list of nodes runnable in this wave
 */
public record Wave(int ordinal, List<TaskNode> nodes) {

    public Wave {
        Objects.requireNonNull(nodes, "nodes");
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal must be >= 1, got " + ordinal);
        }
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("nodes must not be empty");
        }
        nodes = List.copyOf(nodes);
    }

    public int size() {
        return nodes.size();
    }
}
