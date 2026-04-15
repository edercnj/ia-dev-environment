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

class StatusInEnumRuleTest {

    private final StatusInEnumRule rule = new StatusInEnumRule();
    private final ValidationContext ctx = ValidationContext.of("task-TASK-0038-0001-001.md");

    @ParameterizedTest
    @ValueSource(strings = {"Pendente", "Em Andamento", "Concluída", "Bloqueada", "Falha"})
    void validate_allowedStatus_returnsNoViolation(String status) {
        assertThat(rule.validate(ParsedTaskFixtures.withStatus(status), ctx)).isEmpty();
    }

    @Test
    void validate_statusAbsent_returnsError() {
        List<ValidationViolation> v = rule.validate(ParsedTaskFixtures.empty(), ctx);
        assertThat(v).hasSize(1);
        assertThat(v.get(0).ruleId()).isEqualTo(StatusInEnumRule.RULE_ID);
        assertThat(v.get(0).severity()).isEqualTo(Severity.ERROR);
        assertThat(v.get(0).message()).contains("absent");
    }

    @Test
    void validate_statusOutsideEnum_returnsError() {
        ParsedTaskFile p = ParsedTaskFixtures.withStatus("Em Progresso");
        List<ValidationViolation> v = rule.validate(p, ctx);
        assertThat(v).hasSize(1);
        assertThat(v.get(0).message()).contains("Em Progresso").contains("not allowed");
    }
}
