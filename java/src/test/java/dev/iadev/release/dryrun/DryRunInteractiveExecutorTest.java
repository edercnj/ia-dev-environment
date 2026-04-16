package dev.iadev.release.dryrun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DryRunInteractiveExecutor}.
 *
 * <p>TPP ordering: happy (13 phases continue) ->
 * boundary (skip phase) -> boundary (abort phase) ->
 * verification (zero side effects).
 */
@DisplayName("DryRunInteractiveExecutorTest")
class DryRunInteractiveExecutorTest {

    private static final String VERSION = "3.2.0";

    @Nested
    @DisplayName("happy — 13 phases continue")
    class HappyThirteenPhasesContinue {

        @Test
        @DisplayName("execute_allContinue"
                + "_recordsThirteenSimulatedPhases")
        void execute_allContinue_recordsThirteenSimulatedPhases() {
            List<PhaseDescriptor> catalog = buildStandardCatalog();
            FakePhaseCatalogPort catalogPort =
                    new FakePhaseCatalogPort(catalog);
            ScriptedPromptPort promptPort =
                    ScriptedPromptPort.repeating(
                            DryRunPromptChoice.CONTINUE, 13);
            RecordingDryRunStatePort statePort =
                    new RecordingDryRunStatePort();
            DryRunInteractiveExecutor executor =
                    new DryRunInteractiveExecutor(
                            catalogPort, promptPort, statePort);

            DryRunSummary summary = executor.execute(VERSION);

            assertThat(summary.totalPhases()).isEqualTo(13);
            assertThat(summary.simulatedCount()).isEqualTo(13);
            assertThat(summary.skippedCount()).isZero();
            assertThat(summary.abortedCount()).isZero();
            assertThat(summary.notReachedCount()).isZero();
        }

        @Test
        @DisplayName("execute_allContinue"
                + "_aggregatesPredictedCommandCount")
        void execute_allContinue_aggregatesPredictedCommandCount() {
            List<PhaseDescriptor> catalog = buildStandardCatalog();
            int expected = catalog.stream()
                    .mapToInt(p -> p.commands().size())
                    .sum();
            DryRunInteractiveExecutor executor = newExecutor(
                    catalog,
                    ScriptedPromptPort.repeating(
                            DryRunPromptChoice.CONTINUE, 13),
                    new RecordingDryRunStatePort());

            DryRunSummary summary = executor.execute(VERSION);

            assertThat(summary.predictedCommands())
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("execute_allContinue"
                + "_writesAndDeletesDummyState")
        void execute_allContinue_writesAndDeletesDummyState() {
            RecordingDryRunStatePort statePort =
                    new RecordingDryRunStatePort();
            DryRunInteractiveExecutor executor = newExecutor(
                    buildStandardCatalog(),
                    ScriptedPromptPort.repeating(
                            DryRunPromptChoice.CONTINUE, 13),
                    statePort);

            executor.execute(VERSION);

            assertThat(statePort.creates).hasSize(1);
            assertThat(statePort.deletes).hasSize(1);
            assertThat(statePort.deletes.get(0))
                    .isEqualTo(statePort.creates.get(0));
        }
    }

    @Nested
    @DisplayName("boundary — operator skips a phase")
    class BoundaryOperatorSkipsPhase {

        @Test
        @DisplayName("execute_skipAtPhaseThree"
                + "_marksSkippedAndContinues")
        void execute_skipAtPhaseThree_marksSkippedAndContinues() {
            List<PhaseDescriptor> catalog = buildStandardCatalog();
            List<DryRunPromptChoice> script = new ArrayList<>();
            IntStream.range(0, 13).forEach(i -> {
                if (i == 2) {
                    script.add(DryRunPromptChoice.SKIP);
                } else {
                    script.add(DryRunPromptChoice.CONTINUE);
                }
            });
            DryRunInteractiveExecutor executor = newExecutor(
                    catalog,
                    new ScriptedPromptPort(script),
                    new RecordingDryRunStatePort());

            DryRunSummary summary = executor.execute(VERSION);

            assertThat(summary.phaseResults().get(2).outcome())
                    .isEqualTo(DryRunPhaseOutcome.SKIPPED);
            assertThat(summary.phaseResults().get(3).outcome())
                    .isEqualTo(DryRunPhaseOutcome.SIMULATED);
            assertThat(summary.simulatedCount()).isEqualTo(12);
            assertThat(summary.skippedCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("boundary — operator aborts simulation")
    class BoundaryOperatorAborts {

        @Test
        @DisplayName("execute_abortAtPhaseSix"
                + "_stopsAndMarksRemainingNotReached")
        void execute_abortAtPhaseSix_stopsAndMarksRemainingNotReached() {
            List<PhaseDescriptor> catalog = buildStandardCatalog();
            List<DryRunPromptChoice> script = new ArrayList<>();
            IntStream.range(0, 13).forEach(i -> {
                if (i == 5) {
                    script.add(DryRunPromptChoice.ABORT);
                } else {
                    script.add(DryRunPromptChoice.CONTINUE);
                }
            });
            ScriptedPromptPort promptPort =
                    new ScriptedPromptPort(script);
            DryRunInteractiveExecutor executor = newExecutor(
                    catalog, promptPort,
                    new RecordingDryRunStatePort());

            DryRunSummary summary = executor.execute(VERSION);

            assertThat(summary.simulatedCount()).isEqualTo(5);
            assertThat(summary.abortedCount()).isEqualTo(1);
            assertThat(summary.notReachedCount()).isEqualTo(7);
            assertThat(summary.phaseResults().get(5).outcome())
                    .isEqualTo(DryRunPhaseOutcome.ABORTED);
            assertThat(summary.phaseResults().get(6).outcome())
                    .isEqualTo(DryRunPhaseOutcome.NOT_REACHED);
            assertThat(summary.aborted()).isTrue();
            assertThat(promptPort.invocationCount())
                    .isEqualTo(6);
        }

        @Test
        @DisplayName("execute_abort"
                + "_cleansUpStateFileInFinally")
        void execute_abort_cleansUpStateFileInFinally() {
            RecordingDryRunStatePort statePort =
                    new RecordingDryRunStatePort();
            DryRunInteractiveExecutor executor = newExecutor(
                    buildStandardCatalog(),
                    ScriptedPromptPort.of(
                            DryRunPromptChoice.ABORT),
                    statePort);

            executor.execute(VERSION);

            assertThat(statePort.deletes).hasSize(1);
        }
    }

    @Nested
    @DisplayName("verification — zero side effects")
    class VerificationZeroSideEffects {

        @Test
        @DisplayName("execute_standardRun"
                + "_doesNotInvokeAnyExternalCommand")
        void execute_standardRun_doesNotInvokeAnyExternalCommand() {
            RecordingDryRunStatePort statePort =
                    new RecordingDryRunStatePort();
            DryRunInteractiveExecutor executor = newExecutor(
                    buildStandardCatalog(),
                    ScriptedPromptPort.repeating(
                            DryRunPromptChoice.CONTINUE, 13),
                    statePort);

            executor.execute(VERSION);

            assertThat(statePort.executedCommands).isEmpty();
        }

        @Test
        @DisplayName("execute_standardRun"
                + "_exposesVersionOnSummary")
        void execute_standardRun_exposesVersionOnSummary() {
            DryRunInteractiveExecutor executor = newExecutor(
                    buildStandardCatalog(),
                    ScriptedPromptPort.repeating(
                            DryRunPromptChoice.CONTINUE, 13),
                    new RecordingDryRunStatePort());

            DryRunSummary summary = executor.execute(VERSION);

            assertThat(summary.version()).isEqualTo(VERSION);
        }
    }

    // -- Helpers and test doubles --

    private static DryRunInteractiveExecutor newExecutor(
            List<PhaseDescriptor> catalog,
            PromptPort promptPort,
            DryRunStatePort statePort) {
        return new DryRunInteractiveExecutor(
                new FakePhaseCatalogPort(catalog),
                promptPort, statePort);
    }

    private static List<PhaseDescriptor> buildStandardCatalog() {
        return List.of(
                new PhaseDescriptor("INITIALIZED",
                        List.of("detect state")),
                new PhaseDescriptor("DETERMINED",
                        List.of("compute bump")),
                new PhaseDescriptor("VALIDATED",
                        List.of("mvn clean verify",
                                "parse coverage")),
                new PhaseDescriptor("BRANCHED",
                        List.of("git checkout -b release/X.Y.Z")),
                new PhaseDescriptor("UPDATED",
                        List.of("bump pom.xml")),
                new PhaseDescriptor("CHANGELOG_WRITTEN",
                        List.of("x-release-changelog")),
                new PhaseDescriptor("COMMITTED",
                        List.of("git commit")),
                new PhaseDescriptor("PR_OPENED",
                        List.of("git push", "gh pr create")),
                new PhaseDescriptor("APPROVAL_PENDING",
                        List.of("halt for merge")),
                new PhaseDescriptor("TAGGED",
                        List.of("git tag", "git push")),
                new PhaseDescriptor("BACK_MERGED",
                        List.of("backmerge branch")),
                new PhaseDescriptor("PUBLISHED",
                        List.of("gh release create")),
                new PhaseDescriptor("CLEANED",
                        List.of("delete branch", "delete state"))
        );
    }

    private static final class FakePhaseCatalogPort
            implements PhaseCatalogPort {

        private final List<PhaseDescriptor> catalog;

        FakePhaseCatalogPort(List<PhaseDescriptor> catalog) {
            this.catalog = List.copyOf(catalog);
        }

        @Override
        public List<PhaseDescriptor> phases() {
            return catalog;
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

        static ScriptedPromptPort of(
                DryRunPromptChoice... choices) {
            return new ScriptedPromptPort(List.of(choices));
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

        int invocationCount() {
            return cursor;
        }
    }

    private static final class RecordingDryRunStatePort
            implements DryRunStatePort {

        final List<Path> creates = new ArrayList<>();
        final List<Path> deletes = new ArrayList<>();
        final List<String> executedCommands = new ArrayList<>();
        private int counter;

        @Override
        public Path create(String version) {
            Path p = Paths.get("/tmp",
                    "release-state-dryrun-"
                            + version + "-" + (++counter)
                            + ".json");
            creates.add(p);
            return p;
        }

        @Override
        public void delete(Path path) {
            if (path != null) {
                deletes.add(path);
            }
        }
    }
}
