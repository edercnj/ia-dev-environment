package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalPathFinderTest {

    @Nested
    class EmptyDag {

        @Test
        void find_emptyDag_returnsEmptyList() {
            var result = CriticalPathFinder.find(
                    new LinkedHashMap<>(), Map.of());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SingleNode {

        @Test
        void find_singleRoot_returnsSingleElement() {
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("s-001", new DagNode("s-001", "Root",
                    Optional.empty(),
                    new ArrayList<>(), new ArrayList<>()));

            var result = CriticalPathFinder.find(
                    dag, Map.of(0, List.of("s-001")));

            assertThat(result).containsExactly("s-001");
        }
    }

    @Nested
    class LinearChain {

        @Test
        void find_linearABC_pathIsABC() {
            var dag = buildLinearDag();
            var phases = PhaseComputer.compute(dag);

            var result = CriticalPathFinder.find(dag, phases);

            assertThat(result)
                    .containsExactly("A", "B", "C");
        }
    }

    @Nested
    class BranchingDag {

        @Test
        void find_fiveStories_longestPathFound() {
            var dag = buildFiveStoryDag();
            var phases = PhaseComputer.compute(dag);

            var result = CriticalPathFinder.find(dag, phases);

            // Critical path: s-001 -> s-003 -> s-005
            // (length 3, vs s-001 -> s-004 -> s-005 also 3)
            // Both are valid; the path should have length 3
            assertThat(result).hasSize(3);
            assertThat(result.getFirst())
                    .isIn("s-001", "s-002");
            assertThat(result.getLast()).isEqualTo("s-005");
        }
    }

    @Nested
    class MarkCriticalPath {

        @Test
        void markCriticalPath_whenCalled_marksCorrectNodes() {
            var dag = buildFiveStoryDag();
            var phases = PhaseComputer.compute(dag);
            var criticalPath =
                    CriticalPathFinder.find(dag, phases);

            CriticalPathFinder.markCriticalPath(
                    dag, criticalPath);

            for (var id : criticalPath) {
                assertThat(dag.get(id).isOnCriticalPath())
                        .isTrue();
            }
        }

        @Test
        void markCriticalPath_whenCalled_nonCriticalNodesUnmarked() {
            var dag = buildFiveStoryDag();
            var phases = PhaseComputer.compute(dag);
            var criticalPath =
                    CriticalPathFinder.find(dag, phases);

            CriticalPathFinder.markCriticalPath(
                    dag, criticalPath);

            var criticalSet =
                    new java.util.HashSet<>(criticalPath);
            for (var node : dag.values()) {
                if (!criticalSet.contains(node.storyId())) {
                    assertThat(node.isOnCriticalPath())
                            .isFalse();
                }
            }
        }
    }

    private LinkedHashMap<String, DagNode> buildLinearDag() {
        var dag = new LinkedHashMap<String, DagNode>();
        dag.put("A", new DagNode("A", "Root",
                Optional.empty(),
                new ArrayList<>(),
                new ArrayList<>(List.of("B"))));
        dag.put("B", new DagNode("B", "Mid",
                Optional.empty(),
                new ArrayList<>(List.of("A")),
                new ArrayList<>(List.of("C"))));
        dag.put("C", new DagNode("C", "Leaf",
                Optional.empty(),
                new ArrayList<>(List.of("B")),
                new ArrayList<>()));
        return dag;
    }

    private LinkedHashMap<String, DagNode> buildFiveStoryDag() {
        var dag = new LinkedHashMap<String, DagNode>();
        dag.put("s-001", new DagNode("s-001", "Root A",
                Optional.empty(),
                new ArrayList<>(),
                new ArrayList<>(List.of("s-003", "s-004"))));
        dag.put("s-002", new DagNode("s-002", "Root B",
                Optional.empty(),
                new ArrayList<>(),
                new ArrayList<>(List.of("s-003"))));
        dag.put("s-003", new DagNode("s-003", "Mid AB",
                Optional.empty(),
                new ArrayList<>(List.of("s-001", "s-002")),
                new ArrayList<>(List.of("s-005"))));
        dag.put("s-004", new DagNode("s-004", "Mid A",
                Optional.empty(),
                new ArrayList<>(List.of("s-001")),
                new ArrayList<>(List.of("s-005"))));
        dag.put("s-005", new DagNode("s-005", "Leaf",
                Optional.empty(),
                new ArrayList<>(List.of("s-003", "s-004")),
                new ArrayList<>()));
        return dag;
    }
}
