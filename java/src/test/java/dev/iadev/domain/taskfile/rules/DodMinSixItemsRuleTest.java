package dev.iadev.domain.taskfile.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.ParsedTaskFixtures;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;
import org.junit.jupiter.api.Test;

class DodMinSixItemsRuleTest {

    private final DodMinSixItemsRule rule = new DodMinSixItemsRule();
    private final ValidationContext ctx = ValidationContext.of("task-TASK-0038-0001-001.md");

    @Test
    void validate_sixItems_returnsNoViolation() {
        assertThat(rule.validate(ParsedTaskFixtures.withDod(6), ctx)).isEmpty();
    }

    @Test
    void validate_moreThanSix_returnsNoViolation() {
        assertThat(rule.validate(ParsedTaskFixtures.withDod(9), ctx)).isEmpty();
    }

    @Test
    void validate_fiveItems_returnsWarn() {
        ParsedTaskFile p = ParsedTaskFixtures.withDod(5);
        List<ValidationViolation> v = rule.validate(p, ctx);
        assertThat(v).hasSize(1);
        assertThat(v.get(0).ruleId()).isEqualTo(DodMinSixItemsRule.RULE_ID);
        assertThat(v.get(0).severity()).isEqualTo(Severity.WARN);
        assertThat(v.get(0).message()).contains("5").contains("6");
    }

    @Test
    void validate_zeroItems_returnsWarn() {
        assertThat(rule.validate(ParsedTaskFixtures.empty(), ctx))
                .singleElement()
                .extracting(ValidationViolation::severity)
                .isEqualTo(Severity.WARN);
    }
}
