package dev.iadev.domain.implementationmap;

import java.util.List;
import java.util.Map;

/**
 * Supports partial execution modes for epic orchestration.
 *
 * <p>Three modes:
 * <ul>
 *   <li>{@link Mode#FULL} — execute all stories</li>
 *   <li>{@link Mode#BY_PHASE} — execute only stories
 *       from a specific phase</li>
 *   <li>{@link Mode#BY_STORY} — execute only specific
 *       stories (with dependency validation)</li>
 * </ul>
 */
public final class PartialExecution {

    private PartialExecution() {
    }

    /** Execution mode discriminator. */
    public enum Mode {
        /** Execute all stories in all phases. */
        FULL,
        /** Execute only stories from a specific phase. */
        BY_PHASE,
        /** Execute only specific named stories. */
        BY_STORY
    }

    /**
     * Filters stories for a specific phase.
     *
     * @param phaseNumber the phase to filter
     * @param phases      phase-to-stories mapping
     * @return list of story IDs in the phase (empty if phase
     *         does not exist)
     */
    public static List<String> filterByPhase(
            int phaseNumber,
            Map<Integer, List<String>> phases) {
        var stories = phases.get(phaseNumber);
        if (stories == null) {
            return List.of();
        }
        return List.copyOf(stories);
    }

    /**
     * Filters stories by explicit story ID list, validating
     * each exists in the DAG.
     *
     * @param storyIds the story IDs to filter
     * @param dag      the DAG for validation
     * @return list of validated story IDs
     * @throws InvalidDagException if any story ID is not
     *         in the DAG
     */
    public static List<String> filterByStory(
            List<String> storyIds,
            Map<String, DagNode> dag) {
        for (var id : storyIds) {
            if (!dag.containsKey(id)) {
                throw new InvalidDagException(
                        ("Story '%s' not found in "
                        + "implementation map").formatted(id));
            }
        }
        return List.copyOf(storyIds);
    }

    /**
     * Returns all story IDs (full execution mode).
     *
     * @param dag the DAG
     * @return list of all story IDs
     */
    public static List<String> filterFull(
            Map<String, DagNode> dag) {
        return List.copyOf(dag.keySet());
    }
}
