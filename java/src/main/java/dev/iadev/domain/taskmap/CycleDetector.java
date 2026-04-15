package dev.iadev.domain.taskmap;

import dev.iadev.domain.taskmap.exception.CyclicDependencyException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DFS back-edge detector. Throws {@link CyclicDependencyException} on the first cycle
 * encountered, carrying the discovered path so the operator can break it.
 */
final class CycleDetector {

    private CycleDetector() {
        // static-only
    }

    static void check(List<TaskNode> nodes, Set<Edge> edges) {
        Map<String, List<String>> adj = adjacency(nodes, edges);
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();
        for (TaskNode n : nodes) {
            String key = TopologicalSorter.nodeKey(n);
            if (!visited.contains(key)) {
                dfs(key, adj, visited, stack, new ArrayDeque<>());
            }
        }
    }

    private static void dfs(String node, Map<String, List<String>> adj,
            Set<String> visited, Set<String> stack, Deque<String> path) {
        visited.add(node);
        stack.add(node);
        path.push(node);
        for (String next : adj.getOrDefault(node, List.of())) {
            if (stack.contains(next)) {
                throw new CyclicDependencyException(buildCyclePath(path, next));
            }
            if (!visited.contains(next)) {
                dfs(next, adj, visited, stack, path);
            }
        }
        stack.remove(node);
        path.pop();
    }

    private static List<String> buildCyclePath(Deque<String> path, String closingNode) {
        List<String> reversed = new ArrayList<>(path);
        Collections.reverse(reversed);
        int start = reversed.indexOf(closingNode);
        if (start < 0) {
            start = 0;
        }
        List<String> cycle = new ArrayList<>(reversed.subList(start, reversed.size()));
        cycle.add(closingNode);
        return cycle;
    }

    static Map<String, List<String>> adjacency(List<TaskNode> nodes, Set<Edge> edges) {
        Map<String, List<String>> adj = new HashMap<>();
        for (TaskNode n : nodes) {
            adj.put(TopologicalSorter.nodeKey(n), new ArrayList<>());
        }
        for (Edge e : edges) {
            adj.get(e.from()).add(e.to());
        }
        return adj;
    }
}
