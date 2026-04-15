package dev.iadev.domain.taskfile.rules;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationRule;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TF-SCHEMA-001 — ERROR if the ID line is absent or does not match the filename
 * (which MUST follow {@code task-TASK-XXXX-YYYY-NNN.md}).
 */
public final class IdMatchesFilenameRule implements ValidationRule {

    public static final String RULE_ID = "TF-SCHEMA-001";

    private static final Pattern FILENAME_ID = Pattern.compile(
            "^task-(TASK-\\d{4}-\\d{4}-\\d{3})\\.md$");

    @Override
    public List<ValidationViolation> validate(ParsedTaskFile parsed, ValidationContext ctx) {
        Optional<String> taskId = parsed.taskId();
        if (taskId.isEmpty()) {
            return List.of(new ValidationViolation(
                    RULE_ID, Severity.ERROR,
                    "ID line is absent (expected '**ID:** TASK-XXXX-YYYY-NNN')"));
        }
        Matcher m = FILENAME_ID.matcher(ctx.filename());
        if (!m.matches()) {
            return List.of(new ValidationViolation(
                    RULE_ID, Severity.ERROR,
                    "filename '" + ctx.filename()
                            + "' does not match pattern task-TASK-XXXX-YYYY-NNN.md"));
        }
        String expected = m.group(1);
        if (!expected.equals(taskId.get())) {
            return List.of(new ValidationViolation(
                    RULE_ID, Severity.ERROR,
                    "task ID '" + taskId.get()
                            + "' does not match filename ID '" + expected + "'"));
        }
        return List.of();
    }
}
