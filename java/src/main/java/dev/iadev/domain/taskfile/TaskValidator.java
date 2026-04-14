package dev.iadev.domain.taskfile;

import dev.iadev.domain.taskfile.rules.CoalescedReferencesValidIdRule;
import dev.iadev.domain.taskfile.rules.DodMinSixItemsRule;
import dev.iadev.domain.taskfile.rules.IdMatchesFilenameRule;
import dev.iadev.domain.taskfile.rules.OutputsNotEmptyRule;
import dev.iadev.domain.taskfile.rules.StatusInEnumRule;
import dev.iadev.domain.taskfile.rules.TestabilityExactlyOneRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composite validator: runs each {@link ValidationRule} in order and aggregates the
 * resulting violations into a single immutable list.
 *
 * <p>Stateless and thread-safe: the rule list is defensively copied at construction.
 * {@link #defaultValidator()} wires the 6 canonical TF-SCHEMA rules in declaration order.</p>
 */
public final class TaskValidator {

    private final List<ValidationRule> rules;

    public TaskValidator(List<ValidationRule> rules) {
        Objects.requireNonNull(rules, "rules");
        this.rules = List.copyOf(rules);
    }

    /**
     * @return a validator wired with the 6 canonical schema rules
     *         (TF-SCHEMA-001..006) in declaration order
     */
    public static TaskValidator defaultValidator() {
        return new TaskValidator(List.of(
                new IdMatchesFilenameRule(),
                new StatusInEnumRule(),
                new TestabilityExactlyOneRule(),
                new OutputsNotEmptyRule(),
                new DodMinSixItemsRule(),
                new CoalescedReferencesValidIdRule()));
    }

    public List<ValidationViolation> validateAll(ParsedTaskFile parsed, ValidationContext ctx) {
        List<ValidationViolation> all = new ArrayList<>();
        for (ValidationRule rule : rules) {
            all.addAll(rule.validate(parsed, ctx));
        }
        return List.copyOf(all);
    }
}
