package dev.iadev.domain.taskmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Builds coalesced groups (union-find on COALESCED reciprocal pairs) and the resulting
 * collapsed {@link TaskNode} list. Reciprocity is validated upstream by
 * {@link TopologicalSorter}, so this class trusts the input and only computes the union.
 */
final class CoalescedDetector {

    private CoalescedDetector() {
        // static-only
    }

    static List<Set<String>> detectGroups(Map<String, RawTask> byId) {
        Map<String, Set<String>> rep = new HashMap<>();
        for (String id : byId.keySet()) {
            rep.put(id, new TreeSet<>(Set.of(id)));
        }
        for (RawTask t : byId.values()) {
            if (!t.isCoalescedDeclaration()) {
                continue;
            }
            for (String partner : t.testabilityReferenceIds()) {
                union(rep, t.taskId(), partner);
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(
                new TreeMap<>(rep).values().stream()
                        .filter(g -> g.size() > 1)
                        .toList()));
    }

    static List<TaskNode> buildNodes(
            Map<String, RawTask> byId, List<Set<String>> coalesced) {
        Set<String> grouped = new HashSet<>();
        coalesced.forEach(grouped::addAll);
        List<TaskNode> nodes = new ArrayList<>();
        for (Set<String> group : coalesced) {
            String title = group.stream().sorted().map(id -> byId.get(id).title())
                    .reduce((a, b) -> a + " + " + b).orElse("(coalesced)");
            nodes.add(new TaskNode(group, title));
        }
        for (RawTask t : byId.values()) {
            if (!grouped.contains(t.taskId())) {
                nodes.add(new TaskNode(Set.of(t.taskId()), t.title()));
            }
        }
        nodes.sort((a, b) -> TopologicalSorter.nodeKey(a)
                .compareTo(TopologicalSorter.nodeKey(b)));
        return nodes;
    }

    private static void union(Map<String, Set<String>> rep, String a, String b) {
        Set<String> ga = rep.get(a);
        Set<String> gb = rep.get(b);
        if (ga == gb) {
            return;
        }
        ga.addAll(gb);
        for (String member : gb) {
            rep.put(member, ga);
        }
    }
}
