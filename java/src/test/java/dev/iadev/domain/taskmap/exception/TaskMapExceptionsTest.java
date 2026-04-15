package dev.iadev.domain.taskmap.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TaskMapExceptionsTest {

    @Nested
    class CyclicDependency {

        @Test
        void carriesPathInGetterAndMessage() {
            CyclicDependencyException e = new CyclicDependencyException(
                    List.of("T001", "T002", "T001"));
            assertThat(e.cyclePath()).containsExactly("T001", "T002", "T001");
            assertThat(e.getMessage())
                    .contains("T001 -> T002 -> T001")
                    .contains("COALESCED");
        }

        @Test
        void emptyPath_throwsIllegalArgument() {
            assertThatThrownBy(() -> new CyclicDependencyException(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void nullPath_throwsNullPointer() {
            assertThatThrownBy(() -> new CyclicDependencyException(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void cyclePath_isImmutable() {
            CyclicDependencyException e = new CyclicDependencyException(List.of("X"));
            assertThatThrownBy(() -> e.cyclePath().add("Y"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class SelfLoop {

        @Test
        void carriesTaskIdAndMessage() {
            SelfLoopException e = new SelfLoopException("T001");
            assertThat(e.taskId()).isEqualTo("T001");
            assertThat(e.getMessage()).contains("T001").contains("inválido");
        }

        @Test
        void nullTaskId_throwsNullPointer() {
            assertThatThrownBy(() -> new SelfLoopException(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class MissingTaskReference {

        @Test
        void carriesBothIdsAndMessage() {
            MissingTaskReferenceException e = new MissingTaskReferenceException(
                    "T001", "T999");
            assertThat(e.referencingTaskId()).isEqualTo("T001");
            assertThat(e.missingTaskId()).isEqualTo("T999");
            assertThat(e.getMessage()).contains("T001").contains("T999");
        }

        @Test
        void nullArgs_throwNullPointer() {
            assertThatThrownBy(() -> new MissingTaskReferenceException(null, "X"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new MissingTaskReferenceException("X", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class InvalidCoalescence {

        @Test
        void carriesBothIdsAndMessage() {
            InvalidCoalescenceException e = new InvalidCoalescenceException("T001", "T002");
            assertThat(e.declaringTaskId()).isEqualTo("T001");
            assertThat(e.partnerTaskId()).isEqualTo("T002");
            assertThat(e.getMessage()).contains("T001").contains("T002");
        }

        @Test
        void nullArgs_throwNullPointer() {
            assertThatThrownBy(() -> new InvalidCoalescenceException(null, "X"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new InvalidCoalescenceException("X", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
