package dev.iadev.domain.implementationmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Validates structural integrity of the dependency DAG.
 *
 * <p>Performs three checks:
 * <ol>
 *   <li><b>Symmetry</b>: auto-corrects asymmetric edges and
 *       emits warnings</li>
 *   <li><b>Cycle detection</b>: DFS with three-color marking
 *       (WHITE/GRAY/BLACK)</li>
 *   <li><b>Root validation</b>: at least one node must have
 *       empty blockedBy</li>
 * </ol>
 */
public final class DagValidator {

    private static final int WHITE = 0;
    private static final int GRAY = 1;
    private static final int BLACK = 2;

    private DagValidator() {
    }

    /**
     * Validates symmetry of DAG edges, auto-correcting
     * mismatches.
     *
     * @param dag the DAG to validate
     * @return list of warnings for asymmetries found
     */
    public static List<DagWarning> validateSymmetry(
            Map<String, DagNode> dag) {
        var warnings = new ArrayList<DagWarning>();
        checkBlocksSymmetry(dag, warnings);
        checkBlockedBySymmetry(dag, warnings);
        return warnings;
    }

    /**
     * Detects cycles in the DAG using DFS with three-color
     * marking.
     *
     * @param dag the DAG to check
     * @throws CircularDependencyException if a cycle is found
     */
    public static void detectCycles(Map<String, DagNode> dag) {
        var colors = new HashMap<String, Integer>();
        for (var id : dag.keySet()) {
            colors.put(id, WHITE);
        }

        for (var id : dag.keySet()) {
            if (colors.get(id) == WHITE) {
                dfsVisit(id, dag, colors, new ArrayList<>());
            }
        }
    }

    /**
     * Validates that at least one root node (no dependencies)
     * exists.
     *
     * @param dag the DAG to validate
     * @throws InvalidDagException if no root nodes exist
     */
    public static void validateRoots(Map<String, DagNode> dag) {
        if (dag.isEmpty()) {
            return;
        }

        boolean hasRoot = dag.values().stream()
                .anyMatch(n -> n.blockedBy().isEmpty());

        if (!hasRoot) {
            throw new InvalidDagException(
                    "DAG has no root nodes: "
                    + "every node has dependencies");
        }
    }

    /**
     * Validates that all IDs referenced in blockedBy exist as
     * nodes.
     *
     * @param dag the DAG to validate
     * @return list of error messages for missing references
     */
    public static List<String> validateReferences(
            Map<String, DagNode> dag) {
        var errors = new ArrayList<String>();
        for (var node : dag.values()) {
            for (var depId : node.blockedBy()) {
                if (!dag.containsKey(depId)) {
                    errors.add(
                            ("Story '%s' references "
                            + "non-existent dependency '%s'")
                            .formatted(node.storyId(), depId));
                }
            }
        }
        return errors;
    }

    /**
     * Runs all validations: references, symmetry, cycles,
     * roots.
     *
     * @param dag the DAG to validate
     * @return list of non-blocking warnings
     * @throws CircularDependencyException if cycle detected
     * @throws InvalidDagException if no roots or bad references
     */
    public static List<DagWarning> validate(
            Map<String, DagNode> dag) {
        var refErrors = validateReferences(dag);
        if (!refErrors.isEmpty()) {
            throw new InvalidDagException(
                    String.join("; ", refErrors));
        }

        var warnings = validateSymmetry(dag);
        detectCycles(dag);
        validateRoots(dag);
        return warnings;
    }

    private static void checkBlocksSymmetry(
            Map<String, DagNode> dag,
            List<DagWarning> warnings) {
        for (var entry : dag.entrySet()) {
            var nodeId = entry.getKey();
            var node = entry.getValue();
            for (var targetId : node.blocks()) {
                var target = dag.get(targetId);
                if (target == null) {
                    continue;
                }
                var blockedBySet =
                        new HashSet<>(target.blockedBy());
                if (!blockedBySet.contains(nodeId)) {
                    target.blockedBy().add(nodeId);
                    warnings.add(new DagWarning(
                            DagWarning.Type.ASYMMETRIC_DEPENDENCY,
                            ("%s blocks %s, but %s missing %s "
                            + "in blockedBy").formatted(
                                    nodeId, targetId,
                                    targetId, nodeId)
                    ));
                }
            }
        }
    }

    private static void checkBlockedBySymmetry(
            Map<String, DagNode> dag,
            List<DagWarning> warnings) {
        for (var entry : dag.entrySet()) {
            var nodeId = entry.getKey();
            var node = entry.getValue();
            for (var depId : node.blockedBy()) {
                var dep = dag.get(depId);
                if (dep == null) {
                    continue;
                }
                var blocksSet = new HashSet<>(dep.blocks());
                if (!blocksSet.contains(nodeId)) {
                    dep.blocks().add(nodeId);
                    warnings.add(new DagWarning(
                            DagWarning.Type.ASYMMETRIC_DEPENDENCY,
                            ("%s lists %s in blockedBy, but %s "
                            + "missing %s in blocks").formatted(
                                    nodeId, depId,
                                    depId, nodeId)
                    ));
                }
            }
        }
    }

    private static void dfsVisit(
            String nodeId,
            Map<String, DagNode> dag,
            Map<String, Integer> colors,
            List<String> stack) {
        colors.put(nodeId, GRAY);
        stack.add(nodeId);

        var node = dag.get(nodeId);
        if (node != null) {
            for (var neighborId : node.blocks()) {
                var color = colors.getOrDefault(neighborId, WHITE);
                if (color == GRAY) {
                    int cycleStart = stack.indexOf(neighborId);
                    var cycle = new ArrayList<>(
                            stack.subList(cycleStart, stack.size()));
                    throw new CircularDependencyException(cycle);
                }
                if (color == WHITE) {
                    dfsVisit(neighborId, dag, colors, stack);
                }
            }
        }

        stack.removeLast();
        colors.put(nodeId, BLACK);
    }
}
