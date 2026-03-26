package dev.iadev.smoke;

import java.util.List;
import java.util.Map;

/**
 * Declares the expected artifact structure for a single
 * profile: total file count, expected directories, and
 * file counts per category.
 *
 * <p>Immutable value object loaded from the manifest JSON.
 * Used by smoke tests to validate pipeline output.</p>
 *
 * <p>Example:
 * <pre>{@code
 * var profile = new ProfileArtifacts(
 *     125, List.of(".claude", "docs"),
 *     Map.of("rules", 6, "skills", 14));
 * int count = profile.getCategoryCount("rules"); // 6
 * }</pre>
 * </p>
 *
 * @param totalFiles  total expected file count
 * @param directories expected directory paths (relative)
 * @param categories  file count per artifact category
 */
public record ProfileArtifacts(
        int totalFiles,
        List<String> directories,
        Map<String, Integer> categories) {

    /**
     * Compact constructor enforcing immutability.
     */
    public ProfileArtifacts {
        directories = List.copyOf(directories);
        categories = Map.copyOf(categories);
    }

    /**
     * Returns the file count for the given category,
     * or zero if the category is not present.
     *
     * @param category the artifact category name
     * @return file count for the category, or zero
     */
    public int getCategoryCount(String category) {
        return categories.getOrDefault(category, 0);
    }
}
