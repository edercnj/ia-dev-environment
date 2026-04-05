package dev.iadev.domain.traceability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestMethodTest {

    @Nested
    class Construction {

        @Test
        void create_withAllFields_accessorsReturnValues() {
            var method = new TestMethod(
                    "PaymentAcceptanceTest",
                    "at1_paymentApproved",
                    Optional.of("AT-1"),
                    List.of("AT-1"));

            assertThat(method.className())
                    .isEqualTo("PaymentAcceptanceTest");
            assertThat(method.methodName())
                    .isEqualTo("at1_paymentApproved");
            assertThat(method.linkedAtId())
                    .hasValue("AT-1");
            assertThat(method.tags())
                    .containsExactly("AT-1");
        }

        @Test
        void create_withNullLinkedAtId_defaultsToEmpty() {
            var method = new TestMethod(
                    "SomeTest", "shouldWork",
                    null, List.of());

            assertThat(method.linkedAtId()).isEmpty();
        }

        @Test
        void create_withNullTags_defaultsToEmptyList() {
            var method = new TestMethod(
                    "SomeTest", "shouldWork",
                    Optional.empty(), null);

            assertThat(method.tags()).isEmpty();
        }

        @Test
        void create_tagsAreImmutable_copyOfUsed() {
            var tags = new java.util.ArrayList<String>();
            tags.add("AT-1");
            var method = new TestMethod(
                    "SomeTest", "shouldWork",
                    Optional.empty(), tags);

            assertThat(method.tags())
                    .containsExactly("AT-1");
            tags.add("AT-2");
            assertThat(method.tags()).hasSize(1);
        }
    }

    @Nested
    class Validation {

        @Test
        void create_nullClassName_throwsException() {
            assertThatThrownBy(() -> new TestMethod(
                    null, "method",
                    Optional.empty(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void create_blankClassName_throwsException() {
            assertThatThrownBy(() -> new TestMethod(
                    " ", "method",
                    Optional.empty(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void create_nullMethodName_throwsException() {
            assertThatThrownBy(() -> new TestMethod(
                    "TestClass", null,
                    Optional.empty(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("methodName");
        }

        @Test
        void create_blankMethodName_throwsException() {
            assertThatThrownBy(() -> new TestMethod(
                    "TestClass", "",
                    Optional.empty(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("methodName");
        }
    }
}
