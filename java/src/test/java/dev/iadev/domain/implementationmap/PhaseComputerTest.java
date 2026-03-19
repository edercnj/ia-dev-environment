package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhaseComputerTest {

    @Nested
    class EmptyDag {

        @Test
        void compute_emptyDag_returnsEmptyPhases() {
            var phases = PhaseComputer.compute(
                    new LinkedHashMap<>());

            assertThat(phases).isEmpty();
        }
    }

    @Nested
    class LinearDag {

        @Test
        void compute_linearABC_threePhases() {
            // A -> B -> C (linear chain)
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("A", new DagNode("A", "Root",
                    new ArrayList<>(),
                    new ArrayList<>(List.of("B"))));
            dag.put("B", new DagNode("B", "Mid",
                    new ArrayList<>(List.of("A")),
                    new ArrayList<>(List.of("C"))));
            dag.put("C", new DagNode("C", "Leaf",
                    new ArrayList<>(List.of("B")),
                    new ArrayList<>()));

            var phases = PhaseComputer.compute(dag);

            assertThat(phases).hasSize(3);
            assertThat(phases.get(0)).containsExactly("A");
            assertThat(phases.get(1)).containsExactly("B");
            assertThat(phases.get(2)).containsExactly("C");
        }

        @Test
        void compute_linearABC_nodesHaveCorrectPhase() {
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("A", new DagNode("A", "Root",
                    new ArrayList<>(),
                    new ArrayList<>(List.of("B"))));
            dag.put("B", new DagNode("B", "Mid",
                    new ArrayList<>(List.of("A")),
                    new ArrayList<>(List.of("C"))));
            dag.put("C", new DagNode("C", "Leaf",
                    new ArrayList<>(List.of("B")),
                    new ArrayList<>()));

            PhaseComputer.compute(dag);

            assertThat(dag.get("A").phase()).isEqualTo(0);
            assertThat(dag.get("B").phase()).isEqualTo(1);
            assertThat(dag.get("C").phase()).isEqualTo(2);
        }
    }

    @Nested
    class ParallelDag {

        @Test
        void compute_twoRootsOneDep_twoPhases() {
            // A, B -> C
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("A", new DagNode("A", "Root A",
                    new ArrayList<>(),
                    new ArrayList<>(List.of("C"))));
            dag.put("B", new DagNode("B", "Root B",
                    new ArrayList<>(),
                    new ArrayList<>(List.of("C"))));
            dag.put("C", new DagNode("C", "Dep",
                    new ArrayList<>(List.of("A", "B")),
                    new ArrayList<>()));

            var phases = PhaseComputer.compute(dag);

            assertThat(phases).hasSize(2);
            assertThat(phases.get(0))
                    .containsExactlyInAnyOrder("A", "B");
            assertThat(phases.get(1)).containsExactly("C");
        }
    }

    @Nested
    class FiveStoryDag {

        @Test
        void compute_fiveStories_threePhases() {
            var dag = buildFiveStoryDag();

            var phases = PhaseComputer.compute(dag);

            assertThat(phases).hasSize(3);
            assertThat(phases.get(0))
                    .containsExactlyInAnyOrder("s-001", "s-002");
            assertThat(phases.get(1))
                    .containsExactlyInAnyOrder("s-003", "s-004");
            assertThat(phases.get(2))
                    .containsExactly("s-005");
        }
    }

    @Nested
    class ErrorCases {

        @Test
        void compute_unresolvedNodes_throwsInvalidDag() {
            // Unresolvable: A depends on non-existent ref
            // but ref is in blockedBy list
            var dag = new LinkedHashMap<String, DagNode>();
            dag.put("A", new DagNode("A", "Node",
                    new ArrayList<>(List.of("MISSING")),
                    new ArrayList<>()));

            assertThatThrownBy(
                    () -> PhaseComputer.compute(dag))
                    .isInstanceOf(InvalidDagException.class)
                    .hasMessageContaining("unresolvable");
        }
    }

    private LinkedHashMap<String, DagNode> buildFiveStoryDag() {
        var dag = new LinkedHashMap<String, DagNode>();
        dag.put("s-001", new DagNode("s-001", "Root A",
                new ArrayList<>(),
                new ArrayList<>(List.of("s-003", "s-004"))));
        dag.put("s-002", new DagNode("s-002", "Root B",
                new ArrayList<>(),
                new ArrayList<>(List.of("s-003"))));
        dag.put("s-003", new DagNode("s-003", "Mid AB",
                new ArrayList<>(List.of("s-001", "s-002")),
                new ArrayList<>(List.of("s-005"))));
        dag.put("s-004", new DagNode("s-004", "Mid A",
                new ArrayList<>(List.of("s-001")),
                new ArrayList<>(List.of("s-005"))));
        dag.put("s-005", new DagNode("s-005", "Leaf",
                new ArrayList<>(List.of("s-003", "s-004")),
                new ArrayList<>()));
        return dag;
    }
}
