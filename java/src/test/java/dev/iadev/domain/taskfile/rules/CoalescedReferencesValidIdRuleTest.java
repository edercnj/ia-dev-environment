package dev.iadev.domain.taskfile.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.ParsedTaskFixtures;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.TestabilityKind;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoalescedReferencesValidIdRuleTest {

    private final CoalescedReferencesValidIdRule rule = new CoalescedReferencesValidIdRule();

    @Test
    void validate_nonCoalesced_returnsNoViolation() {
        ParsedTaskFile p = ParsedTaskFixtures.withTestability(
                List.of(TestabilityKind.INDEPENDENT), List.of());
        assertThat(rule.validate(p, ValidationContext.of("task-TASK-0038-0001-001.md"))).isEmpty();
    }

    @Test
    void validate_coalescedWithNoKnownSet_isPermissive() {
        ParsedTaskFile p = ParsedTaskFixtures.withTestability(
                List.of(TestabilityKind.COALESCED), List.of("TASK-0038-0001-999"));
        assertThat(rule.validate(p, ValidationContext.of("task-TASK-0038-0001-001.md"))).isEmpty();
    }

    @Test
    void validate_coalescedWithKnownRef_returnsNoViolation() {
        ParsedTaskFile p = ParsedTaskFixtures.withTestability(
                List.of(TestabilityKind.COALESCED), List.of("TASK-0038-0001-004"));
        ValidationContext ctx = ValidationContext.of(
                "task-TASK-0038-0001-001.md", Set.of("TASK-0038-0001-004"));
        assertThat(rule.validate(p, ctx)).isEmpty();
    }

    @Test
    void validate_coalescedWithUnknownRef_returnsError() {
        ParsedTaskFile p = ParsedTaskFixtures.withTestability(
                List.of(TestabilityKind.COALESCED), List.of("TASK-0038-0001-999"));
        ValidationContext ctx = ValidationContext.of(
                "task-TASK-0038-0001-001.md", Set.of("TASK-0038-0001-004"));
        List<ValidationViolation> v = rule.validate(p, ctx);
        assertThat(v).hasSize(1);
        assertThat(v.get(0).ruleId()).isEqualTo(CoalescedReferencesValidIdRule.RULE_ID);
        assertThat(v.get(0).severity()).isEqualTo(Severity.ERROR);
        assertThat(v.get(0).message()).contains("TASK-0038-0001-999");
    }

    @Test
    void validate_coalescedWithMultipleUnknownRefs_returnsMultipleViolations() {
        ParsedTaskFile p = ParsedTaskFixtures.withTestability(
                List.of(TestabilityKind.COALESCED),
                List.of("TASK-0038-0001-999", "TASK-0038-0001-998"));
        ValidationContext ctx = ValidationContext.of(
                "task-TASK-0038-0001-001.md", Set.of("TASK-0038-0001-004"));
        List<ValidationViolation> v = rule.validate(p, ctx);
        assertThat(v).hasSize(2);
    }
}
