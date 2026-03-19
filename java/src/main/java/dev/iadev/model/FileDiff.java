package dev.iadev.model;

/**
 * Represents a file difference between generated output and a reference.
 *
 * <p>Used for golden file comparison and verification reporting.</p>
 *
 * <p>Example:
 * <pre>{@code
 * var diff = new FileDiff("rules/01-identity.md", "- old\n+ new", 120, 125);
 * }</pre>
 * </p>
 *
 * @param path the file path relative to output directory
 * @param diff the textual diff content
 * @param sourceSize the size of the source (generated) file
 * @param referenceSize the size of the reference (golden) file
 */
public record FileDiff(
        String path,
        String diff,
        long sourceSize,
        long referenceSize) {
}
