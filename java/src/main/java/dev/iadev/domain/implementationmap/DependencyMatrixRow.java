package dev.iadev.domain.implementationmap;

import java.util.List;

/**
 * A single row extracted from the dependency matrix table.
 *
 * @param storyId   story identifier (e.g., "story-0006-0001")
 * @param title     human-readable story title
 * @param blockedBy IDs of stories that block this one (empty if root)
 */
public record DependencyMatrixRow(
        String storyId,
        String title,
        List<String> blockedBy
) {

    /**
     * Creates a row with defensive copy of blockedBy list.
     */
    public DependencyMatrixRow {
        blockedBy = List.copyOf(blockedBy);
    }
}
