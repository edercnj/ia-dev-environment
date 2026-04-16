package dev.iadev.release.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Suggested next action presented to the operator on the
 * next prompt of an interactive release flow.
 *
 * <p>Persisted as an entry of the {@code nextActions[]}
 * array in the release state file ({@code schemaVersion: 2})
 * introduced by EPIC-0039 story-0039-0002.
 *
 * @param label   human-readable description of the action
 * @param command slash-command to execute; validated against
 *                {@code ^/[a-z\-]+} by {@link StateFileValidator}
 */
public record NextAction(
        @JsonProperty("label") String label,
        @JsonProperty("command") String command) {

    @JsonCreator
    public NextAction {
        // Jackson canonical constructor; no normalization.
    }
}
