package dev.iadev.release.abort;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iadev.release.state.ReleaseState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Orchestrates the release abort flow: enumerates resources
 * to clean up, requests double confirmation from the operator,
 * and executes cleanup steps with warn-only failure handling.
 *
 * <p>Implements story-0039-0010 §3.2 ({@code --abort}).
 */
public final class AbortOrchestrator {

    private static final Logger LOG =
            Logger.getLogger(
                    AbortOrchestrator.class.getName());

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .configure(
                            DeserializationFeature
                                    .FAIL_ON_UNKNOWN_PROPERTIES,
                            false);

    private final ConfirmationPort confirmationPort;
    private final CleanupPort cleanupPort;

    public AbortOrchestrator(
            ConfirmationPort confirmationPort,
            CleanupPort cleanupPort) {
        this.confirmationPort = confirmationPort;
        this.cleanupPort = cleanupPort;
    }

    /**
     * Executes the abort flow.
     *
     * @param stateFilePath path to the release state file
     * @param forceMode     if true, skip confirmations
     *                      (--yes / --force)
     * @return result with exit code, output, and warnings
     */
    public AbortResult abort(
            Path stateFilePath, boolean forceMode) {
        if (!Files.exists(stateFilePath)) {
            return AbortResult.error(
                    "No active release to abort.",
                    "ABORT_NO_RELEASE");
        }
        return executeAbort(stateFilePath, forceMode);
    }

    private AbortResult executeAbort(
            Path stateFilePath, boolean forceMode) {
        Optional<ReleaseState> stateOpt = readState(stateFilePath);
        if (stateOpt.isEmpty()) {
            return AbortResult.error(
                    "Failed to parse state file.",
                    "STATUS_PARSE_FAILED");
        }
        ReleaseState state = stateOpt.get();

        String dryRunReport = buildDryRunReport(
                state, stateFilePath);

        if (!forceMode) {
            Optional<AbortResult> cancelResult =
                    requestDoubleConfirmation(
                            state, dryRunReport);
            if (cancelResult.isPresent()) {
                return cancelResult.get();
            }
        } else {
            LOG.warning("FORCE MODE: skipping "
                    + "confirmations for abort of v"
                    + state.targetVersion());
        }

        return executeCleanup(state, stateFilePath);
    }

    private Optional<ReleaseState> readState(Path stateFilePath) {
        try {
            String json = Files.readString(stateFilePath);
            return Optional.of(MAPPER.readValue(
                    json, ReleaseState.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private String buildDryRunReport(
            ReleaseState state, Path stateFilePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "=== ABORT release v%s ===%n%n",
                state.targetVersion()));
        sb.append(String.format(
                "Resources to be REMOVED:%n%n"));
        if (state.prNumber() != null) {
            sb.append(String.format(
                    "  PR open:          #%d "
                            + "(will be closed via gh pr close)%n",
                    state.prNumber()));
        }
        if (state.branch() != null) {
            sb.append(String.format(
                    "  Local branch:     %s%n",
                    state.branch()));
            sb.append(String.format(
                    "  Remote branch:    origin/%s%n",
                    state.branch()));
        }
        sb.append(String.format(
                "  State file:       %s%n",
                stateFilePath.getFileName()));
        return sb.toString();
    }

    private Optional<AbortResult> requestDoubleConfirmation(
            ReleaseState state, String dryRunReport) {
        String firstPrompt = dryRunReport
                + String.format(
                "%nConfirm abort release v%s? "
                        + "The resources above will be "
                        + "removed permanently.",
                state.targetVersion());

        if (!confirmationPort.confirm(firstPrompt)) {
            return Optional.of(AbortResult.cancelled(
                    "Abort cancelled by user."));
        }

        String secondPrompt = String.format(
                "Are you absolutely sure? "
                        + "This operation is IRREVERSIBLE. "
                        + "Aborting release v%s.",
                state.targetVersion());

        if (!confirmationPort.confirm(secondPrompt)) {
            return Optional.of(AbortResult.cancelled(
                    "Abort cancelled by user."));
        }

        return Optional.empty();
    }

    private AbortResult executeCleanup(
            ReleaseState state, Path stateFilePath) {
        List<String> warnings = new ArrayList<>();

        closePrSafe(state, warnings);
        deleteLocalBranchSafe(state, warnings);
        deleteRemoteBranchSafe(state, warnings);
        deleteStateFileSafe(stateFilePath, warnings);

        return AbortResult.success(
                String.format(
                        "Release v%s aborted. "
                                + "Cleanup complete.",
                        state.targetVersion()),
                warnings);
    }

    private void closePrSafe(
            ReleaseState state, List<String> warnings) {
        if (state.prNumber() == null) {
            return;
        }
        try {
            cleanupPort.closePr(state.prNumber());
        } catch (CleanupException e) {
            warnings.add(e.errorCode() + ": "
                    + e.getMessage());
        } catch (RuntimeException e) {
            warnings.add("ABORT_PR_CLOSE_FAILED: "
                    + e.getMessage());
        }
    }

    private void deleteLocalBranchSafe(
            ReleaseState state, List<String> warnings) {
        if (state.branch() == null) {
            return;
        }
        try {
            cleanupPort.deleteLocalBranch(state.branch());
        } catch (CleanupException e) {
            warnings.add(e.errorCode() + ": "
                    + e.getMessage());
        } catch (RuntimeException e) {
            warnings.add(
                    "ABORT_BRANCH_DELETE_FAILED: "
                            + e.getMessage());
        }
    }

    private void deleteRemoteBranchSafe(
            ReleaseState state, List<String> warnings) {
        if (state.branch() == null) {
            return;
        }
        try {
            cleanupPort.deleteRemoteBranch(state.branch());
        } catch (CleanupException e) {
            warnings.add(e.errorCode() + ": "
                    + e.getMessage());
        } catch (RuntimeException e) {
            warnings.add(
                    "ABORT_BRANCH_DELETE_FAILED: "
                            + e.getMessage());
        }
    }

    private void deleteStateFileSafe(
            Path stateFilePath, List<String> warnings) {
        try {
            cleanupPort.deleteStateFile(stateFilePath);
        } catch (CleanupException e) {
            warnings.add(e.errorCode() + ": "
                    + e.getMessage());
        } catch (RuntimeException e) {
            warnings.add("ABORT_STATE_FILE_DELETE_FAILED: "
                    + e.getMessage());
        }
    }
}
