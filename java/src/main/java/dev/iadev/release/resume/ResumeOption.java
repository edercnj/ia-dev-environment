package dev.iadev.release.resume;

/**
 * Options presented to the operator when an in-flight
 * release state is detected interactively.
 *
 * <p>Maps to story-0039-0008 §3.1 prompt options.
 */
public enum ResumeOption {

    /** Resume the in-flight release from its current phase. */
    RESUME("Resume from %s"),

    /** Abort the in-flight release (double confirm required). */
    ABORT("Abort release %s (cleanup)"),

    /**
     * Start a new release, discarding the active state.
     * Only available when new versionable commits exist
     * since the base tag of the active state.
     */
    START_NEW("Start new release (discard state)");

    private final String labelTemplate;

    ResumeOption(String labelTemplate) {
        this.labelTemplate = labelTemplate;
    }

    public String label(String versionOrPhase) {
        return labelTemplate.formatted(versionOrPhase);
    }
}
