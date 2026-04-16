package dev.iadev.release.dryrun;

/**
 * Validates the combination of {@code --dry-run} and
 * {@code --interactive} CLI flags.
 *
 * <p>RULE-004 requires every interactive prompt to have a
 * non-interactive equivalent. This validator enforces the
 * dependency contract: {@code --interactive} requires
 * {@code --dry-run} (story-0039-0013 §5.3 error
 * {@code INTERACTIVE_REQUIRES_DRYRUN}).
 *
 * <p>Other flag combinations are valid:
 * <ul>
 *   <li>Neither flag — standard release</li>
 *   <li>{@code --dry-run} alone — verbose simulation</li>
 *   <li>Both flags — interactive simulation</li>
 * </ul>
 */
public final class InteractiveFlagValidator {

    private InteractiveFlagValidator() {
        throw new AssertionError("no instances");
    }

    /**
     * Validates the flag combination.
     *
     * @param dryRun      true if {@code --dry-run} is active
     * @param interactive true if {@code --interactive} is active
     * @throws InteractiveRequiresDryRunException when
     *         {@code --interactive} is set without {@code --dry-run}
     */
    public static void validate(boolean dryRun,
                                boolean interactive) {
        if (interactive && !dryRun) {
            throw new InteractiveRequiresDryRunException();
        }
    }
}
