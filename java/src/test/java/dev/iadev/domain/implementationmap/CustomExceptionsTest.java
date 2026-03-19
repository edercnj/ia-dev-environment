package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomExceptionsTest {

    @Nested
    class CircularDependencyExceptionTests {

        @Test
        void constructor_messageContainsCycleChain() {
            var ex = new CircularDependencyException(
                    List.of("A", "B", "C"));

            assertThat(ex.getMessage())
                    .contains("A -> B -> C -> A");
        }

        @Test
        void getCycle_returnsImmutableList() {
            var ex = new CircularDependencyException(
                    List.of("X", "Y"));

            assertThat(ex.getCycle())
                    .containsExactly("X", "Y");
            assertThatThrownBy(
                    () -> ex.getCycle().add("Z"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }

        @Test
        void toString_containsCycle() {
            var ex = new CircularDependencyException(
                    List.of("A", "B"));

            assertThat(ex.toString()).contains("[A, B]");
        }

        @Test
        void constructor_emptyCycle_noArrow() {
            var ex = new CircularDependencyException(
                    List.of());

            assertThat(ex.getMessage())
                    .contains("Circular dependency detected");
        }
    }

    @Nested
    class InvalidDagExceptionTests {

        @Test
        void constructor_messagePreserved() {
            var ex = new InvalidDagException(
                    "No root nodes found");

            assertThat(ex.getMessage())
                    .isEqualTo("No root nodes found");
        }

        @Test
        void toString_containsMessage() {
            var ex = new InvalidDagException("test error");

            assertThat(ex.toString())
                    .contains("test error");
        }
    }

    @Nested
    class MapParseExceptionTests {

        @Test
        void constructor_messagePreserved() {
            var ex = new MapParseException(
                    "Missing table header");

            assertThat(ex.getMessage())
                    .isEqualTo("Missing table header");
        }

        @Test
        void toString_containsMessage() {
            var ex = new MapParseException("bad format");

            assertThat(ex.toString())
                    .contains("bad format");
        }
    }
}
