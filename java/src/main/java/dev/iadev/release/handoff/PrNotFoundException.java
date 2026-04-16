package dev.iadev.release.handoff;

/**
 * Thrown by a {@link GhCliPort} implementation when
 * {@code gh pr view} returns 404 (PR deleted or inaccessible).
 *
 * <p>Callers classify this as
 * {@link HandoffError#HANDOFF_PR_NOT_FOUND} and exit with
 * code 1 per story-0039-0011 §5.4.
 */
public class PrNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int prNumber;

    /**
     * @param prNumber the PR number that could not be found
     */
    public PrNotFoundException(int prNumber) {
        super("PR #" + prNumber + " not found");
        this.prNumber = prNumber;
    }

    public int prNumber() {
        return prNumber;
    }
}
