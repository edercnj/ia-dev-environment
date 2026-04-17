package dev.iadev.checkpoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Records a single parallelism downgrade decision emitted by the
 * parallelism gate powered by {@code /x-parallel-eval} at either
 * epic-implement Phase 0.5.0 scope or story-implement Phase 1.5 scope
 * (EPIC-0041 / story-0041-0006).
 *
 * <p>A downgrade is produced when {@code /x-parallel-eval} returns exit
 * code {@code 2} (hard / regen conflicts). The orchestrator serializes
 * the conflicting stories (epic scope) or tasks (story scope) and
 * appends one {@code ParallelismDowngrade} per affected phase or wave
 * to the top-level {@code parallelismDowngrades} array inside
 * {@link ExecutionState}.</p>
 *
 * <p>The field is optional (backward compatibility, RULE-006): legacy
 * {@code execution-state.json} files without {@code parallelismDowngrades}
 * MUST still deserialize cleanly via the null-safe compact constructor
 * on {@link ExecutionState} and the null-elision on serialization.</p>
 *
 * @param phase            epic phase number (epic scope) or wave number
 *                         (story scope) whose parallelism was downgraded
 * @param originalGroup    story IDs (epic scope) or task IDs (story scope)
 *                         that were originally in the parallel batch
 * @param adjustedSequence sequence of batches produced by the downgrade;
 *                         each inner list runs together, outer list is
 *                         serialized in order
 * @param reason           short human-readable cause, e.g.
 *                         {@code "hard conflict on SettingsAssembler.java"}
 * @param evaluatedAt      instant at which the gate produced this
 *                         decision
 */
public record ParallelismDowngrade(
        int phase,
        List<String> originalGroup,
        List<List<String>> adjustedSequence,
        String reason,
        Instant evaluatedAt
) {

    /**
     * Jackson-friendly constructor using {@link JsonProperty} binding so
     * the record deserializes correctly even when compiled without
     * {@code -parameters}.
     */
    @JsonCreator
    public ParallelismDowngrade(
            @JsonProperty("phase") int phase,
            @JsonProperty("originalGroup")
                    List<String> originalGroup,
            @JsonProperty("adjustedSequence")
                    List<List<String>> adjustedSequence,
            @JsonProperty("reason") String reason,
            @JsonProperty("evaluatedAt")
                    Instant evaluatedAt) {
        this.phase = phase;
        this.originalGroup = originalGroup == null
                ? List.of()
                : List.copyOf(originalGroup);
        this.adjustedSequence = copyNested(adjustedSequence);
        this.reason = reason;
        this.evaluatedAt = evaluatedAt;
    }

    private static List<List<String>> copyNested(
            List<List<String>> src) {
        if (src == null) {
            return List.of();
        }
        return src.stream()
                .map(inner -> inner == null
                        ? List.<String>of()
                        : List.copyOf(inner))
                .toList();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ParallelismDowngrade p)) {
            return false;
        }
        return phase == p.phase
                && Objects.equals(reason, p.reason)
                && Objects.equals(evaluatedAt, p.evaluatedAt)
                && Objects.equals(originalGroup, p.originalGroup)
                && Objects.equals(adjustedSequence, p.adjustedSequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phase, originalGroup,
                adjustedSequence, reason, evaluatedAt);
    }
}
