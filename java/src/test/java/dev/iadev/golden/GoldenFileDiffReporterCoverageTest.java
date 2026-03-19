package dev.iadev.golden;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage tests for {@link GoldenFileDiffReporter} —
 * targeting uncovered branches in splitLines.
 */
@DisplayName("GoldenFileDiffReporter — coverage")
class GoldenFileDiffReporterCoverageTest {

    @Test
    @DisplayName("empty expected vs non-empty actual"
            + " produces diff")
    void emptyExpected_nonEmptyActual_producesDiff() {
        String diff = GoldenFileDiffReporter.generateDiff(
                "test.md", "", "some content\n");

        assertThat(diff)
                .isNotEmpty()
                .contains("test.md");
    }

    @Test
    @DisplayName("content without trailing newline"
            + " produces diff")
    void noTrailingNewline_producesDiff() {
        String diff = GoldenFileDiffReporter.generateDiff(
                "test.md",
                "line-without-newline",
                "different-content");

        assertThat(diff)
                .isNotEmpty()
                .contains("test.md");
    }
}
