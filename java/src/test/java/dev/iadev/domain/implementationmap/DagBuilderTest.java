package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DagBuilderTest {

    @Nested
    class EmptyInput {

        @Test
        void build_emptyList_returnsEmptyMap() {
            var dag = DagBuilder.build(List.of());

            assertThat(dag).isEmpty();
        }
    }

    @Nested
    class SingleNode {

        @Test
        void build_singleRoot_createsOneNode() {
            var rows = List.of(
                    new DependencyMatrixRow(
                            "s-001", "Root",
                            Optional.empty(), List.of()));

            var dag = DagBuilder.build(rows);

            assertThat(dag).hasSize(1);
            assertThat(dag.get("s-001").blockedBy()).isEmpty();
            assertThat(dag.get("s-001").blocks()).isEmpty();
        }

        @Test
        void build_singleRootWithJiraKey_propagatesKey() {
            var rows = List.of(
                    new DependencyMatrixRow(
                            "s-001", "Root",
                            Optional.of("PROJ-42"),
                            List.of()));

            var dag = DagBuilder.build(rows);

            assertThat(dag.get("s-001").jiraKey())
                    .isPresent()
                    .hasValue("PROJ-42");
        }
    }

    @Nested
    class TwoNodes {

        @Test
        void build_aDependsOnB_reverseEdgeCreated() {
            var rows = List.of(
                    new DependencyMatrixRow(
                            "s-001", "Root",
                            Optional.empty(), List.of()),
                    new DependencyMatrixRow(
                            "s-002", "Child",
                            Optional.empty(),
                            List.of("s-001")));

            var dag = DagBuilder.build(rows);

            assertThat(dag).hasSize(2);
            // s-002 is blocked by s-001
            assertThat(dag.get("s-002").blockedBy())
                    .containsExactly("s-001");
            // reverse: s-001 blocks s-002
            assertThat(dag.get("s-001").blocks())
                    .containsExactly("s-002");
        }
    }

    @Nested
    class FiveNodes {

        @Test
        void build_fiveNodes_allEdgesResolved() {
            var rows = List.of(
                    new DependencyMatrixRow(
                            "s-001", "Root A",
                            Optional.empty(), List.of()),
                    new DependencyMatrixRow(
                            "s-002", "Root B",
                            Optional.empty(), List.of()),
                    new DependencyMatrixRow(
                            "s-003", "Mid",
                            Optional.empty(),
                            List.of("s-001", "s-002")),
                    new DependencyMatrixRow(
                            "s-004", "Mid2",
                            Optional.empty(),
                            List.of("s-001")),
                    new DependencyMatrixRow(
                            "s-005", "Leaf",
                            Optional.empty(),
                            List.of("s-003", "s-004")));

            var dag = DagBuilder.build(rows);

            assertThat(dag).hasSize(5);

            // Root A blocks Mid and Mid2
            assertThat(dag.get("s-001").blocks())
                    .containsExactlyInAnyOrder("s-003", "s-004");
            // Root B blocks Mid only
            assertThat(dag.get("s-002").blocks())
                    .containsExactly("s-003");
            // Mid blocked by Root A and Root B, blocks Leaf
            assertThat(dag.get("s-003").blockedBy())
                    .containsExactly("s-001", "s-002");
            assertThat(dag.get("s-003").blocks())
                    .containsExactly("s-005");
            // Leaf has no outbound edges
            assertThat(dag.get("s-005").blocks()).isEmpty();
        }
    }
}
