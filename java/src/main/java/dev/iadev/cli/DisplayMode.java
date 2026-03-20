package dev.iadev.cli;

/**
 * Display mode for pipeline result formatting.
 *
 * <p>Replaces opaque {@code boolean dryRun} parameters.
 * Call sites become self-documenting:
 * {@code formatResult(DisplayMode.DRY_RUN)} instead of
 * {@code formatResult(true)}.</p>
 *
 * @see CliDisplay#formatResult
 */
public enum DisplayMode {

    /** Real execution mode. */
    LIVE,

    /** Simulation mode — no files are written. */
    DRY_RUN;

    /**
     * Converts a boolean to the corresponding enum value.
     *
     * @param dryRun true for dry-run mode
     * @return DRY_RUN if true, LIVE if false
     */
    public static DisplayMode of(boolean dryRun) {
        return dryRun ? DRY_RUN : LIVE;
    }

    /**
     * Returns whether this mode is dry-run.
     *
     * @return true if DRY_RUN
     */
    public boolean isDryRun() {
        return this == DRY_RUN;
    }
}
