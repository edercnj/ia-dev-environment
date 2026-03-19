package dev.iadev.domain.implementationmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Determines which stories are ready for execution given
 * the set of completed stories.
 *
 * <p>A story is executable when all its dependencies are
 * in the completed set. Results are sorted with critical
 * path stories first, then alphabetically.</p>
 */
public final class ExecutableStories {

    private ExecutableStories() {
    }

    /**
     * Filters stories that are ready for execution.
     *
     * @param dag              the DAG
     * @param completedStories set of story IDs already completed
     * @return sorted list of executable story IDs
     */
    public static List<String> filter(
            Map<String, DagNode> dag,
            Set<String> completedStories) {
        var executable = new ArrayList<String>();

        for (var entry : dag.entrySet()) {
            var id = entry.getKey();
            var node = entry.getValue();

            if (completedStories.contains(id)) {
                continue;
            }

            boolean allDepsComplete = node.blockedBy().stream()
                    .allMatch(completedStories::contains);

            if (allDepsComplete) {
                executable.add(id);
            }
        }

        executable.sort(criticalPathComparator(dag));
        return List.copyOf(executable);
    }

    private static Comparator<String> criticalPathComparator(
            Map<String, DagNode> dag) {
        return (a, b) -> {
            var nodeA = dag.get(a);
            var nodeB = dag.get(b);
            boolean aOnCp = nodeA != null
                    && nodeA.isOnCriticalPath();
            boolean bOnCp = nodeB != null
                    && nodeB.isOnCriticalPath();
            if (aOnCp && !bOnCp) {
                return -1;
            }
            if (!aOnCp && bOnCp) {
                return 1;
            }
            return a.compareTo(b);
        };
    }
}
