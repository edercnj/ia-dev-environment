package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartialExecutionTest {

    @Nested
    class ByPhase {

        @Test
        void filterByPhase_existingPhase_returnsStories() {
            var phases = Map.of(
                    0, List.of("s-001", "s-002"),
                    1, List.of("s-003"));

            var result =
                    PartialExecution.filterByPhase(0, phases);

            assertThat(result)
                    .containsExactly("s-001", "s-002");
        }

        @Test
        void filterByPhase_phase1_returnsOnlyPhase1() {
            var phases = Map.of(
                    0, List.of("s-001", "s-002"),
                    1, List.of("s-003"),
                    2, List.of("s-004"));

            var result =
                    PartialExecution.filterByPhase(1, phases);

            assertThat(result).containsExactly("s-003");
        }

        @Test
        void filterByPhase_nonExistentPhase_returnsEmpty() {
            var phases = Map.of(0, List.of("s-001"));

            var result =
                    PartialExecution.filterByPhase(5, phases);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ByStory {

        @Test
        void filterByStory_validIds_returnsAll() {
            var dag = buildSimpleDag();

            var result = PartialExecution.filterByStory(
                    List.of("s-001"), dag);

            assertThat(result).containsExactly("s-001");
        }

        @Test
        void filterByStory_invalidId_throwsInvalidDag() {
            var dag = buildSimpleDag();

            assertThatThrownBy(
                    () -> PartialExecution.filterByStory(
                            List.of("MISSING"), dag))
                    .isInstanceOf(InvalidDagException.class)
                    .hasMessageContaining("MISSING")
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    class FullExecution {

        @Test
        void filterFull_whenCalled_returnsAllStoryIds() {
            var dag = buildSimpleDag();

            var result = PartialExecution.filterFull(dag);

            assertThat(result)
                    .containsExactlyInAnyOrder(
                            "s-001", "s-002");
        }
    }

    @Nested
    class ModeEnum {

        @Test
        void mode_whenCalled_hasThreeValues() {
            assertThat(PartialExecution.Mode.values())
                    .hasSize(3)
                    .containsExactly(
                            PartialExecution.Mode.FULL,
                            PartialExecution.Mode.BY_PHASE,
                            PartialExecution.Mode.BY_STORY);
        }
    }

    private LinkedHashMap<String, DagNode> buildSimpleDag() {
        var dag = new LinkedHashMap<String, DagNode>();
        dag.put("s-001", new DagNode("s-001", "Root",
                Optional.empty(),
                new ArrayList<>(),
                new ArrayList<>(List.of("s-002"))));
        dag.put("s-002", new DagNode("s-002", "Child",
                Optional.empty(),
                new ArrayList<>(List.of("s-001")),
                new ArrayList<>()));
        return dag;
    }
}
