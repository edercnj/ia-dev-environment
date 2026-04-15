package dev.iadev.domain.taskfile;

import java.util.List;

/**
 * A single schema rule applied to a {@link ParsedTaskFile}.
 *
 * <p>Implementations MUST be stateless and thread-safe. Each rule owns one schema
 * invariant (SRP); the rule returns an empty list when it passes, or one violation
 * per failure mode.</p>
 */
public interface ValidationRule {

    /**
     * @return list of violations (empty if the rule passes); never null
     */
    List<ValidationViolation> validate(ParsedTaskFile parsed, ValidationContext ctx);
}
