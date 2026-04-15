package dev.iadev.domain.taskfile.rules;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationRule;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;

/**
 * TF-SCHEMA-003 — ERROR if section 2.3 (Testabilidade) does not have EXACTLY one
 * checked declaration. Zero or multiple checked checkboxes both fail.
 */
public final class TestabilityExactlyOneRule implements ValidationRule {

    public static final String RULE_ID = "TF-SCHEMA-003";

    @Override
    public List<ValidationViolation> validate(ParsedTaskFile parsed, ValidationContext ctx) {
        int count = parsed.testabilityCheckedKinds().size();
        if (count == 1) {
            return List.of();
        }
        return List.of(new ValidationViolation(
                RULE_ID, Severity.ERROR,
                "testabilidade deve ter exatamente uma declaração marcada (encontradas: "
                        + count + ")"));
    }
}
