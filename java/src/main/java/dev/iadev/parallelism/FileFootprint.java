package dev.iadev.parallelism;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Structured footprint of files a task writes, reads, or regenerates.
 *
 * <p>Consumed by parallelism-aware tooling (e.g., {@code /x-parallel-eval})
 * to detect write-write conflicts deterministically, without relying on
 * prose parsing of "Affected Files" sections.</p>
 *
 * <p>Semantics of each sub-section:</p>
 * <ul>
 *   <li>{@code writes} — paths the task will create or modify</li>
 *   <li>{@code reads}  — paths the task depends on for input only</li>
 *   <li>{@code regens} — paths regenerated as a side-effect
 *       (e.g., {@code .claude/**} mirrors of
 *       {@code java/src/main/resources/targets/claude/**} sources)</li>
 * </ul>
 *
 * <p>All three sets are kept immutable and in insertion order is normalized
 * to alphabetical ordering (via {@link TreeSet}) for determinism
 * (RULE-008 — Determinism).</p>
 *
 * @param writes paths the task writes (non-null, may be empty)
 * @param reads  paths the task reads (non-null, may be empty)
 * @param regens paths the task regenerates (non-null, may be empty)
 */
public record FileFootprint(
        Set<String> writes,
        Set<String> reads,
        Set<String> regens) {

    /**
     * Canonical empty footprint — used when a plan predates the structured
     * block (backward compatibility, RULE-006).
     */
    public static final FileFootprint EMPTY = new FileFootprint(
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet());

    /**
     * Compact constructor: normalizes null to empty and wraps each set in
     * an unmodifiable alphabetical view.
     */
    public FileFootprint {
        writes = normalize(writes);
        reads = normalize(reads);
        regens = normalize(regens);
    }

    private static Set<String> normalize(Set<String> in) {
        if (in == null || in.isEmpty()) {
            return Collections.emptySet();
        }
        TreeSet<String> sorted = new TreeSet<>(in);
        return Collections.unmodifiableSet(sorted);
    }

    /**
     * Returns {@code true} iff all three sub-sections are empty.
     */
    public boolean isEmpty() {
        return writes.isEmpty() && reads.isEmpty() && regens.isEmpty();
    }

    /**
     * Convenience factory for the common write-only case.
     *
     * @param writes paths the task writes (non-null)
     * @return footprint with only the {@code writes} sub-section populated
     */
    public static FileFootprint ofWrites(Set<String> writes) {
        Objects.requireNonNull(writes, "writes");
        return new FileFootprint(writes, Set.of(), Set.of());
    }
}
