package dev.iadev.domain.taskfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaskValidatorTest {

    @Test
    void defaultValidator_wiresAllSixRules() {
        TaskValidator v = TaskValidator.defaultValidator();
        ParsedTaskFile empty = ParsedTaskFixtures.empty();
        List<ValidationViolation> out = v.validateAll(
                empty, ValidationContext.of("task-TASK-0038-0001-001.md"));
        // absent ID, absent status, zero testability, empty outputs, zero DoD items
        // TF-SCHEMA-001, 002, 003, 004 = ERROR; TF-SCHEMA-005 = WARN; TF-SCHEMA-006 = skipped
        assertThat(out).extracting(ValidationViolation::ruleId).containsExactlyInAnyOrder(
                "TF-SCHEMA-001", "TF-SCHEMA-002", "TF-SCHEMA-003", "TF-SCHEMA-004", "TF-SCHEMA-005");
    }

    @Test
    void validateAll_validFile_returnsNoViolations() {
        ParsedTaskFile valid = new ParsedTaskFile(
                Optional.of("TASK-0038-0001-001"),
                Optional.of("story-0038-0001"),
                Optional.of("Pendente"),
                "objective", "inputs", "outputs are non-empty",
                List.of(TestabilityKind.INDEPENDENT), 1, List.of(),
                List.of("- [ ] a", "- [ ] b", "- [ ] c", "- [ ] d", "- [ ] e", "- [ ] f"),
                List.of());
        TaskValidator v = TaskValidator.defaultValidator();
        assertThat(v.validateAll(valid, ValidationContext.of("task-TASK-0038-0001-001.md")))
                .isEmpty();
    }

    @Test
    void validateAll_aggregatesViolationsInRuleOrder() {
        ParsedTaskFile p = ParsedTaskFixtures.empty();
        TaskValidator v = TaskValidator.defaultValidator();
        List<ValidationViolation> out = v.validateAll(
                p, ValidationContext.of("task-TASK-0038-0001-001.md"));
        // first ERROR must be TF-SCHEMA-001 (IdMatchesFilenameRule runs first)
        assertThat(out.get(0).ruleId()).isEqualTo("TF-SCHEMA-001");
    }

    @Test
    void constructor_nullRules_throwsNullPointer() {
        assertThatThrownBy(() -> new TaskValidator(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rules");
    }

    @Test
    void validateAll_returnedListIsImmutable() {
        TaskValidator v = TaskValidator.defaultValidator();
        List<ValidationViolation> out = v.validateAll(
                ParsedTaskFixtures.empty(),
                ValidationContext.of("task-TASK-0038-0001-001.md"));
        assertThatThrownBy(() -> out.add(new ValidationViolation("X", Severity.ERROR, "y")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
