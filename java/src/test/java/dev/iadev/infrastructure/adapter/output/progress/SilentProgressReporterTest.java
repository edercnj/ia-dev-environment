package dev.iadev.infrastructure.adapter.output.progress;

import dev.iadev.domain.port.output.ProgressReporter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link SilentProgressReporter}.
 *
 * <p>Covers @GK-1: SilentProgressReporter produces no output
 * and throws no exceptions.</p>
 */
class SilentProgressReporterTest {

    private final SilentProgressReporter reporter =
            new SilentProgressReporter();

    @Nested
    class ImplementsPort {

        @Test
        void silentReporter_implementsProgressReporter() {
            assertThat(reporter)
                    .isInstanceOf(ProgressReporter.class);
        }
    }

    @Nested
    class NoOutput {

        /**
         * AT-1 (@GK-1): All methods produce no stdout output.
         */
        @Test
        void allMethods_noStdoutOutput() {
            var captured = captureStdout(() -> {
                reporter.reportStart("task", 5);
                reporter.reportProgress("task", 1, "msg");
                reporter.reportComplete("task");
                reporter.reportError("task", "err");
            });

            assertThat(captured).isEmpty();
        }

        /**
         * AT-2 (@GK-1): All methods produce no stderr output.
         */
        @Test
        void allMethods_noStderrOutput() {
            var captured = captureStderr(() -> {
                reporter.reportStart("task", 5);
                reporter.reportProgress("task", 1, "msg");
                reporter.reportComplete("task");
                reporter.reportError("task", "err");
            });

            assertThat(captured).isEmpty();
        }
    }

    @Nested
    class NoExceptions {

        /**
         * AT-3 (@GK-1): reportStart does not throw.
         */
        @Test
        void reportStart_doesNotThrow() {
            assertThatNoException().isThrownBy(
                    () -> reporter.reportStart("task", 10));
        }

        /**
         * AT-4 (@GK-1): reportProgress does not throw.
         */
        @Test
        void reportProgress_doesNotThrow() {
            assertThatNoException().isThrownBy(
                    () -> reporter.reportProgress(
                            "task", 1, "step"));
        }

        /**
         * AT-5 (@GK-1): reportComplete does not throw.
         */
        @Test
        void reportComplete_doesNotThrow() {
            assertThatNoException().isThrownBy(
                    () -> reporter.reportComplete("task"));
        }

        /**
         * AT-6 (@GK-1): reportError does not throw.
         */
        @Test
        void reportError_doesNotThrow() {
            assertThatNoException().isThrownBy(
                    () -> reporter.reportError(
                            "task", "error msg"));
        }
    }

    private static String captureStdout(Runnable action) {
        var original = System.out;
        var baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return baos.toString();
    }

    private static String captureStderr(Runnable action) {
        var original = System.err;
        var baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setErr(original);
        }
        return baos.toString();
    }
}
