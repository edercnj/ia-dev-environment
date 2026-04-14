package dev.iadev.domain.taskfile.rules;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.TestabilityKind;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationRule;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TF-SCHEMA-006 — ERROR if a COALESCED testability declaration references a TASK-ID
 * that does not exist in the validation context's {@code knownTaskIds} set.
 *
 * <p>When {@link ValidationContext#knownTaskIds()} is empty (caller did not supply a
 * known set), this rule is permissive — no violation is emitted. This keeps the parser
 * usable in standalone mode without requiring the caller to enumerate the whole epic.</p>
 */
public final class CoalescedReferencesValidIdRule implements ValidationRule {

    public static final String RULE_ID = "TF-SCHEMA-006";

    @Override
    public List<ValidationViolation> validate(ParsedTaskFile parsed, ValidationContext ctx) {
        if (!parsed.testabilityCheckedKinds().contains(TestabilityKind.COALESCED)) {
            return List.of();
        }
        if (ctx.knownTaskIds().isEmpty()) {
            return List.of();
        }
        Set<String> known = ctx.knownTaskIds().get();
        List<ValidationViolation> violations = new ArrayList<>();
        for (String ref : parsed.testabilityReferenceIds()) {
            if (!known.contains(ref)) {
                violations.add(new ValidationViolation(
                        RULE_ID, Severity.ERROR,
                        "COALESCED references TASK-ID '" + ref
                                + "' which is not in the known task set"));
            }
        }
        return List.copyOf(violations);
    }
}
