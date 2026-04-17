package dev.iadev.parallelism;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable value object describing a pairwise collision
 * between two work units (A and B).
 *
 * <p>Produced by {@link CollisionDetector}. The {@code a}
 * and {@code b} identifiers are always sorted
 * lexicographically so that {@code detect(X, Y)} and
 * {@code detect(Y, X)} produce byte-identical output
 * (RULE-008 — Determinism).</p>
 *
 * @param a              the first unit ID (always the
 *                       lexicographically smaller one)
 * @param b              the second unit ID
 * @param category       the collision category
 * @param sharedPaths    paths causing the collision, in
 *                       alphabetical order (non-null, may
 *                       be empty for hotspot-only collisions)
 * @param reason         optional human-readable reason
 *                       (e.g., {@code "hotspot: pom.xml"});
 *                       {@code null} when not applicable
 */
public record Collision(
        String a,
        String b,
        CollisionCategory category,
        Set<String> sharedPaths,
        String reason) {

    /**
     * Compact constructor: normalizes the shared-paths set
     * to an unmodifiable alphabetical view. Validates
     * that {@code a <= b} lexicographically.
     */
    public Collision {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(category, "category");
        if (a.compareTo(b) > 0) {
            throw new IllegalArgumentException(
                    "a must be <= b, got a=" + a
                            + " b=" + b);
        }
        sharedPaths = (sharedPaths == null
                || sharedPaths.isEmpty())
                ? Set.of()
                : Collections.unmodifiableSet(
                        new TreeSet<>(sharedPaths));
    }
}
