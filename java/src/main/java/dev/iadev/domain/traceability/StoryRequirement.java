package dev.iadev.domain.traceability;

import java.util.Optional;

/**
 * A requirement extracted from a story markdown file.
 *
 * <p>Represents a single Gherkin scenario with its identifier
 * and optional acceptance test linkage.</p>
 *
 * @param gherkinId        the scenario identifier (e.g. "@GK-1")
 * @param title            the scenario title
 * @param acceptanceTestId optional linked acceptance test ID
 *                         (e.g. "AT-1")
 */
public record StoryRequirement(
        String gherkinId,
        String title,
        Optional<String> acceptanceTestId) {

    /**
     * Compact constructor enforcing non-null invariants.
     */
    public StoryRequirement {
        if (gherkinId == null || gherkinId.isBlank()) {
            throw new IllegalArgumentException(
                    "gherkinId must not be null or blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "title must not be null or blank");
        }
        if (acceptanceTestId == null) {
            acceptanceTestId = Optional.empty();
        }
    }
}
