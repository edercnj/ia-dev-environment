package dev.iadev.domain.taskmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WaveTest {

    @Test
    void wave_storesOrdinalAndNodes() {
        TaskNode n = new TaskNode(Set.of("X"), "x");
        Wave w = new Wave(1, List.of(n));
        assertThat(w.ordinal()).isEqualTo(1);
        assertThat(w.size()).isEqualTo(1);
        assertThat(w.nodes()).containsExactly(n);
    }

    @Test
    void wave_supportsMultipleNodes() {
        TaskNode a = new TaskNode(Set.of("A"), "a");
        TaskNode b = new TaskNode(Set.of("B"), "b");
        Wave w = new Wave(2, List.of(a, b));
        assertThat(w.size()).isEqualTo(2);
    }

    @Test
    void zeroOrLowerOrdinal_throwsIllegalArgument() {
        TaskNode n = new TaskNode(Set.of("X"), "x");
        assertThatThrownBy(() -> new Wave(0, List.of(n)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ordinal");
        assertThatThrownBy(() -> new Wave(-1, List.of(n)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyNodes_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Wave(1, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nodes");
    }

    @Test
    void nodes_areDefensivelyCopiedAndImmutable() {
        TaskNode n = new TaskNode(Set.of("X"), "x");
        List<TaskNode> mutable = new ArrayList<>(List.of(n));
        Wave w = new Wave(1, mutable);
        mutable.clear();
        assertThat(w.nodes()).containsExactly(n);
        assertThatThrownBy(() -> w.nodes().add(n))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullNodes_throwsNullPointer() {
        assertThatThrownBy(() -> new Wave(1, null))
                .isInstanceOf(NullPointerException.class);
    }
}
