package dev.iadev.application.assembler;

/**
 * Indicates whether hooks are present in the project.
 *
 * <p>Replaces opaque {@code boolean hasHooks} parameters
 * in method signatures. Call sites become self-documenting:
 * {@code buildSettingsJson(perms, HookPresence.WITH_HOOKS,
 * telemetryEnabled)} instead of
 * {@code buildSettingsJson(perms, true, telemetryEnabled)}.
 * </p>
 *
 * @see SettingsAssembler#buildSettingsJson
 */
public enum HookPresence {

    /** Hooks directory exists and contains entries. */
    WITH_HOOKS,

    /** No hooks detected. */
    WITHOUT_HOOKS;

    /**
     * Converts a boolean to the corresponding enum value.
     *
     * @param hasHooks true if hooks are present
     * @return WITH_HOOKS if true, WITHOUT_HOOKS if false
     */
    public static HookPresence of(boolean hasHooks) {
        return hasHooks ? WITH_HOOKS : WITHOUT_HOOKS;
    }

    /**
     * Returns whether this value represents hooks present.
     *
     * @return true if WITH_HOOKS
     */
    public boolean hasHooks() {
        return this == WITH_HOOKS;
    }
}
