package dev.iadev.domain.implementationmap;

import java.util.List;
import java.util.Map;

/**
 * Consolidated result of the full implementation map pipeline.
 *
 * <p>Produced by
 * {@link ImplementationMapParser#parse(String)} after
 * parsing, building, validating, computing phases, and
 * finding the critical path.</p>
 *
 * @param stories      all DAG nodes keyed by story ID
 * @param phases       stories grouped by execution phase
 * @param criticalPath ordered list of critical path story IDs
 * @param totalPhases  total number of execution phases
 * @param warnings     non-blocking validation warnings
 */
public record ParsedMap(
        Map<String, DagNode> stories,
        Map<Integer, List<String>> phases,
        List<String> criticalPath,
        int totalPhases,
        List<DagWarning> warnings
) {

    /**
     * Creates a ParsedMap with defensive copies.
     */
    public ParsedMap {
        stories = Map.copyOf(stories);
        phases = Map.copyOf(phases);
        criticalPath = List.copyOf(criticalPath);
        warnings = List.copyOf(warnings);
    }
}
