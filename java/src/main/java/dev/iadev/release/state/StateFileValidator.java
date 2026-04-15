package dev.iadev.release.state;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates a {@link ReleaseState} instance against the
 * {@code schemaVersion: 2} contract documented in
 * story-0039-0002 §5.1/5.3.
 *
 * <p>RULE-003 (Schema evolution explícito): v1 is NOT
 * migrated silently — any {@code schemaVersion != 2} is
 * rejected with {@code STATE_SCHEMA_VERSION} and a
 * migration hint instructing the operator to clear the
 * stale state via {@code /x-release --abort}.
 */
public final class StateFileValidator {

    /** Required schema version for every v2 read and write. */
    public static final int REQUIRED_SCHEMA_VERSION = 2;

    /** Allowlist regex for {@code nextActions[].command}. */
    public static final Pattern COMMAND_PATTERN =
            Pattern.compile("^/[a-z\\-]+");

    private static final String MIGRATION_HINT =
            "Aborte a release ativa via "
                    + "/x-release --abort e inicie nova.";

    /**
     * Validates a state instance.
     *
     * @param state state to validate
     * @throws StateFileValidationException when any v2 rule
     *         is violated
     */
    public void validate(ReleaseState state) {
        validateSchemaVersion(state.schemaVersion());
        validateNextActions(state.nextActions());
    }

    private void validateSchemaVersion(int version) {
        if (version == REQUIRED_SCHEMA_VERSION) {
            return;
        }
        throw new StateFileValidationException(
                "STATE_SCHEMA_VERSION",
                "State file v" + version
                        + " nao eh mais suportado. "
                        + MIGRATION_HINT);
    }

    private void validateNextActions(
            List<NextAction> actions) {
        if (actions == null) {
            return;
        }
        for (NextAction action : actions) {
            validateAction(action);
        }
    }

    private void validateAction(NextAction action) {
        String command = action.command();
        if (command == null
                || !COMMAND_PATTERN.matcher(command).find()) {
            throw new StateFileValidationException(
                    "STATE_INVALID_ACTION",
                    "nextActions[].command must match "
                            + COMMAND_PATTERN.pattern()
                            + " (got: " + command + ")");
        }
    }
}
