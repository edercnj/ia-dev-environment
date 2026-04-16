package dev.iadev.release.state;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates a {@link ReleaseState} instance against the
 * {@code schemaVersion: 2} contract documented in
 * story-0039-0002 §5.1/5.3.
 *
 * <p>RULE-003 (Schema evolution is explicit): v1 is NOT
 * migrated silently — any {@code schemaVersion != 2} is
 * rejected with {@code STATE_SCHEMA_VERSION} and a
 * migration hint instructing the operator to clear the
 * stale state via {@code /x-release --abort}.
 */
public final class StateFileValidator {

    /** Required schema version for every v2 read and write. */
    public static final int REQUIRED_SCHEMA_VERSION = 2;

    /**
     * Allowlist regex for {@code nextActions[].command}.
     *
     * <p>End-anchored via {@link java.util.regex.Matcher#matches()}
     * (see {@link #validateAction(NextAction)}) to block
     * shell-style payloads smuggled after a valid prefix
     * (e.g. {@code "/x-release; rm -rf /"}).
     */
    public static final Pattern COMMAND_PATTERN =
            Pattern.compile("^/[a-z\\-]+$");

    private static final String MIGRATION_HINT =
            "Abort the active release via "
                    + "/x-release --abort and start a new one.";

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
        // waitingFor is typed as the WaitingFor enum; Jackson
        // rejects unknown payload values at deserialization
        // time (surfaced as STATE_INVALID_ENUM by the reader
        // layer), so no extra validation is required here.
        validatePhaseDurations(state.phaseDurations());
    }

    private void validateSchemaVersion(int version) {
        if (version == REQUIRED_SCHEMA_VERSION) {
            return;
        }
        throw new StateFileValidationException(
                "STATE_SCHEMA_VERSION",
                "State file v" + version
                        + " is no longer supported. "
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
        if (action == null) {
            throw new StateFileValidationException(
                    "STATE_INVALID_ACTION",
                    "nextActions[] must not contain null"
                            + " items");
        }
        String command = action.command();
        if (command == null
                || !COMMAND_PATTERN.matcher(command)
                        .matches()) {
            throw new StateFileValidationException(
                    "STATE_INVALID_ACTION",
                    "nextActions[].command must match "
                            + COMMAND_PATTERN.pattern()
                            + " (got: " + command + ")");
        }
    }

    private void validatePhaseDurations(
            Map<String, Long> phaseDurations) {
        if (phaseDurations == null) {
            return;
        }
        for (Map.Entry<String, Long> entry
                : phaseDurations.entrySet()) {
            Long seconds = entry.getValue();
            if (seconds == null || seconds < 0) {
                throw new StateFileValidationException(
                        "STATE_INVALID_DURATION",
                        "phaseDurations['" + entry.getKey()
                                + "'] must be a non-negative"
                                + " integer of seconds"
                                + " (got: " + seconds + ")");
            }
        }
    }
}
