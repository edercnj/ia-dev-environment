package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutableStoriesTest {

    @Nested
    class NoCompleted {

        @Test
        void filter_noCompleted_returnsOnlyRoots() {
            var dag = buildThreeNodeDag();

            var result = ExecutableStories.filter(
                    dag, Set.of());

            assertThat(result).containsExactly("s-001");
        }
    }

    @Nested
    class SomeCompleted {

        @Test
        void filter_rootCompleted_returnsChildren() {
            var dag = buildThreeNodeDag();

            var result = ExecutableStories.filter(
                    dag, Set.of("s-001"));

            assertThat(result).containsExactly("s-002");
        }

        @Test
        void filter_allButLeafCompleted_returnsLeaf() {
            var dag = buildThreeNodeDag();

            var result = ExecutableStories.filter(
                    dag, Set.of("s-001", "s-002"));

            assertThat(result).containsExactly("s-003");
        }
    }

    @Nested
    class AllCompleted {

        @Test
        void filter_allCompleted_returnsEmpty() {
            var dag = buildThreeNodeDag();

            var result = ExecutableStories.filter(
                    dag,
                    Set.of("s-001", "s-002", "s-003"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CriticalPathPriority {

        @Test
        void filter_criticalPathFirst_sortedCorrectly() {
            var dag = new LinkedHashMap<String, DagNode>();
            var nodeA = new DagNode("s-001", "A",
                    new ArrayList<>(), new ArrayList<>());
            var nodeB = new DagNode("s-002", "B",
                    new ArrayList<>(), new ArrayList<>());
            nodeB.setOnCriticalPath(true);
            dag.put("s-001", nodeA);
            dag.put("s-002", nodeB);

            var result = ExecutableStories.filter(
                    dag, Set.of());

            // s-002 (on critical path) should come first
            assertThat(result)
                    .containsExactly("s-002", "s-001");
        }
    }

    private LinkedHashMap<String, DagNode> buildThreeNodeDag() {
        var dag = new LinkedHashMap<String, DagNode>();
        dag.put("s-001", new DagNode("s-001", "Root",
                new ArrayList<>(),
                new ArrayList<>(List.of("s-002"))));
        dag.put("s-002", new DagNode("s-002", "Mid",
                new ArrayList<>(List.of("s-001")),
                new ArrayList<>(List.of("s-003"))));
        dag.put("s-003", new DagNode("s-003", "Leaf",
                new ArrayList<>(List.of("s-002")),
                new ArrayList<>()));
        return dag;
    }
}
