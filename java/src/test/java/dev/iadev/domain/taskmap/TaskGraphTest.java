package dev.iadev.domain.taskmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

class TaskGraphTest {

    private static TaskNode singleNode(String id) {
        return new TaskNode(Set.of(id), id);
    }

    @Nested
    class Construction {

        @Test
        void singleNode_zeroEdges_isValid() {
            TaskNode n = singleNode("A");
            TaskGraph g = new TaskGraph(
                    List.of(n), Set.of(), List.of(),
                    List.of(new Wave(1, List.of(n))));
            assertThat(g.totalTasks()).isEqualTo(1);
            assertThat(g.largestWaveSize()).isEqualTo(1);
            assertThat(g.estimatedSpeedup()).isEqualTo(1.0);
        }

        @Test
        void emptyNodes_throwsIllegalArgument() {
            assertThatThrownBy(() -> new TaskGraph(
                    List.of(), Set.of(), List.of(),
                    List.of(new Wave(1, List.of(singleNode("A"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one node");
        }

        @Test
        void nullCollections_throwNullPointer() {
            TaskNode n = singleNode("A");
            List<Wave> waves = List.of(new Wave(1, List.of(n)));
            assertThatThrownBy(() -> new TaskGraph(null, Set.of(), List.of(), waves))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new TaskGraph(List.of(n), null, List.of(), waves))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new TaskGraph(List.of(n), Set.of(), null, waves))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new TaskGraph(List.of(n), Set.of(), List.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Metrics {

        @Test
        void totalTasks_countsCoalescedTasksIndividually() {
            TaskNode single = singleNode("A");
            TaskNode coalesced = new TaskNode(Set.of("B", "C"), "B+C");
            TaskGraph g = new TaskGraph(
                    List.of(single, coalesced), Set.of(), List.of(Set.of("B", "C")),
                    List.of(new Wave(1, List.of(single)),
                            new Wave(2, List.of(coalesced))));
            assertThat(g.totalTasks()).isEqualTo(3);
        }

        @Test
        void largestWaveSize_returnsMaxAcrossWaves() {
            TaskNode a = singleNode("A");
            TaskNode b = singleNode("B");
            TaskNode c = singleNode("C");
            TaskGraph g = new TaskGraph(
                    List.of(a, b, c),
                    Set.of(new Edge("A", "C"), new Edge("B", "C")),
                    List.of(),
                    List.of(new Wave(1, List.of(a, b)), new Wave(2, List.of(c))));
            assertThat(g.largestWaveSize()).isEqualTo(2);
        }

        @Test
        void estimatedSpeedup_isTotalTasksOverWaves() {
            TaskNode a = singleNode("A");
            TaskNode b = singleNode("B");
            TaskNode c = singleNode("C");
            TaskGraph g = new TaskGraph(
                    List.of(a, b, c),
                    Set.of(new Edge("A", "C"), new Edge("B", "C")),
                    List.of(),
                    List.of(new Wave(1, List.of(a, b)), new Wave(2, List.of(c))));
            assertThat(g.estimatedSpeedup()).isEqualTo(1.5);
        }

        @Test
        void estimatedSpeedup_returnsZeroWhenWavesEmpty() {
            TaskGraph g = new TaskGraph(
                    List.of(singleNode("A")), Set.of(), List.of(), List.of());
            assertThat(g.estimatedSpeedup()).isEqualTo(0.0);
            assertThat(g.largestWaveSize()).isEqualTo(0);
        }

        @Test
        void coalescedGroups_areDefensivelyCopied() {
            TaskGraph g = new TaskGraph(
                    List.of(singleNode("A")), Set.of(), List.of(Set.of("X", "Y")),
                    List.of(new Wave(1, List.of(singleNode("A")))));
            assertThatThrownBy(() -> g.coalescedGroups().get(0).add("Z"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
