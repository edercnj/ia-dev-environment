package dev.iadev.domain.implementationmap;

import java.util.List;
import java.util.Optional;

/**
 * A node in the dependency DAG representing a single story.
 *
 * <p>Unlike records, this class uses mutable fields for
 * {@code phase} and {@code isOnCriticalPath} since they are
 * computed after construction by {@link PhaseComputer} and
 * {@link CriticalPathFinder}.</p>
 */
public final class DagNode {

    private static final int UNCOMPUTED_PHASE = -1;

    private final String storyId;
    private final String title;
    private final Optional<String> jiraKey;
    private final List<String> blockedBy;
    private final List<String> blocks;
    private int phase;
    private boolean isOnCriticalPath;

    /**
     * Creates a DAG node with uncomputed phase and not on
     * critical path.
     *
     * @param storyId   story identifier
     * @param title     story title
     * @param jiraKey   optional Jira issue key
     * @param blockedBy mutable list of dependency IDs
     * @param blocks    mutable list of dependent IDs
     */
    public DagNode(
            String storyId,
            String title,
            Optional<String> jiraKey,
            List<String> blockedBy,
            List<String> blocks) {
        this.storyId = storyId;
        this.title = title;
        this.jiraKey = (jiraKey != null)
                ? jiraKey : Optional.empty();
        this.blockedBy = blockedBy;
        this.blocks = blocks;
        this.phase = UNCOMPUTED_PHASE;
        this.isOnCriticalPath = false;
    }

    /** Returns the story identifier. */
    public String storyId() {
        return storyId;
    }

    /** Returns the story title. */
    public String title() {
        return title;
    }

    /** Returns the optional Jira issue key. */
    public Optional<String> jiraKey() {
        return jiraKey;
    }

    /**
     * Returns the mutable list of IDs this node depends on.
     *
     * @return blockedBy list (may be mutated by validator)
     */
    public List<String> blockedBy() {
        return blockedBy;
    }

    /**
     * Returns the mutable list of IDs that depend on this node.
     *
     * @return blocks list (may be mutated by validator)
     */
    public List<String> blocks() {
        return blocks;
    }

    /** Returns the computed execution phase (-1 if uncomputed). */
    public int phase() {
        return phase;
    }

    /** Sets the execution phase (called by PhaseComputer). */
    public void setPhase(int phase) {
        this.phase = phase;
    }

    /** Returns whether this node is on the critical path. */
    public boolean isOnCriticalPath() {
        return isOnCriticalPath;
    }

    /** Marks this node as on (or off) the critical path. */
    public void setOnCriticalPath(boolean onCriticalPath) {
        this.isOnCriticalPath = onCriticalPath;
    }

    @Override
    public String toString() {
        return "DagNode{storyId='%s', phase=%d, critical=%s}"
                .formatted(storyId, phase, isOnCriticalPath);
    }
}
