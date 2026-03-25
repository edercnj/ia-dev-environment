package dev.iadev.domain.implementationmap;

import java.util.List;
import java.util.Optional;

/**
 * A single row extracted from the dependency matrix table.
 *
 * @param storyId   story identifier (e.g., "story-0006-0001")
 * @param title     human-readable story title
 * @param jiraKey   optional Jira issue key (e.g., "PROJ-123")
 * @param blockedBy IDs of stories that block this one (empty if root)
 */
public record DependencyMatrixRow(
        String storyId,
        String title,
        Optional<String> jiraKey,
        List<String> blockedBy
) {

    /**
     * Creates a row with defensive copy of blockedBy list.
     */
    public DependencyMatrixRow {
        blockedBy = List.copyOf(blockedBy);
        jiraKey = (jiraKey != null) ? jiraKey : Optional.empty();
    }
}
