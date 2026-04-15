package dev.iadev.domain.taskmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.taskfile.TestabilityKind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RawTaskTest {

    @Test
    void coalescedDeclaration_isCoalescedTrue() {
        RawTask t = new RawTask("T001", "a", List.of(),
                TestabilityKind.COALESCED, List.of("T002"));
        assertThat(t.isCoalescedDeclaration()).isTrue();
    }

    @Test
    void independentDeclaration_isCoalescedFalse() {
        RawTask t = new RawTask("T001", "a", List.of(),
                TestabilityKind.INDEPENDENT, List.of());
        assertThat(t.isCoalescedDeclaration()).isFalse();
    }

    @Test
    void nullKind_isCoalescedFalse() {
        RawTask t = new RawTask("T001", "a", List.of(), null, List.of());
        assertThat(t.isCoalescedDeclaration()).isFalse();
    }

    @Test
    void dependencies_areDefensivelyCopiedAndImmutable() {
        List<String> mutable = new ArrayList<>(List.of("T002"));
        RawTask t = new RawTask("T001", "a", mutable, null, List.of());
        mutable.add("T999");
        assertThat(t.dependencies()).containsExactly("T002");
        assertThatThrownBy(() -> t.dependencies().add("T999"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void blankTaskId_throwsIllegalArgument() {
        assertThatThrownBy(() -> new RawTask("  ", "a", List.of(), null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId");
    }

    @Test
    void blankTitle_throwsIllegalArgument() {
        assertThatThrownBy(() -> new RawTask("T001", " ", List.of(), null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void nullCollections_throwNullPointer() {
        assertThatThrownBy(() -> new RawTask("T001", "a", null, null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RawTask("T001", "a", List.of(), null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
