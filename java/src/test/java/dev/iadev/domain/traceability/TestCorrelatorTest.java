package dev.iadev.domain.traceability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TestCorrelatorTest {

    @Nested
    class EmptyInput {

        @Test
        void correlate_emptyBoth_returnsEmptyList() {
            var result = TestCorrelator.correlate(
                    List.of(), List.of());

            assertThat(result).isEmpty();
        }

        @Test
        void correlate_nullRequirements_returnsEmptyList() {
            var result = TestCorrelator.correlate(
                    null, List.of());

            assertThat(result).isEmpty();
        }

        @Test
        void correlate_nullTestMethods_returnsEmptyList() {
            var result = TestCorrelator.correlate(
                    List.of(), null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class MappedByNamingConvention {

        @Test
        void correlate_atIdMatchesMethod_statusIsMapped() {
            var requirements = List.of(
                    new StoryRequirement(
                            "@GK-1", "payment approved",
                            Optional.of("AT-1")));

            var testMethods = List.of(
                    new TestMethod(
                            "PaymentAcceptanceTest",
                            "at1_pagamentoAprovadoRetorna200",
                            Optional.of("AT-1"),
                            List.of()));

            var result = TestCorrelator.correlate(
                    requirements, testMethods);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status())
                    .isEqualTo(TraceabilityStatus.MAPPED);
            assertThat(result.getFirst().gherkinId())
                    .isEqualTo("@GK-1");
            assertThat(result.getFirst().testClassName())
                    .hasValue("PaymentAcceptanceTest");
            assertThat(result.getFirst().testMethodName())
                    .hasValue(
                            "at1_pagamentoAprovadoRetorna200");
        }
    }

    @Nested
    class MappedByTag {

        @Test
        void correlate_tagMatchesAtId_statusIsMapped() {
            var requirements = List.of(
                    new StoryRequirement(
                            "@GK-1", "payment approved",
                            Optional.of("AT-1")));

            var testMethods = List.of(
                    new TestMethod(
                            "PaymentAcceptanceTest",
                            "testApprovedPayment",
                            Optional.of("AT-1"),
                            List.of("AT-1")));

            var result = TestCorrelator.correlate(
                    requirements, testMethods);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status())
                    .isEqualTo(TraceabilityStatus.MAPPED);
        }
    }

    @Nested
    class UnmappedRequirement {

        @Test
        void correlate_noMatchingTest_statusIsUnmapped() {
            var requirements = List.of(
                    new StoryRequirement(
                            "@GK-3", "timeout",
                            Optional.of("AT-3")));

            var testMethods = List.of(
                    new TestMethod(
                            "PaymentAcceptanceTest",
                            "at1_approved",
                            Optional.of("AT-1"),
                            List.of()));

            var result = TestCorrelator.correlate(
                    requirements, testMethods);

            var unmapped = result.stream()
                    .filter(e -> e.gherkinId().equals("@GK-3"))
                    .findFirst().orElseThrow();

            assertThat(unmapped.status()).isEqualTo(
                    TraceabilityStatus.UNMAPPED_REQUIREMENT);
            assertThat(unmapped.testClassName()).isEmpty();
            assertThat(unmapped.testMethodName()).isEmpty();
        }
    }

    @Nested
    class UnmappedTest {

        @Test
        void correlate_testWithoutAtLink_statusIsUnmapped() {
            var requirements = List.of(
                    new StoryRequirement(
                            "@GK-1", "payment approved",
                            Optional.of("AT-1")));

            var testMethods = List.of(
                    new TestMethod(
                            "PaymentAcceptanceTest",
                            "at1_approved",
                            Optional.of("AT-1"),
                            List.of()),
                    new TestMethod(
                            "PaymentControllerTest",
                            "shouldHandleNullAmount",
                            Optional.empty(),
                            List.of()));

            var result = TestCorrelator.correlate(
                    requirements, testMethods);

            var unmappedTests = result.stream()
                    .filter(e -> e.status()
                            == TraceabilityStatus.UNMAPPED_TEST)
                    .toList();

            assertThat(unmappedTests).hasSize(1);
            assertThat(unmappedTests.getFirst().testClassName())
                    .hasValue("PaymentControllerTest");
            assertThat(unmappedTests.getFirst().testMethodName())
                    .hasValue("shouldHandleNullAmount");
        }
    }

    @Nested
    class ComplexScenario {

        @Test
        void correlate_mixedStatuses_allClassified() {
            var requirements = List.of(
                    new StoryRequirement(
                            "@GK-1", "approved",
                            Optional.of("AT-1")),
                    new StoryRequirement(
                            "@GK-2", "denied",
                            Optional.of("AT-2")),
                    new StoryRequirement(
                            "@GK-3", "timeout",
                            Optional.of("AT-3")));

            var testMethods = List.of(
                    new TestMethod(
                            "PaymentTest", "at1_approved",
                            Optional.of("AT-1"), List.of()),
                    new TestMethod(
                            "PaymentTest", "at2_denied",
                            Optional.of("AT-2"), List.of()),
                    new TestMethod(
                            "UtilTest", "shouldFormat",
                            Optional.empty(), List.of()));

            var result = TestCorrelator.correlate(
                    requirements, testMethods);

            var mapped = result.stream()
                    .filter(e -> e.status()
                            == TraceabilityStatus.MAPPED)
                    .toList();
            assertThat(mapped).hasSize(2);

            var unmappedReqs = result.stream()
                    .filter(e -> e.status()
                            == TraceabilityStatus
                            .UNMAPPED_REQUIREMENT)
                    .toList();
            assertThat(unmappedReqs).hasSize(1);
            assertThat(unmappedReqs.getFirst().gherkinId())
                    .isEqualTo("@GK-3");

            var unmappedTests = result.stream()
                    .filter(e -> e.status()
                            == TraceabilityStatus.UNMAPPED_TEST)
                    .toList();
            assertThat(unmappedTests).hasSize(1);
            assertThat(unmappedTests.getFirst().testClassName())
                    .hasValue("UtilTest");
        }
    }

    @Nested
    class RequirementWithoutAtId {

        @Test
        void correlate_reqWithoutAtId_statusIsUnmapped() {
            var requirements = List.of(
                    new StoryRequirement(
                            "@GK-1", "no AT linkage",
                            Optional.empty()));

            var testMethods = List.of(
                    new TestMethod(
                            "SomeTest", "at1_method",
                            Optional.of("AT-1"), List.of()));

            var result = TestCorrelator.correlate(
                    requirements, testMethods);

            var reqEntry = result.stream()
                    .filter(e -> e.gherkinId().equals("@GK-1"))
                    .findFirst().orElseThrow();

            assertThat(reqEntry.status()).isEqualTo(
                    TraceabilityStatus.UNMAPPED_REQUIREMENT);
        }
    }
}
