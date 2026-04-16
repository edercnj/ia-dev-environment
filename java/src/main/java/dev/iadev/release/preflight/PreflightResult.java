package dev.iadev.release.preflight;

import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of the pre-flight phase (story-0039-0009 §5.3).
 *
 * <p>Semantics per exit code:
 * <ul>
 *   <li>{@code exitCode == 0} and {@code decision == ABORT}: clean abort, no mutation</li>
 *   <li>{@code exitCode == 0} and {@code decision == PROCEED}: continue to VALIDATE-DEEP</li>
 *   <li>{@code exitCode == 1} and {@code errorCode == PREFLIGHT_INTEGRITY_FAIL}: integrity
 *       failed, dashboard rendered, no prompt offered</li>
 *   <li>{@code exitCode == 1} and {@code errorCode == PREFLIGHT_EDIT_VERSION}: operator
 *       chose "Edit version"; abort with instruction to rerun with {@code --version}</li>
 * </ul></p>
 *
 * @param exitCode  0 for clean exit, 1 for error/edit
 * @param errorCode machine-readable code (present only when exitCode == 1)
 * @param decision  operator decision (empty during evaluation, present after resolve)
 * @param dashboard rendered dashboard text (always present)
 */
public record PreflightResult(
        int exitCode,
        Optional<String> errorCode,
        Optional<PreflightDecision> decision,
        String dashboard) {

    public static final String ERROR_INTEGRITY_FAIL = "PREFLIGHT_INTEGRITY_FAIL";
    public static final String ERROR_EDIT_VERSION = "PREFLIGHT_EDIT_VERSION";

    public PreflightResult {
        Objects.requireNonNull(errorCode, "errorCode");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(dashboard, "dashboard");

        if (exitCode != 0 && exitCode != 1) {
            throw new IllegalArgumentException(
                    "exitCode must be 0 or 1");
        }
        if (exitCode == 0 && errorCode.isPresent()) {
            throw new IllegalArgumentException(
                    "errorCode must be empty when exitCode == 0");
        }
        if (exitCode == 1 && errorCode.isEmpty()) {
            throw new IllegalArgumentException(
                    "errorCode must be present when exitCode == 1");
        }
    }

    /** Integrity FAIL — dashboard rendered, no prompt, exit 1. */
    public static PreflightResult integrityFail(String dashboard) {
        return new PreflightResult(1, Optional.of(ERROR_INTEGRITY_FAIL),
                Optional.empty(), dashboard);
    }

    /** Operator chose "Edit version" — exit 1 with instruction. */
    public static PreflightResult editVersion(String dashboard) {
        return new PreflightResult(1, Optional.of(ERROR_EDIT_VERSION),
                Optional.of(PreflightDecision.EDIT_VERSION), dashboard);
    }

    /** Operator chose "Abort" — clean exit 0. */
    public static PreflightResult abort(String dashboard) {
        return new PreflightResult(0, Optional.empty(),
                Optional.of(PreflightDecision.ABORT), dashboard);
    }

    /** Operator chose "Proceed" — continue to VALIDATE-DEEP. */
    public static PreflightResult proceed(String dashboard) {
        return new PreflightResult(0, Optional.empty(),
                Optional.of(PreflightDecision.PROCEED), dashboard);
    }

    /** Evaluation complete, awaiting operator decision (no decision yet). */
    public static PreflightResult awaitingDecision(String dashboard) {
        return new PreflightResult(0, Optional.empty(),
                Optional.empty(), dashboard);
    }
}
