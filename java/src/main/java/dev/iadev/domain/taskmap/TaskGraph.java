package dev.iadev.domain.taskmap;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable description of a story's task dependency graph plus its computed execution
 * waves and coalesced groups. Produced by the topological sort and consumed by the
 * markdown writer.
 *
 * @param nodes            all nodes (single-task or coalesced super-nodes)
 * @param edges            all directed dependencies between nodes (coalesced edges removed)
 * @param coalescedGroups  the original coalesced groups, kept for renderer transparency
 * @param waves            topological-sort output: ordered list of execution waves
 */
public record TaskGraph(
        List<TaskNode> nodes,
        Set<Edge> edges,
        List<Set<String>> coalescedGroups,
        List<Wave> waves) {

    public TaskGraph {
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(edges, "edges");
        Objects.requireNonNull(coalescedGroups, "coalescedGroups");
        Objects.requireNonNull(waves, "waves");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("graph must contain at least one node");
        }
        nodes = List.copyOf(nodes);
        edges = Set.copyOf(edges);
        coalescedGroups = coalescedGroups.stream().map(Set::copyOf).toList();
        waves = List.copyOf(waves);
    }

    public int totalTasks() {
        return nodes.stream().mapToInt(n -> n.taskIds().size()).sum();
    }

    public int largestWaveSize() {
        return waves.stream().mapToInt(Wave::size).max().orElse(0);
    }

    public double estimatedSpeedup() {
        if (waves.isEmpty()) {
            return 0.0;
        }
        return (double) totalTasks() / waves.size();
    }
}
