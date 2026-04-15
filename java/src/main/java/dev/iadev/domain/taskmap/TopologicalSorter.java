package dev.iadev.domain.taskmap;

import dev.iadev.domain.taskmap.exception.InvalidCoalescenceException;
import dev.iadev.domain.taskmap.exception.MissingTaskReferenceException;
import dev.iadev.domain.taskmap.exception.SelfLoopException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Orchestrates the topological sort: validation -> coalesced detection -> node and edge
 * construction -> cycle detection -> Kahn waves. Pure and stateless; no I/O. Output is
 * deterministic — required for the idempotency contract of
 * {@code task-implementation-map-STORY-XXXX-YYYY.md} (story-0038-0002 schema §6).
 */
public final class TopologicalSorter {

    private TopologicalSorter() {
        // static-only
    }

    public static TaskGraph sort(List<RawTask> rawTasks) {
        Objects.requireNonNull(rawTasks, "rawTasks");
        if (rawTasks.isEmpty()) {
            throw new IllegalArgumentException("rawTasks must not be empty");
        }
        Map<String, RawTask> byId = indexById(rawTasks);
        validateNoSelfLoops(byId.values());
        validateAllReferences(byId);
        validateCoalescenceReciprocity(byId);
        List<Set<String>> coalesced = CoalescedDetector.detectGroups(byId);
        List<TaskNode> nodes = CoalescedDetector.buildNodes(byId, coalesced);
        Map<String, TaskNode> nodeOf = mapTaskIdToNode(nodes);
        Set<Edge> edges = buildEdges(byId, nodeOf);
        CycleDetector.check(nodes, edges);
        List<Wave> waves = KahnSorter.waves(nodes, edges);
        return new TaskGraph(nodes, edges, coalesced, waves);
    }

    static String nodeKey(TaskNode n) {
        return n.taskIds().stream().sorted().findFirst().orElseThrow();
    }

    private static Map<String, RawTask> indexById(List<RawTask> rawTasks) {
        Map<String, RawTask> idx = new TreeMap<>();
        for (RawTask t : rawTasks) {
            idx.put(t.taskId(), t);
        }
        return idx;
    }

    private static void validateNoSelfLoops(java.util.Collection<RawTask> tasks) {
        for (RawTask t : tasks) {
            if (t.dependencies().contains(t.taskId())) {
                throw new SelfLoopException(t.taskId());
            }
        }
    }

    private static void validateAllReferences(Map<String, RawTask> byId) {
        for (RawTask t : byId.values()) {
            for (String dep : t.dependencies()) {
                if (!byId.containsKey(dep)) {
                    throw new MissingTaskReferenceException(t.taskId(), dep);
                }
            }
        }
    }

    private static void validateCoalescenceReciprocity(Map<String, RawTask> byId) {
        for (RawTask t : byId.values()) {
            if (!t.isCoalescedDeclaration()) {
                continue;
            }
            for (String partner : t.testabilityReferenceIds()) {
                RawTask other = byId.get(partner);
                if (other == null || !other.isCoalescedDeclaration()
                        || !other.testabilityReferenceIds().contains(t.taskId())) {
                    throw new InvalidCoalescenceException(t.taskId(), partner);
                }
            }
        }
    }

    private static Map<String, TaskNode> mapTaskIdToNode(List<TaskNode> nodes) {
        Map<String, TaskNode> map = new HashMap<>();
        for (TaskNode n : nodes) {
            for (String id : n.taskIds()) {
                map.put(id, n);
            }
        }
        return map;
    }

    private static Set<Edge> buildEdges(
            Map<String, RawTask> byId, Map<String, TaskNode> nodeOf) {
        Set<Edge> edges = new TreeSet<>(
                Comparator.<Edge, String>comparing(Edge::from).thenComparing(Edge::to));
        for (RawTask t : byId.values()) {
            String toKey = nodeKey(nodeOf.get(t.taskId()));
            for (String dep : t.dependencies()) {
                String fromKey = nodeKey(nodeOf.get(dep));
                if (!fromKey.equals(toKey)) {
                    edges.add(new Edge(fromKey, toKey));
                }
            }
        }
        return edges;
    }
}
