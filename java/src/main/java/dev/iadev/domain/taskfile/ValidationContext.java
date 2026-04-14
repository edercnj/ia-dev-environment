package dev.iadev.domain.taskfile;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Side input for {@link ValidationRule} evaluation.
 *
 * @param filename     the source filename (e.g. {@code "task-TASK-0038-0001-001.md"}); used by
 *                     {@code IdMatchesFilenameRule} (TF-SCHEMA-001)
 * @param knownTaskIds optional set of TASK-IDs known to exist in the surrounding context; used by
 *                     {@code CoalescedReferencesValidIdRule} (TF-SCHEMA-006). When absent, the
 *                     rule is permissive: it emits no violation.
 */
public record ValidationContext(String filename, Optional<Set<String>> knownTaskIds) {

    public ValidationContext {
        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(knownTaskIds, "knownTaskIds");
        knownTaskIds = knownTaskIds.map(Set::copyOf);
    }

    public static ValidationContext of(String filename) {
        return new ValidationContext(filename, Optional.empty());
    }

    public static ValidationContext of(String filename, Set<String> knownTaskIds) {
        return new ValidationContext(filename, Optional.of(knownTaskIds));
    }
}
