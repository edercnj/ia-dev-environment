package dev.iadev.domain.taskmap;

import java.util.Objects;

/**
 * Directed edge in a {@link TaskGraph}: the {@code from} task is a prerequisite of
 * {@code to}. Mirrors a {@code Depends on} entry from a task file's §4.
 *
 * @param from prerequisite TASK-ID
 * @param to   consumer TASK-ID
 */
public record Edge(String from, String to) {

    public Edge {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isBlank()) {
            throw new IllegalArgumentException("from must not be blank");
        }
        if (to.isBlank()) {
            throw new IllegalArgumentException("to must not be blank");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException(
                    "self-loop: from and to are equal ('" + from + "')");
        }
    }
}
