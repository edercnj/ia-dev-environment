package dev.iadev.domain.taskmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kahn's algorithm — repeatedly takes all nodes with zero remaining in-degree as the next
 * wave, then removes them. Within a wave, nodes are sorted by primary TASK-ID for
 * deterministic output (idempotency contract).
 */
final class KahnSorter {

    private KahnSorter() {
        // static-only
    }

    static List<Wave> waves(List<TaskNode> nodes, Set<Edge> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, TaskNode> byKey = new HashMap<>();
        for (TaskNode n : nodes) {
            String key = TopologicalSorter.nodeKey(n);
            inDegree.put(key, 0);
            byKey.put(key, n);
        }
        for (Edge e : edges) {
            inDegree.merge(e.to(), 1, Integer::sum);
        }
        Map<String, List<String>> adj = CycleDetector.adjacency(nodes, edges);
        return drainWaves(inDegree, byKey, adj);
    }

    private static List<Wave> drainWaves(Map<String, Integer> inDegree,
            Map<String, TaskNode> byKey, Map<String, List<String>> adj) {
        List<Wave> waves = new ArrayList<>();
        int ordinal = 1;
        while (!inDegree.isEmpty()) {
            List<String> ready = new ArrayList<>();
            for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
                if (e.getValue() == 0) {
                    ready.add(e.getKey());
                }
            }
            ready.sort(Comparator.naturalOrder());
            waves.add(new Wave(ordinal++, ready.stream().map(byKey::get).toList()));
            for (String key : ready) {
                inDegree.remove(key);
                for (String next : adj.getOrDefault(key, List.of())) {
                    inDegree.computeIfPresent(next, (k, v) -> v - 1);
                }
            }
        }
        return waves;
    }
}
