package dev.iadev.domain.traceability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestMethodScannerTest {

    @Nested
    class EmptyInput {

        @Test
        void scan_nullSourceCode_returnsEmptyList() {
            var result = TestMethodScanner.scan(
                    "SomeTest", null);

            assertThat(result).isEmpty();
        }

        @Test
        void scan_emptySourceCode_returnsEmptyList() {
            var result = TestMethodScanner.scan(
                    "SomeTest", "");

            assertThat(result).isEmpty();
        }

        @Test
        void scan_blankSourceCode_returnsEmptyList() {
            var result = TestMethodScanner.scan(
                    "SomeTest", "   \n  ");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class NamingConvention {

        @Test
        void scan_atPrefixMethod_extractsLinkedAtId() {
            var source = """
                    class PaymentAcceptanceTest {
                        @Test
                        void at1_pagamentoAprovadoRetorna200() {
                            // test body
                        }
                    }
                    """;

            var result = TestMethodScanner.scan(
                    "PaymentAcceptanceTest", source);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().methodName())
                    .isEqualTo(
                            "at1_pagamentoAprovadoRetorna200");
            assertThat(result.getFirst().linkedAtId())
                    .hasValue("AT-1");
        }

        @Test
        void scan_multipleAtMethods_allExtracted() {
            var source = """
                    class PaymentAcceptanceTest {
                        @Test
                        void at1_approved() {}

                        @Test
                        void at2_denied() {}

                        @Test
                        void at3_timeout() {}
                    }
                    """;

            var result = TestMethodScanner.scan(
                    "PaymentAcceptanceTest", source);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).linkedAtId())
                    .hasValue("AT-1");
            assertThat(result.get(1).linkedAtId())
                    .hasValue("AT-2");
            assertThat(result.get(2).linkedAtId())
                    .hasValue("AT-3");
        }

        @Test
        void scan_shouldPrefixMethod_noLinkedAtId() {
            var source = """
                    class PaymentControllerTest {
                        @Test
                        void shouldHandleNullAmount() {
                            // test body
                        }
                    }
                    """;

            var result = TestMethodScanner.scan(
                    "PaymentControllerTest", source);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().methodName())
                    .isEqualTo("shouldHandleNullAmount");
            assertThat(result.getFirst().linkedAtId()).isEmpty();
        }
    }

    @Nested
    class TagAnnotation {

        @Test
        void scan_tagAnnotation_extractsLinkedAtId() {
            var source = """
                    class PaymentAcceptanceTest {
                        @Test
                        @Tag("AT-1")
                        void testApprovedPayment() {
                            // test body
                        }
                    }
                    """;

            var result = TestMethodScanner.scan(
                    "PaymentAcceptanceTest", source);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().linkedAtId())
                    .hasValue("AT-1");
            assertThat(result.getFirst().tags())
                    .containsExactly("AT-1");
        }

        @Test
        void scan_multipleTagAnnotations_allCaptured() {
            var source = """
                    class PaymentAcceptanceTest {
                        @Test
                        @Tag("AT-1")
                        @Tag("regression")
                        void testApprovedPayment() {
                            // test body
                        }
                    }
                    """;

            var result = TestMethodScanner.scan(
                    "PaymentAcceptanceTest", source);

            assertThat(result.getFirst().tags())
                    .contains("AT-1", "regression");
        }
    }

    @Nested
    class MixedMethods {

        @Test
        void scan_mixedMethods_allDiscovered() {
            var source = """
                    class PaymentTest {
                        @Test
                        void at1_approved() {}

                        @Test
                        @Tag("AT-2")
                        void testDenied() {}

                        @Test
                        void shouldHandleNull() {}
                    }
                    """;

            var result = TestMethodScanner.scan(
                    "PaymentTest", source);

            assertThat(result).hasSize(3);

            var linked = result.stream()
                    .filter(m -> m.linkedAtId().isPresent())
                    .toList();
            assertThat(linked).hasSize(2);

            var unlinked = result.stream()
                    .filter(m -> m.linkedAtId().isEmpty())
                    .toList();
            assertThat(unlinked).hasSize(1);
            assertThat(unlinked.getFirst().methodName())
                    .isEqualTo("shouldHandleNull");
        }
    }

    @Nested
    class ClassNamePropagation {

        @Test
        void scan_className_propagatedToAllMethods() {
            var source = """
                    class MyTest {
                        @Test
                        void method1() {}

                        @Test
                        void method2() {}
                    }
                    """;

            var result = TestMethodScanner.scan(
                    "MyTest", source);

            assertThat(result).allSatisfy(m ->
                    assertThat(m.className())
                            .isEqualTo("MyTest"));
        }
    }

    @Nested
    class NoTestMethods {

        @Test
        void scan_classWithoutTestAnnotation_returnsEmpty() {
            var source = """
                    class Helper {
                        void helperMethod() {}
                        static String format() {
                            return "";
                        }
                    }
                    """;

            var result = TestMethodScanner.scan(
                    "Helper", source);

            assertThat(result).isEmpty();
        }
    }
}
