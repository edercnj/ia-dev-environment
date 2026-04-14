package dev.iadev.domain.taskfile.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.ParsedTaskFixtures;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OutputsNotEmptyRuleTest {

    private final OutputsNotEmptyRule rule = new OutputsNotEmptyRule();
    private final ValidationContext ctx = ValidationContext.of("task-TASK-0038-0001-001.md");

    @Test
    void validate_nonEmptyOutputs_returnsNoViolation() {
        ParsedTaskFile p = ParsedTaskFixtures.withOutputs("- File X exists");
        assertThat(rule.validate(p, ctx)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "—", "-"})
    void validate_emptyOrPlaceholderOutputs_returnsError(String raw) {
        ParsedTaskFile p = ParsedTaskFixtures.withOutputs(raw);
        List<ValidationViolation> v = rule.validate(p, ctx);
        assertThat(v).hasSize(1);
        assertThat(v.get(0).ruleId()).isEqualTo(OutputsNotEmptyRule.RULE_ID);
        assertThat(v.get(0).severity()).isEqualTo(Severity.ERROR);
        assertThat(v.get(0).message()).contains("empty");
    }
}
