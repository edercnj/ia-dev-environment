package dev.iadev.release.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iadev.release.state.NextAction;
import dev.iadev.release.state.ReleaseState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Reports the status of an in-progress release by reading
 * the release state file. Read-only — never modifies state.
 *
 * <p>Implements story-0039-0010 §3.1 ({@code --status}).
 */
public final class StatusReporter {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .configure(
                            DeserializationFeature
                                    .FAIL_ON_UNKNOWN_PROPERTIES,
                            false);

    /**
     * Produces a human-readable status report.
     *
     * @param stateFilePath path to the release state file
     * @return result with exit code and formatted output
     */
    public StatusResult report(Path stateFilePath) {
        if (!Files.exists(stateFilePath)) {
            return StatusResult.success(
                    "No release in progress.");
        }
        return readAndRender(stateFilePath);
    }

    private StatusResult readAndRender(Path stateFilePath) {
        ReleaseState state;
        try {
            String json = Files.readString(stateFilePath);
            state = MAPPER.readValue(
                    json, ReleaseState.class);
        } catch (IOException e) {
            return StatusResult.error(
                    "Failed to parse state file: "
                            + sanitizePath(stateFilePath),
                    "STATUS_PARSE_FAILED");
        }
        return StatusResult.success(renderState(state));
    }

    private String renderState(ReleaseState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("Release in progress:\n");
        appendField(sb, "Version",
                formatVersion(state));
        appendField(sb, "Phase", state.phase());
        appendField(sb, "Branch", state.branch());
        appendPrInfo(sb, state);
        appendLastAction(sb, state);
        appendWaitingFor(sb, state);
        appendNextActions(sb, state.nextActions());
        return sb.toString();
    }

    private String formatVersion(ReleaseState state) {
        if (state.previousVersion() != null) {
            return String.format("%s (from v%s)",
                    state.targetVersion(),
                    state.previousVersion());
        }
        return state.targetVersion();
    }

    private void appendPrInfo(
            StringBuilder sb, ReleaseState state) {
        if (state.prNumber() != null) {
            String prStatus = state.prUrl() != null
                    ? String.format("#%d (%s)",
                    state.prNumber(), state.prUrl())
                    : String.format("#%d",
                    state.prNumber());
            appendField(sb, "PR release", prStatus);
        }
    }

    private void appendLastAction(
            StringBuilder sb, ReleaseState state) {
        String timestamp =
                state.lastPhaseCompletedAt();
        if (timestamp == null) {
            return;
        }
        try {
            Duration elapsed = Duration.between(
                    Instant.parse(timestamp),
                    Instant.now());
            appendField(sb, "Last action",
                    formatDuration(elapsed) + " ago");
        } catch (Exception ignored) {
            // Non-critical; skip if timestamp unparseable
        }
    }

    private void appendWaitingFor(
            StringBuilder sb, ReleaseState state) {
        if (state.waitingFor() != null) {
            appendField(sb, "Waiting for",
                    state.waitingFor().name());
        }
    }

    private void appendNextActions(
            StringBuilder sb, List<NextAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        sb.append("  Suggested next actions:\n");
        for (NextAction action : actions) {
            sb.append(String.format("    - %s (%s)%n",
                    action.label(), action.command()));
        }
    }

    private void appendField(
            StringBuilder sb,
            String label,
            String value) {
        sb.append(String.format("  %-16s %s%n",
                label + ":", value));
    }

    static String formatDuration(Duration duration) {
        long totalMinutes = duration.toMinutes();
        if (totalMinutes < 60) {
            return totalMinutes + "min";
        }
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%dh %dmin", hours, minutes);
    }

    private String sanitizePath(Path path) {
        return path.getFileName().toString();
    }
}
