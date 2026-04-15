package dev.iadev.domain.taskfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class TaskFileValidationResultTest {

    @Test
    void of_noViolations_returnsValid() {
        TaskFileValidationResult r = TaskFileValidationResult.of(
                "TASK-0038-0001-001", List.of(), TestabilityKind.INDEPENDENT);
        assertThat(r.valid()).isTrue();
        assertThat(r.violations()).isEmpty();
        assertThat(r.taskId()).isEqualTo("TASK-0038-0001-001");
        assertThat(r.testabilityKind()).isEqualTo(TestabilityKind.INDEPENDENT);
    }

    @Test
    void of_onlyWarnViolations_returnsValid() {
        ValidationViolation warn = new ValidationViolation("TF-SCHEMA-005", Severity.WARN, "few items");
        TaskFileValidationResult r = TaskFileValidationResult.of(
                "TASK-0038-0001-001", List.of(warn), TestabilityKind.INDEPENDENT);
        assertThat(r.valid()).isTrue();
        assertThat(r.violations()).containsExactly(warn);
    }

    @Test
    void of_anyErrorViolation_returnsInvalid() {
        ValidationViolation err = new ValidationViolation("TF-SCHEMA-001", Severity.ERROR, "id");
        ValidationViolation warn = new ValidationViolation("TF-SCHEMA-005", Severity.WARN, "few");
        TaskFileValidationResult r = TaskFileValidationResult.of(
                "TASK-0038-0001-001", List.of(err, warn), null);
        assertThat(r.valid()).isFalse();
        assertThat(r.violations()).hasSize(2);
        assertThat(r.testabilityKind()).isNull();
    }

    @Test
    void violations_areImmutable() {
        ValidationViolation v = new ValidationViolation("TF-SCHEMA-001", Severity.ERROR, "msg");
        TaskFileValidationResult r = TaskFileValidationResult.of(
                "TASK-0038-0001-001", List.of(v), null);
        assertThatThrownBy(() -> r.violations().add(v))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_blankTaskId_throwsIllegalArgument() {
        assertThatThrownBy(
                () -> new TaskFileValidationResult(" ", true, List.of(), TestabilityKind.INDEPENDENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId");
    }

    @Test
    void constructor_nullTaskId_throwsNullPointer() {
        assertThatThrownBy(
                () -> new TaskFileValidationResult(null, true, List.of(), TestabilityKind.INDEPENDENT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullViolations_throwsNullPointer() {
        assertThatThrownBy(
                () -> new TaskFileValidationResult("TASK-0038-0001-001", true, null, TestabilityKind.INDEPENDENT))
                .isInstanceOf(NullPointerException.class);
    }
}
