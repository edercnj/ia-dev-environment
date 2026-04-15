package dev.iadev.domain.taskmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TaskNodeTest {

    @Test
    void singleTaskNode_isNotCoalesced() {
        TaskNode n = new TaskNode(Set.of("TASK-0038-0002-001"), "Schema doc");
        assertThat(n.isCoalesced()).isFalse();
        assertThat(n.taskIds()).containsExactly("TASK-0038-0002-001");
        assertThat(n.title()).isEqualTo("Schema doc");
    }

    @Test
    void multiTaskNode_isCoalesced() {
        TaskNode n = new TaskNode(
                Set.of("TASK-0038-0002-004", "TASK-0038-0002-005"), "Writer + Generator");
        assertThat(n.isCoalesced()).isTrue();
        assertThat(n.taskIds()).hasSize(2);
    }

    @Test
    void taskIds_areDefensivelyCopiedAndImmutable() {
        Set<String> mutable = new HashSet<>(Set.of("TASK-0038-0002-001"));
        TaskNode n = new TaskNode(mutable, "x");
        mutable.add("TASK-0038-0002-002");
        assertThat(n.taskIds()).containsExactly("TASK-0038-0002-001");
        assertThatThrownBy(() -> n.taskIds().add("TASK-Y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyTaskIds_throwsIllegalArgument() {
        assertThatThrownBy(() -> new TaskNode(Set.of(), "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskIds");
    }

    @Test
    void blankTitle_throwsIllegalArgument() {
        assertThatThrownBy(() -> new TaskNode(Set.of("X"), "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void nullTaskIds_throwsNullPointer() {
        assertThatThrownBy(() -> new TaskNode(null, "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullTitle_throwsNullPointer() {
        assertThatThrownBy(() -> new TaskNode(Set.of("X"), null))
                .isInstanceOf(NullPointerException.class);
    }
}
