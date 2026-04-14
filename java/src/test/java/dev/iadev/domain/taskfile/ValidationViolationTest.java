package dev.iadev.domain.taskfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ValidationViolationTest {

    @Test
    void constructor_validInputs_storesAllFields() {
        ValidationViolation v = new ValidationViolation("TF-SCHEMA-001", Severity.ERROR, "id missing");
        assertThat(v.ruleId()).isEqualTo("TF-SCHEMA-001");
        assertThat(v.severity()).isEqualTo(Severity.ERROR);
        assertThat(v.message()).isEqualTo("id missing");
    }

    @Test
    void constructor_blankRuleId_throwsIllegalArgument() {
        assertThatThrownBy(() -> new ValidationViolation(" ", Severity.ERROR, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleId");
    }

    @Test
    void constructor_blankMessage_throwsIllegalArgument() {
        assertThatThrownBy(() -> new ValidationViolation("TF-SCHEMA-001", Severity.WARN, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
    }

    @Test
    void constructor_nullRuleId_throwsNullPointer() {
        assertThatThrownBy(() -> new ValidationViolation(null, Severity.ERROR, "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullSeverity_throwsNullPointer() {
        assertThatThrownBy(() -> new ValidationViolation("TF-SCHEMA-001", null, "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullMessage_throwsNullPointer() {
        assertThatThrownBy(() -> new ValidationViolation("TF-SCHEMA-001", Severity.ERROR, null))
                .isInstanceOf(NullPointerException.class);
    }
}
