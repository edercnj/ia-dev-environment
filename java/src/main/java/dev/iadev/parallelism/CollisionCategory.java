package dev.iadev.parallelism;

/**
 * Classification of a pairwise file-collision between two
 * work units (task, story, or epic) that may run in parallel.
 *
 * <p>The categories match the RULE-003 table in the
 * {@code parallelism-heuristics} knowledge pack:</p>
 *
 * <ul>
 *   <li>{@link #HARD}  — both units write the same path.
 *       MUST be serialized.</li>
 *   <li>{@link #REGEN} — one unit writes a path the other
 *       regenerates, OR both regenerate the same path.
 *       MUST be serialized.</li>
 *   <li>{@link #SOFT}  — read-only overlap. Safe to run in
 *       parallel; kept here so callers can explicitly reason
 *       about the category.</li>
 * </ul>
 */
public enum CollisionCategory {
    HARD,
    REGEN,
    SOFT
}
