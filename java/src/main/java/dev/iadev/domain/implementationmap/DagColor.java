package dev.iadev.domain.implementationmap;

/**
 * Represents the three-color marking used in DFS cycle
 * detection within the DAG validator.
 *
 * <p>Each color corresponds to a node visitation state
 * during depth-first search:
 * <ul>
 *   <li>{@link #WHITE} — not yet visited</li>
 *   <li>{@link #GRAY} — currently being processed
 *       (revisiting indicates a cycle)</li>
 *   <li>{@link #BLACK} — fully processed</li>
 * </ul>
 *
 * @see DagValidator
 */
public enum DagColor {

    /** Node has not been visited yet. */
    WHITE,

    /** Node is currently being processed (cycle if revisited). */
    GRAY,

    /** Node has been fully processed. */
    BLACK
}
