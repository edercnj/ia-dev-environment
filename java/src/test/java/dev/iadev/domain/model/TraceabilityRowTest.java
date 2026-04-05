package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TraceabilityRow")
class TraceabilityRowTest {

    @Nested
    @DisplayName("constructor — validation")
    class Validation {

        @Test
        @DisplayName("throws when reqId is null")
        void constructor_nullReqId_throwsException() {
            assertThatThrownBy(() -> new TraceabilityRow(
                    null, "scenario",
                    "TestClass", "method",
                    ExecutionStatus.PASS, 95))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("reqId");
        }

        @Test
        @DisplayName("throws when reqId is blank")
        void constructor_blankReqId_throwsException() {
            assertThatThrownBy(() -> new TraceabilityRow(
                    "  ", "scenario",
                    "TestClass", "method",
                    ExecutionStatus.PASS, 95))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("reqId");
        }

        @Test
        @DisplayName("throws when scenarioTitle is null")
        void constructor_nullScenarioTitle_throwsException() {
            assertThatThrownBy(() -> new TraceabilityRow(
                    "AT-1", null,
                    "TestClass", "method",
                    ExecutionStatus.PASS, 95))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "scenarioTitle");
        }

        @Test
        @DisplayName("throws when executionStatus is null")
        void constructor_nullStatus_throwsException() {
            assertThatThrownBy(() -> new TraceabilityRow(
                    "AT-1", "scenario",
                    "TestClass", "method",
                    null, 95))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "executionStatus");
        }
    }

    @Nested
    @DisplayName("constructor — happy path")
    class HappyPath {

        @Test
        @DisplayName("creates row with all fields")
        void constructor_allFields_allAccessible() {
            var row = new TraceabilityRow(
                    "AT-1", "scenario title",
                    "PaymentTest", "testMethod",
                    ExecutionStatus.PASS, 94);

            assertThat(row.reqId()).isEqualTo("AT-1");
            assertThat(row.scenarioTitle())
                    .isEqualTo("scenario title");
            assertThat(row.testClassName())
                    .isEqualTo("PaymentTest");
            assertThat(row.testMethodName())
                    .isEqualTo("testMethod");
            assertThat(row.executionStatus())
                    .isEqualTo(ExecutionStatus.PASS);
            assertThat(row.lineCoverage()).isEqualTo(94);
        }

        @Test
        @DisplayName("allows null optional fields")
        void constructor_nullOptionals_createsRow() {
            var row = new TraceabilityRow(
                    "AT-2", "unmapped scenario",
                    null, null,
                    ExecutionStatus.UNMAPPED, null);

            assertThat(row.testClassName()).isNull();
            assertThat(row.testMethodName()).isNull();
            assertThat(row.lineCoverage()).isNull();
        }
    }

    @Nested
    @DisplayName("optional accessors")
    class OptionalAccessors {

        @Test
        @DisplayName("optionalTestClassName returns "
                + "present when set")
        void optionalTestClassName_set_returnsPresent() {
            var row = new TraceabilityRow(
                    "AT-1", "scenario",
                    "TestClass", "method",
                    ExecutionStatus.PASS, 95);

            assertThat(row.optionalTestClassName())
                    .isPresent()
                    .contains("TestClass");
        }

        @Test
        @DisplayName("optionalTestClassName returns "
                + "empty when null")
        void optionalTestClassName_null_returnsEmpty() {
            var row = new TraceabilityRow(
                    "AT-1", "scenario",
                    null, null,
                    ExecutionStatus.UNMAPPED, null);

            assertThat(row.optionalTestClassName())
                    .isEmpty();
        }

        @Test
        @DisplayName("optionalTestMethodName returns "
                + "present when set")
        void optionalTestMethodName_set_returnsPresent() {
            var row = new TraceabilityRow(
                    "AT-1", "scenario",
                    "TestClass", "testIt",
                    ExecutionStatus.PASS, 95);

            assertThat(row.optionalTestMethodName())
                    .isPresent()
                    .contains("testIt");
        }

        @Test
        @DisplayName("optionalLineCoverage returns "
                + "present when set")
        void optionalLineCoverage_set_returnsPresent() {
            var row = new TraceabilityRow(
                    "AT-1", "scenario",
                    "TestClass", "method",
                    ExecutionStatus.PASS, 97);

            assertThat(row.optionalLineCoverage())
                    .isPresent()
                    .contains(97);
        }

        @Test
        @DisplayName("optionalLineCoverage returns "
                + "empty when null")
        void optionalLineCoverage_null_returnsEmpty() {
            var row = new TraceabilityRow(
                    "AT-1", "scenario",
                    null, null,
                    ExecutionStatus.UNMAPPED, null);

            assertThat(row.optionalLineCoverage())
                    .isEmpty();
        }
    }
}
