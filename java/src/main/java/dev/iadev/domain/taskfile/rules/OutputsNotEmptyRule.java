package dev.iadev.domain.taskfile.rules;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationRule;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;

/**
 * TF-SCHEMA-004 — ERROR if section 2.2 Outputs is empty or contains only a dash placeholder
 * ({@code —} or {@code -}). Outputs must be verifiable via grep/assert/test (RULE-TF-02).
 */
public final class OutputsNotEmptyRule implements ValidationRule {

    public static final String RULE_ID = "TF-SCHEMA-004";

    @Override
    public List<ValidationViolation> validate(ParsedTaskFile parsed, ValidationContext ctx) {
        String outputs = parsed.outputs().strip();
        if (outputs.isEmpty() || "—".equals(outputs) || "-".equals(outputs)) {
            return List.of(new ValidationViolation(
                    RULE_ID, Severity.ERROR,
                    "section 2.2 Outputs is empty; outputs must be verifiable (RULE-TF-02)"));
        }
        return List.of();
    }
}
