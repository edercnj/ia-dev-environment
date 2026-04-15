package dev.iadev.domain.taskfile.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.ParsedTaskFixtures;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.TestabilityKind;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestabilityExactlyOneRuleTest {

    private final TestabilityExactlyOneRule rule = new TestabilityExactlyOneRule();
    private final ValidationContext ctx = ValidationContext.of("task-TASK-0038-0001-001.md");

    @Test
    void validate_exactlyOneChecked_returnsNoViolation() {
        ParsedTaskFile p = ParsedTaskFixtures.withTestability(
                List.of(TestabilityKind.INDEPENDENT), List.of());
        assertThat(rule.validate(p, ctx)).isEmpty();
    }

    @Test
    void validate_zeroChecked_returnsError() {
        ParsedTaskFile p = ParsedTaskFixtures.empty();
        List<ValidationViolation> v = rule.validate(p, ctx);
        assertThat(v).hasSize(1);
        assertThat(v.get(0).ruleId()).isEqualTo(TestabilityExactlyOneRule.RULE_ID);
        assertThat(v.get(0).severity()).isEqualTo(Severity.ERROR);
        assertThat(v.get(0).message()).contains("0");
    }

    @Test
    void validate_multipleChecked_returnsError() {
        ParsedTaskFile p = ParsedTaskFixtures.withTestability(
                List.of(TestabilityKind.INDEPENDENT, TestabilityKind.REQUIRES_MOCK), List.of());
        List<ValidationViolation> v = rule.validate(p, ctx);
        assertThat(v).hasSize(1);
        assertThat(v.get(0).message()).contains("2");
    }
}
