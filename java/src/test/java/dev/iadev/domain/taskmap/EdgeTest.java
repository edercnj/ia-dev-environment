package dev.iadev.domain.taskmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EdgeTest {

    @Test
    void validEdge_storesEndpoints() {
        Edge e = new Edge("TASK-0038-0002-001", "TASK-0038-0002-002");
        assertThat(e.from()).isEqualTo("TASK-0038-0002-001");
        assertThat(e.to()).isEqualTo("TASK-0038-0002-002");
    }

    @Test
    void selfLoop_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Edge("TASK-0038-0002-001", "TASK-0038-0002-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("self-loop");
    }

    @Test
    void blankFrom_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Edge("  ", "TASK-Y"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

    @Test
    void blankTo_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Edge("TASK-X", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to");
    }

    @Test
    void nullFrom_throwsNullPointer() {
        assertThatThrownBy(() -> new Edge(null, "TASK-Y"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullTo_throwsNullPointer() {
        assertThatThrownBy(() -> new Edge("TASK-X", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void edgeEquality_basedOnEndpoints() {
        assertThat(new Edge("A", "B")).isEqualTo(new Edge("A", "B"));
        assertThat(new Edge("A", "B")).isNotEqualTo(new Edge("B", "A"));
    }
}
