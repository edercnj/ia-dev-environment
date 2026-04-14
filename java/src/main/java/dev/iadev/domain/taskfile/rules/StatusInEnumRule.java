package dev.iadev.domain.taskfile.rules;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationRule;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;
import java.util.Set;

/**
 * TF-SCHEMA-002 — ERROR if the Status field is absent or outside the permitted enum.
 */
public final class StatusInEnumRule implements ValidationRule {

    public static final String RULE_ID = "TF-SCHEMA-002";

    static final Set<String> ALLOWED = Set.of(
            "Pendente", "Em Andamento", "Concluída", "Bloqueada", "Falha");

    @Override
    public List<ValidationViolation> validate(ParsedTaskFile parsed, ValidationContext ctx) {
        if (parsed.status().isEmpty()) {
            return List.of(new ValidationViolation(
                    RULE_ID, Severity.ERROR,
                    "Status line is absent (expected '**Status:** <enum>')"));
        }
        String status = parsed.status().get();
        if (!ALLOWED.contains(status)) {
            return List.of(new ValidationViolation(
                    RULE_ID, Severity.ERROR,
                    "Status '" + status + "' is not allowed. Permitted: " + ALLOWED));
        }
        return List.of();
    }
}
