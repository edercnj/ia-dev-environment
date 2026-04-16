package dev.iadev.release.handoff;

import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the {@code x-pr-fix} handoff loop from the
 * {@code x-release} APPROVAL_GATE halt point: invokes the
 * {@code x-pr-fix} skill, re-checks PR state via {@code gh}
 * and derives the next prompt option set per story-0039-0011
 * §5.3 decision table.
 *
 * <p>Flow (per story §3.1):
 * <ol>
 *   <li>Invoke {@code x-pr-fix} via
 *       {@link SkillInvokerPort}.</li>
 *   <li>If the skill throws, classify as
 *       {@link HandoffError#HANDOFF_SKILL_FAILED} and return a
 *       retry/continue/abort option set (warn-only).</li>
 *   <li>Otherwise, re-read the PR via
 *       {@link GhCliPort#viewPr}.</li>
 *   <li>If {@code gh pr view} throws
 *       {@link PrNotFoundException}, return
 *       {@link HandoffError#HANDOFF_PR_NOT_FOUND} with
 *       exitCode 1.</li>
 *   <li>Resolve options from {@link PrState} via
 *       {@link #resolveOptions}.</li>
 * </ol>
 *
 * <p>Dependency direction: this class lives in the
 * {@code application} layer and depends only on its sibling
 * ports ({@link SkillInvokerPort}, {@link GhCliPort}). Zero
 * framework imports (Rule 04).
 *
 * <p>Introduced by story-0039-0011.
 */
public final class HandoffOrchestrator {

    /** Skill name invoked for the handoff (per EPIC-0036). */
    static final String FIX_SKILL = "x-pr-fix";

    private final SkillInvokerPort skillInvoker;
    private final GhCliPort ghCli;

    /**
     * @param skillInvoker port that invokes the {@code Skill}
     *                     tool
     * @param ghCli        port that runs {@code gh pr view}
     */
    public HandoffOrchestrator(SkillInvokerPort skillInvoker,
                               GhCliPort ghCli) {
        this.skillInvoker = Objects.requireNonNull(
                skillInvoker, "skillInvoker");
        this.ghCli = Objects.requireNonNull(ghCli, "ghCli");
    }

    /**
     * Runs the full handoff loop for the given PR.
     *
     * @param prNumber positive PR number
     * @return a {@link HandoffResult} carrying the new option
     *         set and any error classification
     * @throws IllegalArgumentException when {@code prNumber}
     *         is not positive (Rule 06 — input hardening)
     */
    public HandoffResult handoff(int prNumber) {
        validatePrNumber(prNumber);
        try {
            skillInvoker.invoke(FIX_SKILL,
                    Integer.toString(prNumber));
        } catch (SkillInvocationException e) {
            return HandoffResult.skillFailed(
                    skillFailedOptions());
        }
        try {
            PrState fresh = ghCli.viewPr(prNumber);
            return HandoffResult.success(
                    resolveOptions(fresh));
        } catch (PrNotFoundException e) {
            return HandoffResult.prNotFound();
        }
    }

    /**
     * Resolves the option set for a given PR state per the
     * decision table in story-0039-0011 §5.3.
     *
     * @param state refreshed PR state snapshot
     * @return ordered option labels (main option first)
     */
    public static List<String> resolveOptions(PrState state) {
        Objects.requireNonNull(state, "state");
        return switch (state.state()) {
            case MERGED -> mergedOptions();
            case CLOSED -> closedOptions();
            case OPEN -> state.reviewDecision()
                    == PrReviewDecision.APPROVED
                    ? approvedOpenOptions()
                    : openNotApprovedOptions();
        };
    }

    private static List<String> mergedOptions() {
        return List.of(
                "Continuar release",
                "Sair e retomar depois",
                "Abortar");
    }

    private static List<String> closedOptions() {
        return List.of(
                "Reabrir PR",
                "Iniciar novo release",
                "Abortar");
    }

    private static List<String> approvedOpenOptions() {
        return List.of(
                "Mergear no GitHub e voltar",
                "Sair e retomar depois",
                "Abortar");
    }

    private static List<String> openNotApprovedOptions() {
        return List.of(
                "Rodar fix-comments novamente",
                "Sair e retomar depois",
                "Abortar");
    }

    private static List<String> skillFailedOptions() {
        return List.of(
                "Tentar novamente",
                "Continuar mesmo assim",
                "Abortar");
    }

    private static void validatePrNumber(int prNumber) {
        if (prNumber <= 0) {
            throw new IllegalArgumentException(
                    "prNumber must be positive, got: "
                            + prNumber);
        }
    }
}
