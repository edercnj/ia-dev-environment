package dev.iadev.infrastructure.adapter.output.progress;

import dev.iadev.domain.port.output.ProgressReporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConsoleProgressReporter}.
 *
 * <p>Covers @GK-2 through @GK-5: output formatting, stderr
 * routing, interleaved tasks, and special characters.</p>
 */
class ConsoleProgressReporterTest {

    private final ConsoleProgressReporter reporter =
            new ConsoleProgressReporter();

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream stdoutCapture;
    private ByteArrayOutputStream stderrCapture;

    @BeforeEach
    void setUp() {
        stdoutCapture = new ByteArrayOutputStream();
        stderrCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdoutCapture));
        System.setErr(new PrintStream(stderrCapture));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Nested
    class ImplementsPort {

        @Test
        void consoleReporter_implementsProgressReporter() {
            assertThat(reporter)
                    .isInstanceOf(ProgressReporter.class);
        }
    }

    @Nested
    class ReportStartFormat {

        /**
         * AT-7 (@GK-2): reportStart formats correctly.
         */
        @Test
        void reportStart_formatsWithTaskNameAndSteps() {
            reporter.reportStart("generate", 10);

            assertThat(stdout())
                    .isEqualTo("[START] generate (10 steps)\n");
        }

        @Test
        void reportStart_singleStep_formatsCorrectly() {
            reporter.reportStart("compile", 1);

            assertThat(stdout())
                    .isEqualTo("[START] compile (1 steps)\n");
        }
    }

    @Nested
    class ReportProgressFormat {

        /**
         * AT-8 (@GK-2): reportProgress formats correctly.
         */
        @Test
        void reportProgress_formatsWithStepAndMessage() {
            reporter.reportProgress(
                    "generate", 5, "Processing rules");

            assertThat(stdout()).isEqualTo(
                    "[5] generate: Processing rules\n");
        }

        @Test
        void reportProgress_firstStep_formatsCorrectly() {
            reporter.reportProgress(
                    "build", 1, "Initializing");

            assertThat(stdout()).isEqualTo(
                    "[1] build: Initializing\n");
        }
    }

    @Nested
    class ReportCompleteFormat {

        /**
         * AT-9 (@GK-2): reportComplete formats correctly.
         */
        @Test
        void reportComplete_formatsWithTaskName() {
            reporter.reportComplete("generate");

            assertThat(stdout())
                    .isEqualTo("[DONE] generate\n");
        }
    }

    @Nested
    class ReportErrorFormat {

        /**
         * AT-10 (@GK-3): reportError writes to stderr.
         */
        @Test
        void reportError_writesToStderr() {
            reporter.reportError(
                    "generate", "Template not found");

            assertThat(stderr()).isEqualTo(
                    "[ERROR] generate: Template not found\n");
        }

        /**
         * AT-11 (@GK-3): reportError does not write to stdout.
         */
        @Test
        void reportError_doesNotWriteToStdout() {
            reporter.reportError(
                    "generate", "Template not found");

            assertThat(stdout()).isEmpty();
        }
    }

    @Nested
    class InterleavedTasks {

        /**
         * AT-12 (@GK-4): Interleaved tasks preserve task names.
         */
        @Test
        void interleavedTasks_preserveTaskNames() {
            reporter.reportStart("task-a", 5);
            reporter.reportStart("task-b", 3);
            reporter.reportProgress(
                    "task-a", 1, "step A1");
            reporter.reportProgress(
                    "task-b", 1, "step B1");

            var output = stdout();
            assertThat(output)
                    .contains("[START] task-a (5 steps)")
                    .contains("[START] task-b (3 steps)")
                    .contains("[1] task-a: step A1")
                    .contains("[1] task-b: step B1");
        }

        /**
         * AT-13 (@GK-4): Chronological order is preserved.
         */
        @Test
        void interleavedTasks_chronologicalOrder() {
            reporter.reportStart("task-a", 5);
            reporter.reportStart("task-b", 3);
            reporter.reportProgress(
                    "task-a", 1, "step A1");
            reporter.reportProgress(
                    "task-b", 1, "step B1");

            var output = stdout();
            int taskAStart = output.indexOf(
                    "[START] task-a");
            int taskBStart = output.indexOf(
                    "[START] task-b");
            int taskAProgress = output.indexOf(
                    "[1] task-a:");
            int taskBProgress = output.indexOf(
                    "[1] task-b:");

            assertThat(taskAStart)
                    .isLessThan(taskBStart);
            assertThat(taskBStart)
                    .isLessThan(taskAProgress);
            assertThat(taskAProgress)
                    .isLessThan(taskBProgress);
        }
    }

    @Nested
    class SpecialCharacters {

        /**
         * AT-14 (@GK-5): Unicode/accented characters preserved.
         */
        @Test
        void reportProgress_specialChars_preserved() {
            reporter.reportProgress(
                    "task", 1,
                    "Arquivo: caf\u00e9_r\u00e9sum\u00e9.md (100%)");

            assertThat(stdout()).contains(
                    "caf\u00e9_r\u00e9sum\u00e9.md (100%)");
        }

        @Test
        void reportError_specialChars_preserved() {
            reporter.reportError(
                    "task",
                    "Falha no arquivo: caf\u00e9.yml");

            assertThat(stderr()).contains(
                    "caf\u00e9.yml");
        }
    }

    @Nested
    class FullWorkflow {

        /**
         * AT-15 (@GK-2): Complete happy path workflow.
         */
        @Test
        void completeWorkflow_allOutputCorrect() {
            reporter.reportStart("generate", 3);
            reporter.reportProgress(
                    "generate", 1, "Step one");
            reporter.reportProgress(
                    "generate", 2, "Step two");
            reporter.reportProgress(
                    "generate", 3, "Step three");
            reporter.reportComplete("generate");

            var output = stdout();
            assertThat(output)
                    .contains("[START] generate (3 steps)")
                    .contains("[1] generate: Step one")
                    .contains("[2] generate: Step two")
                    .contains("[3] generate: Step three")
                    .contains("[DONE] generate");
            assertThat(stderr()).isEmpty();
        }
    }

    private String stdout() {
        System.out.flush();
        return stdoutCapture.toString();
    }

    private String stderr() {
        System.err.flush();
        return stderrCapture.toString();
    }
}
