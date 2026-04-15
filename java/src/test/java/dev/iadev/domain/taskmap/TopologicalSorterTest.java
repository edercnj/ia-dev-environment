package dev.iadev.domain.taskmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.taskfile.TestabilityKind;
import dev.iadev.domain.taskmap.exception.CyclicDependencyException;
import dev.iadev.domain.taskmap.exception.InvalidCoalescenceException;
import dev.iadev.domain.taskmap.exception.MissingTaskReferenceException;
import dev.iadev.domain.taskmap.exception.SelfLoopException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TopologicalSorterTest {

    private static RawTask task(String id, String title, List<String> deps,
            TestabilityKind kind, List<String> refs) {
        return new RawTask(id, title, deps, kind, refs);
    }

    private static RawTask independent(String id, String title, List<String> deps) {
        return task(id, title, deps, TestabilityKind.INDEPENDENT, List.of());
    }

    @Nested
    class HappyPath {

        @Test
        void singleTask_producesSingleWaveSingleNode() {
            TaskGraph g = TopologicalSorter.sort(List.of(
                    independent("TASK-0038-0002-001", "schema doc", List.of())));
            assertThat(g.nodes()).hasSize(1);
            assertThat(g.edges()).isEmpty();
            assertThat(g.waves()).hasSize(1);
            assertThat(g.waves().get(0).nodes()).hasSize(1);
            assertThat(g.coalescedGroups()).isEmpty();
            assertThat(g.estimatedSpeedup()).isEqualTo(1.0);
        }

        @Test
        void linearDependencyChain_producesOneTaskPerWave() {
            TaskGraph g = TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of()),
                    independent("T002", "b", List.of("T001")),
                    independent("T003", "c", List.of("T002"))));
            assertThat(g.waves()).hasSize(3);
            assertThat(g.waves().get(0).nodes().get(0).taskIds()).containsExactly("T001");
            assertThat(g.waves().get(1).nodes().get(0).taskIds()).containsExactly("T002");
            assertThat(g.waves().get(2).nodes().get(0).taskIds()).containsExactly("T003");
        }

        @Test
        void parallelTasks_inSameWave() {
            TaskGraph g = TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of()),
                    independent("T002", "b", List.of()),
                    independent("T003", "c", List.of("T001", "T002"))));
            assertThat(g.waves()).hasSize(2);
            assertThat(g.waves().get(0).size()).isEqualTo(2);
            assertThat(g.waves().get(1).size()).isEqualTo(1);
            assertThat(g.estimatedSpeedup()).isEqualTo(1.5);
        }

        @Test
        void coalescedPair_collapsesIntoSingleNode() {
            TaskGraph g = TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of()),
                    task("T002", "b", List.of("T001"),
                            TestabilityKind.COALESCED, List.of("T003")),
                    task("T003", "c", List.of("T001"),
                            TestabilityKind.COALESCED, List.of("T002"))));
            assertThat(g.coalescedGroups()).hasSize(1);
            assertThat(g.coalescedGroups().get(0)).containsExactlyInAnyOrder("T002", "T003");
            assertThat(g.nodes()).hasSize(2);
            assertThat(g.totalTasks()).isEqualTo(3);
            assertThat(g.waves()).hasSize(2);
        }

        @Test
        void edgeKey_usesLowestTaskIdOfCoalescedGroup() {
            TaskGraph g = TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of()),
                    task("T002", "b", List.of("T001"),
                            TestabilityKind.COALESCED, List.of("T003")),
                    task("T003", "c", List.of("T001"),
                            TestabilityKind.COALESCED, List.of("T002"))));
            assertThat(g.edges()).extracting(Edge::to)
                    .containsExactly("T002");
        }
    }

    @Nested
    class Validation {

        @Test
        void selfLoop_throwsSelfLoopException() {
            assertThatThrownBy(() -> TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of("T001")))))
                    .isInstanceOf(SelfLoopException.class)
                    .extracting(e -> ((SelfLoopException) e).taskId())
                    .isEqualTo("T001");
        }

        @Test
        void missingReference_throwsMissingTaskReferenceException() {
            assertThatThrownBy(() -> TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of("T999")))))
                    .isInstanceOf(MissingTaskReferenceException.class);
        }

        @Test
        void cycleNonCoalesced_throwsCyclicDependencyException() {
            assertThatThrownBy(() -> TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of("T002")),
                    independent("T002", "b", List.of("T001")))))
                    .isInstanceOf(CyclicDependencyException.class)
                    .satisfies(e -> assertThat(((CyclicDependencyException) e).cyclePath())
                            .contains("T001").contains("T002"));
        }

        @Test
        void asymmetricCoalescence_throwsInvalidCoalescenceException() {
            assertThatThrownBy(() -> TopologicalSorter.sort(List.of(
                    task("T001", "a", List.of(),
                            TestabilityKind.COALESCED, List.of("T002")),
                    independent("T002", "b", List.of()))))
                    .isInstanceOf(InvalidCoalescenceException.class)
                    .satisfies(e -> {
                        InvalidCoalescenceException ic = (InvalidCoalescenceException) e;
                        assertThat(ic.declaringTaskId()).isEqualTo("T001");
                        assertThat(ic.partnerTaskId()).isEqualTo("T002");
                    });
        }

        @Test
        void emptyInput_throwsIllegalArgument() {
            assertThatThrownBy(() -> TopologicalSorter.sort(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void nullInput_throwsNullPointer() {
            assertThatThrownBy(() -> TopologicalSorter.sort(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Determinism {

        @Test
        void twoSortsOfSameInput_produceEquivalentGraphs() {
            List<RawTask> input = List.of(
                    independent("T003", "c", List.of("T001", "T002")),
                    independent("T001", "a", List.of()),
                    independent("T002", "b", List.of()));
            TaskGraph g1 = TopologicalSorter.sort(input);
            TaskGraph g2 = TopologicalSorter.sort(input);
            assertThat(g1.nodes()).isEqualTo(g2.nodes());
            assertThat(g1.edges()).isEqualTo(g2.edges());
            assertThat(g1.waves()).isEqualTo(g2.waves());
        }

        @Test
        void inputOrderShuffled_producesSameWaveContents() {
            TaskGraph g1 = TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of()),
                    independent("T002", "b", List.of("T001"))));
            TaskGraph g2 = TopologicalSorter.sort(List.of(
                    independent("T002", "b", List.of("T001")),
                    independent("T001", "a", List.of())));
            assertThat(g1.waves()).isEqualTo(g2.waves());
        }
    }
}
