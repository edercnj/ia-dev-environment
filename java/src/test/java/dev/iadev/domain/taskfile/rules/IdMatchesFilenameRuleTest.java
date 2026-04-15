package dev.iadev.domain.taskfile.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.ParsedTaskFixtures;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdMatchesFilenameRuleTest {

    private final IdMatchesFilenameRule rule = new IdMatchesFilenameRule();

    @Test
    void validate_idMatchesFilename_returnsNoViolation() {
        ParsedTaskFile p = ParsedTaskFixtures.withId("TASK-0038-0001-001");
        List<ValidationViolation> v = rule.validate(p, ValidationContext.of("task-TASK-0038-0001-001.md"));
        assertThat(v).isEmpty();
    }

    @Test
    void validate_idAbsent_returnsError() {
        ParsedTaskFile p = ParsedTaskFixtures.empty();
        List<ValidationViolation> v = rule.validate(p, ValidationContext.of("task-TASK-0038-0001-001.md"));
        assertThat(v).hasSize(1);
        assertThat(v.get(0).ruleId()).isEqualTo(IdMatchesFilenameRule.RULE_ID);
        assertThat(v.get(0).severity()).isEqualTo(Severity.ERROR);
        assertThat(v.get(0).message()).contains("absent");
    }

    @Test
    void validate_filenameMalformed_returnsError() {
        ParsedTaskFile p = ParsedTaskFixtures.withId("TASK-0038-0001-001");
        List<ValidationViolation> v = rule.validate(p, ValidationContext.of("not-a-task-file.md"));
        assertThat(v).hasSize(1);
        assertThat(v.get(0).message()).contains("does not match pattern");
    }

    @Test
    void validate_idAndFilenameDisagree_returnsError() {
        ParsedTaskFile p = ParsedTaskFixtures.withId("TASK-0038-0001-001");
        List<ValidationViolation> v = rule.validate(p, ValidationContext.of("task-TASK-0038-0001-002.md"));
        assertThat(v).hasSize(1);
        assertThat(v.get(0).message())
                .contains("TASK-0038-0001-001")
                .contains("TASK-0038-0001-002");
    }
}
