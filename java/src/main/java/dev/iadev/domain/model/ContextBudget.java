package dev.iadev.domain.model;

/**
 * Context budget classification for skills based on their
 * line count.
 *
 * <p>Used by orchestrator skills to decide between inline
 * execution and subagent delegation. The field is purely
 * informational and does not affect how Claude Code loads
 * the skill.</p>
 *
 * <ul>
 *   <li>{@link #LIGHT} — below 200 lines (~3K tokens)</li>
 *   <li>{@link #MEDIUM} — 200-500 lines (~3-7K tokens)</li>
 *   <li>{@link #HEAVY} — above 500 lines (>7K tokens)</li>
 * </ul>
 *
 * @see #fromLineCount(int)
 */
public enum ContextBudget {

    LIGHT("light"),
    MEDIUM("medium"),
    HEAVY("heavy");

    private static final int LIGHT_THRESHOLD = 200;
    private static final int HEAVY_THRESHOLD = 500;

    private final String value;

    ContextBudget(String value) {
        this.value = value;
    }

    /**
     * Returns the lowercase string representation used
     * in YAML frontmatter.
     *
     * @return the budget value string
     */
    public String value() {
        return value;
    }

    /**
     * Classifies a skill based on its line count.
     *
     * @param lineCount the number of lines in the skill
     *                  template
     * @return the corresponding budget classification
     */
    public static ContextBudget fromLineCount(
            int lineCount) {
        if (lineCount < LIGHT_THRESHOLD) {
            return LIGHT;
        }
        if (lineCount <= HEAVY_THRESHOLD) {
            return MEDIUM;
        }
        return HEAVY;
    }
}
