package dev.iadev.domain.taskmap;

import java.util.Objects;
import java.util.Set;

/**
 * A node in a {@link TaskGraph}. Represents either a single task or a coalesced group of
 * tasks that must land in the same commit (RULE-TF-04). The collapsed shape is decided
 * by the topological sort, so a {@code TaskNode} is always either:
 *
 * <ul>
 *   <li>a single-task node ({@code taskIds} has size 1), or</li>
 *   <li>a coalesced super-node ({@code taskIds} has size ≥ 2).</li>
 * </ul>
 *
 * @param taskIds non-empty, immutable set of TASK-IDs covered by this node
 * @param title   short human-readable label used by the Mermaid renderer
 */
public record TaskNode(Set<String> taskIds, String title) {

    public TaskNode {
        Objects.requireNonNull(taskIds, "taskIds");
        Objects.requireNonNull(title, "title");
        if (taskIds.isEmpty()) {
            throw new IllegalArgumentException("taskIds must not be empty");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        taskIds = Set.copyOf(taskIds);
    }

    public boolean isCoalesced() {
        return taskIds.size() > 1;
    }
}
