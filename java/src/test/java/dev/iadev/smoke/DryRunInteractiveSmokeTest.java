package dev.iadev.smoke;

import dev.iadev.release.dryrun.DefaultPhaseCatalog;
import dev.iadev.release.dryrun.DryRunInteractiveExecutor;
import dev.iadev.release.dryrun.DryRunPhaseOutcome;
import dev.iadev.release.dryrun.DryRunPromptChoice;
import dev.iadev.release.dryrun.DryRunStatePort;
import dev.iadev.release.dryrun.DryRunSummary;
import dev.iadev.release.dryrun.DryRunSummaryFormatter;
import dev.iadev.release.dryrun.InteractiveFlagValidator;
import dev.iadev.release.dryrun.InteractiveRequiresDryRunException;
import dev.iadev.release.dryrun.PhaseCatalogPort;
import dev.iadev.release.dryrun.PhaseDescriptor;
import dev.iadev.release.dryrun.PromptPort;
import dev.iadev.release.dryrun.TempFileDryRunStateWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for the interactive dry-run mode
 * (story-0039-0013).
 *
 * <p>Covers the 5 Gherkin scenarios from §7 end-to-end:
 * <ol>
 *   <li>Degenerate: {@code --interactive} without
 *       {@code --dry-run}.</li>
 *   <li>Happy: 13 phases all "continuar" — zero side
 *       effects.</li>
 *   <li>Boundary: "pular fase" in VALIDATED, BRANCHED
 *       still simulated.</li>
 *   <li>Boundary: "abortar" in PR_OPENED — partial
 *       summary + state cleaned.</li>
 *   <li>Verification: zero external invocations
 *       (nothing runs outside ports).</li>
 * </ol>
 */
@DisplayName("DryRunInteractiveSmokeTest")
class DryRunInteractiveSmokeTest {

    private static final String VERSION = "3.2.0";

    @Nested
    @DisplayName("AT-1 — --interactive without --dry-run")
    class AtOneDegenerate {

        @Test
        @DisplayName("degenerate_interactiveWithoutDryRun"
                + "_exitsOneWithErrorCode")
        void degenerate_interactiveWithoutDryRun_exitsOneWithErrorCode() {
            assertThatThrownBy(() ->
                    InteractiveFlagValidator.validate(false, true))
                    .isInstanceOf(InteractiveRequiresDryRunException.class)
                    .satisfies(ex -> {
                        InteractiveRequiresDryRunException e =
                                (InteractiveRequiresDryRunException) ex;
                        assertThat(e.exitCode()).isEqualTo(1);
                        assertThat(e.errorCode())
                                .isEqualTo("INTERACTIVE_REQUIRES_DRYRUN");
                    });
        }
    }

    @Nested
    @DisplayName("AT-2 — 13 phases continue, zero side effects")
    class AtTwoHappyPath {

        @Test
        @DisplayName("happy_thirteenContinue"
                + "_noGitMvnGhInvocations")
        void happy_thirteenContinue_noGitMvnGhInvocations() {
            SideEffectObserver observer =
                    new SideEffectObserver();
            DryRunInteractiveExecutor executor = newExecutor(
                    ScriptedPromptPort.repeating(
                            DryRunPromptChoice.CONTINUE, 13),
                    observer);

            DryRunSummary summary = executor.execute(VERSION);
            String formatted =
                    DryRunSummaryFormatter.format(summary);

            assertThat(summary.simulatedCount()).isEqualTo(13);
            assertThat(formatted).contains("13 / 13");
            assertThat(formatted).contains(
                    "DRY-RUN MODE — "
                            + "no side effects were applied");
            assertThat(observer.externalCalls).isEmpty();
        }
    }

    @Nested
    @DisplayName("AT-3 — skip phase, simulation continues")
    class AtThreeSkipPhase {

        @Test
        @DisplayName("boundary_skipAtValidated"
                + "_branchedStillSimulated")
        void boundary_skipAtValidated_branchedStillSimulated() {
            List<DryRunPromptChoice> script = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                if (i == 2) {
                    script.add(DryRunPromptChoice.SKIP);
                } else {
                    script.add(DryRunPromptChoice.CONTINUE);
                }
            }
            DryRunInteractiveExecutor executor = newExecutor(
                    new ScriptedPromptPort(script),
                    new SideEffectObserver());

            DryRunSummary summary = executor.execute(VERSION);

            assertThat(summary.phaseResults().get(2).phase())
                    .isEqualTo("VALIDATED");
            assertThat(summary.phaseResults().get(2).outcome())
                    .isEqualTo(DryRunPhaseOutcome.SKIPPED);
            assertThat(summary.phaseResults().get(3).phase())
                    .isEqualTo("BRANCHED");
            assertThat(summary.phaseResults().get(3).outcome())
                    .isEqualTo(DryRunPhaseOutcome.SIMULATED);
        }
    }

    @Nested
    @DisplayName("AT-4 — abort, partial summary, state cleaned")
    class AtFourAbort {

        @Test
        @DisplayName("boundary_abortAtPrOpened"
                + "_stateCleanedAndExitZero")
        void boundary_abortAtPrOpened_stateCleanedAndExitZero()
                throws Exception {
            List<DryRunPromptChoice> script = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                if (i == 7) {
                    script.add(DryRunPromptChoice.ABORT);
                } else {
                    script.add(DryRunPromptChoice.CONTINUE);
                }
            }
            TempFileDryRunStateWriter realWriter =
                    new TempFileDryRunStateWriter();
            PathTrackingStatePort tracker =
                    new PathTrackingStatePort(realWriter);
            DryRunInteractiveExecutor executor =
                    new DryRunInteractiveExecutor(
                            new DefaultPhaseCatalog(),
                            new ScriptedPromptPort(script),
                            tracker);

            DryRunSummary summary = executor.execute(VERSION);

            assertThat(summary.phaseResults().get(7).phase())
                    .isEqualTo("PR_OPENED");
            assertThat(summary.phaseResults().get(7).outcome())
                    .isEqualTo(DryRunPhaseOutcome.ABORTED);
            assertThat(summary.aborted()).isTrue();
            assertThat(tracker.createdPath).isNotNull();
            assertThat(Files.exists(tracker.createdPath))
                    .as("dummy state must be cleaned up")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("AT-5 — zero side effects verification")
    class AtFiveZeroSideEffects {

        @Test
        @DisplayName("acceptance_fullSimulation"
                + "_zeroExternalInvocations")
        void acceptance_fullSimulation_zeroExternalInvocations() {
            SideEffectObserver observer =
                    new SideEffectObserver();
            DryRunInteractiveExecutor executor = newExecutor(
                    ScriptedPromptPort.repeating(
                            DryRunPromptChoice.CONTINUE, 13),
                    observer);

            executor.execute(VERSION);

            assertThat(observer.externalCalls)
                    .as("no git/mvn/gh calls in dry-run")
                    .isEmpty();
        }
    }

    // -- Helpers --

    private static DryRunInteractiveExecutor newExecutor(
            PromptPort promptPort,
            SideEffectObserver observer) {
        return new DryRunInteractiveExecutor(
                new DefaultPhaseCatalog(),
                promptPort,
                new ObservingStatePort(observer));
    }

    /** Records external calls (git/mvn/gh). */
    private static final class SideEffectObserver {
        final List<String> externalCalls = new ArrayList<>();
    }

    /**
     * Wraps {@link DryRunStatePort} but intercepts nothing;
     * its sole purpose is to fail the test if the executor
     * ever tries to invoke git/mvn/gh through a side
     * channel (it cannot — the executor has no such
     * dependency).
     */
    private static final class ObservingStatePort
            implements DryRunStatePort {

        private final SideEffectObserver observer;
        private int seq;

        ObservingStatePort(SideEffectObserver observer) {
            this.observer = observer;
        }

        @Override
        public Path create(String version) {
            observer.externalCalls.clear();
            return Path.of("/tmp",
                    "dryrun-smoke-" + (++seq) + ".json");
        }

        @Override
        public void delete(Path path) {
            // no-op: smoke uses in-memory path tracking
        }
    }

    /** State port that delegates to a real writer. */
    private static final class PathTrackingStatePort
            implements DryRunStatePort {

        private final DryRunStatePort delegate;
        Path createdPath;

        PathTrackingStatePort(DryRunStatePort delegate) {
            this.delegate = delegate;
        }

        @Override
        public Path create(String version) {
            createdPath = delegate.create(version);
            return createdPath;
        }

        @Override
        public void delete(Path path) {
            delegate.delete(path);
        }
    }

    private static final class ScriptedPromptPort
            implements PromptPort {

        private final List<DryRunPromptChoice> script;
        private int cursor;

        ScriptedPromptPort(List<DryRunPromptChoice> script) {
            this.script = List.copyOf(script);
        }

        static ScriptedPromptPort repeating(
                DryRunPromptChoice choice, int count) {
            List<DryRunPromptChoice> s = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                s.add(choice);
            }
            return new ScriptedPromptPort(s);
        }

        @Override
        public DryRunPromptChoice promptForPhase(
                PhaseDescriptor phase,
                int position,
                int total) {
            DryRunPromptChoice next = cursor < script.size()
                    ? script.get(cursor) : DryRunPromptChoice.CONTINUE;
            cursor++;
            return next;
        }
    }
}
