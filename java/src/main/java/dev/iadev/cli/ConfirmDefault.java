package dev.iadev.cli;

/**
 * Default answer for yes/no confirmation prompts.
 *
 * <p>Replaces opaque {@code boolean defaultValue} parameters.
 * Call sites become self-documenting:
 * {@code confirm(ConfirmDefault.DEFAULT_YES)} instead of
 * {@code confirm(true)}.</p>
 *
 * @see TerminalProvider#confirm
 */
public enum ConfirmDefault {

    /** Default answer is yes (confirmed). */
    DEFAULT_YES,

    /** Default answer is no (declined). */
    DEFAULT_NO;

    /**
     * Converts a boolean to the corresponding enum value.
     *
     * @param defaultValue true for default yes
     * @return DEFAULT_YES if true, DEFAULT_NO if false
     */
    public static ConfirmDefault of(boolean defaultValue) {
        return defaultValue ? DEFAULT_YES : DEFAULT_NO;
    }

    /**
     * Returns whether the default is yes.
     *
     * @return true if DEFAULT_YES
     */
    public boolean isYes() {
        return this == DEFAULT_YES;
    }
}
