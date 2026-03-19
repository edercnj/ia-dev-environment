package dev.iadev.domain.implementationmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes execution phases from a validated DAG.
 *
 * <p>Uses iterative BFS-like resolution (Kahn's algorithm
 * adapted for layer computation):
 * <ul>
 *   <li>Phase 0: root nodes (no dependencies)</li>
 *   <li>Phase N: nodes whose dependencies are all in phases
 *       0..N-1</li>
 * </ul>
 */
public final class PhaseComputer {

    private PhaseComputer() {
    }

    /**
     * Computes phases for all nodes in the DAG.
     *
     * <p>Sets each node's phase via
     * {@link DagNode#setPhase(int)}.</p>
     *
     * @param dag the validated DAG
     * @return map of phase number to list of story IDs
     * @throws InvalidDagException if some stories cannot be
     *         resolved (indicates undetected cycle or missing dep)
     */
    public static Map<Integer, List<String>> compute(
            Map<String, DagNode> dag) {
        var phases = new LinkedHashMap<Integer, List<String>>();
        var resolved = new HashSet<String>();
        int currentPhase = 0;

        while (resolved.size() < dag.size()) {
            var phaseStories = new ArrayList<String>();

            for (var entry : dag.entrySet()) {
                var id = entry.getKey();
                var node = entry.getValue();
                if (resolved.contains(id)) {
                    continue;
                }
                if (allDepsResolved(node, resolved)) {
                    phaseStories.add(id);
                    node.setPhase(currentPhase);
                }
            }

            if (phaseStories.isEmpty()) {
                throw new InvalidDagException(
                        buildDiagnostic(dag, resolved));
            }

            phases.put(currentPhase, List.copyOf(phaseStories));
            resolved.addAll(phaseStories);
            currentPhase++;
        }

        return Map.copyOf(phases);
    }

    private static boolean allDepsResolved(
            DagNode node, HashSet<String> resolved) {
        return node.blockedBy().stream()
                .allMatch(resolved::contains);
    }

    private static String buildDiagnostic(
            Map<String, DagNode> dag,
            HashSet<String> resolved) {
        var unresolved = new ArrayList<String>();
        for (var id : dag.keySet()) {
            if (!resolved.contains(id)) {
                unresolved.add(id);
            }
        }
        return "Cannot compute phases: unresolvable stories (%s)"
                .formatted(String.join(", ", unresolved));
    }
}
