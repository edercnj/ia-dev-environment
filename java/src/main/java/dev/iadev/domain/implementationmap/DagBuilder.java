package dev.iadev.domain.implementationmap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a directed acyclic graph from dependency matrix rows.
 *
 * <p>Creates a {@link DagNode} for each row, populating both
 * {@code blockedBy} (inbound edges) and {@code blocks}
 * (outbound edges) for bidirectional traversal.</p>
 */
public final class DagBuilder {

    private DagBuilder() {
    }

    /**
     * Builds the DAG from parsed dependency matrix rows.
     *
     * <p>For each row, creates a node with its declared blockedBy
     * list. Then resolves reverse edges: if A is blockedBy B,
     * then B blocks A.</p>
     *
     * @param rows the parsed dependency matrix rows
     * @return map of storyId to DagNode (insertion-ordered)
     */
    public static Map<String, DagNode> build(
            List<DependencyMatrixRow> rows) {
        var dag = new LinkedHashMap<String, DagNode>();

        for (var row : rows) {
            dag.put(row.storyId(), new DagNode(
                    row.storyId(),
                    row.title(),
                    row.jiraKey(),
                    new ArrayList<>(row.blockedBy()),
                    new ArrayList<>()
            ));
        }

        resolveReverseEdges(dag);

        return dag;
    }

    private static void resolveReverseEdges(
            Map<String, DagNode> dag) {
        for (var node : dag.values()) {
            for (var depId : node.blockedBy()) {
                var dep = dag.get(depId);
                if (dep != null
                        && !dep.blocks().contains(node.storyId())) {
                    dep.blocks().add(node.storyId());
                }
            }
        }
    }
}
