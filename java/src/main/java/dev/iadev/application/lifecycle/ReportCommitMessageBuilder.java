package dev.iadev.application.lifecycle;

/**
 * Canonical builder of Conventional Commit messages for epic
 * report commits added in story-0046-0005 (atomic commit of
 * epic reports in {@code x-epic-implement}).
 *
 * <p>Two message templates are supported:</p>
 * <ul>
 *   <li>{@link #executionPlan(String, int, int)} — emitted
 *       once, before the first wave, when the execution plan
 *       report is written.</li>
 *   <li>{@link #phaseReport(String, int, int, int)} — emitted
 *       once per wave, after all stories in that wave reach
 *       the terminal state.</li>
 * </ul>
 *
 * <p>All methods fail loudly via
 * {@link IllegalArgumentException} when inputs violate the
 * contract; this mirrors RULE-046-08 (fail loud on status
 * update failure) and keeps the caller honest.</p>
 */
public final class ReportCommitMessageBuilder {

    private static final String SCHEMA_VERSION = "v2.0";

    private ReportCommitMessageBuilder() {
        // utility class
    }

    /**
     * Build the commit message for the {@code execution plan}
     * report added in story-0046-0005.
     *
     * @param epicId canonical four-digit epic identifier
     *               (e.g. {@code "0046"}); must be non-blank
     * @param waves  total number of waves in the execution
     *               plan; must be {@code >= 0}
     * @param stories total number of stories across all
     *                waves; must be {@code >= 0}
     * @return multi-line Conventional Commit message with the
     *         canonical subject {@code "docs(epic-XXXX): add
     *         execution plan"} and a body summarising the
     *         plan
     * @throws IllegalArgumentException when any input violates
     *                                  its contract
     */
    public static String executionPlan(
            String epicId, int waves, int stories) {
        requireEpicId(epicId);
        if (waves < 0) {
            throw new IllegalArgumentException(
                    "waves must be non-negative: " + waves);
        }
        if (stories < 0) {
            throw new IllegalArgumentException(
                    "stories must be non-negative: " + stories);
        }
        String subject = String.format(
                "docs(epic-%s): add execution plan", epicId);
        String body = String.format(
                "- Waves: %d%n- Stories: %d%n- Schema: %s",
                waves, stories, SCHEMA_VERSION);
        String ref = String.format(
                "Refs: plans/epic-%s/reports/epic-execution-plan-%s.md",
                epicId, epicId);
        return subject + "\n\n" + body + "\n\n" + ref + "\n";
    }

    /**
     * Build the commit message for a per-wave
     * {@code phase-report} in story-0046-0005.
     *
     * @param epicId    canonical four-digit epic identifier;
     *                  must be non-blank
     * @param wave      1-based wave number; must be
     *                  {@code >= 1}
     * @param storyCount number of stories that reached the
     *                   terminal state in this wave; must be
     *                   {@code >= 0}
     * @param commitCount number of story-finalize commits
     *                    produced by this wave; must be
     *                    {@code >= 0}
     * @return multi-line Conventional Commit message with the
     *         canonical subject {@code "docs(epic-XXXX): add
     *         phase-N report"} and a body summarising the
     *         wave outcome
     * @throws IllegalArgumentException when any input violates
     *                                  its contract
     */
    public static String phaseReport(
            String epicId,
            int wave,
            int storyCount,
            int commitCount) {
        requireEpicId(epicId);
        if (wave < 1) {
            throw new IllegalArgumentException(
                    "wave must be >= 1: " + wave);
        }
        if (storyCount < 0) {
            throw new IllegalArgumentException(
                    "storyCount must be non-negative: "
                            + storyCount);
        }
        if (commitCount < 0) {
            throw new IllegalArgumentException(
                    "commitCount must be non-negative: "
                            + commitCount);
        }
        String subject = String.format(
                "docs(epic-%s): add phase-%d report",
                epicId, wave);
        String body = String.format(
                "- Wave %d complete: %d stories DONE%n"
                        + "- Commits: %d story-finalize",
                wave, storyCount, commitCount);
        String ref = String.format(
                "Refs: plans/epic-%s/reports/"
                        + "phase-%d-completion-%s.md",
                epicId, wave, epicId);
        return subject + "\n\n" + body + "\n\n" + ref + "\n";
    }

    private static void requireEpicId(String epicId) {
        if (epicId == null || epicId.isBlank()) {
            throw new IllegalArgumentException(
                    "epicId must be non-blank");
        }
        if (!epicId.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "epicId must be exactly 4 digits: "
                            + epicId);
        }
    }
}
