package dev.iadev.domain.implementationmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the critical path (longest dependency chain) in a DAG.
 *
 * <p>Uses topological-sort-based longest path algorithm via
 * dynamic programming. Also provides a method to mark nodes
 * on the critical path.</p>
 */
public final class CriticalPathFinder {

    private CriticalPathFinder() {
    }

    /**
     * Finds the critical path in the DAG.
     *
     * @param dag    the DAG with phases computed
     * @param phases phase mapping (used for context, not
     *               algorithm)
     * @return ordered list of story IDs on the critical path
     *         (root to leaf)
     */
    public static List<String> find(
            Map<String, DagNode> dag,
            Map<Integer, List<String>> phases) {
        if (dag.isEmpty()) {
            return List.of();
        }

        var sorted = topologicalSort(dag);
        var dist = new HashMap<String, Integer>();
        var pred = new HashMap<String, String>();

        for (var id : sorted) {
            dist.put(id, 0);
            pred.put(id, null);
        }

        for (var id : sorted) {
            var node = dag.get(id);
            if (node == null) {
                continue;
            }
            int currentDist = dist.getOrDefault(id, 0);
            for (var depId : node.blocks()) {
                int newDist = currentDist + 1;
                int depDist = dist.getOrDefault(depId, 0);
                if (newDist > depDist) {
                    dist.put(depId, newDist);
                    pred.put(depId, id);
                }
            }
        }

        int maxDist = -1;
        String endNode = "";
        for (var entry : dist.entrySet()) {
            if (entry.getValue() > maxDist) {
                maxDist = entry.getValue();
                endNode = entry.getKey();
            }
        }

        if (endNode.isEmpty()) {
            return List.of();
        }

        return reconstructPath(endNode, pred);
    }

    /**
     * Marks nodes on the critical path with
     * {@code isOnCriticalPath = true}.
     *
     * @param dag          the DAG
     * @param criticalPath the critical path IDs
     */
    public static void markCriticalPath(
            Map<String, DagNode> dag,
            List<String> criticalPath) {
        Set<String> pathSet = new HashSet<>(criticalPath);
        for (var node : dag.values()) {
            node.setOnCriticalPath(
                    pathSet.contains(node.storyId()));
        }
    }

    private static List<String> topologicalSort(
            Map<String, DagNode> dag) {
        var inDegree = new HashMap<String, Integer>();
        for (var entry : dag.entrySet()) {
            inDegree.put(entry.getKey(),
                    entry.getValue().blockedBy().size());
        }

        var queue = new ArrayList<String>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        var sorted = new ArrayList<String>();
        int front = 0;
        while (front < queue.size()) {
            var nodeId = queue.get(front++);
            sorted.add(nodeId);
            var node = dag.get(nodeId);
            if (node == null) {
                continue;
            }
            for (var depId : node.blocks()) {
                var currentDeg = inDegree.get(depId);
                if (currentDeg == null) {
                    continue;
                }
                int newDeg = currentDeg - 1;
                inDegree.put(depId, newDeg);
                if (newDeg == 0) {
                    queue.add(depId);
                }
            }
        }

        return sorted;
    }

    private static List<String> reconstructPath(
            String endNode,
            Map<String, String> predecessors) {
        var path = new ArrayList<String>();
        String current = endNode;
        while (current != null) {
            path.add(current);
            current = predecessors.get(current);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }
}
