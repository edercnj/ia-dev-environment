package dev.iadev.release.dryrun;

/**
 * Thrown when {@code --interactive} is supplied without
 * {@code --dry-run}. Carries the stable error code
 * {@code INTERACTIVE_REQUIRES_DRYRUN} and exit code 1.
 *
 * <p>Introduced by story-0039-0013.
 */
public final class InteractiveRequiresDryRunException
        extends RuntimeException {

    private static final String ERROR_CODE =
            "INTERACTIVE_REQUIRES_DRYRUN";
    private static final int EXIT_CODE = 1;

    /**
     * Creates a new exception with the canonical message.
     */
    public InteractiveRequiresDryRunException() {
        super("INTERACTIVE_REQUIRES_DRYRUN: "
                + "--interactive requires --dry-run.");
    }

    /**
     * @return stable error code identifier
     */
    public String errorCode() {
        return ERROR_CODE;
    }

    /**
     * @return process exit code (1)
     */
    public int exitCode() {
        return EXIT_CODE;
    }
}
