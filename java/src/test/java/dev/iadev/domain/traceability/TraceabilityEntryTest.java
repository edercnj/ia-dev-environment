package dev.iadev.domain.traceability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceabilityEntryTest {

    @Nested
    class Construction {

        @Test
        void create_withAllFields_accessorsReturnValues() {
            var entry = new TraceabilityEntry(
                    "@GK-1",
                    Optional.of("AT-1"),
                    Optional.of("PaymentTest"),
                    Optional.of("at1_approved"),
                    TraceabilityStatus.MAPPED);

            assertThat(entry.gherkinId()).isEqualTo("@GK-1");
            assertThat(entry.acceptanceTestId())
                    .hasValue("AT-1");
            assertThat(entry.testClassName())
                    .hasValue("PaymentTest");
            assertThat(entry.testMethodName())
                    .hasValue("at1_approved");
            assertThat(entry.status())
                    .isEqualTo(TraceabilityStatus.MAPPED);
        }

        @Test
        void create_nullOptionals_defaultToEmpty() {
            var entry = new TraceabilityEntry(
                    "@GK-1", null, null, null,
                    TraceabilityStatus.UNMAPPED_REQUIREMENT);

            assertThat(entry.acceptanceTestId()).isEmpty();
            assertThat(entry.testClassName()).isEmpty();
            assertThat(entry.testMethodName()).isEmpty();
        }
    }

    @Nested
    class Validation {

        @Test
        void create_nullGherkinId_throwsException() {
            assertThatThrownBy(() -> new TraceabilityEntry(
                    null, Optional.empty(), Optional.empty(),
                    Optional.empty(), TraceabilityStatus.MAPPED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gherkinId");
        }

        @Test
        void create_blankGherkinId_throwsException() {
            assertThatThrownBy(() -> new TraceabilityEntry(
                    "", Optional.empty(), Optional.empty(),
                    Optional.empty(), TraceabilityStatus.MAPPED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gherkinId");
        }

        @Test
        void create_nullStatus_throwsException() {
            assertThatThrownBy(() -> new TraceabilityEntry(
                    "@GK-1", Optional.empty(), Optional.empty(),
                    Optional.empty(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status");
        }
    }

    @Nested
    class Factories {

        @Test
        void mapped_createsFullyLinkedEntry() {
            var entry = TraceabilityEntry.mapped(
                    "@GK-1", "AT-1",
                    "PaymentTest", "at1_approved");

            assertThat(entry.gherkinId()).isEqualTo("@GK-1");
            assertThat(entry.acceptanceTestId())
                    .hasValue("AT-1");
            assertThat(entry.testClassName())
                    .hasValue("PaymentTest");
            assertThat(entry.testMethodName())
                    .hasValue("at1_approved");
            assertThat(entry.status())
                    .isEqualTo(TraceabilityStatus.MAPPED);
        }

        @Test
        void unmappedRequirement_noTestFields() {
            var entry = TraceabilityEntry
                    .unmappedRequirement("@GK-3", "AT-3");

            assertThat(entry.gherkinId()).isEqualTo("@GK-3");
            assertThat(entry.acceptanceTestId())
                    .hasValue("AT-3");
            assertThat(entry.testClassName()).isEmpty();
            assertThat(entry.testMethodName()).isEmpty();
            assertThat(entry.status())
                    .isEqualTo(
                            TraceabilityStatus.UNMAPPED_REQUIREMENT);
        }

        @Test
        void unmappedTest_noRequirementFields() {
            var entry = TraceabilityEntry
                    .unmappedTest("SomeTest", "shouldWork");

            assertThat(entry.gherkinId()).isEqualTo("UNLINKED");
            assertThat(entry.acceptanceTestId()).isEmpty();
            assertThat(entry.testClassName())
                    .hasValue("SomeTest");
            assertThat(entry.testMethodName())
                    .hasValue("shouldWork");
            assertThat(entry.status())
                    .isEqualTo(TraceabilityStatus.UNMAPPED_TEST);
        }
    }
}
