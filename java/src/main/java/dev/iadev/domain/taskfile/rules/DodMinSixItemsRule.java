package dev.iadev.domain.taskfile.rules;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationRule;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;

/**
 * TF-SCHEMA-005 — WARN (advisory) if the DoD checklist has fewer than 6 items.
 * Does not invalidate the file (WARN severity).
 */
public final class DodMinSixItemsRule implements ValidationRule {

    public static final String RULE_ID = "TF-SCHEMA-005";

    static final int MIN_DOD_ITEMS = 6;

    @Override
    public List<ValidationViolation> validate(ParsedTaskFile parsed, ValidationContext ctx) {
        int count = parsed.dodItems().size();
        if (count >= MIN_DOD_ITEMS) {
            return List.of();
        }
        return List.of(new ValidationViolation(
                RULE_ID, Severity.WARN,
                "DoD has " + count + " items; minimum recommended is "
                        + MIN_DOD_ITEMS + " (RULE-TF-04)"));
    }
}
