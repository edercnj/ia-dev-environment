package dev.iadev.release.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

/**
 * Canonical record for the release state file
 * ({@code plans/release-state-X.Y.Z.json},
 * {@code schemaVersion: 2}).
 *
 * <p>All fields match the wire contract documented in
 * {@code references/state-file-schema.md} §5.1/5.2 and in
 * story-0039-0002 §5.1. The record is immutable; Jackson
 * binds via the canonical constructor (annotated with
 * {@link JsonCreator}).
 *
 * <p>Field order is alphabetical-first for required fields,
 * then grouped by phase origin for optional fields — the
 * {@link JsonPropertyOrder} annotation locks the wire-level
 * property order for deterministic golden-file regeneration.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
        "schemaVersion",
        "version",
        "phase",
        "branch",
        "baseBranch",
        "hotfix",
        "dryRun",
        "signedTag",
        "interactive",
        "noWaitCi",
        "startedAt",
        "lastPhaseCompletedAt",
        "phasesCompleted",
        "targetVersion",
        "previousVersion",
        "bumpType",
        "prNumber",
        "prUrl",
        "prTitle",
        "changelogEntry",
        "tagMessage",
        "worktreePath",
        "nextActions",
        "waitingFor",
        "phaseDurations",
        "ciCheckedAt",
        "ciStatus",
        "lastPromptAnsweredAt",
        "githubReleaseUrl"
})
public record ReleaseState(
        @JsonProperty("schemaVersion") int schemaVersion,
        @JsonProperty("version") String version,
        @JsonProperty("phase") String phase,
        @JsonProperty("branch") String branch,
        @JsonProperty("baseBranch") String baseBranch,
        @JsonProperty("hotfix") boolean hotfix,
        @JsonProperty("dryRun") boolean dryRun,
        @JsonProperty("signedTag") boolean signedTag,
        @JsonProperty("interactive") boolean interactive,
        @JsonProperty("noWaitCi") Boolean noWaitCi,
        @JsonProperty("startedAt") String startedAt,
        @JsonProperty("lastPhaseCompletedAt")
        String lastPhaseCompletedAt,
        @JsonProperty("phasesCompleted")
        List<String> phasesCompleted,
        @JsonProperty("targetVersion") String targetVersion,
        @JsonProperty("previousVersion")
        String previousVersion,
        @JsonProperty("bumpType") String bumpType,
        @JsonProperty("prNumber") Integer prNumber,
        @JsonProperty("prUrl") String prUrl,
        @JsonProperty("prTitle") String prTitle,
        @JsonProperty("changelogEntry")
        String changelogEntry,
        @JsonProperty("tagMessage") String tagMessage,
        @JsonProperty("worktreePath") String worktreePath,
        @JsonProperty("nextActions")
        List<NextAction> nextActions,
        @JsonProperty("waitingFor") WaitingFor waitingFor,
        @JsonProperty("phaseDurations")
        Map<String, Long> phaseDurations,
        @JsonProperty("ciCheckedAt") String ciCheckedAt,
        @JsonProperty("ciStatus") String ciStatus,
        @JsonProperty("lastPromptAnsweredAt")
        String lastPromptAnsweredAt,
        @JsonProperty("githubReleaseUrl")
        String githubReleaseUrl) {

    @JsonCreator
    public ReleaseState {
        // Canonical constructor for Jackson; immutable record.
    }

    /**
     * Copy-with helper producing a new record instance whose
     * {@code nextActions} field is replaced. Used in tests and
     * in the validator's programmatic update flow to avoid
     * introducing a mutable builder for a record.
     *
     * @param replacement new nextActions list (may be {@code null})
     * @return new ReleaseState with every other field preserved
     */
    public ReleaseState withNextActions(
            List<NextAction> replacement) {
        return new ReleaseState(
                schemaVersion, version, phase, branch,
                baseBranch, hotfix, dryRun, signedTag,
                interactive, noWaitCi, startedAt, lastPhaseCompletedAt,
                phasesCompleted, targetVersion, previousVersion,
                bumpType, prNumber, prUrl, prTitle,
                changelogEntry, tagMessage, worktreePath,
                replacement, waitingFor, phaseDurations,
                ciCheckedAt, ciStatus,
                lastPromptAnsweredAt, githubReleaseUrl);
    }
}
